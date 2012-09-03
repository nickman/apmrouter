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

import junit.framework.Assert;

import org.helios.apmrouter.metric.AgentIdentity;
import org.helios.apmrouter.metric.ICEMetric;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.trace.ITracer;
import org.helios.apmrouter.trace.TracerFactory;
import org.junit.Test;

/**
 * <p>Title: MetricConstructionTestCase</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.apmrouter.metric.MetricConstructionTestCase</code></p>
 */

public class MetricConstructionTestCase {
	/** The default host */
	protected String defaultHost = AgentIdentity.ID.getHostName();
	/** The default agent */
	protected String defaultAgent = AgentIdentity.ID.getAgentName();
	/** The default tracer */
	protected ITracer tracer = TracerFactory.getTracer();

	private static void log(Object obj) {
		System.out.println(obj);
	}
	private static void loge(Object obj) {
		System.err.println(obj);
	}
	
	/**
	 * Tests the default host and agent name
	 */
	@Test
	public void testDefaultAgentIdentity() {
		Assert.assertEquals("The host name was not [" + defaultHost + "]", defaultHost, tracer.getHost());
		Assert.assertEquals("The agent name was not [" + defaultAgent + "]", defaultAgent, tracer.getAgent());
	}
	
	@Test
	public void testNoNamespaceNames() {
		ICEMetric metric = tracer.trace(0, "Venus", MetricType.LONG);
		Assert.assertNotNull("The metric was null", metric);
		Assert.assertEquals("The host name was not [" + defaultHost + "]", defaultHost, metric.getHost());
		Assert.assertEquals("The agent name was not [" + defaultAgent + "]", defaultAgent, metric.getAgent());
		log(metric.getFQN());
		
	}

}
