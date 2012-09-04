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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * <p>Title: MetricConstructionTestCase</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.apmrouter.metric.MetricConstructionTestCase</code></p>
 */

public class MetricConstructionTestCase extends BaseTestCase {
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
		testNamespaceNames(name, ns);
	}
	
	/**
	 * Tests the basic representation of metric name construction with one namespace
	 */
	@Test
	public void testOneNamespaceNames() {
		final String name = "Moon1";
		final String[] ns = new String[]{"Venus"};
		testNamespaceNames(name, ns);
	}
	
	/**
	 * Tests the basic representation of metric name construction with two namespaces
	 */
	@Test
	public void testTwoNamespaceNames() {
		final String name = "Coordinate";
		final String[] ns = new String[]{"SolarSystem", "Venus"};
		testNamespaceNames(name, ns);
	}
	
	/**
	 * Tests the basic representation of metric name construction with ten namespaces.
	 * Random words courtesy of <a href="http://watchout4snakes.com/creativitytools/randomword/randomwordplus.aspx">Random Word Generator</a>
	 */
	@Test
	public void testTenNamespaceNames() {
		final String name = "Exchequer";
		final String[] ns = new String[]{
				"Tutelage", "Coquette", "Logrolling", "Swimsuit", "Patrolman",
				"Industrialization", "Medulla", "Nighthawk", "Cabelgram", "Espresso"			
		};
		testNamespaceNames(name, ns);
	}
	
	/**
	 * Tests the basic representation of metric name construction with thirty namespaces.
	 * Random words courtesy of <a href="http://watchout4snakes.com/creativitytools/randomword/randomwordplus.aspx">Random Word Generator</a>
	 */
	@Test
	public void testThirtyNamespaceNames() {
		final String name = "Exchequer";
		final String[] ns = new String[]{
				"Tutelage", "Coquette", "Logrolling", "Swimsuit", "Patrolman",
				"Industrialization", "Medulla", "Nighthawk", "Cabelgram", "Espresso",
				"TutelageX", "CoquetteX", "LogrollingX", "SwimsuitX", "PatrolmanX",
				"IndustrializationX", "MedullaX", "NighthawkX", "CabelgramX", "EspressoX",
				"TutelageY", "CoquetteY", "LogrollingY", "SwimsuitY", "PatrolmanY",
				"IndustrializationY", "MedullaY", "NighthawkY", "CabelgramY", "EspressoY"							
				
		};
		testNamespaceNames(name, ns);
	}
	
	/**
	 * Tests the basic representation of metric name construction with twenty mapped namespaces.
	 * Random words courtesy of <a href="http://watchout4snakes.com/creativitytools/randomword/randomwordplus.aspx">Random Word Generator</a>
	 */
	@Test
	public void testTwentyNamespaceMappedNames() {
		final String name = "Exchequer";
		final String[] ns = new String[]{
				"A=Tutelage", "B=Coquette", "C=Logrolling", "D=Swimsuit", "F=Patrolman",
				"G=Industrialization", "H=Medulla", "I=Nighthawk", "J=Cabelgram", "K=Espresso",
				"L=TutelageX", "M=CoquetteX", "N=LogrollingX", "O=SwimsuitX", "P=PatrolmanX",
				"Q=IndustrializationX", "R=MedullaX", "S=NighthawkX", "T=CabelgramX", "U=EspressoX"							
		};
		testNamespaceNames(name, ns);
	}
	
	
	
	/**
	 * Template method for testing namespace and FQN construction
	 * @param name The metric name
	 * @param ns The metric namespace
	 */
	protected void testNamespaceNames(final String name, final String[] ns) {
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
		if(ns.length>0) {
			boolean flat = ns[0].indexOf('=')==-1;
			Assert.assertEquals("The metric was flattness not [" + flat + "]", flat, metric.isFlat());
			Assert.assertEquals("The metric was mapness not [" + !flat + "]", !flat, metric.isMapped());
			if(flat) {
				for(String s: metric.getNamespace()) {
					Assert.assertEquals("The namespace [" + s + "] was not supposed to contain a \"=\"", -1, s.indexOf('='));
				}
			} else {
				for(String s: metric.getNamespace()) {
					Assert.assertNotSame("The namespace [" + s + "] was supposed to contain a \"=\"", -1, s.indexOf('='));
				}
			}
		} else {
			Assert.assertEquals("The metric was flattness not [true]", true, metric.isFlat());
			Assert.assertEquals("The metric was mapness not [false]", false, metric.isMapped());
		}
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
			testTenNamespaceNames();
			testThirtyNamespaceNames();
			testTwentyNamespaceMappedNames();
		}
	}

}
