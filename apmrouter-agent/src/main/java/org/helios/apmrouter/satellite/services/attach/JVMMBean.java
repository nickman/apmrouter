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

import java.util.Date;
import java.util.Properties;

/**
 * <p>Title: JVMMBean</p>
 * <p>Description: MBean interface for {@link JVM}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.satellite.services.attach.JVMMBean</code></p>
 */

public interface JVMMBean {
	
	/**
	 * Attaches to this JVM
	 * @throws Exception thrown on any error
	 */
	public void attach() throws Exception;
	
	/**
	 * Indicates if this JVM is attached
	 * @return true if attached, false otherwise
	 */
	public boolean isAttached();
	
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
	
	/**
	 * Returns the cleaned display name for the vm
	 * @return the cleaned display name for the vm
	 */
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
	 * Returns the last attach exception or null if one has never occured
	 * @return the last attach exception
	 */
	public Throwable getLastAttachException();

	/**
	 * Returns the last attach date 
	 * @return the last attach date or null if an attach has never occured
	 */
	public Date getLastAttachDate();

	/**
	 * Returns the last detach date 
	 * @return the last detach date or null if a detach has never occured
	 */
	public Date getLastDetachDate();
	

}
