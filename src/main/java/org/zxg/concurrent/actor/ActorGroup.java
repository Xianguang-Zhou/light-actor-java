/*
 * Copyright (c) 2018, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public class ActorGroup {

	ActorScheduler[] schedulers;
	AtomicInteger schedulerIndex = new AtomicInteger();
	int schedulersSize;

	public ActorGroup(Iterable<? extends ScheduledExecutorService> executors, int executorsSize, long delay,
			TimeUnit unit) {
		this.schedulers = new ActorScheduler[executorsSize];
		int index = 0;
		for (ScheduledExecutorService executor : executors) {
			schedulers[index++] = new ActorScheduler(executor, delay, unit);
		}
		this.schedulersSize = executorsSize;
	}

	public void close(boolean mayInterruptIfRunning) {
		for (ActorScheduler scheduler : schedulers) {
			scheduler.close(mayInterruptIfRunning);
		}
	}

	final ActorScheduler nextScheduler() {
		return schedulers[Math.abs(schedulerIndex.getAndIncrement() % schedulersSize)];
	}
}
