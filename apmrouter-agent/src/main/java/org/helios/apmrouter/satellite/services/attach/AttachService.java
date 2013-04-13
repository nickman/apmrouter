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

public class AttachService {
	/** Indicates if this VM was able to load the attach service */
	public static final boolean available;
	/** The Attach service bootstrap */
	protected static final VirtualMachineBootstrap bootstrap;
	/** The VM id for this JVM */
	public static final String JVM_ID = "" + ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
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
}
