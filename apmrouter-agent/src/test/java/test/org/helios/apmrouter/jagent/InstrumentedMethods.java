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
package test.org.helios.apmrouter.jagent;

import java.util.Random;

import org.helios.apmrouter.instrumentation.Trace;
import org.helios.apmrouter.instrumentation.TraceCollection;
import org.helios.apmrouter.util.SystemClock;

/**
 * <p>Title: InstrumentedMethods</p>
 * <p>Description: Some instrumented methods to test</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.apmrouter.jagent.InstrumentedMethods</code></p>
 */

public class InstrumentedMethods {
	static Random r = new Random(System.currentTimeMillis());
	@Trace(namespace={"test=InstrumentedMethods", "name=randomSleep"})
	public void randomSleep() {
		SystemClock.sleep(Math.abs(r.nextInt(1000)));
	}
	
//	@Trace(namespace={"test=InstrumentedMethods", "name=doubleSecretProbation"}, 
//			collections={TraceCollection.TXROLL, TraceCollection.TIMENS, TraceCollection.TXCLEAR})
//	@Trace(namespace={"test=InstrumentedMethods", "name=doubleSecretProbation"}, 
//	collections={TraceCollection.TIMENS})	
//	public void doubleSecretProbation() {
//		SystemClock.sleep(Math.abs(r.nextInt(10)));
//		randomSleep();
//	}
}
