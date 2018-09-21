/*
 * Copyright (c) 2018, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public final class ReceiveBuilder {

	private Receive receive;

	public ReceiveBuilder() {
		receive = new Receive();
	}

	public ReceiveBuilder matchAny(Consumer<Object> receiver) {
		receive.receiveRules.add(new ReceiveRule(receiver));
		return this;
	}

	public ReceiveBuilder match(Predicate<Object> matcher, Consumer<Object> receiver) {
		receive.receiveRules.add(new ReceiveRule(matcher, receiver));
		return this;
	}

	public ReceiveBuilder after(long time, TimeUnit unit, Runnable hook) {
		receive.afterTime = time;
		receive.afterTimeUnit = unit;
		receive.afterHook = hook;
		return this;
	}

	public Receive build() throws InvalidReceiveException {
		if (receive.receiveRules.isEmpty() && receive.afterHook == null) {
			throw new InvalidReceiveException();
		}
		return receive;
	}
}