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

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;

import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.metric.AgentIdentity;
import org.helios.apmrouter.satellite.services.cascade.Cascader;
import org.helios.apmrouter.util.SimpleLogger;
import org.helios.vm.VirtualMachine;
import org.helios.vm.VirtualMachineBootstrap;
import org.helios.vm.VirtualMachineDescriptor;

/**
 * <p>Title: AttachService</p>
 * <p>Description: Provides satellite <a href="http://docs.oracle.com/javase/6/docs/technotes/guides/attach/">Attach Services</a></p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.satellite.AttachService</code></p>
 */

public class AttachService implements AttachServiceMBean, NotificationListener, NotificationFilter {
	/**  */
	private static final long serialVersionUID = -2338420252724673586L;
	/** Indicates if this VM was able to load the attach service */
	public static final boolean available;
	/** The Attach service bootstrap */
	protected static final VirtualMachineBootstrap bootstrap;
	
	/** The mounted virtual machines */
	protected final Map<VirtualMachine, String> jvmMountPoints = new ConcurrentHashMap<VirtualMachine, String>();
	
	
	/** The VM id for this JVM */
	public static final String JVM_ID = "" + ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
	
	/** The ObjectName template for AttachService MBean instances */
	public static final String OBJECT_NAME_TEMPLATE = "org.helios.vm:service=JVM,name=%s,id=%s";
	/** The cascade mountpoint template */
	public static final String MOUNT_TEMPLATE = "//%s/%s";
	
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
	
	
	
	
	/** The virtual machine instance for this JVM */
	protected final VirtualMachine virtualMachine;
	/** The virtual machine descriptor for this JVM */
	protected final VirtualMachineDescriptor descriptor;
	/** The cleaned display name */
	protected final String cleanDisplayName;
	/** The ObjectName that this instance will be registered with */
	protected final ObjectName objectName;
	
	/** The mount point for this JVM */
	protected String mountPoint = null;
	/** The mount id for this JVM */
	protected String mountId = null;
	
	/*
	 * 
	
	When a mounted and cascaded JVM stops, an MBeanServerNotification will be emitted by [JMImplementation:type=MBeanServerDelegate]
	The notification type is: JMX.mbean.unregistered
	A cascaded notification will look like:
	javax.management.MBeanServerNotification
		[source=JMImplementation:type=MBeanServerDelegate]
		[type=JMX.mbean.unregistered]
		[message=]
		[mbeanName=//local.hserval/7361/JMImplementation:type=MBeanServerDelegate]

	 */

	/**
	 * Registers all the located JVMs except this one.
	 */
	public static void registerAll() {
		if(!available) return;
		for(VirtualMachineDescriptor vmd : VirtualMachineDescriptor.getVirtualMachineDescriptors()) {
			if(JVM_ID.equals(vmd.id())) continue;
			try {
				VirtualMachine vm = vmd.provider().attachVirtualMachine(vmd);
				new AttachService(vm, vmd);
			} catch (Exception ex) {
				SimpleLogger.error("Failed to register JVM [", vmd.id(), "]", ex);
			}
			
		}
	}
	
	static {
		VirtualMachineBootstrap _bootstrap = null;
		boolean _available = false;
		try {
			_bootstrap = VirtualMachineBootstrap.getInstance();
			_available = true;
			SimpleLogger.info("AttachService Loaded");			
		} catch (Exception ex) {
			_available = false;
			_bootstrap = null;
			SimpleLogger.info("AttachService Not Available");
		}
		available = _available;
		bootstrap = _bootstrap;
	}
	
	/**
	 * Initializes the attach service 
	 */
	public static void initAttachService() {
		if(!available) {
			SimpleLogger.warn("Unable to initialize AttachService.");
			return;
		}
		StringBuilder b = new StringBuilder("Attach Service Located JVMs:");
		for(VirtualMachineDescriptor vmd: VirtualMachine.list()) {
			if(JVM_ID.equals(vmd.id())) {
				b.append("\n\t").append(vmd.id()).append("\t:\t (ME) ").append(vmd.displayName());
			} else {
				b.append("\n\t").append(vmd.id()).append("\t:\t").append(vmd.displayName());
			}
			
		}
		SimpleLogger.info(b);
	}


	/**
	 * Creates a new AttachService
	 * @param virtualMachine The virtual machine instance for this JVM
	 * @param descriptor The virtual machine descriptor for this JVM
	 */
	public AttachService(VirtualMachine virtualMachine, VirtualMachineDescriptor descriptor) {
		if(JVM_ID.equals(descriptor.id())) {
			throw new RuntimeException("DON'T try to register a VirtualMachine inside itself. It's not natural.", new Throwable());
		}
		this.descriptor = descriptor;
		this.virtualMachine = virtualMachine;
		cleanDisplayName = cleanDisplayName(this.descriptor);
		objectName = JMXHelper.objectName(String.format(OBJECT_NAME_TEMPLATE, cleanDisplayName, this.virtualMachine.id()));
		if(!JMXHelper.getHeliosMBeanServer().isRegistered(objectName)) {
			JMXHelper.registerMBean(this, objectName);
		}
		try { JMXHelper.getHeliosMBeanServer().addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, this, this, null); } catch (Exception ex) {}
		if(Cascader.available) mount();
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationFilter#isNotificationEnabled(javax.management.Notification)
	 */
	@Override
	public boolean isNotificationEnabled(Notification notification) {
		if(!(notification instanceof MBeanServerNotification) || !notification.getType().equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) return false;
		return mountPoint!=null && ((MBeanServerNotification)notification).getMBeanName().toString().startsWith(mountPoint);
	}


	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	@Override
	public void handleNotification(Notification notification, Object handback) {
		Matcher matcher = MOUNT_POINT_REGEX.matcher(((MBeanServerNotification)notification).getMBeanName().toString());
		if(matcher.matches() && mountPoint!=null) {
			String _mountPoint = matcher.group(1);
			if(mountPoint.equals(_mountPoint)) {
				mountPoint = null;
				SimpleLogger.info("Cascade Unloaded. Detaching JVM [", virtualMachine.id(), "]");
				detach();
			}					
		}
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.AttachServiceMBean#installManagementAgent()
	 */
	@Override
	public synchronized void installManagementAgent() {
		String connectorAddress = getLocalConnectorAddress();
		if(connectorAddress==null || connectorAddress.trim().isEmpty()) {
			String javaHome = getSystemProperties().getProperty("java.home");
		    String managementAgent = javaHome + "/lib/management-agent.jar";
		    virtualMachine.loadAgent(managementAgent, "com.sun.management.jmxremote");
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.AttachServiceMBean#installAltDomainManagementAgent()
	 */
	@Override
	public synchronized void installAltDomainManagementAgent() {
		String agentFile = null;
		try {
			agentFile = AlternateDomainAgent.writeAgentJar();
		} catch (Exception e) {
			throw new RuntimeException("Failed to create AlternateDomainAgent jar", e);
		}
		// FIXME: Should only do this once
		try {
			virtualMachine.loadAgent(agentFile, "");
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.AttachServiceMBean#mount(java.lang.String, java.lang.String)
	 */
	@Override
	public void mount(String host, String agent) {
		if(!jvmMountPoints.containsKey(virtualMachine)) {
			synchronized(jvmMountPoints) {
				if(!jvmMountPoints.containsKey(virtualMachine)) {
					if(!Cascader.available) throw new RuntimeException("The cascading service is not available");
					if(host==null || host.trim().isEmpty()) host = AgentIdentity.ID.getHostName();
					if(agent==null || agent.trim().isEmpty()) {
						agent = getHeliosAgentName();
						if(agent==null || agent.trim().isEmpty()) {
							agent = getId();
						}			
					}
					mountPoint = String.format(MOUNT_TEMPLATE, host, cleanDisplayName);
					String connectorAddress = getLocalConnectorAddress();
					try {
						if(connectorAddress==null || connectorAddress.trim().isEmpty()) {
							installManagementAgent();
							connectorAddress = getLocalConnectorAddress();
						}
						JMXServiceURL serviceURL = new JMXServiceURL(connectorAddress);
						mountId = Cascader.mount(serviceURL, null, null, mountPoint);
						jvmMountPoints.put(virtualMachine, mountPoint);
					} catch (Exception ex) {
						ex.printStackTrace(System.err);
						throw new RuntimeException("Failed to mount JVM [" + virtualMachine.id() + "]", ex);
					}					
				} else {					
					throw new RuntimeException("JVM [" + virtualMachine.id() + "] is already mounted");
				}
			}
		} else {
			throw new RuntimeException("JVM [" + virtualMachine.id() + "] is already mounted");
		}		
	}
	

	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.AttachServiceMBean#mount(java.lang.String)
	 */
	@Override
	public void mount(String agent) {
		mount(null, agent);
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.AttachServiceMBean#mount()
	 */
	@Override
	public void mount() {
		mount(null, null);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.AttachServiceMBean#unmount()
	 */
	@Override
	public void unmount() {
		Cascader.unmount(mountId);
		jvmMountPoints.remove(virtualMachine);
		mountId = null;
		mountPoint = null;
		
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.AttachServiceMBean#detach()
	 */
	@Override
	public void detach() {
		virtualMachine.detach();
		jvmMountPoints.remove(virtualMachine);
		JMXHelper.unregisterMBean(objectName);
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.AttachServiceMBean#getAgentProperties()
	 */
	@Override
	public Properties getAgentProperties() {
		return virtualMachine.getAgentProperties();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.AttachServiceMBean#getSystemProperties()
	 */
	@Override
	public Properties getSystemProperties() {
		return virtualMachine.getSystemProperties();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.AttachServiceMBean#getDisplayName()
	 */
	@Override
	public String getDisplayName() {		
		return descriptor.displayName();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.AttachServiceMBean#getId()
	 */
	@Override
	public String getId() {
		return virtualMachine.id();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.AttachServiceMBean#getProviderName()
	 */
	@Override
	public String getProviderName() {
		return virtualMachine.provider().name();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.AttachServiceMBean#getProviderType()
	 */
	@Override
	public String getProviderType() {		
		return virtualMachine.provider().type();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.AttachServiceMBean#getLocalConnectorAddress()
	 */
	@Override
	public String getLocalConnectorAddress() {		
		return virtualMachine.getAgentProperties().getProperty(JMX_CONN_ADDR);
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.AttachServiceMBean#getMainClass()
	 */
	@Override
	public String getMainClass() {		
		return virtualMachine.getAgentProperties().getProperty(JVM_MAIN_CLASS);
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.AttachServiceMBean#getJavaVersion()
	 */
	@Override
	public String getJavaVersion() {		
		return getSystemProperties().getProperty(JVM_JAVA_VERSION);
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.AttachServiceMBean#getHeliosAgentName()
	 */
	@Override
	public String getHeliosAgentName() {		
		return getSystemProperties().getProperty(JVM_HELIOS_NAME);
	}



	/**
	 * Returns the mount point name for this jvm's cascade
	 * @return the mount point name for this jvm's cascade or null if it is not mounted
	 */
	@Override
	public String getMountPoint() {
		return mountPoint;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.satellite.services.attach.AttachServiceMBean#getCleanDisplayName()
	 */
	@Override
	public String getCleanDisplayName() {
		return cleanDisplayName;
	}


	/**
	 * Returns the mount id for this jvm's cascade
	 * @return the id name for this jvm's cascade or null if it is not mounted
	 */
	@Override
	public String getMountId() {
		return mountId;
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



	
}
