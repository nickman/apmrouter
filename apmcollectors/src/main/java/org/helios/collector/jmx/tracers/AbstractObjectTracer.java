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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;


/**
 * <p>Title: AbstractObjectTracer </p>
 * <p>Description: Base Class for all Object Tracers</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public abstract class AbstractObjectTracer implements IObjectTracer {
	protected String segmentSuffix = null;
	protected String segmentPrefix = null;
	protected String metricName = null;
	protected boolean logErrors = false;
	protected Logger log = null;
	protected Map<String,Object> bindings = null;

	/**
	 * @return the metricName
	 */
	public String getMetricName() {
		if(metricName!=null && metricName.trim().length()>0)
			return metricName;
		else
			return "";		
	}

	/**
	 * @param metricName the metricName to set
	 */
	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

	/**
	 * @return the segmentSuffix
	 */
	public String getSegmentSuffix() {
		if(segmentSuffix!=null && segmentSuffix.trim().length()>0)
			return segmentSuffix;
		else
			return "";

	}

	/**
	 * @param segmentSuffix the segmentSuffix to set
	 */
	public void setSegmentSuffix(String segmentSuffix) {
		this.segmentSuffix = segmentSuffix;
	}

	/**
	 * @return the segmentPrefix
	 */
	public String getSegmentPrefix() {
		if(segmentPrefix!=null && segmentPrefix.trim().length()>0)
			return segmentPrefix;
		else
			return "";
	}

	/**
	 * @param segmentPrefix the segmentPrefix to set
	 */
	public void setSegmentPrefix(String segmentPrefix) {
		this.segmentPrefix = segmentPrefix;
	}

	/**
	 * @return the logErrors
	 */
	public boolean isLogErrors() {
		return logErrors;
	}

	/**
	 * @param logErrors the logErrors to set
	 */
	public void setLogErrors(boolean logErrors) {
		this.logErrors = logErrors;
	}	

	public void prepareBindings(Object...args){
		bindings=new HashMap<String, Object>();
		bindings.put("segmentSuffix",getSegmentSuffix());
		bindings.put("segmentPrefix",getSegmentPrefix());
		bindings.put("metricName",getMetricName());
		if(log==null){
			// Implementation class didn't instantiate logger, so base class is instantiating it now
			log=Logger.getLogger(AbstractObjectTracer.class);
		}
		bindings.put("log",log);
		bindings.put("logErrors",logErrors);
		if(args!=null){
			String name = null;
			int mod = 0;
			for(int i = 1; i < args.length; i++) {
				mod = i%2;
				if(mod==1) {
					name = args[i-1].toString();
				} else {
					if(name!=null && args[i-1]!=null) {
						bindings.put(name, args[i-1]);
					}
				}			
			}
		}
	}
	
	public void printBindings(){
		if(bindings!=null && log!=null){
			Iterator<Map.Entry<String,Object>> iterator = bindings.entrySet().iterator();
			while(iterator.hasNext()){
				Map.Entry<String,Object> singleObj = (Map.Entry<String,Object>)iterator.next();
				log.trace(singleObj.getKey() + " -- " + singleObj.getValue());
			}
		}
	}
}
