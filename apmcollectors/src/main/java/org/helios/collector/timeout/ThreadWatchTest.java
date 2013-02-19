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
package org.helios.collector.timeout;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

/**
 * <p>Title: ThreadWatchTest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class ThreadWatchTest {
	static Logger LOG = Logger.getLogger(ThreadWatchTest.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();
		LOG.info("ThreadWatchTest");
		ThreadWatcher tw = ThreadWatcher.getInstance();
		
		try {
			tw.watch(1000);
			Thread.currentThread().join(200);
			tw.stop();
			tw.watch(100, ThreadWatcher.LOGGING_LISTENER);
			Thread.currentThread().join(200);
			try {
				tw.watch(1000, ThreadWatcher.INTERUPTING_LISTENER);
				Thread.currentThread().join(1200);
			} catch (Exception e) {
				LOG.info("Watch Overrun:" + e);				
			}
			
			
			for(int i = 0; i < 100000000; i++) {
				try {
					tw.watch(2, ThreadWatcher.INTERUPTING_LISTENER);
					Thread.sleep(20000);
				} catch (Exception e) {
					LOG.error("BigTest Exception Handler:" + e);
					Thread.sleep(1000);
					
				}
			}
			
			tw.watch(100000000, ThreadWatcher.LOGGING_LISTENER);
			
			Thread.currentThread().join();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
