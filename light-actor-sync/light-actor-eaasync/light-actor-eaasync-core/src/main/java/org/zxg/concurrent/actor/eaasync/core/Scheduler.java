/*
 * Copyright (c) 2019, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.zxg.concurrent.actor.eaasync.core;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

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
		this.executor.execute(new StartTask(actor));
	}

	public void stop(Actor actor, Object reason) {
		this.executor.execute(new StopTask(actor, reason));
	}

	public ScheduledFuture<?> after(Actor actor, Receive receive) {
		return this.executor.schedule(actor::onAfter, receive.afterTime, receive.afterTimeUnit);
	}

	public static Scheduler ofThread() {
		return threadLocal.get();
	}
}
