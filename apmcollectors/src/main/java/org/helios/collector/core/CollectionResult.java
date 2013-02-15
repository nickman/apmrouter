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
 * 
 * <p>Title: CollectionResult</p>
 * <p>Description: Class that lists down valid results that can be returned 
 * by the collectors' collectCallback method. </p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class CollectionResult {
	
	/** 
	 * Possible results for collectCallback method implemented by collectors 
	 */
	public static enum Result{
		SUCCESSFUL,
		PARTIAL,
		FAILURE,
		ALREADY_COLLECTING
	}
	
	/** 
	 * Result for last collect should be set by collectCallback 
	 * method of concrete collector classes
	 */
	protected Result resultForLastCollection;
	
	/**
	 * Reference to the exception object that might get generated during
	 * the execution of collectCallback in concrete collectors
	 */
	protected Exception anyException=null;

	/**
	 * @return the resultForLastCollection
	 */
	public Result getResultForLastCollection() {
		return resultForLastCollection;
	}

	/**
	 * @param resultForLastCollection the resultForLastCollection to set
	 * @return this CollectionResult
	 */
	public CollectionResult setResultForLastCollection(Result resultForLastCollection) {
		this.resultForLastCollection = resultForLastCollection;
		return this;
	}

	/**
	 * @return the anyException
	 */
	public Exception getAnyException() {
		return anyException;
	}

	/**
	 * @param anyException the anyException to set
	 */
	public void setAnyException(Exception anyException) {
		this.anyException = anyException;
	}

	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString()
	{
	    final String TAB = "    ";
	    String retValue = "";
	    retValue = "CollectionResult ( "
	        + super.toString() + TAB
	        + "resultForLastCollection = " + this.resultForLastCollection + TAB
	        + "anyException = " + this.anyException + TAB
	        + " )";
	    return retValue;
	}	
}