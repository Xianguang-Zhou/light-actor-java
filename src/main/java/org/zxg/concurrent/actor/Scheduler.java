/*
 * Copyright (c) 2018, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
final class Scheduler {

	private ScheduledExecutorService executor;

	public Scheduler(ScheduledExecutorService executor) {
		this.executor = executor;
	}

	public void send(Actor actor, Object message) {
		this.executor.execute(new Task(actor, message));
	}

	public ScheduledFuture<?> after(Actor actor) {
		return this.executor.schedule(actor::after, actor.receive.afterTime, actor.receive.afterTimeUnit);
	}

	public void start(Actor actor) {
		this.executor.execute(actor::start);
	}

	public void stop(Actor actor) {
		this.executor.execute(actor::stop);
	}
}
