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
package org.helios.apmrouter.jmx;

import java.util.Set;
import java.util.concurrent.*;


/**
 * <p>Title: ListenerAwareRunnableScheduledFuture</p>
 * <p>Description: A RunnableScheduledFuture implementation that wraps an actual RunnableScheduledFuture and fires registered runnables on completion or cancellation</p>  
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.ListenerAwareRunnableScheduledFuture</code></p>
 */
public class ListenerAwareRunnableScheduledFuture<V> implements RunnableScheduledFuture<V> {
	/** The wrapped RunnableScheduledFuture */
	protected final RunnableScheduledFuture<V> inner;
	/** A set of registered listeners */
	protected final Set<Runnable> completionListeners = new CopyOnWriteArraySet<Runnable>();

	/**
	 * Creates a new ListenerAwareRunnableScheduledFuture
	 * @param inner The wrapped RunnableScheduledFuture 
	 */
	public ListenerAwareRunnableScheduledFuture(RunnableScheduledFuture<V> inner) {
		this.inner = inner;
	}
	
	/**
	 * Adds a completion listener.If the task is already cancelled, it will be fired immediately
	 * @param listener the listener to add
	 */
	public void addCompletionListener(Runnable listener) {
		completionListeners.add(listener);
		if(isDone() || isCancelled()) {
			listener.run();
		}		
	}
	
	/**
	 * Removes a completion listener
	 * @param listener the listener to remove
	 */
	public void removeCompletionListener(Runnable listener) {
		completionListeners.remove(listener);
	}
	
	
	/**
	 * 
	 * @see java.util.concurrent.RunnableFuture#run()
	 */
	@Override
	public void run() {
		try {
			inner.run();
		} finally {
			if(!isPeriodic()) {
				for(Runnable r: completionListeners) {
					r.run();
				}
				completionListeners.clear();
			}
		}
	}
	

	/**
	 * Attempts to cancel this task
	 * @param mayInterruptIfRunning Interrupts the running thread if true
	 * @return false if the task could not be cancelled, typically because it has already completed normally; true otherwise
	 * @see java.util.concurrent.Future#cancel(boolean)
	 */
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		boolean done = inner.cancel(mayInterruptIfRunning);
		if(done) {
			for(Runnable r: completionListeners) {
				r.run();
			}			
		}
		completionListeners.clear();
		return done;
	}
	

	/**
	 * @param unit
	 * @return
	 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
	 */
	@Override
	public long getDelay(TimeUnit unit) {
		return inner.getDelay(unit);
	}


	/**
	 * @return
	 * @see java.util.concurrent.RunnableScheduledFuture#isPeriodic()
	 */
	@Override
	public boolean isPeriodic() {
		return inner.isPeriodic();
	}
	

	/**
	 * @param o
	 * @return
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Delayed o) {
		return inner.compareTo(o);
	}

	/**
	 * @return
	 * @see java.util.concurrent.Future#isCancelled()
	 */
	@Override
	public boolean isCancelled() {
		return inner.isCancelled();
	}

	/**
	 * @return
	 * @see java.util.concurrent.Future#isDone()
	 */
	@Override
	public boolean isDone() {
		return inner.isDone();
	}

	/**
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @see java.util.concurrent.Future#get()
	 */
	@Override
	public V get() throws InterruptedException, ExecutionException {
		return inner.get();
	}

	/**
	 * @param timeout
	 * @param unit
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public V get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		return inner.get(timeout, unit);
	}

	
	

}
