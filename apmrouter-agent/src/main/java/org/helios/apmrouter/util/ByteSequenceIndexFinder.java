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
package org.helios.apmrouter.util;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferIndexFinder;

public class ByteSequenceIndexFinder implements ChannelBufferIndexFinder {
	private final byte[] sequence;
	private int pos = 0;
	
	
	/**
	 * Creates a new ByteSequenceIndexFinder
	 * @param sequence The sequence we're searching for
	 */
	public ByteSequenceIndexFinder(byte[] sequence) {
		if(sequence==null || sequence.length==0) throw new IllegalArgumentException("The passed byte sequence was null or zero-length", new Throwable());
		this.sequence = sequence;
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.buffer.ChannelBufferIndexFinder#find(org.jboss.netty.buffer.ChannelBuffer, int)
	 */
	@Override
	public boolean find(ChannelBuffer buffer, int guessedIndex) {
		boolean found = (buffer.getByte(guessedIndex)==sequence[pos]);
		if(found) pos++;
		else pos = 0;
		if(pos==sequence.length) {
			pos = 0;
			return true;
		}
		return false;
	}
	
}