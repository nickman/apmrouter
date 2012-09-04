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
package test.org.helios.apmrouter.metric;

import java.lang.reflect.Method;

import org.helios.apmrouter.metric.AgentIdentity;
import org.helios.apmrouter.metric.catalog.ICEMetricCatalog;
import org.helios.apmrouter.trace.ITracer;
import org.helios.apmrouter.trace.TracerFactory;
import org.helios.apmrouter.util.StringHelper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TestName;

/**
 * <p>Title: BaseTestCase</p>
 * <p>Description: Base test class</p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.apmrouter.metric.BaseTestCase</code></p>
 */
@Ignore
public class BaseTestCase {
	/** The default host */
	protected String defaultHost = AgentIdentity.ID.getHostName();
	/** The default agent */
	protected String defaultAgent = AgentIdentity.ID.getAgentName();
	/** The fqn prefix */
	String fqnPrefix = defaultHost + "/" + defaultAgent;
	/** The default tracer */
	protected ITracer tracer = TracerFactory.getTracer();
	/** The currently executing test name */
	@Rule public final TestName name = new TestName();

	/**
	 * Prints the name of the current catalog and the current test
	 */
	@Before
	public void printTestName() {
		String scn = "<None>";
		String cn = ICEMetricCatalog.getInstance().getCatalogClassName();
		if(cn!=null) {
			String[] frags = cn.split("\\.");
			scn = frags[frags.length-1];
		}
		
		log("[" + scn  + "]-->" + name.getMethodName());
	}
	
	/** The metric catalog impl classes to test */
	protected static final String[] METRIC_CAT_CLASSES = new String[]{
		"org.helios.apmrouter.metric.catalog.heap.StringKeyedHeapMetricCatalog", 
		"org.helios.apmrouter.metric.catalog.heap.LongKeyedHeapMetricCatalog",
		"org.helios.apmrouter.metric.catalog.direct.StringKeyedChronicleMetricCatalog",
		"org.helios.apmrouter.metric.catalog.direct.LongKeyedChronicleMetricCatalog"
	};
	
	/** The reflection invoked method to reset the metric catalog */
	protected volatile Method catalogResetMethod = null;
	/**
	 * Resets the metric catalog
	 * @param newClassName The new metric catalog impl class name
	 * @throws Exception Thrown on any exception
	 */
	protected void resetCatalog(String newClassName) throws Exception {
		// reset(String newClassName)
		if(catalogResetMethod==null) {
			catalogResetMethod = ICEMetricCatalog.class.getDeclaredMethod("reset", String.class);
			catalogResetMethod.setAccessible(true);
		}
		catalogResetMethod.invoke(null, newClassName);
	}
	
	/**
	 * Concats namespace names 
 	 * @param names The names to concat
	 * @return the concated names
	 */
	public static String concats(String...names) {
		return StringHelper.fastConcatAndDelim("/", names);
	}

	protected static void log(Object obj) {
		System.out.println(obj);
	}
	protected static void loge(Object obj) {
		System.err.println(obj);
	}
	

}
