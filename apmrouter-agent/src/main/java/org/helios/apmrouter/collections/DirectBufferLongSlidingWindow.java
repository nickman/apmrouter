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

import java.nio.LongBuffer;

/**
 * <p>Title: DirectBufferLongSlidingWindow</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.DirectBufferLongSlidingWindow</code></p>
 */

public class DirectBufferLongSlidingWindow implements ILongSlidingWindow {

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#insert(long[])
	 */
	@Override
	public void insert(long... values) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#insert(java.nio.LongBuffer)
	 */
	@Override
	public void insert(LongBuffer longBuff) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#insert(long)
	 */
	@Override
	public Long insert(long value) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#inc(int, long)
	 */
	@Override
	public long inc(int index, long value) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#inc(int)
	 */
	@Override
	public long inc(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#inc(long)
	 */
	@Override
	public long inc(long value) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#inc()
	 */
	@Override
	public long inc() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#find(long)
	 */
	@Override
	public int find(long value) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#set(long)
	 */
	@Override
	public void set(long value) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#asDoubleArray()
	 */
	@Override
	public double[] asDoubleArray() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#load(byte[])
	 */
	@Override
	public void load(byte[] arr) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#reinitAndLoad(byte[])
	 */
	@Override
	public void reinitAndLoad(byte[] arr) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#clear()
	 */
	@Override
	public void clear() {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#get(int)
	 */
	@Override
	public long get(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#size()
	 */
	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#sum(int)
	 */
	@Override
	public long sum(int within) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#sum()
	 */
	@Override
	public long sum() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#avg(int)
	 */
	@Override
	public long avg(int within) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#avg()
	 */
	@Override
	public long avg() {
		// TODO Auto-generated method stub
		return 0;
	}

}
