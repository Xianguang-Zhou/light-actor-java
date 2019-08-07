/*
 * Copyright (c) 2018, 2019, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor.async.core;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
final class Scheduler {

	ActorGroup group;
	private ScheduledExecutorService executor;

	public Scheduler(ActorGroup group, ScheduledExecutorService executor) {
		this.group = group;
		this.executor = executor;
	}

	public void send(Actor actor, Object message) {
		this.executor.execute(new ReceiveTask(actor, message));
	}

	public ScheduledFuture<?> after(Actor actor) {
		return this.executor.schedule(actor::onAfter, actor.receive.afterTime, actor.receive.afterTimeUnit);
	}

	public void start(Actor actor) {
		this.executor.execute(actor::onStart);
	}

	public void stop(Actor actor, Object reason) {
		this.executor.execute(new StopTask(actor, reason));
	}

	public void execute(Runnable command) {
		this.executor.execute(command);
	}
}
