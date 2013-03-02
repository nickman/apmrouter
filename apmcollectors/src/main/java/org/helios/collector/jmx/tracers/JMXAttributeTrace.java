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

import java.util.ArrayList;
import java.util.List;


/**
 * <p>Title: JMXAttributeTrace </p>
 * <p>Description: Simple POJO for containing a JMX Attribute Trace</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class JMXAttributeTrace {
	protected String targetAttributeName = null;
	protected String[] segmentPrefixElements = null;
	protected String segment = null;
	protected String metricName = null;
	protected String traceType = "STICKY_INT_AVG";
	
	protected String[] resolvedPrefix = null;
	
	//protected String simpleObjectTracerClass = null;
	protected List<IObjectFormatter> objectFormatters = new ArrayList<IObjectFormatter>();
	protected List<IObjectTracer> objectTracers = new ArrayList<IObjectTracer>();
	
	protected boolean mandatory = false;
	protected String defaultValue = "0";
	//protected boolean groovyTracers = false;


	/**
	 * Copy Constructor
	 *
	 * @param jMXAttributeTrace a <code>JMXAttributeTrace</code> object
	 */
	public JMXAttributeTrace(JMXAttributeTrace jMXAttributeTrace) 
	{
	    this.targetAttributeName = jMXAttributeTrace.targetAttributeName;
	    this.segmentPrefixElements = jMXAttributeTrace.segmentPrefixElements;
	    this.segment = jMXAttributeTrace.segment;
	    this.metricName = jMXAttributeTrace.metricName;
	    this.traceType = jMXAttributeTrace.traceType;
	    this.resolvedPrefix = jMXAttributeTrace.resolvedPrefix;
	    this.objectFormatters = jMXAttributeTrace.objectFormatters;
	    this.objectTracers = jMXAttributeTrace.objectTracers;
	    this.mandatory = jMXAttributeTrace.mandatory;
	    this.defaultValue = jMXAttributeTrace.defaultValue;
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
	public JMXAttributeTrace(){
		
	}
	
	/**
	 * @return the tracerType
	 */
	public String getTraceType() {
		return traceType;
	}
	/**
	 * @param tracerType the tracerType to set
	 */
	public void setTraceType(String tracerType) {
		this.traceType = tracerType;
	}
	/**
	 * @return the metricName
	 */
	public String getMetricName() {
/*		if(metricName==null) 
			return targetAttributeName;
		else */
			return metricName;
	}
	/**
	 * @param metricName the metricName to set
	 */
	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}
	/**
	 * @return the mandatory
	 */
	public boolean isMandatory() {
		return mandatory;
	}
	/**
	 * @param mandatory the mandatory to set
	 */
	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}
	/**
	 * @return the defaultValue
	 */
	public String getDefaultValue() {
		return defaultValue;
	}
	/**
	 * @param defaultValue the defaultValue to set
	 */
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
//	/**
//	 * @return the groovyTracers
//	 */
//	public boolean isGroovyTracers() {
//		return groovyTracers;
//	}
//	/**
//	 * @param groovyTracers the groovyTracers to set
//	 */
//	public void setGroovyTracers(boolean groovyTracers) {
//		this.groovyTracers = groovyTracers;
//	}

	/**
	 * @return the segmentPrefixElements
	 */
	public String[] getSegmentPrefixElements() {
		return (String[])segmentPrefixElements.clone();
	}
	
	/**
	 * @param segmentPrefixElements the segmentPrefixElements to set
	 */
	public void setSegmentPrefixElements(String[] segmentPrefixElements) {
		this.segmentPrefixElements = segmentPrefixElements;
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
	    retValue.append("metricName = " + this.metricName + TAB);
	    retValue.append("traceType = " + this.traceType + TAB);
	    retValue.append("simpleObjectTracer = " + this.objectFormatters + TAB);
	    retValue.append("objectTracers = " + this.objectTracers + TAB);
	    retValue.append("mandatory = " + this.mandatory + TAB);
	    retValue.append("defaultValue = " + this.defaultValue + TAB);
	    //retValue.append("groovyTracers = " + this.groovyTracers + TAB);
	    retValue.append(" )");
	
	    return retValue.toString();
	}

}
