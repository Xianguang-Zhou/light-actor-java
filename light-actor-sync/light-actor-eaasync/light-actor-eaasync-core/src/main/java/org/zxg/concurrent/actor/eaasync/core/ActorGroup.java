/*
 * Copyright (c) 2019, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor.eaasync.core;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public class ActorGroup {

	private Scheduler[] schedulers;
	private AtomicInteger schedulerIndex = new AtomicInteger();
	private int schedulersSize;

	public ActorGroup(Iterable<? extends ScheduledExecutorService> executors, int executorsSize) {
		if (executorsSize <= 0) {
			throw new IllegalArgumentException("Argument \"executorsSize\" should be a positive number.");
		}
		this.schedulers = new Scheduler[executorsSize];
		int index = 0;
		for (ScheduledExecutorService executor : executors) {
			schedulers[index++] = new Scheduler(this, executor);
		}
		if (index < executorsSize) {
			throw new IllegalArgumentException(
					"Argument \"executorsSize\" is not equal to the size of argument \"executors\".");
		}
		this.schedulersSize = index;
	}

	final Scheduler nextScheduler() {
		return schedulers[Math.abs(schedulerIndex.getAndIncrement() % schedulersSize)];
	}
}
