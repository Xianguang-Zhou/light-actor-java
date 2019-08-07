/*
 * Copyright (c) 2018, 2019, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor.async.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.zxg.concurrent.actor.async.core.exception.ActorException;
import org.zxg.concurrent.actor.async.core.exception.ActorRegisteredException;
import org.zxg.concurrent.actor.async.core.exception.ActorStoppedException;
import org.zxg.concurrent.actor.async.core.exception.InvalidActorNameException;
import org.zxg.concurrent.actor.async.core.exception.RepeatedActorNameException;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public class ActorGroup {

	private Scheduler[] schedulers;
	private AtomicInteger schedulerIndex = new AtomicInteger();
	private int schedulersSize;
	Map<String, Actor> registry;

	public ActorGroup(Iterable<? extends ScheduledExecutorService> executors, int executorsSize) {
		if (executorsSize <= 0) {
			throw new IllegalArgumentException("Argument \"executorsSize\" should be a positive number.");
		}
		this.schedulers = new Scheduler[executorsSize];
		int index = 0;
		for (ScheduledExecutorService executor : executors) {
			schedulers[index++] = new Scheduler(this, executor);
		}
		if (index < executorsSize) {
			throw new IllegalArgumentException(
					"Argument \"executorsSize\" is not equal to the size of argument \"executors\".");
		}
		this.schedulersSize = index;
		this.registry = new ConcurrentHashMap<>();
	}

	public final void register(String name, Actor actor)
			throws RepeatedActorNameException, ActorRegisteredException, ActorStoppedException {
		if (actor == null) {
			throw new NullPointerException();
		}
		final Ref<ActorException> exRef = new Ref<>();
		Actor oldActor = registry.computeIfAbsent(name, (String nameKey) -> {
			if (!actor.isStopped()) {
				if (actor.nameRef.compareAndSet(null, nameKey)) {
					if (!actor.isStopped()) {
						return actor;
					} else {
						exRef.value = new ActorStoppedException(actor);
					}
				} else {
					exRef.value = new ActorRegisteredException();
				}
			} else {
				exRef.value = new ActorStoppedException(actor);
			}
			return null;
		});
		if (exRef.value != null) {
			throw exRef.value;
		} else if (oldActor != actor) {
			throw new RepeatedActorNameException();
		}
	}

	public final void unregister(String name) throws ActorStoppedException, InvalidActorNameException {
		final Ref<ActorException> exRef = new Ref<>();
		final Ref<Boolean> existsRef = new Ref<>(false);
		registry.computeIfPresent(name, (String nameKey, Actor actorValue) -> {
			existsRef.value = true;
			if (!actorValue.isStopped()) {
				actorValue.nameRef.set(null);
				return null;
			} else {
				exRef.value = new ActorStoppedException(actorValue);
			}
			return actorValue;
		});
		if (exRef.value != null) {
			throw exRef.value;
		} else if (!existsRef.value) {
			throw new InvalidActorNameException();
		}
	}

	public final Actor whereis(String name) {
		return registry.get(name);
	}

	public final Iterable<String> registered() {
		return registry.keySet();
	}

	final Scheduler nextScheduler() {
		return schedulers[Math.abs(schedulerIndex.getAndIncrement() % schedulersSize)];
	}
}
