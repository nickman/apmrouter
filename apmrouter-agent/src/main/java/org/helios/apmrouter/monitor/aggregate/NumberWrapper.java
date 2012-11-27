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
package org.helios.apmrouter.monitor.aggregate;


/**
 * <p>Title: NumberWrapper</p>
 * <p>Description: Accepts an {@link INumberProvider} and wraps it to disguise it as a {@link Number}.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.monitor.aggregate.NumberWrapper</code></p>
 */

public class NumberWrapper {
	/** The wrapped number provider */
	private final INumberProvider provider;

	/**
	 * Returns a Number which is a synthetic which delegates to the passed {@link INumberProvider}
	 * @param provider The inner provider
	 * @return a Number
	 */
	public static Number getNumber(INumberProvider provider) {
		if(provider==null) throw new IllegalArgumentException("The passed number provider was null", new Throwable());
		return new NumberWrapper(provider).getNumber();
	}
	
	/**
	 * Creates a new NumberWrapper
	 * @param provider The provider to wrap
	 */
	private NumberWrapper(INumberProvider provider) {
		this.provider = provider;
	}
	
	/**
	 * @return
	 */
	private Number getNumber() {
		return new Number() {

			@Override
			public int intValue() {
				return provider.getNumber().intValue();
			}

			@Override
			public long longValue() {				
				return provider.getNumber().longValue();
			}

			@Override
			public float floatValue() {
				return provider.getNumber().floatValue();
			}

			@Override
			public double doubleValue() {
				return provider.getNumber().doubleValue();
			}			
		};
	}
}
