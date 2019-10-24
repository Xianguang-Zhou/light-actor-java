/*
 * Copyright (c) 2019, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor.eaasync.core;

import static com.ea.async.Async.await;
import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public abstract class Actor implements Comparable<Actor> {

	private static final Object AFTER_MESSAGE = new Object();

	private Scheduler scheduler;
	private AtomicReference<ActorState> stateRef = new AtomicReference<>(ActorState.CREATED);
	AtomicReference<String> nameRef = new AtomicReference<>();
	private CompletableFuture<Object> next;
	private Deque<Object> savedMessages = new LinkedList<>();
	private Set<Actor> links = new ConcurrentSkipListSet<>();
	private volatile boolean isTrapStop = false;
	private Set<Actor> monitors = new ConcurrentSkipListSet<>();
	private Set<Actor> monitoredActors = new ConcurrentSkipListSet<>();

	protected Actor(ActorGroup group) {
		this.scheduler = group.nextScheduler();
	}

	protected abstract CompletableFuture<Object> run();

	@Override
	public int compareTo(Actor other) {
		return this.hashCode() - other.hashCode();
	}

	public final void start() {
		stateRef.getAndUpdate(state -> {
			if (state == ActorState.CREATED) {
				this.scheduler.start(this);
				return ActorState.STARTED;
			} else {
				return state;
			}
		});
	}

	public final void send(Object message) {
		if (message == null) {
			throw new NullPointerException();
		}
		if (stateRef.get() != ActorState.STARTED) {
			return;
		}
		this.scheduler.send(this, message);
	}

	public final CompletableFuture<Void> stop() {
		return stop(null);
	}

	public final CompletableFuture<Void> stop(Object reason) {
		if (stateRef.compareAndSet(ActorState.STARTED, ActorState.STOPPED)) {
			this.scheduler.stop(this, reason);
		}
		CompletableFuture<Void> future = new CompletableFuture<>();
		if (current() != this) {
			future.complete(null);
		}
		return future;
	}

	public final ActorState getState() {
		return stateRef.get();
	}

	public final boolean isStopped() {
		return stateRef.get() == ActorState.STOPPED;
	}

	public final boolean isStarted() {
		return stateRef.get() == ActorState.STARTED;
	}

	public final void link(Actor actor) {
		if (this == actor || this.isStopped() || actor.isStopped()) {
			return;
		}
		actor.links.add(this);
		this.links.add(actor);
	}

	public final void unlink(Actor actor) {
		if (this.isStopped() || actor.isStopped()) {
			return;
		}
		actor.links.remove(this);
		this.links.remove(actor);
	}

	public final void setTrapStop(boolean trapStop) {
		if (this.isStopped()) {
			return;
		}
		this.isTrapStop = trapStop;
	}

	public final boolean isTrapStop() {
		return this.isTrapStop;
	}

	public final void monitor(Actor actor) {
		if (this == actor || this.isStopped() || actor.isStopped()) {
			return;
		}
		actor.monitors.add(this);
		this.monitoredActors.add(actor);
	}

	public final void demonitor(Actor actor) {
		if (this.isStopped() || actor.isStopped()) {
			return;
		}
		actor.monitors.remove(this);
		this.monitoredActors.remove(actor);
	}

	public final ActorGroup getGroup() {
		return scheduler.group;
	}

	public final String getName() {
		if (this.isStopped()) {
			return null;
		}
		return nameRef.get();
	}

	public static Actor current() {
		Scheduler scheduler = Scheduler.ofThread();
		if (null != scheduler) {
			return scheduler.currentActor;
		}
		return null;
	}

	public static CompletableFuture<Object> receive() {
		Actor self = current();
		if (null == self) {
			return completedFuture(null);
		}
		return self.internalReceive();
	}

	public static CompletableFuture<Void> receive(Receive receive) {
		Actor self = current();
		if (null == self) {
			return completedFuture(null);
		}

		ScheduledFuture<?> afterFuture = null;
		if (receive.afterHook != null) {
			afterFuture = self.scheduler.after(self, receive);
		}

		Deque<Object> unmatchedMessages = new LinkedList<>();
		do {
			Object message = await(self.internalReceive());
			if (AFTER_MESSAGE == message) {
				self.restoreUnmatchedMessages(unmatchedMessages);
				return receive.afterHook.run();
			}
			for (ReceiveRule rule : receive.receiveRules) {
				boolean isMatched = false;
				try {
					isMatched = rule.matcher.test(message);
				} catch (Exception ex) {
					if (afterFuture != null) {
						afterFuture.cancel(false);
					}
					unmatchedMessages.offer(message);
					self.restoreUnmatchedMessages(unmatchedMessages);
					CompletableFuture<Void> future = new CompletableFuture<>();
					future.completeExceptionally(ex);
					return future;
				}
				if (isMatched) {
					if (null == afterFuture || afterFuture.cancel(false)) {
						self.restoreUnmatchedMessages(unmatchedMessages);
						return rule.receiver.accept(message);
					} else {
						unmatchedMessages.offer(message);
						self.restoreUnmatchedMessages(unmatchedMessages);
						return completedFuture(null);
					}
				}
			}
			unmatchedMessages.offer(message);
		} while (true);
	}

	private final void restoreUnmatchedMessages(Deque<Object> unmatchedMessages) {
		Object message;
		while ((message = unmatchedMessages.pollLast()) != null) {
			this.savedMessages.offerFirst(message);
		}
	}

	private final CompletableFuture<Object> internalReceive() {
		if (isStopped()) {
			scheduler.currentActor = null;
			CompletableFuture<Object> future = new CompletableFuture<>();
			return future;
		}
		if (this.savedMessages.isEmpty()) {
			scheduler.currentActor = null;
			this.next = new CompletableFuture<>();
			return this.next;
		} else {
			return completedFuture(this.savedMessages.poll());
		}
	}

	void onReceive(Object message) {
		if (isStopped()) {
			return;
		}
		savedMessages.offer(message);
		if (null != next && !next.isDone()) {
			scheduler.currentActor = this;
			this.next.complete(savedMessages.poll());
		}
	}

	void onAfter() {
		onReceive(AFTER_MESSAGE);
	}

	CompletableFuture<Void> onStart() {
		scheduler.currentActor = this;
		stateRef.compareAndSet(ActorState.CREATED, ActorState.STARTED);
		Object reason;
		try {
			reason = await(run());
		} catch (Exception ex) {
			reason = ex;
		}
		stateRef.compareAndSet(ActorState.STARTED, ActorState.STOPPED);
		return onStop(reason);
	}

	CompletableFuture<Void> onStop(Object reason) {
		String name = nameRef.get();
		if (name != null) {
			nameRef.set(null);
			scheduler.group.registry.computeIfPresent(name, (String nameKey, Actor actorValue) -> {
				if (actorValue == this) {
					return null;
				} else {
					return actorValue;
				}
			});
		}

		scheduler.currentActor = null;

		DownMessage downMessage = null;
		for (Actor actor : this.monitors) {
			actor.monitoredActors.remove(this);
			if (downMessage == null) {
				downMessage = new DownMessage(this, reason);
			}
			actor.send(downMessage);
		}

		ExitMessage exitMessage = null;
		for (Actor actor : this.links) {
			actor.links.remove(this);
			if (actor.isTrapStop) {
				if (exitMessage == null) {
					exitMessage = new ExitMessage(this, reason);
				}
				actor.send(exitMessage);
			} else {
				await(actor.stop(reason));
			}
		}

		for (Actor actor : this.monitoredActors) {
			actor.monitors.remove(this);
		}
		this.savedMessages = null;
		this.links.clear();
		this.monitors.clear();
		this.monitoredActors.clear();

		return completedFuture(null);
	}
}
