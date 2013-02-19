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
package org.helios.collector.apache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.helios.collector.url.URLCollector;
import org.helios.collector.core.CollectionResult;
import org.helios.apmrouter.util.StringHelper;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * <p>Title: ApacheModStatusCollector</p>
 * <p>Description: Collect Apache statistics published by mod_status and push traces to Helios OpenTrace</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
@ManagedResource
public class ApacheModStatusCollector extends URLCollector{
	protected Map<String, Integer> scoreBoard = new HashMap<String, Integer>(11);
	protected Map<String, String> scoreBoardDecode = new HashMap<String, String>(11);
	private static final String APACHE_COLLECTOR_VERSION="0.1";
	
	/**
	 * Constructor
	 * 
	 * @param url
	 */
	public ApacheModStatusCollector(String url) {
		super(url);
		scoreBoard.put("_", 0);
		scoreBoard.put("S", 0);
		scoreBoard.put("R", 0);
		scoreBoard.put("W", 0);
		scoreBoard.put("K", 0);
		scoreBoard.put("D", 0);
		scoreBoard.put("C", 0);
		scoreBoard.put("L", 0);
		scoreBoard.put("G", 0);
		scoreBoard.put("I", 0);
		scoreBoard.put(".", 0);
		scoreBoardDecode.put("_", "Waiting for Connection");
		scoreBoardDecode.put("S", "Starting Up");
		scoreBoardDecode.put("R", "Reading Request");
		scoreBoardDecode.put("W", "Sending Reply");
		scoreBoardDecode.put("K", "Keepalive");
		scoreBoardDecode.put("D", "DNS Lookup");
		scoreBoardDecode.put("C", "Closing Connection");
		scoreBoardDecode.put("L", "Logging");
		scoreBoardDecode.put("G", "Gracefully Finishing");
		scoreBoardDecode.put("I", "Idle Cleanup of Worker");
		scoreBoardDecode.put(".", "Open Slot");
	}
	
	/**
	 * Reset Apache scoreboard
	 */
	protected void resetScore() {		
		for(Iterator<String> iter = scoreBoard.keySet().iterator(); iter.hasNext();) {
			scoreBoard.put(iter.next(), 0);		
		}
	}
	
	
	/**
	 * Implementation of abstract collectCallback() method from base class (AbstractCollector)
	 * 
	 * @throws Exception
	 */
	public CollectionResult collectCallback() {
		long start = System.currentTimeMillis();
		CollectionResult colResult = null;
		try {			
			colResult = new CollectionResult();
			try {
				// See if this can tap into the response of URLCollector itself
				httpClient.executeMethod(getMethod);
				String response = getMethod.getResponseBodyAsString();
				tracer.traceGauge(1, defaultAvailabilityLabel, tracingNameSpace);
				String[] lines = response.split("\n");				
				for(String line: lines) {
					String[] nameValuePair = line.split(": ");					
					if(line.startsWith("Total Accesses")) {
						tracer.traceDeltaGauge(convertToLong(nameValuePair[1]), "Access Rate", tracingNameSpace);
					} else if(line.startsWith("Total kBytes")) {
						tracer.traceDeltaGauge(convertToLong(nameValuePair[1]), "Data Rate(kb)", tracingNameSpace);
						tracer.traceDeltaGauge(convertToLong(nameValuePair[1]), "Data Rate(kb)", tracingNameSpace);
					} else if(line.startsWith("CPULoad")) {
						tracer.traceGauge(convertToLong(nameValuePair[1]), "CPU Load", tracingNameSpace);
					} else if(line.startsWith("ReqPerSec")) {
						tracer.traceGauge(convertToLong(nameValuePair[1]), "Request Rate/s", tracingNameSpace);
					} else if(line.startsWith("BytesPerSec")) {
						tracer.traceGauge(convertToLong(nameValuePair[1]), "Byte Rate/s", tracingNameSpace);
					} else if(line.startsWith("BytesPerReq")) {
						tracer.traceGauge(convertToLong(nameValuePair[1]), "Bytes per Request", tracingNameSpace);
					} else if(line.startsWith("BusyWorkers")) {
						tracer.traceGauge(convertToInt(nameValuePair[1]), "Busy Workers", tracingNameSpace);
					} else if(line.startsWith("IdleWorkers")) {
						tracer.traceGauge(convertToInt(nameValuePair[1]), "Idle Workers", tracingNameSpace);
					} else if(line.startsWith("Scoreboard")) {
						computeScore(nameValuePair[1]);
						for(Entry<String, Integer> entry: scoreBoard.entrySet()) {
							String stateName = this.scoreBoardDecode.get(entry.getKey());
							int stateCount = entry.getValue();
							
							/*********************  NEED TO RESOLVE THIS STRINGHELPER.APPEND METHOD **************************/
							//--tracer.traceSticky(stateCount, stateName, StringHelper.append(tracingNameSpace, true, "Worker Status"));
							
							
						}
						resetScore();
					}
				}
			} catch (Exception e) {				
				tracer.traceGauge(0, defaultAvailabilityLabel, tracingNameSpace);
				e.printStackTrace();
			}
			long elapsed = System.currentTimeMillis()-start;
			tracer.traceGauge(elapsed, "Elapsed Time", tracingNameSpace);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return colResult;
	}

	public int convertToInt(String strToConvert){
		int returnValue = 0;
		if(strToConvert!=null && strToConvert.trim().length()>0){
			try{
				returnValue = Integer.parseInt(strToConvert);
			}catch(NumberFormatException nex){
				// Do nothing as the traced value would be reset to zero
			}
		}
		return returnValue;
	}
	
	public long convertToLong(String strToConvert){
		long returnValue = 0L;
		if(strToConvert!=null && strToConvert.trim().length()>0){
			try{
				returnValue = new Integer(strToConvert).longValue();
			}catch(NumberFormatException nex){
				// Do nothing as the traced value would be reset to zero
			}
		}
		return returnValue;
	}	
	
	protected void computeScore(String line) {
		for(int i = 0; i < line.length(); i++) {
			try {				
				String ch = new String(new char[]{line.charAt(i)});
				int x = scoreBoard.get(ch)+1;
				scoreBoard.put(ch, x);
			} catch (Exception e) {
				log.trace(e.getMessage());
			}
		}
	}	

	@ManagedAttribute
	public String getCollectorVersion() {
		return "ApacheModStatusCollector v. "+APACHE_COLLECTOR_VERSION;
	}
	
}
