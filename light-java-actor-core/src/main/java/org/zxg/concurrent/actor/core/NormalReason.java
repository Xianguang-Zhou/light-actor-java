/*
 * Copyright (c) 2018, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor.core;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public final class NormalReason extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NormalReason() {
		super("NormalReason", null, true, false);
	}

	@Override
	public String toString() {
		return "NormalReason";
	}
}
