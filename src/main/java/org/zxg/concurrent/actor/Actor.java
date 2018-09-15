/*
 * Copyright (c) 2018, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public abstract class Actor {

	private ActorScheduler scheduler;

	protected Actor(ActorGroup group) {
		scheduler = group.nextScheduler();
	}

	protected abstract void receive(Object message);

	public final void send(Object message) {
		this.scheduler.send(this, message);
	}
}

