/*
 * Copyright (c) 2018, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor;

import java.util.LinkedList;
import java.util.Queue;
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

	protected Actor(ActorGroup group) {
		this.savedMessages = new LinkedList<Object>();
		this.receive = createReceive();
		this.scheduler = group.nextScheduler();
		this.scheduler.start(this);
	}

	protected abstract Receive createReceive();

	protected void preStart() {
	}

	protected void postStop() {
	}

	public final void send(Object message) {
		if (isStoped) {
			return;
		}
		this.scheduler.send(this, message);
	}

	public final void close() {
		if (isStoped) {
			return;
		}
		this.scheduler.stop(this);
	}

	public final boolean isStoped() {
		return isStoped;
	}

	final void stop() {
		if (isStoped) {
			return;
		}
		isStoped = true;

		String name = nameRef.getAndSet(null);
		if (name != null) {
			scheduler.group.registry.computeIfPresent(name, (String nameKey, Actor actorValue) -> {
				if (actorValue == this) {
					return null;
				} else {
					return actorValue;
				}
			});
		}

		postStop();
	}

	final void start() {
		preStart();
		if (receive.afterHook != null) {
			this.afterFuture = this.scheduler.after(this);
		}
	}

	final void after() {
		if (isStoped) {
			return;
		}
		receive.afterHook.run();
		sendSavedMessages();
		this.afterFuture = this.scheduler.after(this);
	}

	final void receive(Object message) {
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
