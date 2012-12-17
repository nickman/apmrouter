/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2012, Helios Development Group and individual contributors
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
package org.helios.apmrouter.byteman;

import org.helios.apmrouter.trace.ITracer;
import org.helios.apmrouter.trace.TracerFactory;
import org.jboss.byteman.rule.Rule;
import org.jboss.byteman.rule.RuleMBean;
import org.jboss.byteman.rule.helper.Helper;

/**
 * <p>Title: APMAgentHelper</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.APMAgentHelper</code></p>
 */

public class APMAgentHelper extends Helper {
	/** The {@link ITracer} instance created when this helper is activated */
	protected static ITracer itracer = null;
	
	/**
	 * Creates a new APMAgentHelper
	 * @param rule The rules the helper is created for
	 */
	public APMAgentHelper(Rule rule) {
		super(rule);
	}
	
	/**
	 * Called when the first instance of this helper class is instantiated for an active rule
	 */
	public static void activated() {
		itracer = TracerFactory.getTracer();
	}
	
	/**
	 * Called when the last rule using this helper class is uninstalled
	 */
	public static void deactivated() {
		/* */
	}
	
	
	/**
	 * Called when a rule using this helper is installed
	 * @param rule The rule that this helper was instantiated for
	 */
	public static void installed(Rule rule) {
		rule.getRuleScript();
	}
	
	/**
	 * Called when a rule using this helper is uninstalled
	 * @param rule The rule that this helper was instantiated for
	 */
	public static void uninstalled(Rule rule) {
		/* */
	}
	

}
