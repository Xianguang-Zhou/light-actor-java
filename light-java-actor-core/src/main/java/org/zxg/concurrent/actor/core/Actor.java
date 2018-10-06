/*
 * Copyright (c) 2018, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor.core;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public abstract class Actor {

	Receive receive;
	private Scheduler scheduler;
	private Queue<Object> savedMessages;
	private ScheduledFuture<?> afterFuture;
	private AtomicReference<ActorState> stateRef = new AtomicReference<>(ActorState.CREATED);
	AtomicReference<String> nameRef = new AtomicReference<>();
	private Set<Actor> links;
	private volatile boolean isTrapStop = false;
	private Set<Actor> monitors;

	protected Actor(ActorGroup group) {
		this.savedMessages = new LinkedList<>();
		this.links = new ConcurrentSkipListSet<>();
		this.monitors = new ConcurrentSkipListSet<>();
		this.receive = createReceive();
		if (this.receive == null) {
			throw new NullPointerException();
		}
		this.scheduler = group.nextScheduler();
	}

	protected abstract Receive createReceive();

	protected void preStart() throws Exception {
	}

	protected void postStop(Object reason) {
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

	public final void stop() {
		stop(null);
	}

	public final void stop(Object reason) {
		Ref<Boolean> needStop = new Ref<>(false);
		stateRef.getAndUpdate(state -> {
			if (state == ActorState.STARTED) {
				needStop.value = true;
				return ActorState.STOPED;
			} else {
				return state;
			}
		});
		if (needStop.value) {
			this.scheduler.stop(this, reason);
		}
	}

	public final ActorState getState() {
		return stateRef.get();
	}

	public final boolean isStoped() {
		return stateRef.get() == ActorState.STOPED;
	}

	public final boolean isStarted() {
		return stateRef.get() == ActorState.STARTED;
	}

	public final void link(Actor actor) {
		if (this.isStoped() || actor.isStoped()) {
			return;
		}
		actor.links.add(this);
		this.links.add(actor);
	}

	public final void unlink(Actor actor) {
		if (this.isStoped() || actor.isStoped()) {
			return;
		}
		actor.links.remove(this);
		this.links.remove(actor);
	}

	public final void setTrapStop(boolean trapStop) {
		if (this.isStoped()) {
			return;
		}
		this.isTrapStop = trapStop;
	}

	public final boolean isTrapStop() {
		return this.isTrapStop;
	}

	public final void monitor(Actor actor) {
		if (this.isStoped() || actor.isStoped()) {
			return;
		}
		actor.monitors.add(this);
	}

	public final void demonitor(Actor actor) {
		if (this.isStoped() || actor.isStoped()) {
			return;
		}
		actor.monitors.remove(this);
	}

	public final ActorGroup getGroup() {
		return scheduler.group;
	}

	public final String getName() {
		if (this.isStoped()) {
			return null;
		}
		return nameRef.get();
	}

	final void onStop(Object reason) {
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

		try {
			postStop(reason);
		} catch (Exception ex) {
		}

		if (reason != null) {
			DownMessage downMessage = null;
			for (Actor actor : monitors) {
				if (downMessage == null) {
					downMessage = new DownMessage(this, reason);
				}
				actor.send(downMessage);
			}
		}

		ExitMessage exitMessage = null;
		for (Actor actor : links) {
			actor.links.remove(this);
			if (actor.isTrapStop) {
				if (exitMessage == null) {
					exitMessage = new ExitMessage(this, reason);
				}
				actor.send(exitMessage);
			} else {
				actor.stop(reason);
			}
		}

		this.savedMessages = null;
		this.links = null;
		this.monitors = null;
	}

	final void onStart() {
		try {
			preStart();
		} catch (Exception ex) {
			stop(ex);
			return;
		}
		if (receive.afterHook != null) {
			this.afterFuture = this.scheduler.after(this);
		}
	}

	final void onAfter() {
		if (isStoped()) {
			return;
		}
		try {
			receive.afterHook.run();
		} catch (Exception ex) {
			stop(ex);
			return;
		}
		sendSavedMessages();
		this.afterFuture = this.scheduler.after(this);
	}

	final void onReceive(Object message) {
		if (isStoped()) {
			return;
		}
		try {
			for (ReceiveRule rule : receive.receiveRules) {
				if (rule.matcher.test(message)) {
					sendSavedMessages();

					if (this.afterFuture != null) {
						this.afterFuture.cancel(false);
						this.afterFuture = this.scheduler.after(this);
					}

					rule.receiver.accept(message);
					return;
				}
			}
		} catch (Exception ex) {
			stop(ex);
			return;
		}
		savedMessages.offer(message);
	}

	private final void sendSavedMessages() {
		Object savedMessage;
		while ((savedMessage = savedMessages.poll()) != null) {
			send(savedMessage);
		}
	}
}
