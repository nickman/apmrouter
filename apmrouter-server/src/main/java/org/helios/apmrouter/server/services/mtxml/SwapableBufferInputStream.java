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
package org.helios.apmrouter.server.services.mtxml;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;

/**
 * <p>Title: SwapableBufferInputStream</p>
 * <p>Description: A wrapper for a {@link ChannelBufferInputStream} that allows the actual underlying {@link ChannelBuffer} to be swaped out. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.mtxml.SwapableBufferInputStream</code></p>
 */

public class SwapableBufferInputStream extends InputStream {
	/** The current delegate input stream */
	protected final AtomicReference<ChannelBufferInputStream> delegate = new AtomicReference<ChannelBufferInputStream>(null);
	
	/**
	 * Creates a new SwapableBufferInputStream
	 * @param buffer The initial buffer delegate
	 */
	public SwapableBufferInputStream(ChannelBuffer buffer) {
		delegate.set(new ChannelBufferInputStream(buffer));
	}
	
	/**
	 * Creates a new SwapableBufferInputStream with no initial delegate
	 */
	public SwapableBufferInputStream() {

	}
	
	
	/**
	 * Atomically swaps out the current buffer stream for a new buffer stream 
	 * @param buffer The underlying buffer for the new stream
	 */
	public void swapBuffer(ChannelBuffer buffer) {
		ChannelBufferInputStream d = delegate.getAndSet(new ChannelBufferInputStream(buffer));
		if(d!=null) {
			try { d.close(); } catch (Exception ex) {}
		}
	}
	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#read()
	 */
	@Override
	public int read() throws IOException {
		ChannelBufferInputStream d = delegate.get();
		if(d==null) throw new IOException("No delegate ChannelBufferInputStream was set", new Throwable());
		return delegate.get().read();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#read(byte[])
	 */
	@Override
	public int read(byte[] b) throws IOException {
		ChannelBufferInputStream d = delegate.get();
		if(d==null) throw new IOException("No delegate ChannelBufferInputStream was set", new Throwable());
		return d.read(b);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		ChannelBufferInputStream d = delegate.get();
		if(d==null) throw new IOException("No delegate ChannelBufferInputStream was set", new Throwable());
		return d.read(b, off, len);
	}



	/**
	 * @param n
	 * @return
	 * @throws IOException
	 * @see java.io.InputStream#skip(long)
	 */
	public long skip(long n) throws IOException {
		ChannelBufferInputStream d = delegate.get();
		if(d==null) throw new IOException("No delegate ChannelBufferInputStream was set", new Throwable());		
		return d.skip(n);
	}

	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		ChannelBufferInputStream d = delegate.get();
		if(d==null) return "SwapableBufferInputStream [empty]";		
		return "SwapableBufferInputStream [" + d.toString() + "]";
	}

	/**
	 * @return
	 * @throws IOException
	 * @see java.io.InputStream#available()
	 */
	public int available() throws IOException {
		ChannelBufferInputStream d = delegate.get();
		if(d==null) throw new IOException("No delegate ChannelBufferInputStream was set", new Throwable());				
		return d.available();
	}

	/**
	 * @throws IOException
	 * @see java.io.InputStream#close()
	 */
	public void close() throws IOException {
		ChannelBufferInputStream d = delegate.get();
		if(d!=null) {				
			d.close();
			delegate.set(null);
		}
	}

	/**
	 * @param readlimit
	 * @see java.io.InputStream#mark(int)
	 */
	public void mark(int readlimit) {
		ChannelBufferInputStream d = delegate.get();
		if(d!=null) {				
			d.mark(readlimit);
		}
	}

	/**
	 * @throws IOException
	 * @see java.io.InputStream#reset()
	 */
	public void reset() throws IOException {
		ChannelBufferInputStream d = delegate.get();
		if(d==null) throw new IOException("No delegate ChannelBufferInputStream was set", new Throwable());				
		d.reset();
	}

	/**
	 * @return
	 * @see java.io.InputStream#markSupported()
	 */
	public boolean markSupported() {
		ChannelBufferInputStream d = delegate.get();
		return d==null ? false : d.markSupported(); 
	}

}
