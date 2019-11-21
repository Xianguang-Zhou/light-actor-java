/*
 * Copyright (c) 2018, 2019, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
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
package org.zxg.concurrent.actor.async.behaviour.server;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.zxg.concurrent.actor.async.core.Actor;
import org.zxg.concurrent.actor.async.core.DownMessage;
import org.zxg.concurrent.actor.async.core.Receive;
import org.zxg.concurrent.actor.async.core.ReceiveBuilder;
import org.zxg.concurrent.actor.async.core.TypeMatcher;
import org.zxg.concurrent.actor.async.core.exception.ActorInterruptedException;
import org.zxg.concurrent.actor.async.core.exception.ActorStateException;
import org.zxg.concurrent.actor.async.core.exception.ActorStoppedException;

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
		call(request, responseHandler, failure -> client.stop(failure), client);
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
						failureHandler.accept(new ActorStoppedException(server));
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
