/*
 * Copyright (c) 2019, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor.eaasync.core;

import static com.ea.async.Async.await;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
@FunctionalInterface
public interface AsyncConsumer<T> {

	CompletableFuture<Void> accept(T t);

	default AsyncConsumer<T> andThen(AsyncConsumer<? super T> after) {
		Objects.requireNonNull(after);
		return (T t) -> {
			await(accept(t));
			return after.accept(t);
		};
	}
}