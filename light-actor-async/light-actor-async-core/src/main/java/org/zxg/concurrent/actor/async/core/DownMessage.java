/*
 * Copyright (c) 2018, 2019, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor.async.core;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public final class DownMessage {

	public final Actor from;
	public final Object reason;

	DownMessage(Actor from, Object reason) {
		this.from = from;
		this.reason = reason;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DownMessage [from=");
		builder.append(from);
		builder.append(", reason=");
		builder.append(reason);
		builder.append("]");
		return builder.toString();
	}
}
