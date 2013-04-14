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

import java.util.Properties;

/**
 * <p>Title: AttachServiceMBean</p>
 * <p>Description: JMX MBean interface for the {@link AttachService}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.satellite.services.attach.AttachServiceMBean</code></p>
 */

public interface AttachServiceMBean {
	/**
	 * Detaches from the virtual machine and unregisters the MBean.
	 */
	public void detach();
	
	/**
	 * Returns the current agent properties in the target virtual machine.
	 * @return The agent properties
	 */
	public Properties getAgentProperties();
	
	/**
	 * Returns the current system properties in the target virtual machine.
	 * @return The system properties
	 */
	public Properties getSystemProperties();
	
	/**
	 * Return the display name assigned to the JVM by the virtual agent descriptor.
	 * @return the JVM descriptor
	 */
	public String getDisplayName();
	
	public String getCleanDisplayName();
	
	/**
	 * Returns the identifier for this Java virtual machine.
	 * @return The identifier for this Java virtual machine.
	 */
	public String getId();
	
	/**
	 * Returns the JVM's attach provider name
	 * @return the JVM's attach provider name
	 */
	public String getProviderName();
	
	/**
	 * Returns the JVM's attach provider type
	 * @return the JVM's attach provider type
	 */
	public String getProviderType();
	
	/**
	 * Returns the local JMX connector address for this VM, or null if one is not installed.
	 * @return the local JMX connector address 
	 */
	public String getLocalConnectorAddress();
	
	/**
	 * Returns the JVM's main class
	 * @return the JVM's main class 
	 */
	public String getMainClass();
	
	/**
	 * Returns the JVM's java version
	 * @return the JVM's java version 
	 */
	public String getJavaVersion();
	
	/**
	 * Returns the name of the helios agent running in the JVM, or null if it is not running
	 * @return the name of the helios agent running in the JVM
	 */
	public String getHeliosAgentName();
	
	/**
	 * Mounts the JVM's cascaded MBeanServer locally using the passed host and agent as cascade naming prefixes.
	 * @param host The host name of the JVM
	 * @param agent The agent name of the JVM
	 */
	public void mount(String host, String agent);
	
	/**
	 * Mounts the JVM's cascaded MBeanServer locally using an implied host and the passed agent as cascade naming prefixes.
	 * @param agent The agent name of the JVM
	 */
	public void mount(String agent);
	
	/**
	 * Unmounts this JVM
	 */
	public void unmount();
	
	/**
	 * Installs the JMX management agent on this JVM
	 */
	public void installManagementAgent();
	
	/**
	 * Installs the Alternate Domain JMX management agent on this JVM
	 * so that non-platform mbeanservers can be accessed through their own connector.
	 */
	public void installAltDomainManagementAgent();	
	
	/**
	 * Returns the mount point name for this jvm's cascade
	 * @return the mount point name for this jvm's cascade or null if it is not mounted
	 */
	public String getMountPoint();


	/**
	 * Returns the mount id for this jvm's cascade
	 * @return the id name for this jvm's cascade or null if it is not mounted
	 */
	public String getMountId();


	
	/**
	 * Mounts the JVM's cascaded MBeanServer locally using an implied host and agent as cascade naming prefixes.
	 */
	public void mount();
	
	
	
	
	
}
