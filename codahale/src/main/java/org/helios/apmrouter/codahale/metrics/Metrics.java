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
package org.helios.apmrouter.codahale.metrics;

import java.util.concurrent.TimeUnit;

import org.helios.apmrouter.codahale.helios.HeliosReporter;
import org.helios.apmrouter.jmx.ConfigurationHelper;

import com.yammer.metrics.core.MetricPredicate;
import com.yammer.metrics.core.MetricsRegistry;

/**
 * <p>Title: Metrics</p>
 * <p>Description: Replacement default registry that does not start a JMX Reporter</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.codahale.metrics.Metrics</code></p>
 */

public class Metrics {
	private static final MetricsRegistry DEFAULT_REGISTRY;// = new MetricsRegistry();
	
	/** The system property indicating if metrics should be mapped (value should be "true" or "false") Default is true */
	public static final String MAPPED_PROP = "org.helios.codahale.mapped";
	
	static {
		System.out.println("======= Initializing Default Metrics Registry =======");
		System.out.flush();
		try {
			DEFAULT_REGISTRY = new MetricsRegistry();
			boolean mapped = ConfigurationHelper.getBooleanSystemThenEnvProperty(MAPPED_PROP, true);
			HeliosReporter.enable(DEFAULT_REGISTRY, 15000, TimeUnit.MILLISECONDS, MetricPredicate.ALL, mapped); 
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			throw new RuntimeException("Failed to initialize Metrics", t);
		}
	}
	
	private Metrics() { /* unused */ }
	
	   /**
     * Returns the (static) default registry.
     *
     * @return the metrics registry
     */
    public static MetricsRegistry defaultRegistry() {
        return DEFAULT_REGISTRY;
    }
}
