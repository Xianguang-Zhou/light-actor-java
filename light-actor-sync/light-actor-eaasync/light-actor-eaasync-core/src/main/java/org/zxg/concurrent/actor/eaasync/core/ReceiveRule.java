/*
 * Copyright (c) 2019, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
