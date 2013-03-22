/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package org.helios.apmrouter.byteman.sockets.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;

/**
 * <p>Title: TrackingSocketImplFactory</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.sockets.impl.TrackingSocketImplFactory</code></p>
 */

public class TrackingSocketImplFactory implements SocketImplFactory {
	/** The delegate socket impl factory */
	protected SocketImplFactory delegate;
	/** The default socket impl's ctor */
	protected volatile Constructor<? extends SocketImpl> socketImplCtor;
	
	/**
	 * Indicates if the socket impl factory has been installed
	 * @return true if the socket impl factory has been installed, false otherwise
	 */
	public static boolean isInstalled() {
		try {
			Field f2 = Socket.class.getDeclaredField("factory");			
			f2.setAccessible(true);						
			SocketImplFactory sif = (SocketImplFactory)f2.get(null);
			return(sif!=null && TrackingSocketImplFactory.class.getName().equals(sif.getClass().getName()));
		} catch (Exception ex) {
			return false;
		}		
	}
	
	
	/**
	 * Creates a new TrackingSocketImplFactory
	 * @param delegate the delegate factory
	 */
	public TrackingSocketImplFactory(SocketImplFactory delegate) {		
		this.delegate = delegate;
		socketImplCtor = null;
	}
	
	/**
	 * Creates a new TrackingSocketImplFactory with no delegate
	 */
	public TrackingSocketImplFactory() {		
		this.delegate = null;
	}
	
	
	
	/** The default actual socket impl class */
	public static String DEFAULT_SOCKET_IMPL = "java.net.SocksSocketImpl";
	/**
	 * {@inheritDoc}
	 * @see java.net.SocketImplFactory#createSocketImpl()
	 */
	@Override
	public SocketImpl createSocketImpl() {
		try {
			if(socketImplCtor==null) {
				synchronized(this) {
					if(socketImplCtor==null) {
						try {
							socketImplCtor = (Constructor<? extends SocketImpl>) Class.forName(DEFAULT_SOCKET_IMPL).getDeclaredConstructor();
						} catch (Exception ex) {
							throw new RuntimeException("Failed to get ctor for ", ex);
						}
					}
				}
			}
			return new TrackingSocketImpl(delegate!=null ? (ISocketImpl)delegate.createSocketImpl() : (ISocketImpl)socketImplCtor.newInstance());
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			throw new RuntimeException("Failed to create SocketImpl", ex);
		}
	}
	
		

}
