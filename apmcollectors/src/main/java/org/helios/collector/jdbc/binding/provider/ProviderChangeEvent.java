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
package org.helios.collector.jdbc.binding.provider;

import java.util.Set;

/**
 * <p>Title: ProviderChangeEvent</p>
 * <p>Description: Runnable task for asynch notifications to provider listeners </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class ProviderChangeEvent implements Runnable {
	/** The listeners to be notified */
	protected Set<IBindVariableProviderListener> listeners = null;
	/** The provider that fired the event */
	protected IBindVariableProvider provider = null;
	/** The old value of the provider */
	protected Object oldValue = null;
	/** The new value of the provider */
	protected Object newValue = null;
	
	
	/**
	 * Creates a new ProviderChangeEvent task.
	 * @param listeners The listeners to be notified.
	 * @param provider The provider that fired the event
	 * @param oldValue The old value of the provider 
	 * @param newValue The new value of the provider 
	 */
	public ProviderChangeEvent(Set<IBindVariableProviderListener> listeners, IBindVariableProvider provider, Object oldValue, Object newValue) {
		this.listeners = listeners;
		this.provider = provider;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}


	/**
	 * Executes the notification against each listener.
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		if(listeners==null) return;
		for(IBindVariableProviderListener listener: listeners) {
			listener.onValueChanged(provider, oldValue, newValue);
		}
		listeners = null;
		oldValue = null;
		newValue = null;
		provider = null;

	}

}
