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

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>Title: JMXObject2</p>
 * <p>Description: Wrapper class to hold JMXAttributeTraces for same target MBean</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class JMXObject2{
    public ObjectName targetObjectName = null;
    protected List<JMXAttributeTrace2> targetAttributeTraces = new ArrayList<JMXAttributeTrace2>();
    protected Set<String> attributeNames = new HashSet<String>();
    protected List<JMXAttributeTrace2> resolvedAttributes = new ArrayList<JMXAttributeTrace2>();
    protected boolean isProcessed = false;
    protected static Logger log = Logger.getLogger(JMXObject.class);
    protected String[] segmentPrefixElements = null;
    protected String[] targetAttributes = null;
    @Autowired
    private ApplicationContext context;

    public JMXObject2(){

    }

    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }
    /**
     * Copy Constructor
     *
     * @param jMXObject a <code>JMXObject</code> object
     */
    public JMXObject2(JMXObject2 jMXObject)
    {
        this.targetObjectName = jMXObject.targetObjectName;
        this.targetAttributeTraces = jMXObject.targetAttributeTraces;
        this.attributeNames = jMXObject.attributeNames;
        this.resolvedAttributes = jMXObject.resolvedAttributes;
        this.isProcessed = jMXObject.isProcessed;
        this.context = jMXObject.context;
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
    public List<JMXAttributeTrace2> getTargetAttributeTraces() {
        return targetAttributeTraces;
    }
    /**
     * @return the lastReturnedObjects
     */
    public List<JMXAttributeTrace2> getResolvedAttributes() {
        if(resolvedAttributes == null){
            resolvedAttributes = new ArrayList<JMXAttributeTrace2>();
        }
        return resolvedAttributes;
    }

    public void clearResolvedAttributes(){
        resolvedAttributes = new ArrayList<JMXAttributeTrace2>();
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
     * @return the segmentPrefixElements
     */
    public String[] getSegmentPrefixElements() {
        if(segmentPrefixElements!=null)
            return (String[])segmentPrefixElements.clone();
        else
            return new String[]{};
    }

    /**
     * @param segmentPrefixElements the segmentPrefixElements to set
     */
    public void setSegmentPrefixElements(String[] segmentPrefixElements) {
        this.segmentPrefixElements = segmentPrefixElements;
    }

    public String[] getTargetAttributes() {
        return targetAttributes;
    }

    public void setTargetAttributes(String[] targetAttributes) {
        this.targetAttributes = targetAttributes;
        createJMXAttributeTraces();
    }

    private void createJMXAttributeTraces() {
        if(targetAttributes!=null)   {
           JMXAttributeTrace2 trace = null;
           for(String attributeRecord: targetAttributes){
               String[] columns = attributeRecord.split("\\|",-1);
               if(columns!=null && columns.length <1)
                   return;
               for(int i=0;i<columns.length;i++){
                    if(i==0){
                        trace = new JMXAttributeTrace2();
                        trace.setTargetAttributeName(columns[i]);
                        attributeNames.add(trace.getTargetAttributeName());
                    }else if(i==1){
                        if(columns[i]!=null && columns[i].trim().length()>1)
                            trace.setSegment(columns[i]);
                    }else if(i==2){
                        if(columns[i]!=null && columns[i].trim().length()>1)
                            trace.setTraceType(columns[i]);
                    }else if(i==3){
                        if(columns[i]!=null && columns[i].trim().length()>1){
                            if(context.containsBean(columns[i]))  {
                                trace.setObjectFormatter((IObjectFormatter)context.getBean(columns[i]));
                            }
                        }
                    }else if(i==4){
                        if(columns[i]!=null && columns[i].trim().length()>1) {
                            if(context.containsBean(columns[i]))  {
                                trace.setObjectTracer((IObjectTracer)context.getBean(columns[i]));
                            }
                        }
                    }

               }
               targetAttributeTraces.add(trace);
           }
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
