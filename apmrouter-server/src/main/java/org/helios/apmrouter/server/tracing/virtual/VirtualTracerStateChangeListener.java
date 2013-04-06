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
package org.helios.apmrouter.server.tracing.virtual;

import java.util.Collections;
import java.util.EnumSet;
import java.util.regex.Pattern;

import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.helios.apmrouter.jmx.JMXHelper;

/**
 * <p>Title: VirtualTracerStateChangeListener</p>
 * <p>Description: Helper class to register to receive callbacks on virtual tracer state changes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.tracing.virtual.VirtualTracerStateChangeListener</code></p>
 */

public abstract class VirtualTracerStateChangeListener implements NotificationListener, NotificationFilter {
	/**  */
	private static final long serialVersionUID = 9213264714326434471L;
	/** The {@link VirtualState}s to listen on */
	protected final EnumSet<VirtualState> states;
	/** The host pattern matcher */
	protected final Pattern hostPattern;
	/** The agent pattern matcher */
	protected final Pattern agentPattern;
	/** The tracer name pattern matcher */
	protected final Pattern tracerPattern;
	
	/** The VirtualAgentManager JMX ObjectName */
	public static final ObjectName VA_MGR_OBJECT_NAME = JMXHelper.objectName("org.helios.apmrouter.server.tracing:service=VirtualAgentManager,name=VirtualAgentManager"); 

	/**
	 * Creates a new VirtualTracerStateChangeListener
	 * @param hostPattern A regex expression to match the host name of the notifications to be delivered. A null value will match all hosts.
	 * @param agentPattern A regex expression to match the agent name of the notifications to be delivered. A null value will match all agents.
	 * @param tracerPattern A regex expression to match the tracer name of the notifications to be delivered. A null value will match all tracer names.
	 * @param states The {@link VirtualState}s to listen on
	 */
	public VirtualTracerStateChangeListener(String hostPattern, String agentPattern, String tracerPattern, VirtualState...states) {
		if(states==null || states.length<1) throw new IllegalArgumentException("The passed VirtualStates array was null or empty", new Throwable());
		this.states = EnumSet.noneOf(VirtualState.class);
		Collections.addAll(this.states, states);
		this.hostPattern = hostPattern==null||hostPattern.trim().isEmpty() ? null : Pattern.compile(hostPattern); 
		this.agentPattern = agentPattern==null||agentPattern.trim().isEmpty() ? null : Pattern.compile(agentPattern);
		this.tracerPattern = tracerPattern==null||tracerPattern.trim().isEmpty() ? null : Pattern.compile(tracerPattern);
	}

	/**
	 * Creates a new VirtualTracerStateChangeListener for all hosts, agents and tracer names 
	 * @param states The {@link VirtualState}s to listen on
	 */
	public VirtualTracerStateChangeListener(VirtualState...states) {
		this(null, null, null, states);
	}
	
	/**
	 * Creates a new VirtualTracerStateChangeListener for all hosts, agents and the specified tracer name pattern
	 * @param tracerPattern A regex expression to match the tracer name of the notifications to be delivered. A null value will match all tracer names.
	 * @param states The {@link VirtualState}s to listen on
	 */
	public VirtualTracerStateChangeListener(String tracerPattern, VirtualState...states) {
		this(null, null, tracerPattern, states);
	}
	
	/**
	 * Creates a new VirtualTracerStateChangeListener for all hosts, agents and the specified tracer name pattern and all states
	 * @param tracerPattern A regex expression to match the tracer name of the notifications to be delivered. A null value will match all tracer names.
	 */
	public VirtualTracerStateChangeListener(String tracerPattern) {
		this(null, null, tracerPattern, VirtualState.values());
	}
	
	
	
	/**
	 * Creates a new VirtualTracerStateChangeListener for all hosts and agents and all states 
	 */
	public VirtualTracerStateChangeListener() {
		this(null, null, null, VirtualState.values());
	}
	
	/**
	 * Registers this listener with the VirtualAgentManager
	 */
	public void registerListener() {
		try {
			JMXHelper.getHeliosMBeanServer().addNotificationListener(VA_MGR_OBJECT_NAME, this, this, null);
		} catch (Exception e) {
			throw new RuntimeException("Failed to register VirtualTracerStateChangeListener", e);
		}
	}
	
	/**
	 * Unregisters this listener from the VirtualAgentManager
	 */
	public void unregisterListener() {
		try {
			JMXHelper.getHeliosMBeanServer().removeNotificationListener(VA_MGR_OBJECT_NAME, this, this, null);
		} catch (Exception e) {
			throw new RuntimeException("Failed to unregister VirtualTracerStateChangeListener", e);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationFilter#isNotificationEnabled(javax.management.Notification)
	 */
	@Override
	public boolean isNotificationEnabled(Notification notification) {
		if(notification instanceof AttributeChangeNotification && VirtualAgentManager.TRACER_STATE_CHANGE_NOTIF.equals(notification.getUserData())) {
			try {
				AttributeChangeNotification acn = (AttributeChangeNotification)notification;
				String[] vaSrc = (String[])acn.getSource();
				if(hostPattern!=null) {
					if(!hostPattern.matcher(vaSrc[0]).matches()) return false;
				}
				if(agentPattern!=null) {
					if(!agentPattern.matcher(vaSrc[1]).matches()) return false;
				}
				if(tracerPattern!=null) {
					if(!tracerPattern.matcher(vaSrc[2]).matches()) return false;
				}
				return states.contains(VirtualState.value(acn.getNewValue().toString()));
			} catch (Exception ex) {
				return false;
			}
		}
		return false;
	}


}
