/*
 * Copyright (c) 2018, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor.core;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
final class ReceiveRule {

	private static final Predicate<Object> anyMatcher = message -> true;

	public Predicate<Object> matcher;
	public Consumer<Object> receiver;

	public ReceiveRule(Consumer<Object> receiver) {
		this.matcher = anyMatcher;
		this.receiver = receiver;
	}

	public ReceiveRule(Predicate<Object> matcher, Consumer<Object> receiver) {
		this.matcher = matcher;
		this.receiver = receiver;
	}
}
