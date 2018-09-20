/*
 * Copyright (c) 2018, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.zxg.concurrent.actor;

import java.util.LinkedList;
import java.util.Queue;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public abstract class Actor {

	private Receive receive;
	private Scheduler scheduler;
	private Queue<Object> savedMessages;

	protected Actor(ActorGroup group) {
		this.savedMessages = new LinkedList<Object>();
		this.receive = createReceive();
		this.scheduler = group.nextScheduler();
	}

	protected abstract Receive createReceive();

	public final void send(Object message) {
		this.scheduler.send(this, message);
	}

	final void receive(Object message) {
		for (ReceiveRule rule : receive.receiveRules) {
			if (rule.matcher.test(message)) {
				Object savedMessage;
				while ((savedMessage = savedMessages.poll()) != null) {
					send(savedMessage);
				}
				rule.receiver.accept(message);
				return;
			}
		}
		savedMessages.offer(message);
	}
}
