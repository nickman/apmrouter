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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.metric.ICEMetric;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.trace.ITracer;
import org.helios.apmrouter.trace.TracerFactory;
import org.helios.apmrouter.util.SimpleLogger;
import org.jboss.byteman.agent.RuleScript;
import org.jboss.byteman.rule.Rule;
import org.jboss.byteman.rule.helper.Helper;
import org.snmp4j.PDU;


/**
 * <p>Title: APMAgentHelper</p>
 * <p>Description: An APM Agent {@link ITracer} implementation exposed as a  byteman {@link Helper}, plus a handful of supporting utility constructs.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.APMAgentHelper</code></p>
 */

public class APMAgentHelper extends Helper implements ITracer {
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
		RuleScript ruleMBean = rule.getRuleScript();
		try {
			if(!JMXHelper.getHeliosMBeanServer().isRegistered(ruleMBean.getObjectName())) {
				JMXHelper.getHeliosMBeanServer().registerMBean(ruleMBean, ruleMBean.getObjectName());
			}
		} catch (Exception ex) {
			SimpleLogger.warn("Failed to register Rule MBean for [", ruleMBean, "]", ex);
		}
	}
	
	/**
	 * Called when a rule using this helper is uninstalled
	 * @param rule The rule that this helper was instantiated for
	 */
	public static void uninstalled(Rule rule) {
		RuleScript ruleMBean = rule.getRuleScript();
		try {
			if(JMXHelper.getHeliosMBeanServer().isRegistered(ruleMBean.getObjectName())) {
				JMXHelper.getHeliosMBeanServer().unregisterMBean(ruleMBean.getObjectName());
			}
		} catch (Exception ex) {
			SimpleLogger.warn("Failed to unregister Rule MBean for [", ruleMBean, "]", ex);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#resetStats()
	 */
	@Override
	public void resetStats() {
		itracer.resetStats();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#trace(java.lang.Object, java.lang.CharSequence, org.helios.apmrouter.metric.MetricType, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric trace(Object value, CharSequence name, MetricType type,
			CharSequence... namespace) {
		return itracer.trace(value, name, type, namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceDirect(long, java.util.concurrent.TimeUnit, java.lang.Object, java.lang.CharSequence, org.helios.apmrouter.metric.MetricType, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceDirect(long timeout, TimeUnit unit, Object value,
			CharSequence name, MetricType type, CharSequence... namespace) {
		return itracer.traceDirect(timeout, unit, value, name, type, namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceDirect(java.lang.Object, java.lang.CharSequence, org.helios.apmrouter.metric.MetricType, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceDirect(Object value, CharSequence name,
			MetricType type, CharSequence... namespace) {
		return itracer.traceDirect(value, name, type, namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceCounter(long, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceCounter(long value, CharSequence name,
			CharSequence... namespace) {
		return itracer.traceCounter(value, name, namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceIncrement(long, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceIncrement(long value, CharSequence name,
			CharSequence... namespace) {
		return itracer.traceIncrement(value, name, namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceIncrement(java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceIncrement(CharSequence name,
			CharSequence... namespace) {
		return itracer.traceIncrement(name, namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceIntervalIncrement(long, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceIntervalIncrement(long value, CharSequence name,
			CharSequence... namespace) {
		return itracer.traceIntervalIncrement(value, name, namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceIntervalIncrement(java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceIntervalIncrement(CharSequence name,
			CharSequence... namespace) {
		return itracer.traceIntervalIncrement(name, namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceGauge(long, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceGauge(long value, CharSequence name,
			CharSequence... namespace) {
		return itracer.traceGauge(value, name, namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceDeltaGauge(long, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceDeltaGauge(long value, CharSequence name,
			CharSequence... namespace) {
		return itracer.traceDeltaGauge(value, name, namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceDeltaCounter(long, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceDeltaCounter(long value, CharSequence name,
			CharSequence... namespace) {
		return itracer.traceDeltaCounter(value, name, namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceString(java.lang.CharSequence, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceString(CharSequence value, CharSequence name,
			CharSequence... namespace) {
		return itracer.traceString(value, name, namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceError(java.lang.Throwable, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceError(Throwable value, CharSequence name,
			CharSequence... namespace) {
		return itracer.traceError(value, name, namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceBlob(java.io.Serializable, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceBlob(Serializable value, CharSequence name,
			CharSequence... namespace) {
		return itracer.traceBlob(value, name, namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceBlobDirect(java.io.Serializable, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceBlobDirect(Serializable value, CharSequence name,
			CharSequence... namespace) {
		return itracer.traceBlobDirect(value, name, namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#tracePDU(org.snmp4j.PDU, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric tracePDU(PDU pdu, CharSequence name,
			CharSequence... namespace) {
		return itracer.tracePDU(pdu, name, namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#tracePDUDirect(org.snmp4j.PDU, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric tracePDUDirect(PDU pdu, CharSequence name,
			CharSequence... namespace) {
		return itracer.tracePDUDirect(pdu, name, namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#getHost()
	 */
	@Override
	public String getHost() {
		return itracer.getHost();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#getAgent()
	 */
	@Override
	public String getAgent() {
		return itracer.getAgent();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#getSentMetrics()
	 */
	@Override
	public long getSentMetrics() {
		return itracer.getSentMetrics();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#getDroppedMetrics()
	 */
	@Override
	public long getDroppedMetrics() {
		return itracer.getDroppedMetrics();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#getQueuedMetrics()
	 */
	@Override
	public long getQueuedMetrics() {
		return itracer.getQueuedMetrics();
	}
	
	
	public Object getFieldValue(Object target, String fieldName) {
		try {
			Field f = null;
			try {
				f = target.getClass().getDeclaredField(fieldName);
			} catch (NoSuchFieldException nox) {
				f = target.getClass().getField(fieldName);
			}
			f.setAccessible(true);
			Object value = f.get(Modifier.isStatic(f.getModifiers()) ? null : target);
			SimpleLogger.info("Extracted field value [", value, "]");
			return value;
		} catch (Exception ex) {
			SimpleLogger.warn("Failed to get field value for [" , target.getClass().getName() , ".", fieldName, "]", ex);
			return null;
		}
	}
	
	public void logMessage(String msg) {
		SimpleLogger.info(msg);
	}

}
