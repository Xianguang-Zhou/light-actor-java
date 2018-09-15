/*
 * Copyright (c) 2018, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public class ActorGroup {

	private ActorScheduler[] schedulers;
	private AtomicInteger schedulerIndex = new AtomicInteger();
	private int schedulersSize;

	public ActorGroup(Iterable<? extends Executor> executors, int executorsSize) {
		this.schedulers = new ActorScheduler[executorsSize];
		int index = 0;
		for (Executor executor : executors) {
			schedulers[index++] = new ActorScheduler(executor);
		}
		this.schedulersSize = executorsSize;
	}

	final ActorScheduler nextScheduler() {
		return schedulers[Math.abs(schedulerIndex.getAndIncrement() % schedulersSize)];
	}
}
