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

/**
 * <p>Title: UnsafeLongStack</p>
 * <p>Description: An unsafe long stack implementation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.UnsafeLongStack</code></p>
 */

public class UnsafeLongStack implements ILongStack {
	/** The underlying UnsafeLongArray */
	protected final UnsafeLongArray array;
	
	/**
	 * De-allocates this UnsafeLongStack 
	 */
	@Override
	public void destroy() {
		array.destroy();
	}
	
	/**
	 * Creates a new UnsafeLongStack
	 */
	public UnsafeLongStack() {
		array = UnsafeArrayBuilder.newBuilder()
				.allocationIncrement(1)
				.initialCapacity(1)
				.maxCapacity(1024)
				.minCapacity(0)
				.sorted(false)
				.clearedSlotsFree(1)
				.buildLongArray();
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongStack#get(int)
	 */
	@Override
	public long get(int index) {
		if(index<0 || index+1 > size()) throw new IllegalArgumentException("Invalid index requested [" + index + "] for stack with size [" + size() + "]", new Throwable());
		return array.get(index);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongStack#push(long[])
	 */
	@Override
	public ILongStack push(long... values) {
		if(values!=null) {
			for(long v: values) {
				array.rollRight(0, v);
			}
		}
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongStack#pop()
	 */
	@Override
	public long pop() {
		if(size()<1) throw new IllegalStateException("Cannot pop an empty stack", new Throwable());
		long value = array.get(0);
		array.rollLeft(true, 0);
		return value;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongStack#peek()
	 */
	@Override
	public long peek() {
		if(size()<1) throw new IllegalStateException("Cannot peek an empty stack", new Throwable());
		return array.get(0);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongStack#size()
	 */
	@Override
	public int size() {
		return array.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongStack#emptySlotsFree()
	 */
	@Override
	public int emptySlotsFree() {
		return array.clearedSlotsFree();
	}
	
	/**
	 * <p>Renders an information string about his long stack.</p>
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("[").append(getClass().getSimpleName()).append("]");
		b.append("\n\tSize:").append(array.size());
		b.append("\n\tFree Slots:").append(array.clearedSlotsFree());
		b.append("\n\tCapacity:").append(array.capacity());		
		return b.toString();
	}
	
	public static void main(String[] args) {
		log("LongStack test");
		UnsafeLongStack stack = new UnsafeLongStack();
		log(stack);
		for(int i = 0; i < 10; i++) {
			stack.push(i);
		}
		log(stack);
		for(int i = 0; i < 10; i++) {
			long a = stack.pop();
			log("Popped:" + a);
		}
		log(stack);
		
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

}
