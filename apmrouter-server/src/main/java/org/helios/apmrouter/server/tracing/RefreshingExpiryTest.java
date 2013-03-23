/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package org.helios.apmrouter.server.tracing;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: RefreshingExpiryTest</p>
 * <p>Description: Approach test for a refreshing expiry queue</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.tracing.RefreshingExpiryTest</code></p>
 */
@SuppressWarnings("javadoc")
public class RefreshingExpiryTest {
	
	protected static final DelayQueue<Expirer> queue = new DelayQueue<Expirer>(); 
	
	public static void main(String[] args) {
		log("RefreshingExpiryTest");
		Random random = new Random(System.currentTimeMillis());
		for(int i = 0; i < 100; i++) {
			queue.add(new Expirer(Math.abs(random.nextInt(10000)+3000), "Ex{" + i + "}"));
		}
//		queue.add(new Expirer(5000, "Five"));
//		queue.add(new Expirer(10000, "Ten"));
		new Thread("ExpiryThread") {
			int cnt = 0;
			public void run() {
				while(queue.size()>0) {
					try {
						Expirer ex = queue.take();
						cnt++;
						ex.expire();
						log("Queue Removed --->" + ex + "\t\tExpired Count:" + cnt);
						
					} catch (InterruptedException e) {
						e.printStackTrace();
						Thread.interrupted();
					}
				}
			}
		}.start();
		for(int i = 0; i < 10000000; i++) {			
			try { Thread.currentThread().join(Math.abs(random.nextInt(1000))); } catch (Exception ex) {}
			int queueSize = queue.size();
			if(queueSize==0) break;
			log("Queue Size:" + queue.size());
			for(Expirer ex: queue) {
				ex.touch();
			}
		}
	}

	
	public static void log(Object msg) {
		System.out.println("[" + Thread.currentThread().getName() + "]:" + msg);
	}
	
	protected static class Expirer implements Delayed {
		private static final AtomicLong serial = new AtomicLong();
		protected final long timeout;
		protected final long id;
		protected final long initTime = System.currentTimeMillis();
		protected final AtomicLong lastTouch = new AtomicLong(initTime);
		protected final String name;
		protected boolean expired = false;
		
		

		public Expirer(long timeout, String name) {
			super();
			this.timeout = timeout;
			this.name = name;
			id = serial.incrementAndGet();
		}
		
		public void touch() {
			if(expired) throw new IllegalStateException("This expirer is already expired", new Throwable());
			lastTouch.set(System.currentTimeMillis());
			log("Touched [" + name + "]. New delay:" + getDelay(TimeUnit.MILLISECONDS) + " ms.");
		}
		
		protected void expire() {
			expired = true;
		}

		@Override
		public int compareTo(Delayed otherDelayed) {
			long myDelay = getTimeToExpiry();
			long hisDelay = otherDelayed.getDelay(TimeUnit.MILLISECONDS);
			if(myDelay > hisDelay) return 1;
			if(myDelay < hisDelay) return -1;
			if(otherDelayed instanceof Expirer) {
				return ((Expirer)otherDelayed).id > id ? 1 : -1;
			}
			return 0;
		}
		
		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(getTimeToExpiry(), TimeUnit.MILLISECONDS);
		}
		
		public long getTimeToExpiry() {
			long d = System.currentTimeMillis()-lastTouch.get();			
			return timeout - d;			
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			if(!expired) {
				return String.format(
						"Expirer [name:%s, id:%s, lastTouch:%s, timeout:%s ms., time-to-expiry:%s ms. ]",
						name, id, new Date(lastTouch.get()), timeout, getTimeToExpiry());
			}
			return String.format("EXPIRED Expirer [name:%s, id:%s, timeout:%s ms., live-time:%s s.]", name, id, timeout, TimeUnit.SECONDS.convert(System.currentTimeMillis()-initTime, TimeUnit.MILLISECONDS));
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (id ^ (id >>> 32));
			return result;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Expirer other = (Expirer) obj;
			if (id != other.id)
				return false;
			return true;
		}
		
		
		
	}
}
