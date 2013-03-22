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
package org.helios.apmrouter.satellite;

import org.helios.apmrouter.jmx.ConfigurationHelper;
import org.helios.apmrouter.monitor.DefaultMonitorBoot;
import org.helios.apmrouter.util.SimpleLogger;

import static org.helios.apmrouter.sender.SenderFactory.DEFAULT_SENDER_URI;
import static org.helios.apmrouter.sender.SenderFactory.SENDER_URI_PROP;

/**
 * <p>Title: Satellite</p>
 * <p>Description: Command line entry point to launch Satellite</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.satellite.Satellite</code></p>
 */

public class Satellite {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String uri = ConfigurationHelper.getSystemThenEnvProperty(SENDER_URI_PROP, DEFAULT_SENDER_URI);
		String agent = ConfigurationHelper.getSystemThenEnvProperty("org.helios.agent", "satellite");
		System.setProperty("org.helios.agent", agent);
		StringBuilder b = new StringBuilder("\n\t========================================================================");
		b.append("\n\tStarting Helios APMRouter Satellite Agent v ").append(Satellite.class.getPackage().getImplementationVersion());
		b.append("\n\tHelios APMRouter Server:").append(uri);
		b.append("\n\tHelios Agent Name:").append(agent);
		b.append("\n\t========================================================================\n");
		SimpleLogger.info(b.toString());
		//System.setProperty(SENDER_URI_PROP, uri);
		//SenderFactory.getInstance().getDefaultSender().sendHello();
		//SystemClock.sleep(2000);
		SimpleLogger.info("Starting Satellite Monitors");
		DefaultMonitorBoot.satellite();
		try { Thread.currentThread().join(); } catch (Exception ex) {/* No Op */}
	}

}
