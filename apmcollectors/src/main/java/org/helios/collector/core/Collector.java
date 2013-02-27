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
package org.helios.collector.core;

/**
 * <p>Title: Collector</p>
 * <p>Description: Interface for all collectors</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public interface Collector {

	//public void preInit();
	public void init();
	public void initCollector();
	//public void postInit();
	
	public void start() throws Exception;
	public void preStart();
	public void startCollector();
	public void postStart();	

	public void collect();
	public void preCollect();
	public CollectionResult collectCallback() throws CollectorException;
	public void postCollect();	
	
	public void stop()  throws Exception;
	public void preStop();
	public void stopCollector();
	public void postStop();
	
	public void reset();
	//public void preReset();
	public void resetCollector();
	//public void postReset();	
	
	public void destroy() throws Exception;
	//public void preDestroy();
	public void destroyCollector();
	//public void postDestroy();	
	
	//public boolean isRunning();
	//public String getState();
	//public long getCollectPeriod();
	//public void setCollectPeriod(long period);
	//public void startCollector(long seconds);
}
