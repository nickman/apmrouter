/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package org.helios.apmrouter.spring.ctx;

import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.Resource;

/**
 * <p>Title: NamedGenericXmlApplicationContext</p>
 * <p>Description: An extension of {@link GenericXmlApplicationContext} that supports a settable and gettable application name</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.spring.ctx.NamedGenericXmlApplicationContext</code></p>
 */

public class NamedGenericXmlApplicationContext extends GenericXmlApplicationContext {
	/** The app name for this context */
	protected String applicationName = null;
	/**
	 * Creates a new NamedGenericXmlApplicationContext
	 */
	public NamedGenericXmlApplicationContext() {

	}

	/**
	 * Creates a new NamedGenericXmlApplicationContext
	 * @param resources
	 */
	public NamedGenericXmlApplicationContext(Resource... resources) {
		super(resources);
	}

	/**
	 * Creates a new NamedGenericXmlApplicationContext
	 * @param resourceLocations
	 */
	public NamedGenericXmlApplicationContext(String... resourceLocations) {
		super(resourceLocations);
	}

	/**
	 * Creates a new NamedGenericXmlApplicationContext
	 * @param relativeClass
	 * @param resourceNames
	 */
	public NamedGenericXmlApplicationContext(Class<?> relativeClass,
			String... resourceNames) {
		super(relativeClass, resourceNames);
	}

	
	/**
	 * Sets the application context app name for this context
	 * @param name the application context app name for this context
	 */
	public void setApplicationName(String name) {
		this.applicationName = name;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.support.AbstractApplicationContext#getApplicationName()
	 */
	@Override
	public String getApplicationName() {
		return applicationName;
	}
}
