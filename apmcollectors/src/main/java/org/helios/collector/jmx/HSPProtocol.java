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
package org.helios.collector.jmx;

import java.text.MessageFormat;
import javax.management.ObjectName;
import org.helios.apmrouter.jmx.JMXHelper;

/**
 * <p>Title: HSPProtocol</p>
 * <p>Description: Enumerates the HSP sub-protocols and their configuration options</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.collectors.jmx.HSPProtocol</code></p>
 */

public enum HSPProtocol {
	/**
	 * <p>Provides MBeanServerConnections to a local In-VM Proxy to a remote MBeanServer. Uses the passed ObjectName and boolean to populate the template with:<ol>
	 * <li>The host name</li>
	 * <li>The VM Id</li>
	 * <li>The MBeanServer's DefaultDomain</li>
	 * <li>Shared indicator</li>
	 * <li>Boolean indicating if the connection should proxy pooled (or dedicated) connections.<b>(Optional. Defaults to true)</b></li>
	 * </ol>
	 */
	hsp("Provides MBeanServerConnections to a local In-VM Proxy to a remote MBeanServer", "service:jmx:hsp://{0}/{1}/{2}?shared={3}", 2, new LocalProxyHSPURLFactory()),
	/**
	 * <p>Provides MBeanServerConnections to a local In-VM MBeanServer. Uses the passed ObjectName to populate the template with:<ol>
	 * <li>The MBeanServer's DefaultDomain</li>
	 * </ol>
	 */
	lhsp("Provides MBeanServerConnections to a local In-VM MBeanServer", "service:jmx:lhsp://{0}", 2, new LocalHSPURLFactory()),
	/**
	 * <p>Provides MBeanServerConnections to a remote In-VM Proxy to a remote MBeanServer. Uses the passed remoting JMXServiceURL and ObjectName to populate the template with:<ol>
	 * 	<li>The remoting JMXServiceURL</li>
	 * 	<li>The MBean ObjectName of the MBeanServerConnectionFactory</li>
	 * </ol>
	 */
	rhsp("Provides MBeanServerConnections to a remote In-VM Proxy to a remote MBeanServer", "{0}/{1}", 2, new RemoteProxyHSPURLFactory());
	
	/**
	 * Creates a new HSPProtocol
	 * @param description A description of the HSP sub-protocol
	 * @param template The JMXService URL template for the sub-protocol
	 * @param bindCount The minimum number of template items required to populate a JMXService URL template
	 * @param urlFactory The HSP JMXServiceURL factory
	 */
	private HSPProtocol(String description, String template, int bindCount, HSPURLFactory urlFactory) {
		this.description = description;
		this.template = template;
		this.bindCount = bindCount;
		this.urlFactory = urlFactory;
	}
	
	/** A description of the HSP sub-protocol */
	private final String description;
	/** The JMXService URL template for the sub-protocol */
	private final String template;
	/** The minimum number of template items required to populate a JMXService URL template */
	private final int bindCount;
	/** The URL factory */
	private final HSPURLFactory urlFactory;
	
	/**
	 * Generates a full JMXServieURL in the form of a string for the passed parameters for this sub-protocol.
	 * @param params The values used to populate the template
	 * @return a full JMXServieURL in the form of a string
	 */
	public String formatServiceURL(Object...params) {
		if(params==null || params.length < bindCount) {
			throw new RuntimeException("Invalid argument count for protocol [" + this.name() + "]. Mandatory parameter count [" + bindCount + "]. Format [" + template + "]", new Throwable() );
		}
		return urlFactory.processArgs(template, params);
	}
	
	
	/**
	 * A description of the HSP sub-protocol
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * The JMXService URL template for the sub-protocol 
	 * @return the template
	 */
	public String getTemplate() {
		return template;
	}
	
	/**
	 * <p>Title: HSPURLFactory</p>
	 * <p>Description: Defines a class that generates an HSP service URL string for the passed JMX ObjectName of the target MBeanServerConnectionFactory</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 */
	private static interface HSPURLFactory {
		/**
		 * generates an HSP service URL string for the passed JMX ObjectName of the target MBeanServerConnectionFactory
		 * @param connectionFactory The connection factory ObjectName
		 * @param template The HSP URL template
		 * @return an HSP JMXServiceURL string
		 */
		public String getServiceURL(ObjectName connectionFactory, String template);
		/**
		 * Comforts the generic arguments to adapt for the <code>getServiceURL</code> call. 
		 * @param template The HSP URL template
		 * @param args The egenric arguments.
		 * @return an HSP JMXServiceURL string
		 */
		public String processArgs(String template, Object...args);
	}
	
	/**
	 * <p>Title: LocalHSPURLFactory</p>
	 * <p>Description: Generates an LHSP URL string for a connection to an in-vm MBeanServer.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 */
	private static class LocalHSPURLFactory implements HSPURLFactory {
		/**
		 * generates an LHSP service URL string for the passed JMX ObjectName of the target MBeanServerConnectionFactory
		 * @param connectionFactory The connection factory ObjectName
		 * @return an LHSP JMXServiceURL string
		 */
		public String getServiceURL(ObjectName connectionFactory, String template) {
			return MessageFormat.format(template, connectionFactory.getKeyProperty("domain"));
		}

		/**
		 * Comforts the generic arguments to adapt for the <code>getServiceURL</code> call. 
		 * @param template The HSP URL template
		 * @param args The egenric arguments.
		 * @return an HSP JMXServiceURL string
		 */
		public String processArgs(String template, Object...args) {
			return getServiceURL((ObjectName)args[0], template);
		}
		
	}
	
	/**
	 * <p>Title: LocalProxyHSPURLFactory</p>
	 * <p>Description: Generates an HSP URL string for a connection to an in-vm MBeanServerConnectionFactory.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 */
	private static class LocalProxyHSPURLFactory implements HSPURLFactory {
		/**
		 * generates an HSP service URL string for the passed JMX ObjectName of the target MBeanServerConnectionFactory
		 * @param connectionFactory The connection factory ObjectName
		 * @return an HSP JMXServiceURL string
		 */
		public String getServiceURL(ObjectName connectionFactory, String template) {
			return MessageFormat.format(template, connectionFactory.getKeyProperty("host"), connectionFactory.getKeyProperty("vm"), connectionFactory.getKeyProperty("domain"), connectionFactory.getKeyProperty("shared"));
		}
		
		/**
		 * Comforts the generic arguments to adapt for the <code>getServiceURL</code> call. 
		 * @param template The HSP URL template
		 * @param args The egenric arguments.
		 * @return an HSP JMXServiceURL string
		 */
		public String processArgs(String template, Object...args) {
			ObjectName shared;
			if(args.length==1) {
				shared = JMXHelper.objectName(args[0].toString() + ",shared=true");
			} else if(args.length==2) {
				shared = JMXHelper.objectName(args[0].toString() + ",shared=" + Boolean.parseBoolean(args[1].toString()));
			} else {
				throw new RuntimeException("Invalid number of arguments:" + args.length);
			}
			return getServiceURL(shared, template);
		}
		
	}
	
	/**
	 * <p>Title: RemoteProxyHSPURLFactory</p>
	 * <p>Description: Generates an RHSP URL string for a connection to a remote MBeanServerConnectionFactory.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 */
	private static class RemoteProxyHSPURLFactory implements HSPURLFactory {
		/**
		 * generates an RHSP service URL string for the passed JMX ObjectName of the target MBeanServerConnectionFactory
		 * @param connectionFactory The connection factory ObjectName
		 * @return an RHSP JMXServiceURL string
		 */
		public String getServiceURL(ObjectName connectionFactory, String template) {
			return MessageFormat.format(template, connectionFactory.toString());
		}
		
		/**
		 * Comforts the generic arguments to adapt for the <code>getServiceURL</code> call. 
		 * @param template The HSP URL template
		 * @param args The egenric arguments.
		 * @return an HSP JMXServiceURL string
		 */
		public String processArgs(String template, Object...args) {
			return getServiceURL((ObjectName)args[1], MessageFormat.format(template, (String)args[0], "{0}"));
		}
		
	}
	
	
}
