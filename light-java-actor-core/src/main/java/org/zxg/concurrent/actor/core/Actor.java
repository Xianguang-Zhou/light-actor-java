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
	private volatile boolean isStoped = false;
	AtomicReference<String> nameRef = new AtomicReference<>();
	private Set<Actor> links;
	private volatile boolean isTrapStop = false;
	private Set<Actor> monitors;

	protected Actor(ActorGroup group) {
		this.savedMessages = new LinkedList<>();
		this.links = new ConcurrentSkipListSet<>();
		this.monitors = new ConcurrentSkipListSet<>();
		this.receive = createReceive();
		this.scheduler = group.nextScheduler();
	}

	protected abstract Receive createReceive();

	protected void preStart() {
	}

	protected void postStop(Object reason) {
	}

	public final void start() {
		this.scheduler.start(this);
	}

	public final void send(Object message) {
		if (isStoped) {
			return;
		}
		this.scheduler.send(this, message);
	}

	public final void stop(Object reason) {
		if (isStoped) {
			return;
		}
		this.scheduler.stop(this, reason);
	}

	public final boolean isStoped() {
		return isStoped;
	}

	public final void link(Actor actor) {
		if (isStoped || actor.isStoped) {
			return;
		}
		actor.links.add(this);
		this.links.add(actor);
	}

	public final void unlink(Actor actor) {
		if (isStoped || actor.isStoped) {
			return;
		}
		actor.links.remove(this);
		this.links.remove(actor);
	}

	public final void setTrapStop(boolean trapStop) {
		if (isStoped) {
			return;
		}
		this.isTrapStop = trapStop;
	}

	public final boolean isTrapStop() {
		return this.isTrapStop;
	}

	public final void monitor(Actor actor) {
		if (isStoped || actor.isStoped) {
			return;
		}
		actor.monitors.add(this);
	}

	public final void demonitor(Actor actor) {
		if (isStoped || actor.isStoped) {
			return;
		}
		actor.monitors.remove(this);
	}

	public final ActorGroup getGroup() {
		return scheduler.group;
	}

	public final String getName() {
		if (isStoped) {
			return null;
		}
		return nameRef.get();
	}

	final void onStop(Object reason) {
		if (isStoped) {
			return;
		}
		isStoped = true;

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

		postStop(reason);

		if (reason != null) {
			for (Actor actor : monitors) {
				actor.send(new DownMessage(this, reason));
			}
		}

		for (Actor actor : links) {
			if (actor.isTrapStop) {
				actor.send(new ExitMessage(this, reason));
			} else {
				actor.stop(reason);
			}
		}
	}

	final void onStart() {
		preStart();
		if (receive.afterHook != null) {
			this.afterFuture = this.scheduler.after(this);
		}
	}

	final void onAfter() {
		if (isStoped) {
			return;
		}
		receive.afterHook.run();
		sendSavedMessages();
		this.afterFuture = this.scheduler.after(this);
	}

	final void onReceive(Object message) {
		if (isStoped) {
			return;
		}
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
		savedMessages.offer(message);
	}

	private final void sendSavedMessages() {
		Object savedMessage;
		while ((savedMessage = savedMessages.poll()) != null) {
			send(savedMessage);
		}
	}
}
