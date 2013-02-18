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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.log4j.Logger;

/**
 * <p>Title: JMXObject</p>
 * <p>Description: Wrapper class to hold JMXAttributeTraces for same target MBean</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class JMXObject {
	protected ObjectName targetObjectName = null;
	protected List<JMXAttributeTrace> targetAttributeTraces = new ArrayList<JMXAttributeTrace>();
	protected Set<String> attributeNames = new HashSet<String>(); 
	protected List<JMXAttributeTrace> resolvedAttributes = new ArrayList<JMXAttributeTrace>();
	protected boolean isProcessed = false;
	protected static Logger log = Logger.getLogger(JMXObject.class);
	
	public JMXObject(){
		
	}
	
	/**
	 * Copy Constructor
	 *
	 * @param jMXObject a <code>JMXObject</code> object
	 */
	public JMXObject(JMXObject jMXObject) 
	{
	    this.targetObjectName = jMXObject.targetObjectName;
	    this.targetAttributeTraces = jMXObject.targetAttributeTraces;
	    this.attributeNames = jMXObject.attributeNames;
	    this.resolvedAttributes = jMXObject.resolvedAttributes;
	    this.isProcessed = jMXObject.isProcessed;
	}
	/**
	 * @return the objectName
	 */
	public String getTargetObjectName() {
		return targetObjectName.toString();
	}
	/**
	 * @param objectName the objectName to set
	 */
	public void setTargetObjectName(String objectName) {
		try {
			this.targetObjectName = ObjectName.getInstance(objectName);
		} catch (MalformedObjectNameException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @return the attributeTraces
	 */
	public List<JMXAttributeTrace> getTargetAttributeTraces() {
		return targetAttributeTraces;
	}

	/**
	 * @param attributeTraces the attributeTraces to set
	 */
	public void setTargetAttributeTraces(List<JMXAttributeTrace> targetAttributes) {
		this.targetAttributeTraces = targetAttributes;
		for(JMXAttributeTrace trace : this.targetAttributeTraces) {
			attributeNames.add(trace.getTargetAttributeName());
		}
		/*Iterator<JMXAttributeTrace> iter = targetAttributes.iterator();
		while(iter.hasNext()){
			log.debug(  iter.next().toString()   );
		}*/
	}
	/**
	 * @return the lastReturnedObjects
	 */
	public List<JMXAttributeTrace> getResolvedAttributes() {
		if(resolvedAttributes == null){
			resolvedAttributes = new ArrayList<JMXAttributeTrace>();
		}
		return resolvedAttributes;
	}
	
	public void clearResolvedAttributes(){
		resolvedAttributes = new ArrayList<JMXAttributeTrace>();
	}
	
	/**
	 * @return the attributeNames
	 */
	public Set<String> getAttributeNames() {
		return attributeNames;
	}
	/**
	 * @param attributeNames the attributeNames to set
	 */
	public void setAttributeNames(List<String> attributeNames) {
		if(attributeNames!=null) {
			this.attributeNames.addAll(attributeNames);
		}		
	}
	
	/**
	 * @return the isProcessed
	 */
	public boolean isProcessed() {
		return isProcessed;
	}
	/**
	 * @param isProcessed the isProcessed to set
	 */
	public void setProcessed(boolean isProcessed) {
		this.isProcessed = isProcessed;
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
	    retValue.append("JMXObject ( " + super.toString() + TAB);
	    retValue.append("targetObjectName = " + this.targetObjectName + TAB);
	    retValue.append("targetAttributeTraces = " + this.targetAttributeTraces + TAB);
	    retValue.append("attributeNames = " + this.attributeNames + TAB);
	    retValue.append("resolvedAttributes = " + this.resolvedAttributes + TAB);
	    retValue.append("isProcessed = " + this.isProcessed + TAB);
	    retValue.append(" )");
	
	    return retValue.toString();
	}

}
