/*
 * Copyright (c) 2018, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor.core;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
final class ReceiveTask implements Runnable {

	private Actor actor;
	private Object message;

	public ReceiveTask(Actor actor, Object message) {
		this.actor = actor;
		this.message = message;
	}

	@Override
	public void run() {
		this.actor.onReceive(this.message);
	}
}
