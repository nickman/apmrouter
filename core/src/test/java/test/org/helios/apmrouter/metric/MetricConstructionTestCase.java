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
import java.util.Arrays;

import org.helios.apmrouter.metric.AgentIdentity;
import org.helios.apmrouter.metric.ICEMetric;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.metric.catalog.ICEMetricCatalog;
import org.helios.apmrouter.trace.ITracer;
import org.helios.apmrouter.trace.TracerFactory;
import org.helios.apmrouter.util.StringHelper;
import org.junit.Assert;
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
	/** The fqn prefix */
	String fqnPrefix = defaultHost + "/" + defaultAgent;
	/** The default tracer */
	protected ITracer tracer = TracerFactory.getTracer();
	
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
	
	/**
	 * Tests the basic representation of metric name construction with no namespace
	 */
	@Test
	public void testNoNamespaceNames() {
		final String name = "Venus";
		final String[] ns = new String[]{};
		ICEMetric metric = tracer.trace(0, name, MetricType.LONG);
		Assert.assertNotNull("The metric was null", metric);
		Assert.assertEquals("The host name was not [" + defaultHost + "]", defaultHost, metric.getHost());
		Assert.assertEquals("The agent name was not [" + defaultAgent + "]", defaultAgent, metric.getAgent());
		String fqn = fqnPrefix + concats(ns) + ICEMetric.NADELIM + name;
		Assert.assertEquals("The fqn was not [" + fqn + "]", fqn, metric.getFQN());
		Assert.assertArrayEquals("The namespace was not [" + "]", ns, metric.getNamespace());
		Assert.assertEquals("The namespaceF was not [" + "]", "", metric.getNamespaceF());
		Assert.assertEquals("The name was not [" + name + "]", name, metric.getName());	
		//log(metric);
	}
	
	/**
	 * Tests the basic representation of metric name construction with one namespace
	 */
	@Test
	public void testOneNamespaceNames() {
		final String name = "Moon1";
		final String[] ns = new String[]{"Venus"};
		ICEMetric metric = tracer.trace(0, name, MetricType.LONG, ns);
		//log(metric.getFQN());
		Assert.assertNotNull("The metric was null", metric);
		Assert.assertEquals("The host name was not [" + defaultHost + "]", defaultHost, metric.getHost());
		Assert.assertEquals("The agent name was not [" + defaultAgent + "]", defaultAgent, metric.getAgent());
		String fqn = fqnPrefix + concats(ns) + ICEMetric.NADELIM + name;
		Assert.assertEquals("The fqn was not [" + fqn + "]", fqn, metric.getFQN());
		Assert.assertArrayEquals("The namespace was not " + Arrays.toString(ns), ns, metric.getNamespace());
		Assert.assertEquals("The namespaceF was not [" + ICEMetric.NSDELIM + ns[0] + "]", ICEMetric.NSDELIM + ns[0], metric.getNamespaceF());
		Assert.assertEquals("The namespace[0] was not [" + ns[0] + "]", ns[0], metric.getNamespace(0));
		Assert.assertEquals("The name was not [" + name + "]", name, metric.getName());			
	}
	
	/**
	 * Tests the basic representation of metric name construction with one namespace
	 */
	@Test
	public void testTwoNamespaceNames() {
		final String name = "Coordinate";
		final String[] ns = new String[]{"SolarSystem", "Venus"};
		ICEMetric metric = tracer.trace(0, name, MetricType.LONG, ns);
		//log(metric.getFQN());
		Assert.assertNotNull("The metric was null", metric);
		Assert.assertEquals("The host name was not [" + defaultHost + "]", defaultHost, metric.getHost());
		Assert.assertEquals("The agent name was not [" + defaultAgent + "]", defaultAgent, metric.getAgent());
		String fqn = fqnPrefix + concats(ns) + ICEMetric.NADELIM + name;
		//log(fqn);
		Assert.assertEquals("The fqn was not [" + fqn + "]", fqn, metric.getFQN());
		Assert.assertArrayEquals("The namespace was not " + Arrays.toString(ns), ns, metric.getNamespace());
		Assert.assertEquals("The namespaceF was not [" + concats(ns) + "]", concats(ns), metric.getNamespaceF());
		Assert.assertEquals("The namespace size was not [" + ns.length + "]", ns.length, metric.getNamespaceSize());
		for(int i = 0; i < ns.length; i++) {
			Assert.assertEquals("The namespace[" + i + "] was not [" + ns[i] + "]", ns[i], metric.getNamespace(i));
		}
		Assert.assertEquals("The name was not [" + name + "]", name, metric.getName());			
	}
	
	
	
	/**
	 * Executes all the standard tests for each metric catalog type
	 * @throws Exception Thrown on any exception
	 */
	@Test
	public void allTestsForEachMetricCatalogType() throws Exception {
		for(String metCat: METRIC_CAT_CLASSES) {
			log("Testing MetCat [" + metCat + "]");
			resetCatalog(metCat);
			testNoNamespaceNames();
			testOneNamespaceNames();
		}
	}

}
