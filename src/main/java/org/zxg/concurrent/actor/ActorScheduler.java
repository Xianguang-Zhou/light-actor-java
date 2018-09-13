/*
 * Copyright (c) 2018, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
class ActorScheduler implements Runnable {

	static final Object closeMessage = new Object();

	Queue<Actor> actors;
	Iterator<Actor> actorIterator;
	ScheduledFuture<?> scheduledFuture;

	public ActorScheduler(ScheduledExecutorService executor, long delay, TimeUnit unit) {
		this.actors = new ConcurrentLinkedQueue<Actor>();
		this.actorIterator = actors.iterator();
		this.scheduledFuture = executor.scheduleWithFixedDelay(this, delay, delay, unit);
	}

	public final void close(boolean mayInterruptIfRunning) {
		this.scheduledFuture.cancel(mayInterruptIfRunning);
	}

	@Override
	public final void run() {
		if (!actorIterator.hasNext()) {
			if (actors.isEmpty()) {
				return;
			}
			actorIterator = actors.iterator();
		}

		Actor actor = actorIterator.next();

		for (Iterator<Object> messageIterator = actor.mailbox.iterator(); messageIterator.hasNext();) {
			Object message = messageIterator.next();
			if (message == closeMessage) {
				actorIterator.remove();
				return;
			} else if (actor.receive(message)) {
				messageIterator.remove();
				return;
			}
		}
	}

	public final void add(Actor actor) {
		this.actors.offer(actor);
	}
}
