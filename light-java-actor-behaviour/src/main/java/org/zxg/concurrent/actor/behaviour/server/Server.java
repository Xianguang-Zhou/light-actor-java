/*
 * Copyright (c) 2018, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor.behaviour.server;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.zxg.concurrent.actor.core.Actor;
import org.zxg.concurrent.actor.core.DownMessage;
import org.zxg.concurrent.actor.core.Receive;
import org.zxg.concurrent.actor.core.ReceiveBuilder;
import org.zxg.concurrent.actor.core.TypeMatcher;
import org.zxg.concurrent.actor.core.exception.ActorInterruptedException;
import org.zxg.concurrent.actor.core.exception.ActorStateException;
import org.zxg.concurrent.actor.core.exception.ActorStoppedException;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public class Server {

	public final Actor server;

	public Server(Actor server) {
		Objects.requireNonNull(server);
		this.server = server;
	}

	public final void cast(Object request) {
		server.send(request);
	}

	public final void call(Object request, Consumer<Object> responseHandler, Actor client) {
		call(request, responseHandler, failure -> {
		}, client);
	}

	public final void call(Object request, Consumer<Object> responseHandler,
			Consumer<ActorStateException> failureHandler, Actor client) {
		Objects.requireNonNull(responseHandler);
		Objects.requireNonNull(failureHandler);
		Objects.requireNonNull(client);
		call(request, response -> client.execute(() -> responseHandler.accept(response)),
				failure -> client.execute(() -> failureHandler.accept(failure)));
	}

	protected final void call(Object request, Consumer<Object> responseHandler,
			Consumer<ActorStateException> failureHandler) {
		server.execute(() -> {
			Actor actor = new Actor(server.getGroup()) {
				@Override
				protected void preStart() throws Exception {
					server.send(new Request(request, this));
				}

				@Override
				protected Receive createReceive() {
					return new ReceiveBuilder().match(new TypeMatcher(DownMessage.class), message -> {
						failureHandler.accept(new ActorStoppedException());
						stop();
					}).matchAny(response -> {
						responseHandler.accept(response);
						stop();
					}).build();
				}
			};
			actor.monitor(server);
			actor.start();
		}, failureHandler);
	}

	public final Object call(Object request) throws ActorStateException {
		Object waitObject = new Object();
		AtomicReference<Object> responseRef = new AtomicReference<>();
		AtomicReference<ActorStateException> failureRef = new AtomicReference<>();
		call(request, response -> {
			responseRef.set(response);
			synchronized (waitObject) {
				waitObject.notify();
			}
		}, failure -> {
			failureRef.set(failure);
			synchronized (waitObject) {
				waitObject.notify();
			}
		});
		synchronized (waitObject) {
			if (responseRef.get() == null && failureRef.get() == null) {
				try {
					waitObject.wait();
				} catch (InterruptedException ex) {
					throw new ActorInterruptedException(ex);
				}
			}
		}
		if (failureRef.get() != null) {
			throw failureRef.get();
		} else {
			return responseRef.get();
		}
	}
}
