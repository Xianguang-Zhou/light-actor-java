/*
 * Copyright (c) 2019, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor.eaasync.core;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.zxg.concurrent.actor.eaasync.core.exception.InvalidReceiveException;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public final class ReceiveBuilder {

	private Receive receive;

	public ReceiveBuilder() {
		receive = new Receive();
	}

	public ReceiveBuilder ignore(Predicate<Object> matcher) {
		if (matcher == null) {
			throw new NullPointerException();
		}
		receive.receiveRules.add(new ReceiveRule(matcher));
		return this;
	}

	public ReceiveBuilder match(Predicate<Object> matcher, AsyncConsumer<Object> receiver) {
		if (matcher == null || receiver == null) {
			throw new NullPointerException();
		}
		receive.receiveRules.add(new ReceiveRule(matcher, receiver));
		return this;
	}

	public ReceiveBuilder matchAny(AsyncConsumer<Object> receiver) {
		if (receiver == null) {
			throw new NullPointerException();
		}
		receive.receiveRules.add(new ReceiveRule(receiver));
		return this;
	}

	public ReceiveBuilder ingoreAny() {
		receive.receiveRules.add(new ReceiveRule());
		return this;
	}

	public ReceiveBuilder after(long time, TimeUnit unit, AsyncRunnable hook) {
		if (unit == null || hook == null) {
			throw new NullPointerException();
		}
		if (time <= 0) {
			throw new IllegalArgumentException("Argument \"time\" should be a positive number.");
		}
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
