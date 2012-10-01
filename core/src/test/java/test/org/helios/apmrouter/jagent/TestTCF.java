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

/**
 * <p>Title: TestTCF</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.apmrouter.jagent.TestTCF</code></p>
 */

public class TestTCF {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("Starting Test");
		InstrumentedMethods im = new InstrumentedMethods();
		for(int i = 0; i < 100; i++) {
			im.foo();
		}
		log("Done");
	}
	
	public static void log(Object msg) {
		System.out.println("[TestTCF]:" + msg);
	}

	
	// -javaagent:/home/nwhitehead/.m2/repository/org/helios/apmrouter/jagent/1.0-SNAPSHOT/jagent-1.0-SNAPSHOT.jar=file:/home/nwhitehead/hprojects/apmrouter/core/src/test/resources/aop/tcf-agent.xml

}
