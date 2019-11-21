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

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.function.Predicate;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
final class ReceiveRule {

	private static final Predicate<Object> anyMatcher = message -> true;
	private static final AsyncConsumer<Object> emptyReceiver = message -> completedFuture(null);

	public Predicate<Object> matcher;
	public AsyncConsumer<Object> receiver;

	public ReceiveRule() {
		this.matcher = anyMatcher;
		this.receiver = emptyReceiver;
	}

	public ReceiveRule(Predicate<Object> matcher) {
		this.matcher = matcher;
		this.receiver = emptyReceiver;
	}

	public ReceiveRule(AsyncConsumer<Object> receiver) {
		this.matcher = anyMatcher;
		this.receiver = receiver;
	}

	public ReceiveRule(Predicate<Object> matcher, AsyncConsumer<Object> receiver) {
		this.matcher = matcher;
		this.receiver = receiver;
	}
}
