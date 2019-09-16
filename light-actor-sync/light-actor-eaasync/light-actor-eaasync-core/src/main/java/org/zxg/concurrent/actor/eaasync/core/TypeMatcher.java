/*
 * Copyright (c) 2019, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor.eaasync.core;

import java.util.function.Predicate;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public final class TypeMatcher implements Predicate<Object> {

	public final Class<?> type;

	public TypeMatcher(Class<?> type) {
		this.type = type;
	}

	@Override
	public boolean test(Object object) {
		return this.type.isInstance(object);
	}
}
