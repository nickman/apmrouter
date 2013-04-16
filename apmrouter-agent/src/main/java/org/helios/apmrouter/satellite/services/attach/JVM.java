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
package org.helios.apmrouter.satellite.services.attach;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import org.helios.apmrouter.jmx.ConfigurationHelper;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.vm.VirtualMachine;
import org.helios.vm.VirtualMachineDescriptor;

/**
 * <p>Title: JVM</p>
 * <p>Description: Represents an attachable Java Virtual Machine.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.satellite.services.attach.JVM</code></p>
 */

public class JVM extends NotificationBroadcasterSupport implements JVMMBean {
	/** Known extensions that might be used as a JVM launch main class directive */
	public static final Set<String> ARCHIVE_NAMES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"jar", "zip", "war", "ear", "sar"
	)));	
	
	/** The agent property name for the jmx connector address */
	public static final String JMX_CONN_ADDR = "com.sun.management.jmxremote.localConnectorAddress";
	/** The agent property name for the JVM's main class */
	public static final String JVM_MAIN_CLASS = "sun.java.command";
	/** The system property name for the JVM's java version */
	public static final String JVM_JAVA_VERSION = "java.version";
	/** The system property name for the JVM's helios agent name */
	public static final String JVM_HELIOS_NAME = "org.helios.agent";
	
	/** The regex to extract the mount point from an expired ObjectName */
	public static final Pattern MOUNT_POINT_REGEX = Pattern.compile("(//.*?/.*?)/.*");
	/** A white space splitter regex */
	public static final Pattern WHITE_SPACE_EXPR = Pattern.compile("\\s+");
	/** A display name splitter for main class names */
	public static final Pattern SLASH_AND_DOT_EXPR = Pattern.compile("\\\\|/|\\.|\\s+");
	/** A display name splitter for main jars */
	public static final Pattern SLASH_EXPR = Pattern.compile("\\\\|/|\\s+");
	/** The assigned cleaned name if one cannot be determined */
	public static final String UNKNOWN = "Unknown";
	/** The MBeanServer builder override system property */
	public static final String MBEANSERVER_BUILDER_PROP = "javax.management.builder.initial";
	/** The agent property name where we store an exception message on an agent deploy failure */
	public static final String AGENT_DEPLOY_ERR_PROP = "javax.management.agent.deploy.error";
	/** The agent deploy failure message delimiter */
	public static final String AGENT_DEPLOY_ERR_DELIM = "\t~";
	/** The System prop/env variable override for the JMX notification thread pool size */
	public static final String NOTIF_THREAD_POOL_PROP = "javax.management.agent.deploy.error";
	/** The default JMX notification thread pool size */
	public static final int NOTIF_THREAD_POOL_SIZE = 2;
	/** The JMX notification id serial */
	public static final AtomicLong notificationSerial = new AtomicLong(0L);
	
	/** The JVM attached notification type prefix*/
	public static String JVM_ATTACH_NOTIF_PREFIX = "org.helios.jvms.attach";	
	/** The JVM attached notification type */
	public static String JVM_ATTACHED_NOTIF = JVM_ATTACH_NOTIF_PREFIX + ".attached";
	/** The JVM detached notification type */
	public static String JVM_DETACHED_NOTIF = JVM_ATTACH_NOTIF_PREFIX + ".detached";
	/** The JVM attach exception notification type */
	public static String JVM_ATTACH_EX_NOTIF = JVM_ATTACH_NOTIF_PREFIX + ".exception";
	
	
	/** The JVM connected notification type prefix*/
	public static String JVM_CONNECT_NOTIF_PREFIX = "org.helios.jvms.connect";		
	/** The JVM connected notification type */
	public static String JVM_CONNECTED_NOTIF = JVM_CONNECT_NOTIF_PREFIX + ".connected";
	/** The JVM disconnected notification type */
	public static String JVM_DISCONNECT_NOTIF = JVM_CONNECT_NOTIF_PREFIX + ".disconnected";
	/** The JVM connection exception notification type */
	public static String JVM_CONNECTED_EX_NOTIF = JVM_CONNECT_NOTIF_PREFIX + ".exception";	

	/** The JVM mount notification type prefix*/
	public static String JVM_MOUNT_NOTIF_PREFIX = "org.helios.jvms.mount";		
	/** The JVM MBeanServer mounted notification type */
	public static String JVM_MOUNTED_NOTIF = JVM_MOUNT_NOTIF_PREFIX + ".mounted";
	/** The JVM MBeanServer unmounted notification type */
	public static String JVM_UNMOUNTED_NOTIF = JVM_MOUNT_NOTIF_PREFIX + ".unmounted";
	/** The JVM MBeanServer mount exception notification type */
	public static String JVM_MOUNTED_EX_NOTIF = JVM_MOUNT_NOTIF_PREFIX + ".exception";
	
	
	/** The notifications emitted by a JVM MBean */
	protected static final MBeanNotificationInfo[] NOTIF_TYPES = new MBeanNotificationInfo[]{
		new MBeanNotificationInfo(new String[]{JVM_ATTACHED_NOTIF, JVM_DETACHED_NOTIF, JVM_ATTACH_EX_NOTIF}, Notification.class.getName(), "Notification emitted when a JVM's attached state changes"),
		new MBeanNotificationInfo(new String[]{JVM_CONNECTED_NOTIF, JVM_DISCONNECT_NOTIF, JVM_CONNECTED_EX_NOTIF}, Notification.class.getName(), "Notification emitted when a JVM's connected state changes"),
		new MBeanNotificationInfo(new String[]{JVM_MOUNTED_NOTIF, JVM_UNMOUNTED_NOTIF, JVM_MOUNTED_EX_NOTIF}, Notification.class.getName(), "Notification emitted when a JVM's mounted state changes")
	};
	
	/** Shared JMX notification pool */
	protected static final Executor notificationThreadPool = Executors.newFixedThreadPool(
			ConfigurationHelper.getIntSystemThenEnvProperty(NOTIF_THREAD_POOL_PROP, NOTIF_THREAD_POOL_SIZE), new ThreadFactory(){
		private final AtomicInteger serial = new AtomicInteger(0);
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "JVMAttachNotificationThread#" + serial.incrementAndGet());
			t.setDaemon(true);
			return t;
		}
	});

	/** The virtual machine instance for this JVM */
	protected VirtualMachine virtualMachine;
	/** The virtual machine descriptor for this JVM */
	protected final VirtualMachineDescriptor descriptor;
	/** The cleaned display name */
	protected final String cleanDisplayName;
	/** The ObjectName that this instance will be registered with */
	protected final ObjectName objectName;
	/** Indicates if this JVM is attached */
	protected final AtomicBoolean attached = new AtomicBoolean(false);
	/** The most recent attach exception */
	protected final AtomicReference<Throwable> lastAttachException = new AtomicReference<Throwable>(null);
	/** The most recent attach timestamp */
	protected final AtomicLong lastAttachTime = new AtomicLong(-1L); 
	/** The most recent detach timestamp */
	protected final AtomicLong lastDetachTime = new AtomicLong(-1L); 
	
	/**
	 * Creates a new JVM
	 * @param descriptor The Attach API supplied {@link VirtualMachineDescriptor}
	 */
	public JVM(VirtualMachineDescriptor descriptor) {
		super(notificationThreadPool, NOTIF_TYPES);
		this.descriptor = descriptor;
		cleanDisplayName = cleanDisplayName(descriptor);
		objectName = JMXHelper.objectName("org.helios.jvms", "main", cleanDisplayName, "pid", descriptor.id());
		JMXHelper.registerMBean(this, objectName);
	}
	
	/**
	 * Sends a JMX notification
	 * @param notifType The notification type
	 * @param message  The notification message
	 * @param userData The user data for the notification
	 */
	protected void jvmNotification(String notifType, String message, Serializable userData) {
		Notification notif = new Notification(notifType, objectName, notificationSerial.incrementAndGet(), System.currentTimeMillis(), message);
		if(userData!=null) notif.setUserData(userData);
		this.sendNotification(notif);
	}
	
	/**
	 * Sends a JMX notification
	 * @param notifType The notification type
	 * @param message  The notification message
	 */
	protected void jvmNotification(String notifType, String message) {
		jvmNotification(notifType, message, null);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.JVMMBean#isAttached()
	 */
	@Override
	public boolean isAttached() {
		return attached.get();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.JVMMBean#attach()
	 */
	@Override
	public void attach() throws Exception {
		if(!attached.get()) {
			synchronized(descriptor) {
				try {
					virtualMachine = VirtualMachine.attach(descriptor);
					attached.set(true);
					lastAttachTime.set(System.currentTimeMillis());
					jvmNotification(JVM_ATTACHED_NOTIF, "Attached to JVM [" + descriptor.displayName() + "/" + virtualMachine.id() + "]");
				} catch (Throwable t) {
					lastAttachException.set(t);
					jvmNotification(JVM_ATTACH_EX_NOTIF, "JVM Attach Failed for [" + descriptor.displayName() + "/" + virtualMachine.id() + "]", t);					
					t.printStackTrace(System.err);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.AttachServiceMBean#detach()
	 */
	@Override
	public void detach() {
		if(attached.get()) {
			synchronized(descriptor) {
				try { virtualMachine.detach(); } catch (Exception ex) {}
				attached.set(false);
				lastDetachTime.set(System.currentTimeMillis());
				jvmNotification(JVM_DETACHED_NOTIF, "JVM Detached [" + descriptor.displayName() + "/" + virtualMachine.id() + "]");
			}
		}						
	}
	
	/**
	 * Tests the attach state of the JVM.
	 * If the test fails, throws a RuntimeException and marks the JVM detached.
	 * @return true if the JVM is still attached, false otherwise
	 */
	protected boolean tesAttachState() {
		if(!attached.get()) return false;
		try {
			virtualMachine.getAgentProperties();
			return true;
		} catch (Exception ex) {
			lastAttachException.set(ex);
			detach();
			return false;
		}
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.JVMMBean#getAgentProperties()
	 */
	@Override
	public Properties getAgentProperties() {
		if(!attached.get()) return null;		
		return virtualMachine.getAgentProperties();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.JVMMBean#getSystemProperties()
	 */
	@Override
	public Properties getSystemProperties() {
		if(!attached.get()) return null;
		return virtualMachine.getSystemProperties();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.JVMMBean#getDisplayName()
	 */
	@Override
	public String getDisplayName() {		
		return descriptor.displayName();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.JVMMBean#getId()
	 */
	@Override
	public String getId() {
		return descriptor.id();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.JVMMBean#getProviderName()
	 */
	@Override
	public String getProviderName() {		
		return descriptor.provider().name();
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.JVMMBean#getProviderType()
	 */
	@Override
	public String getProviderType() {		
		return descriptor.provider().type();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.JVMMBean#getLocalConnectorAddress()
	 */
	@Override
	public String getLocalConnectorAddress() {
		if(!attached.get()) return null;
		try {
			return virtualMachine.getAgentProperties().getProperty(JMX_CONN_ADDR);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			return null;
		}
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.JVMMBean#getMainClass()
	 */
	@Override
	public String getMainClass() {
		if(!attached.get()) return null;
		return virtualMachine.getAgentProperties().getProperty(JVM_MAIN_CLASS);
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.JVMMBean#getJavaVersion()
	 */
	@Override
	public String getJavaVersion() {
		if(!attached.get()) return null;
		return virtualMachine.getAgentProperties().getProperty(JVM_JAVA_VERSION);
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.JVMMBean#getHeliosAgentName()
	 */
	@Override
	public String getHeliosAgentName() {		
		if(!attached.get()) return null;
		return virtualMachine.getAgentProperties().getProperty(JVM_HELIOS_NAME);
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.JVMMBean#getCleanDisplayName()
	 */
	@Override
	public String getCleanDisplayName() {
		return cleanDisplayName;
	}

	
	
	
	/**
	 * Determines if the passed display name has a recognized archive as the first argument in the display name
	 * @param displayName The display name to test
	 * @return true if the first arg of the display name is a recognized archive, false otherwise
	 */
	public static boolean hasArchiveMain(String displayName) {
		if(displayName==null || displayName.trim().isEmpty()) return false;
		String[] frags = SLASH_AND_DOT_EXPR.split(displayName);
		if(frags!=null && frags.length>0) {
			String ext = frags[frags.length-1];
			if(ext!=null && !ext.trim().isEmpty()) {
				return ARCHIVE_NAMES.contains(ext.trim().toLowerCase());
			}
		} 
		return false;
	}

	/**
	 * Examines the VM display name (command line main class) and attempts to clean it up for use as a key
	 * @param vmd The VirtualMachineDescriptor of the target JVM
	 * @return The cleaned name, or <b><code>"Unknown"</code></b> if the display name did not conform to a recognized pattern
	 */
	public static String cleanDisplayName(VirtualMachineDescriptor vmd) {
		if(vmd==null) return UNKNOWN;
		String display = vmd.displayName();
		String[] dfragments = WHITE_SPACE_EXPR.split(display);
		if(dfragments.length>0 && dfragments[0]!=null && !dfragments[0].trim().isEmpty()) {
			String name = UNKNOWN;
			if(hasArchiveMain(dfragments[0])) {
				dfragments = SLASH_EXPR.split(dfragments[0]);				
			} else {
				dfragments = SLASH_AND_DOT_EXPR.split(dfragments[0]);								
			}
			name = dfragments[dfragments.length-1];			
			return name;
		}
		return UNKNOWN;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.JVMMBean#getLastAttachException()
	 */
	@Override
	public Throwable getLastAttachException() {
		return lastAttachException.get();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.JVMMBean#getLastAttachDate()
	 */
	@Override
	public Date getLastAttachDate() {
		long ts = lastAttachTime.get();
		if(ts==-1L) return null;
		return new Date(ts);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.JVMMBean#getLastDetachDate()
	 */
	@Override
	public Date getLastDetachDate() {
		long ts = lastDetachTime.get();
		if(ts==-1L) return null;
		return new Date(ts);
	}



	
	
	
}
