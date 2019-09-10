/*
 * Copyright (c) 2019, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor.eaasync.core;

import static com.ea.async.Async.await;
import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public abstract class Actor {

	private Scheduler scheduler;
	private AtomicReference<ActorState> stateRef = new AtomicReference<>(ActorState.CREATED);
	private CompletableFuture<Object> next;
	private Queue<Object> messages = new LinkedList<>();

	protected Actor(ActorGroup group) {
		this.scheduler = group.nextScheduler();
	}

	protected abstract CompletableFuture<Void> run();

	CompletableFuture<Void> internalRun() {
		stateRef.compareAndSet(ActorState.CREATED, ActorState.STARTED);
		Void result = await(run());
		stateRef.compareAndSet(ActorState.STARTED, ActorState.STOPPED);
		scheduler.currentActor = null;
		return completedFuture(result);
	}

	public final void start() {
		stateRef.getAndUpdate(state -> {
			if (state == ActorState.CREATED) {
				this.scheduler.start(this);
				return ActorState.STARTED;
			} else {
				return state;
			}
		});
	}

	public final void send(Object message) {
		if (message == null) {
			throw new NullPointerException();
		}
		if (stateRef.get() != ActorState.STARTED) {
			return;
		}
		this.scheduler.send(this, message);
	}

	public final ActorState getState() {
		return stateRef.get();
	}

	public final boolean isStopped() {
		return stateRef.get() == ActorState.STOPPED;
	}

	public final boolean isStarted() {
		return stateRef.get() == ActorState.STARTED;
	}

	public final ActorGroup getGroup() {
		return scheduler.group;
	}

	public static CompletableFuture<Object> receive() {
		Scheduler scheduler = Scheduler.ofThread();
		if (null == scheduler) {
			return completedFuture(null);
		}
		Actor self = scheduler.currentActor;
		if (null == self) {
			return completedFuture(null);
		}
		if (self.messages.isEmpty()) {
			scheduler.currentActor = null;
			self.next = new CompletableFuture<>();
			return self.next;
		} else {
			return completedFuture(self.messages.poll());
		}
	}

	void onReceive(Object message) {
		if (isStopped()) {
			return;
		}
		messages.offer(message);
		if (null != next && !next.isDone()) {
			scheduler.currentActor = this;
			this.next.complete(messages.poll());
		}
	}

	void onStart() {
		scheduler.currentActor = this;
		internalRun();
	}
}
