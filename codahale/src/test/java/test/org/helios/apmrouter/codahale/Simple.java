/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2012, Helios Development Group and individual contributors
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
package test.org.helios.apmrouter.codahale;

import java.util.Random;

import com.yammer.metrics.annotation.Timed;



/**
 * <p>Title: Simple</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.apmrouter.codahale.Simple</code></p>
 */

public class Simple {
	protected static final Random R = new Random(System.currentTimeMillis());
	
	protected static int posInt(int max) {
		return Math.abs(R.nextInt(max));
	}
	protected static int posInt() {
		return Math.abs(R.nextInt());
	}
	protected static long posLong() {
		return Math.abs(R.nextLong());
	}
	
	// -javaagent:c:\hprojects\apmrouter\jagent\target\jagent-1.0-SNAPSHOT.jar=file:/C:\hprojects\apmrouter\jagent\src\test\resources\agent.xml
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("Simple Codahale Test");		
		Simple s = new Simple();
		int loopCount = 10000;
		for(int i = 0; i < loopCount; i++) {
			s.instrumentMe();
			try { Thread.sleep(300); } catch (Exception e) {}
		}
		log("Done");

	}
	
	@Timed(name="Instr", scope="t")
	protected void instrumentMe() {
		sleepRandom(3000);
	}
	
	protected void sleepRandom(int max) {
		try { Thread.sleep(posInt(max)); } catch (Exception ex) {}
	}
	
	
	
	/**
	 * Out logger
	 * @param msg The message
	 */
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * Error logger
	 * @param msg The error message
	 */
	public static void loge(Object msg) {
		System.err.println(msg);
	}
	
	/**
	 * Error logger
	 * @param msg The error message
	 * @param t The throwable to print the stack trace for
	 */
	public static void loge(Object msg, Throwable t) {
		System.err.println(msg);
		t.printStackTrace(System.err);
	}
}
