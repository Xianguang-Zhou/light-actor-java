/*
 * Copyright (c) 2018, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor.core.exception;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public class ActorException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ActorException() {
		super();
	}

	public ActorException(String message) {
		super(message);
	}

	public ActorException(Throwable cause) {
		super(cause);
	}

	public ActorException(String message, Throwable cause) {
		super(message, cause);
	}

	public ActorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
