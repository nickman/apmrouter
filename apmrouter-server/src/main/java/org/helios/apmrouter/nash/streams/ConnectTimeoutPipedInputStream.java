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
package org.helios.apmrouter.nash.streams;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: ConnectTimeoutPipedInputStream</p>
 * <p>Description: A wrapper for a pending {@link PipedInputStream} that blocks until the delegate is available and timesout on any request against a pending delegate.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.nash.streams.ConnectTimeoutPipedInputStream</code></p>
 */
public class ConnectTimeoutPipedInputStream extends PipedInputStream {
	/**  The delegate piped input stream */
	protected PipedInputStream pipeIn = null;
	/** The delegate availability latch */
	protected final CountDownLatch latch = new CountDownLatch(1);
	/** The delegate availability timeout */
	protected final long timeout;
	/** The delegate availability timeout unit */
	protected final TimeUnit unit;
	
	/**
	 * Sets the delegate inout pipe and drops the latch
	 * @param pipeIn the pipeIn to set
	 */
	public void setPipeIn(PipedInputStream pipeIn) {
		this.pipeIn = pipeIn;
		latch.countDown();
	}	
	
	/**
	 * Creates a new ConnectTimeoutPipedInputStream
	 * @param timeout The availability timeout
	 * @param unit The availability timeout unit
	 * @throws IOException thrown on any IO exception
	 */
	public ConnectTimeoutPipedInputStream(long timeout, TimeUnit unit) {
		this.timeout = timeout;
		this.unit = unit;
	}

	/**
	 * Creates a new ConnectTimeoutPipedInputStream
	 * @param src The source piped output stream
	 * @param timeout The availability timeout
	 * @param unit The availability timeout unit
	 * @throws IOException thrown on any IO exception
	 */
	public ConnectTimeoutPipedInputStream(PipedOutputStream src, long timeout, TimeUnit unit) throws IOException {
		super(src);
		this.timeout = timeout;
		this.unit = unit;
	}

	/**
	 * Creates a new ConnectTimeoutPipedInputStream
	 * @param pipeSize The initial pipe size
	 * @param timeout The availability timeout
	 * @param unit The availability timeout unit
	 * @throws IOException thrown on any IO exception
	 */
	public ConnectTimeoutPipedInputStream(int pipeSize, long timeout, TimeUnit unit) {
		super(pipeSize);
		this.timeout = timeout;
		this.unit = unit;
	}

	/**
	 * Creates a new ConnectTimeoutPipedInputStream
	 * @param src The source piped output stream
	 * @param pipeSize The initial pipe size
	 * @param timeout The availability timeout
	 * @param unit The availability timeout unit
	 * @throws IOException thrown on any IO exception 
	 */
	public ConnectTimeoutPipedInputStream(PipedOutputStream src, int pipeSize, long timeout, TimeUnit unit) throws IOException {
		super(src, pipeSize);
		this.timeout = timeout;
		this.unit = unit;
	}

	/**
	 * @param b
	 * @return
	 * @throws IOException
	 * @see java.io.InputStream#read(byte[])
	 */
	public int read(byte[] b) throws IOException {
		waitForPipe();
		return pipeIn.read(b);
	}

	/**
	 * @param src
	 * @throws IOException
	 * @see java.io.PipedInputStream#connect(java.io.PipedOutputStream)
	 */
	public void connect(PipedOutputStream src) throws IOException {
		waitForPipe();
		pipeIn.connect(src);
	}

	/**
	 * @param n
	 * @return
	 * @throws IOException
	 * @see java.io.InputStream#skip(long)
	 */
	public long skip(long n) throws IOException {
		waitForPipe();
		return pipeIn.skip(n);
	}

	/**
	 * @return
	 * @throws IOException
	 * @see java.io.PipedInputStream#read()
	 */
	public int read() throws IOException {
		waitForPipe();
		return pipeIn.read();
	}

	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		waitForPipe();
		return pipeIn.toString();
	}

	/**
	 * @param b
	 * @param off
	 * @param len
	 * @return
	 * @throws IOException
	 * @see java.io.PipedInputStream#read(byte[], int, int)
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		waitForPipe();
		return pipeIn.read(b, off, len);
	}

	/**
	 * @param readlimit
	 * @see java.io.InputStream#mark(int)
	 */
	public void mark(int readlimit) {
		waitForPipe();
		pipeIn.mark(readlimit);
	}

	/**
	 * @throws IOException
	 * @see java.io.InputStream#reset()
	 */
	public void reset() throws IOException {
		waitForPipe();
		pipeIn.reset();
	}

	/**
	 * @return
	 * @throws IOException
	 * @see java.io.PipedInputStream#available()
	 */
	public int available() throws IOException {
		waitForPipe();
		return pipeIn.available();
	}

	/**
	 * @throws IOException
	 * @see java.io.PipedInputStream#close()
	 */
	public void close() throws IOException {
		waitForPipe();
		pipeIn.close();
	}

	/**
	 * @return
	 * @see java.io.InputStream#markSupported()
	 */
	public boolean markSupported() {
		waitForPipe();
		return pipeIn.markSupported();
	}
	
	private void waitForPipe() {
		try {
			if(!latch.await(timeout, unit)) {
				throw new RuntimeException("Timed out waiting for delegate pipe availability", new Throwable());
			}
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted while waiting for delegate pipe availability", e);
		}				
	}



}
