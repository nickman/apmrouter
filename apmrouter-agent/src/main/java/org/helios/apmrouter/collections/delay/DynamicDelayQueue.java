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
package org.helios.apmrouter.collections.delay;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: DynamicDelayQueue</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.delay.DynamicDelayQueue</code></p>
 * @param <E> the type of elements held in this collection
 */

public class DynamicDelayQueue<E extends NotifyingDelay> extends DelayQueue<E> implements DelayChangeReceiver<NotifyingDelay> {

	/**
	 * Creates a new DynamicDelayQueue
	 */
	public DynamicDelayQueue() {

	}

	/**
	 * Creates a new DynamicDelayQueue
	 * @param c A collection of Es to initialize the queue with
	 */
	public DynamicDelayQueue(Collection<? extends E> c) {
		super(c);
	}

	/**
	 * <p>Called when a contained {@link NotifyingDelay} indicates its delay driver has changed.
	 * This call removes the changed instance and [if the remove is successful] re-inserts it so it
	 * assumes its new position in the queue.</p>
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.delay.DelayChangeReceiver#onDelayChange(org.helios.apmrouter.collections.delay.NotifyingDelay, long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onDelayChange(NotifyingDelay notifyingDelay, long updatedTimestamp) {
		if(super.remove(notifyingDelay)) {
			notifyingDelay.setUpdatedTimestamp(updatedTimestamp);
			super.add((E) notifyingDelay);
		}		
	}

	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.DelayQueue#add(java.util.concurrent.Delayed)
	 */
	@Override
	public boolean add(E e) {
		if(e!=null) e.setDelayChangeReceiver(this);
		return super.add(e);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.AbstractQueue#addAll(java.util.Collection)
	 */
	@Override
	public boolean addAll(Collection<? extends E> c) {
		if(c==null || c.isEmpty()) return false;
		for(E einstance : c) {
			einstance.setDelayChangeReceiver(this);
		}
		return super.addAll(c);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.DelayQueue#clear()
	 */
	@Override
	public void clear() {
		for(E einstance: this) {
			einstance.clearDelayChangeReceiver();
		}
		super.clear();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.DelayQueue#drainTo(java.util.Collection)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public int drainTo(Collection<? super E> c) {
		if(c!=null) {
			super.drainTo(c);
			for(Object einstance: c) {
				((E)einstance).clearDelayChangeReceiver();
			}			
		}
		return 0;		
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.DelayQueue#drainTo(java.util.Collection, int)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public int drainTo(Collection<? super E> c, int maxElements) {
		if(c!=null) {
			super.drainTo(c, maxElements);
			for(Object einstance: c) {
				((E)einstance).clearDelayChangeReceiver();
			}			
		}
		return 0;		
	}


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.DelayQueue#iterator()
	 */
	@Override
	public Iterator<E> iterator() {
		final Iterator<E> fiterator = super.iterator();
		return new Iterator<E>() {
			E currentE = null;
			@Override
			public boolean hasNext() {
				return fiterator.hasNext();
			}

			@Override
			public E next() {
				currentE = fiterator.next(); 
				return currentE;
			}

			@Override
			public void remove() {
				currentE.clearDelayChangeReceiver();				
			}			
		};		
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.DelayQueue#offer(java.util.concurrent.Delayed)
	 */
	@Override
	public boolean offer(E e) {		
		boolean added = super.offer(e);
		if(added) {
			e.setDelayChangeReceiver(this);
		}
		return added;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.DelayQueue#offer(java.util.concurrent.Delayed, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public boolean offer(E e, long timeout, TimeUnit unit) {
		boolean added = super.offer(e, timeout, unit);
		if(added) {
			e.setDelayChangeReceiver(this);
		}
		return added;
	}	
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.DelayQueue#poll()
	 */
	@Override
	public E poll() {
		E e = super.poll();
		if(e!=null) {
			e.clearDelayChangeReceiver();
		}
		return e;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.DelayQueue#poll(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		E e = super.poll(timeout, unit);
		if(e!=null) {
			e.clearDelayChangeReceiver();
		}
		return e;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.DelayQueue#put(java.util.concurrent.Delayed)
	 */
	@Override
	public void put(E e) {
		if(e!=null) {
			e.setDelayChangeReceiver(this);
		}
		super.put(e);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.AbstractQueue#remove()
	 */
	@Override
	public E remove() {
		E e = super.remove();
		e.clearDelayChangeReceiver();
		return e;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.DelayQueue#remove(java.lang.Object)
	 */
	@Override
	public boolean remove(Object o) {
		if(super.remove(o)) {
			if(o instanceof NotifyingDelay) {
				((NotifyingDelay)o).clearDelayChangeReceiver();
			}
			return true;
		}
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.AbstractCollection#removeAll(java.util.Collection)
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		boolean changed = false;
		for(Object o: c) {
			if(remove(o)) {
				changed = true;				
			}
		}
		return changed;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.AbstractCollection#retainAll(java.util.Collection)
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		boolean changed = false;
		for(E e: this) {
			if(c.contains(e)) {
				if(remove(e)) {
					changed = true;
				}
			}
		}
		return changed;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.DelayQueue#take()
	 */
	@Override
	public E take() throws InterruptedException {
		E e = super.take();
		e.clearDelayChangeReceiver();
		return e;
	}


	
}
