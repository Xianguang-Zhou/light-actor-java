/*
 * Copyright (c) 2019, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor.eaasync.core.exception;

import org.zxg.concurrent.actor.eaasync.core.Actor;
import org.zxg.concurrent.actor.eaasync.core.ActorState;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public class ActorStoppedException extends ActorStateException {

	private static final long serialVersionUID = 1L;

	public ActorStoppedException(Actor actor) {
		super(actor, ActorState.STOPPED);
	}
}
