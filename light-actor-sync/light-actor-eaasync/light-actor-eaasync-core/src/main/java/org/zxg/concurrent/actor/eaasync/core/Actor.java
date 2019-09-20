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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public abstract class Actor {

	private static final Object AFTER_MESSAGE = new Object();

	private Scheduler scheduler;
	private AtomicReference<ActorState> stateRef = new AtomicReference<>(ActorState.CREATED);
	private CompletableFuture<Object> next;
	private Deque<Object> messages = new LinkedList<>();

	protected Actor(ActorGroup group) {
		this.scheduler = group.nextScheduler();
	}

	protected abstract CompletableFuture<Void> run();

	private CompletableFuture<Void> internalRun() {
		stateRef.compareAndSet(ActorState.CREATED, ActorState.STARTED);
		Void result = await(run());
		stateRef.compareAndSet(ActorState.STARTED, ActorState.STOPPED);
		scheduler.currentActor = null;
		return completedFuture(result);
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

	public final ActorState getState() {
		return stateRef.get();
	}

	public final boolean isStopped() {
		return stateRef.get() == ActorState.STOPPED;
	}

	public final boolean isStarted() {
		return stateRef.get() == ActorState.STARTED;
	}

	public final ActorGroup getGroup() {
		return scheduler.group;
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
			this.messages.offerFirst(message);
		}
	}

	private CompletableFuture<Object> internalReceive() {
		if (this.messages.isEmpty()) {
			scheduler.currentActor = null;
			this.next = new CompletableFuture<>();
			return this.next;
		} else {
			return completedFuture(this.messages.poll());
		}
	}

	void onReceive(Object message) {
		if (isStopped()) {
			return;
		}
		messages.offer(message);
		if (null != next && !next.isDone()) {
			scheduler.currentActor = this;
			this.next.complete(messages.poll());
		}
	}

	void onAfter() {
		onReceive(AFTER_MESSAGE);
	}

	void onStart() {
		scheduler.currentActor = this;
		internalRun();
	}
}
