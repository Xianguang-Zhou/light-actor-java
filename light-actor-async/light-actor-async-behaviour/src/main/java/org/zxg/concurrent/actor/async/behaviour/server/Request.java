/*
 * Copyright (c) 2018, 2019, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor.async.behaviour.server;

import org.zxg.concurrent.actor.async.core.Actor;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public final class Request {

	public final Object content;
	private final Actor from;

	Request(Object content, Actor from) {
		this.content = content;
		this.from = from;
	}

	public void reply(Object response) {
		this.from.send(response);
	}
}
