/*
 * Copyright (c) 2019, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor.eaasync.core;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
final class StopTask implements Runnable {

	private Actor actor;
	private Object reason;

	public StopTask(Actor actor, Object reason) {
		this.actor = actor;
		this.reason = reason;
	}

	@Override
	public void run() {
		this.actor.onStop(this.reason);
	}
}
