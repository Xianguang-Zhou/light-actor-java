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
package org.zxg.concurrent.actor.async.core;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.zxg.concurrent.actor.async.core.exception.ActorStateException;
import org.zxg.concurrent.actor.async.core.exception.ActorStoppedException;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public abstract class Actor implements Comparable<Actor> {

	Receive receive;
	private Scheduler scheduler;
	private Queue<Object> savedMessages;
	private ScheduledFuture<?> afterFuture;
	private AtomicReference<ActorState> stateRef = new AtomicReference<>(ActorState.CREATED);
	AtomicReference<String> nameRef = new AtomicReference<>();
	private Set<Actor> links;
	private volatile boolean isTrapStop = false;
	private Set<Actor> monitors;
	private Set<Actor> monitoredActors;

	protected Actor(ActorGroup group) {
		this.savedMessages = new LinkedList<>();
		this.links = new ConcurrentSkipListSet<>();
		this.monitors = new ConcurrentSkipListSet<>();
		this.monitoredActors = new ConcurrentSkipListSet<>();
		this.receive = createReceive();
		if (this.receive == null) {
			throw new NullPointerException();
		}
		this.scheduler = group.nextScheduler();
	}

	protected abstract Receive createReceive();

	protected void preStart() throws Exception {
	}

	protected void postStop(Object reason) {
	}

	@Override
	public int compareTo(Actor other) {
		return this.hashCode() - other.hashCode();
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

	public final void stop() {
		stop(null);
	}

	public final void stop(Object reason) {
		if (stateRef.compareAndSet(ActorState.STARTED, ActorState.STOPPED)) {
			this.scheduler.stop(this, reason);
		}
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

	public final void link(Actor actor) {
		if (this == actor || this.isStopped() || actor.isStopped()) {
			return;
		}
		actor.links.add(this);
		this.links.add(actor);
	}

	public final void unlink(Actor actor) {
		if (this.isStopped() || actor.isStopped()) {
			return;
		}
		actor.links.remove(this);
		this.links.remove(actor);
	}

	public final void setTrapStop(boolean trapStop) {
		if (this.isStopped()) {
			return;
		}
		this.isTrapStop = trapStop;
	}

	public final boolean isTrapStop() {
		return this.isTrapStop;
	}

	public final void monitor(Actor actor) {
		if (this == actor || this.isStopped() || actor.isStopped()) {
			return;
		}
		actor.monitors.add(this);
		this.monitoredActors.add(actor);
	}

	public final void demonitor(Actor actor) {
		if (this.isStopped() || actor.isStopped()) {
			return;
		}
		actor.monitors.remove(this);
		this.monitoredActors.remove(actor);
	}

	public final ActorGroup getGroup() {
		return scheduler.group;
	}

	public final String getName() {
		if (this.isStopped()) {
			return null;
		}
		return nameRef.get();
	}

	public final void execute(Runnable command) {
		execute(command, failure -> {
		});
	}

	public final void execute(Runnable command, Consumer<ActorStateException> failureHandler) {
		if (command == null) {
			throw new NullPointerException();
		}
		ActorState state = this.getState();
		if (state != ActorState.STARTED) {
			failureHandler.accept(new ActorStateException(this, state));
			return;
		}
		this.scheduler.execute(() -> {
			if (this.isStopped()) {
				failureHandler.accept(new ActorStoppedException(this));
				return;
			}
			try {
				command.run();
			} catch (Exception ex) {
				stop(ex);
			}
		});
	}

	final void onStop(Object reason) {
		String name = nameRef.get();
		if (name != null) {
			nameRef.set(null);
			scheduler.group.registry.computeIfPresent(name, (String nameKey, Actor actorValue) -> {
				if (actorValue == this) {
					return null;
				} else {
					return actorValue;
				}
			});
		}

		try {
			postStop(reason);
		} catch (Exception ex) {
		}

		DownMessage downMessage = null;
		for (Actor actor : this.monitors) {
			actor.monitoredActors.remove(this);
			if (downMessage == null) {
				downMessage = new DownMessage(this, reason);
			}
			actor.send(downMessage);
		}

		ExitMessage exitMessage = null;
		for (Actor actor : this.links) {
			actor.links.remove(this);
			if (actor.isTrapStop) {
				if (exitMessage == null) {
					exitMessage = new ExitMessage(this, reason);
				}
				actor.send(exitMessage);
			} else {
				actor.stop(reason);
			}
		}

		for (Actor actor : this.monitoredActors) {
			actor.monitors.remove(this);
		}
		this.savedMessages = null;
		this.links.clear();
		this.monitors.clear();
		this.monitoredActors.clear();
	}

	final void onStart() {
		stateRef.compareAndSet(ActorState.CREATED, ActorState.STARTED);
		try {
			preStart();
		} catch (Exception ex) {
			stop(ex);
			return;
		}
		if (receive.afterHook != null) {
			this.afterFuture = this.scheduler.after(this);
		}
	}

	final void onAfter() {
		if (isStopped()) {
			return;
		}
		try {
			receive.afterHook.run();
		} catch (Exception ex) {
			stop(ex);
			return;
		}
		sendSavedMessages();
		this.afterFuture = this.scheduler.after(this);
	}

	final void onReceive(Object message) {
		if (isStopped()) {
			return;
		}
		try {
			for (ReceiveRule rule : receive.receiveRules) {
				if (rule.matcher.test(message)) {
					sendSavedMessages();

					if (this.afterFuture != null) {
						this.afterFuture.cancel(false);
						this.afterFuture = this.scheduler.after(this);
					}

					rule.receiver.accept(message);
					return;
				}
			}
		} catch (Exception ex) {
			stop(ex);
			return;
		}
		savedMessages.offer(message);
	}

	private final void sendSavedMessages() {
		Object savedMessage;
		while ((savedMessage = savedMessages.poll()) != null) {
			send(savedMessage);
		}
	}
}
