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
