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
 * <p>Title: AbstractCollectorMXBean</p>
 * <p>Description: MXBean Interface for all collectors</p>
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public interface AbstractCollectorMXBean {
	public void start() throws Exception;
	public void stop() throws Exception;
	public abstract void reset();
	public boolean isStarted();
	public abstract long getCollectionPeriod();
	public boolean isLogErrors();
	public boolean isLogCollectionResult();
	public String getBlackoutStart();
	public void setBlackoutStart(String blackoutStart);
	public String getBlackoutEnd();
	public void setBlackoutEnd(String blackoutEnd);
	public String getLastTimeCollectionStarted();
	public String getLastTimeCollectionCompleted();
	public String getLastTimeCollectionSucceeded();
	public String getLastTimeCollectionFailed();
	public long getLastCollectionElapsedTime();
	public int getTotalCollectionCount();
	public int getTotalSuccessCount();
	public int getTotalFailureCount();
	public int getConsecutiveFailureCount();
	public boolean isFallbackFrequencyActivated();
	public int getMaxRestartAttempts();
	public String[] getTracingNameSpace();
	public int getNumberOfActiveCollectors();
	public String currentState();
	public String getBeanName();
	public String getLevel();
	public void setLevel(String levelName);
}
