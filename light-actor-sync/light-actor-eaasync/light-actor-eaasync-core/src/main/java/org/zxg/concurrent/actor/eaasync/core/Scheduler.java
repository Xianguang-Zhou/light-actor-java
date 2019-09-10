/*
 * Copyright (c) 2019, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor.eaasync.core;

import java.util.concurrent.ScheduledExecutorService;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
final class Scheduler {

	private static ThreadLocal<Scheduler> threadLocal = new ThreadLocal<Scheduler>();

	ActorGroup group;
	private ScheduledExecutorService executor;
	volatile Actor currentActor;

	public Scheduler(ActorGroup group, ScheduledExecutorService executor) {
		this.group = group;
		this.executor = executor;
		this.executor.execute(() -> {
			threadLocal.set(this);
		});
	}

	public void send(Actor actor, Object message) {
		this.executor.execute(new ReceiveTask(actor, message));
	}

	public void start(Actor actor) {
		this.executor.execute(actor::onStart);
	}

	public static Scheduler ofThread() {
		return threadLocal.get();
	}
}
