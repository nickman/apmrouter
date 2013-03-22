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
package org.helios.collector.ping;

import org.helios.collector.core.AbstractCollector;
import org.helios.collector.core.CollectionResult;
import org.helios.collector.core.CollectorException;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Title: PingCollector</p>
 * <p>Description: Ping Time Collector</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead(whitehead.nicholas@gmail.com)
 * 		   Sandeep Malhotra (smalhotra@heliosdev.org)
 */
@ManagedResource
public class PingCollector extends AbstractCollector {

	/** PingCollector collector version */
	private static final String PING_COLLECTOR_VERSION="0.1";
	/**	true if platform is windows */
	protected boolean isWindows = true;
	/** A list of servers to ping */
	protected List<String> servers = new ArrayList<String>();
	/** The packet size of each ping. Defaults to 32 */
	protected int packetSize = 32;
	/** The number of packets to be sent. Defaults to 2 */
	protected int packetCount = 1;
	/** The timeout in seconds for each request. Defaults to 5 */
	protected int timeout = 5;	
	
	public CollectionResult collectCallback() {
		long start = System.currentTimeMillis();
		CollectionResult result = new CollectionResult();
		try{
		for(String s: servers) {
			String[] fragment = s.split("\\|");
			String ip = fragment[0].trim();
			String name = fragment[1].trim();
			
			String output = ping(ip);
			if(isWindows) {
				processWindowsPing(output, name);
			} else {
				processLinuxPing(output, name);
			}	
		
		}
		result.setResultForLastCollection(CollectionResult.Result.SUCCESSFUL);
		}catch(Exception ex){
			if(logErrors)
				error("An error occured during collect callback for PingCollector: "+this.getBeanName(),ex);
			result.setResultForLastCollection(CollectionResult.Result.FAILURE);
			result.setAnyException(ex);
			return result;			
		}finally{
			tracer.traceGauge(System.currentTimeMillis()-start, "Elapsed Time", "Collectors", getClass().getSimpleName());
		}
		return result;
	}
	
	/**
	 * Returns the Ping Collector version
	 */
	public String getCollectorVersion() {
		return "PingCollector v. " + PING_COLLECTOR_VERSION;
	}
	
	/**
	 * Performs any tasks that are necessary before the Ping collector can be started
	 */
	public void startCollector() throws CollectorException {
		isWindows = System.getProperty("os.name").toUpperCase().contains("WINDOWS");
	}
	
	/**
     * Issues an OS Ping to the specified address.
	 * @param ipAddress The IP Address to ping.
	 * @return A string representing the native ping request output.
	 */
	public String ping(String ipAddress) throws Exception{
		long start = System.currentTimeMillis();
		StringBuilder buff = new StringBuilder();
		InputStream reader = null;
		try {
			ProcessBuilder pb = null;
			if(isWindows) {
				pb = new ProcessBuilder("ping.exe", "-n", "" + packetCount, "-l", "" + packetSize, "-w", "" + (timeout*1000), ipAddress);
			} else {
				pb = new ProcessBuilder("/bin/ping", "-c", "" + packetCount, "-s", "" + packetSize, "-W", "" + timeout, ipAddress);
			}
			Process p = pb.start();
			reader = new BufferedInputStream(p.getInputStream());			
			for (;;) {
				int c = reader.read();
				if (c == -1)
					break;
				buff.append((char) c);
			}
			info("ping took: "+ (System.currentTimeMillis() - start));
			return buff.toString();
		}finally {
			try { reader.close(); } catch (Exception e) {debug(e.getMessage());}
		}
	}	
	
	/**
     * Processes the output of a Windows Ping
	 * @param output The output of the ping command
     * @param hostName The hostName that was pinged.
	 */
	public void processWindowsPing(String output, String hostName) {
		StringReader reader = new StringReader(output);
		BufferedReader br = new BufferedReader(reader);
		String line = null;
		try {
			while((line=br.readLine().trim())!=null) {
				if(line.startsWith("Minimum")) {
					String readings[] = line.split(",");
					for(String reading: readings) {
						try {
							String readingFragments[] = reading.replaceAll("\\s+", "").split("=");
							String timeType = readingFragments[0];
							long value = Long.parseLong(readingFragments[1].substring(0, readingFragments[1].indexOf("ms")));
							tracer.traceGauge(value, timeType, "Collectors", getClass().getSimpleName(), "os=windows","host="+hostName);
						} catch (Exception e) {debug(e.getMessage());}						
					}
				} else if(line.startsWith("Packets:")) {
					try {
						int value = Integer.parseInt(line.split("\\(")[1].split("\\)")[0].split("%")[0]);
						tracer.traceGauge(value, "Packet Loss", "Collectors", getClass().getSimpleName(), "os=windows","host="+hostName);
						if(value==100) {
							tracer.traceGauge(-1, "Minimum", "Collectors", getClass().getSimpleName(), "os=windows","host="+hostName);
							tracer.traceGauge(-1, "Maximum", "Collectors", getClass().getSimpleName(), "os=windows","host="+hostName);
							tracer.traceGauge(-1, "Average", "Collectors", getClass().getSimpleName(), "os=windows","host="+hostName);
						}
					} catch (Exception e) {}
				}			
			}
		} catch (Exception e) {
			// Noop
		}		
	}
	
	/**
     * Processes the output of a Linux Ping
	 * @param output The output of the ping command
     * @param hostName The hostName that was pinged.
	 */
	public void processLinuxPing(String output, String hostName) {
		StringReader reader = new StringReader(output);
		BufferedReader br = new BufferedReader(reader);
		String line = null;
		try {
			while((line=br.readLine())!=null) {
				line = line.trim();
				if(line.startsWith("rtt")) {
					String readings[] = line.split(" = ");
					String[] values = readings[1].split("/");
					long value = 0L; 
					try {
						value = (long)Float.parseFloat(values[0]);
						tracer.traceGauge(value, "Minimum", "Collectors", getClass().getSimpleName(), "os=linux","host="+hostName);
					} catch (Exception e)  {debug(e.getMessage());}
					try {
						value = (long)Float.parseFloat(values[1]);
						tracer.traceGauge(value, "Average", "Collectors", getClass().getSimpleName(), "os=linux","host="+hostName);
					} catch (Exception e)  {}		
					try {
						value = (long)Float.parseFloat(values[2]);
						tracer.traceGauge(value, "Maximum", "Collectors", getClass().getSimpleName(), "os=linux","host="+hostName);
					} catch (Exception e)  {}		
				} else if(line.contains("packets transmitted")) {
					try {
						int value = Integer.parseInt(line.split(",")[2].trim().split("\\s+")[0].replaceAll("%",""));						
						tracer.traceGauge(value, "Packet Loss", "Collectors", getClass().getSimpleName(), "os=linux","host="+hostName);
						if(value==100) {
							tracer.traceGauge(-1, "Minimum", "Collectors", getClass().getSimpleName(), "os=linux","host="+hostName);
							tracer.traceGauge(-1, "Maximum", "Collectors", getClass().getSimpleName(), "os=linux","host="+hostName);
							tracer.traceGauge(-1, "Average", "Collectors", getClass().getSimpleName(), "os=linux","host="+hostName);							
						}						
					} catch (Exception e) {}
				}			
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}	
	
	/**
	 * @return the servers
	 */
	public List<String> getServers() {
		return servers;
	}

	/**
	 * @param servers the servers to set
	 */
	public void setServers(List<String> servers) {
		this.servers = servers;
	}

	/**
	 * @return the packetSize
	 */
	public int getPacketSize() {
		return packetSize;
	}

	/**
	 * @param packetSize the packetSize to set
	 */
	public void setPacketSize(int packetSize) {
		this.packetSize = packetSize;
	}

	/**
	 * @return the packetCount
	 */
	public int getPacketCount() {
		return packetCount;
	}

	/**
	 * @param packetCount the packetCount to set
	 */
	public void setPacketCount(int packetCount) {
		this.packetCount = packetCount;
	}

	/**
	 * @return the timeout
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * @param timeout the timeout to set
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	/**
     * Display a list of servers monitored by this collector.
	 * @return A string report.
	 */
	@ManagedOperation
	public String reportServers() {
		StringBuilder buff = new StringBuilder();
		for(String s: servers) {
			String[] fragment = s.split("\\|");
			buff.append("IP Address:").append(fragment[0].trim()).append("\n");
			buff.append("Host Name:").append(fragment[1].trim()).append("\n");
			buff.append("===============================================\n");
		}
		return buff.toString();
	}	
	
}