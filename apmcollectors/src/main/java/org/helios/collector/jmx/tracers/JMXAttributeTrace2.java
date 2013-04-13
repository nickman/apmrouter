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
package org.helios.collector.jmx.tracers;

import org.helios.apmrouter.metric.MetricType;

import java.util.ArrayList;
import java.util.List;


/**
 * <p>Title: JMXAttributeTrace2 </p>
 * <p>Description: Simple POJO for containing a JMX Attribute Trace</p>
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class JMXAttributeTrace2 {
    protected String targetAttributeName = null;
    protected String segment = null;
    protected String metricName = null;
    protected String traceType = "LONG_GAUGE";
    protected MetricType resolvedTraceMetricType = MetricType.LONG_GAUGE;
    protected String[] resolvedPrefix = null;
    protected List<IObjectFormatter> objectFormatters = new ArrayList<IObjectFormatter>();
    protected List<IObjectTracer> objectTracers = new ArrayList<IObjectTracer>();
    //protected boolean mandatory = false;
    //protected String defaultValue = "0";


    /**
     * Copy Constructor
     *
     * @param jMXAttributeTrace a <code>JMXAttributeTrace</code> object
     */
    public JMXAttributeTrace2(JMXAttributeTrace2 jMXAttributeTrace)
    {
        this.targetAttributeName = jMXAttributeTrace.targetAttributeName;
        this.segment = jMXAttributeTrace.segment;
        //this.metricName = jMXAttributeTrace.metricName;
        this.traceType = jMXAttributeTrace.traceType;
        this.resolvedPrefix = jMXAttributeTrace.resolvedPrefix;
        this.objectFormatters = jMXAttributeTrace.objectFormatters;
        this.objectTracers = jMXAttributeTrace.objectTracers;
        //this.mandatory = jMXAttributeTrace.mandatory;
        //this.defaultValue = jMXAttributeTrace.defaultValue;
        this.resolvedTraceMetricType = jMXAttributeTrace.resolvedTraceMetricType;
        //this.groovyTracers = jMXAttributeTrace.groovyTracers;
    }
    /**
     * @return the attributeName
     */
    public String getTargetAttributeName() {
        return targetAttributeName;
    }
    /**
     * @param attributeName the attributeName to set
     */
    public void setTargetAttributeName(String attributeName) {
        this.targetAttributeName = attributeName;
    }


    /**
     * Default Constructor
     */
    public JMXAttributeTrace2(){

    }

    /**
     * @return the tracerType
     */
    public String getTraceType() {
        return traceType;
    }


    /**
     * @param traceType the tracerType to set
     */
    public void setTraceType(String traceType) {
        this.traceType = traceType;
        try{
            this.resolvedTraceMetricType = MetricType.valueOfName(traceType);
        } catch (IllegalArgumentException iex){
            /*Ignore exception as default Metric Type
              of LONG_GAUGE will be assigned to this tracer */
        }
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public MetricType getResolvedTraceMetricType() {
        return resolvedTraceMetricType;
    }

    public void setResolvedTraceMetricType(MetricType resolvedTraceMetricType) {
        this.resolvedTraceMetricType = resolvedTraceMetricType;
    }


    /**
     * @return the segment
     */
    public String getSegment() {
        return segment;
    }
    /**
     * @param segment the segment to set
     */
    public void setSegment(String segment) {
        this.segment = segment;
    }
    /**
     * @return the resolvedPrefix
     */
    public String[] getResolvedPrefix() {
        return (String[])resolvedPrefix.clone();
    }
    /**
     * @param resolvedPrefix the resolvedPrefix to set
     */
    public void setResolvedPrefix(String[] resolvedPrefix) {
        this.resolvedPrefix = resolvedPrefix;
    }

    /**
     * @return the objectFormatters
     */
    public List<IObjectFormatter> getObjectFormatters() {
        return objectFormatters;
    }
    /**
     * @param objectFormatters the objectFormatters to set
     */
    public void setObjectFormatters(List<IObjectFormatter> objectFormatters) {
        this.objectFormatters = objectFormatters;
    }
    /**
     * @return the objectTracers
     */
    public List<IObjectTracer> getObjectTracers() {
        return objectTracers;
    }
    /**
     * @param objectTracers the objectTracers to set
     */
    public void setObjectTracers(List<IObjectTracer> objectTracers) {
        this.objectTracers = objectTracers;
    }
    /**
     * Constructs a <code>StringBuilder</code> with all attributes
     * in name = value format.
     *
     * @return a <code>String</code> representation
     * of this object.
     */
    public String toString()
    {
        final String TAB = "    ";
        StringBuilder retValue = new StringBuilder("");
        retValue.append("JMXAttributeTrace ( " + super.toString() + TAB);
        retValue.append("targetAttributeName = " + this.targetAttributeName + TAB);
        retValue.append("segment = " + this.segment + TAB);
        //retValue.append("metricName = " + this.metricName + TAB);
        retValue.append("traceType = " + this.traceType + TAB);
        retValue.append("simpleObjectTracer = " + this.objectFormatters + TAB);
        retValue.append("objectTracers = " + this.objectTracers + TAB);
        //retValue.append("defaultValue = " + this.defaultValue + TAB);
        //retValue.append("groovyTracers = " + this.groovyTracers + TAB);
        retValue.append(" )");

        return retValue.toString();
    }

}
