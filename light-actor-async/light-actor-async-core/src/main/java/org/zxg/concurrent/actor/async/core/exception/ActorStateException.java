/*
 * Copyright (c) 2018, 2019, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor.async.core.exception;

import org.zxg.concurrent.actor.async.core.Actor;
import org.zxg.concurrent.actor.async.core.ActorState;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public class ActorStateException extends ActorException {

	private static final long serialVersionUID = 1L;

	public ActorStateException(Actor actor, ActorState state) {
		super(String.format("The state of the actor \"%s\" is \"%s\".", actor.toString(), state.toString()));
	}
}
