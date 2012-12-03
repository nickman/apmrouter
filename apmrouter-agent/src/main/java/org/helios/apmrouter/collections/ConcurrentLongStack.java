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
package org.helios.apmrouter.collections;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * <p>Title: ConcurrentLongStack</p>
 * <p>Description: A concurrent thread-safe version of {@link UnsafeLongStack}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.ConcurrentLongStack</code></p>
 */

public class ConcurrentLongStack extends UnsafeLongStack {
	/** The reentrant read/write lock */
	private final ReentrantReadWriteLock readWriteLock;
	/** The concurrent read lock */
	private final ReadLock readLock;
	/** The exclusive write lock */
	private final WriteLock writeLock;
	
	/**
	 * De-allocates this LongStack 
	 */
	@Override
	public void destroy() {
		writeLock.lock();
		try {
			super.destroy();
		} finally {
			writeLock.unlock();
		}
	}


	/**
	 * Creates a new ConcurrentLongStack
	 */
	public ConcurrentLongStack() {
		readWriteLock = new ReentrantReadWriteLock(false);
		readLock = readWriteLock.readLock();
		writeLock = readWriteLock.writeLock();		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.UnsafeLongStack#push(long[])
	 */
	@Override
	public ILongStack push(long... values) {
		writeLock.lock();
		try {
			return super.push(values);
		} finally {
			writeLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongStack#get(int)
	 */
	@Override
	public long get(int index) {
		readLock.lock();
		try {
			return super.get(index);
		} finally {
			readLock.unlock();
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.UnsafeLongStack#pop()
	 */
	@Override
	public long pop() {
		writeLock.lock();
		try {
			return super.pop();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.UnsafeLongStack#peek()
	 */
	@Override
	public long peek() {
		readLock.lock();
		try {
			return super.peek();
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.UnsafeLongStack#size()
	 */
	@Override
	public int size() {
		readLock.lock();
		try {
			return super.size();
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.UnsafeLongStack#emptySlotsFree()
	 */
	@Override
	public int emptySlotsFree() {
		readLock.lock();
		try {
			return super.emptySlotsFree();
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.UnsafeLongStack#toString()
	 */
	@Override
	public String toString() {
		readLock.lock();
		try {
			return super.toString();
		} finally {
			readLock.unlock();
		}
	}

}
