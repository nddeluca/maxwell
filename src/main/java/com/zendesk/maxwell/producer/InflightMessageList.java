package com.zendesk.maxwell.producer;
/* respresents a list of inflight messages -- stuff being sent over the
	 network, that may complete in any order.  Allows for only bumping
	 the binlog position upon completion of the oldest outstanding item.

	 Assumes .addInflight(position) will be call monotonically.
	 */

import com.zendesk.maxwell.replication.BinlogPosition;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Iterator;

public class InflightMessageList {
	class InflightTXMessage {
		public final BinlogPosition position;
		public boolean isComplete;

		public InflightTXMessage(BinlogPosition position) {
			this.position = position;
			this.isComplete = false;
		}
	}

	private final HashSet<String> nonTXMessages;
	private final LinkedHashMap<String, InflightTXMessage> txMessages;

	private final Lock txLock = new ReentrantLock();
	private final Lock nonTXLock = new ReentrantLock();
	private final Condition nonTXMessagesNotFull = nonTXLock.newCondition(); 

	private final int MAX_INFLIGHT_NON_TX_MESSAGES = 10000;

	public InflightMessageList() {
		this.nonTXMessages = new HashSet<>();
		this.txMessages = new LinkedHashMap<>();
	}

	public void addTXMessage(String rowId, BinlogPosition position) {
		txLock.lock();

		try {
			InflightTXMessage m = new InflightTXMessage(position);
			txMessages.put(rowId, m);
		} finally {
			txLock.unlock();
		}

		return;
	}

	public void addNonTXMessage(String rowId) throws InterruptedException {
		nonTXLock.lock();

		try {
			while(nonTXMessages.size() > MAX_INFLIGHT_NON_TX_MESSAGES) {
				nonTXMessagesNotFull.await();
			}

			this.nonTXMessages.add(rowId);
		} finally {
			nonTXLock.unlock();
		}
	}

	/* returns the position that stuff is complete up to, or null if there were no changes */
	public BinlogPosition completeTXMessage(String rowId) {
		BinlogPosition completeUntil = null;

		txLock.lock();

		try {
			InflightTXMessage m = txMessages.get(rowId);
			assert(m != null);

			m.isComplete = true;
			Iterator<InflightTXMessage> iterator = txMessages.values().iterator();

			while ( iterator.hasNext() ) {
				InflightTXMessage msg = iterator.next();
				if ( !msg.isComplete )
					break;

				completeUntil = msg.position;
				iterator.remove();
			}

		} finally {
			txLock.unlock();
		}

		return completeUntil;
	}

	public void completeNonTXMessage(String rowId) {
		nonTXLock.lock();

		try {
			nonTXMessages.remove(rowId);
			nonTXMessagesNotFull.signal();
		} finally {
			nonTXLock.unlock();
		}

		return;
	}
}
