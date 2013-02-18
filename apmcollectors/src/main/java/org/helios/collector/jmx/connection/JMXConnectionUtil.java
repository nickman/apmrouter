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
package org.helios.collector.jmx.connection;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

/**
 * <p>Title: JMXConnectionUtil </p>
 * <p>Description: A class that provides utility methods to get an MBean Server Connections with timeout.
 *    The idea is taken from a great blog posting by Eamonn McManus
 *    at http://weblogs.java.net/blog/2007/05/23/making-jmx-connection-timeout</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class JMXConnectionUtil {

    private static final ThreadFactory daemonThreadFactory = new DaemonThreadFactory();
    
    public static MBeanServerConnection connectWithTimeout(
    	    final JMXServiceURL url, final Map<String, Object> environment, long timeout, TimeUnit unit)
    	    throws IOException, InterruptedException {
    	final BlockingQueue<Object> connQueue = new ArrayBlockingQueue<Object>(1);
    	ExecutorService executor = Executors.newSingleThreadExecutor(daemonThreadFactory);
    	executor.submit(new Runnable() {
    	    public void run() {
	    		try {
	    		    MBeanServerConnection connection = JMXConnectorFactory.connect(url, environment).getMBeanServerConnection();
	    		    if (!connQueue.offer(connection))
	    		    	connection=null;
	    		} catch (Throwable t) {
	    			t.printStackTrace();
	    		    connQueue.offer(t);
	    		}
    	    }
    	});
    	Object result;
    	try {
    	    result = connQueue.poll(timeout, unit);
    	    if (result == null) {
	    		if (!connQueue.offer(""))
	    		    result = connQueue.take();
    	    }
    	} catch (InterruptedException e) {
    		throw e;
    	} finally { 
    		executor.shutdown(); 
    	}
    	
    	if (result == null)
    	    throw new SocketTimeoutException("Connect timed out while acquiring an MBean Server Connection for url: " + url);
    	if (result instanceof MBeanServerConnection)
    	    return (MBeanServerConnection) result;
    	else
    	    throw new IOException("Unable to get an MBean Server connection");
    }

    public static MBeanServerConnection connectWithTimeout(
    	    final Context context, final String jndiName, long timeout, TimeUnit unit)
    	    throws IOException, InterruptedException {
    	final BlockingQueue<Object> connQueue = new ArrayBlockingQueue<Object>(1);
    	ExecutorService executor = Executors.newSingleThreadExecutor(daemonThreadFactory);
    	executor.submit(new Runnable() {
    	    public void run() {
	    		try {
	    			MBeanServerConnection connection = (MBeanServerConnection)context.lookup(jndiName);
	    		    if (!connQueue.offer(connection))
	    		    	connection=null;
	    		} catch (Throwable t) {
	    			t.printStackTrace();
	    		    connQueue.offer(t);
	    		}
    	    }
    	});
    	Object result;
    	try {
    	    result = connQueue.poll(timeout, unit);
    	    if (result == null) {
	    		if (!connQueue.offer(""))
	    		    result = connQueue.take();
    	    }
    	} catch (InterruptedException e) {
    	    throw e;
    	} finally {
    	    executor.shutdown();
    	}
    	if (result == null)
    	    throw new SocketTimeoutException("Connect timed out while acquiring an MBean Server Connection");
    	if (result instanceof MBeanServerConnection)
    	    return (MBeanServerConnection) result;
    	else
    	    throw new IOException("Unable to get an MBean Server connection");
    }    
    
	/**
	 * private class to return Daemon threads to create MBean Server Connection
	 */
    private static class DaemonThreadFactory implements ThreadFactory {
    	public Thread newThread(Runnable r) {
    	    Thread t = Executors.defaultThreadFactory().newThread(r);
    	    t.setDaemon(true);
    	    return t;
    	}
    }
}
