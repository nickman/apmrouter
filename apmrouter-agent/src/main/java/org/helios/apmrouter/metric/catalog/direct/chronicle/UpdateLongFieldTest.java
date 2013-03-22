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
package org.helios.apmrouter.metric.catalog.direct.chronicle;

import vanilla.java.chronicle.Excerpt;
import vanilla.java.chronicle.impl.IndexedChronicle;

import java.io.File;

/**
 * <p>Title: UpdateLongFieldTest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.metric.catalog.direct.chronicle.UpdateLongFieldTest</code></p>
 */
public class UpdateLongFieldTest {
	static final File cdir = new File(System.getProperty("java.io.tmpdir") + File.separator + "updatet");
	
	static {
		cdir.mkdir();
		log("Chronicle Directory:" + cdir.getAbsolutePath());
		new File(cdir.getAbsolutePath() + File.separator +  "updatet.data").deleteOnExit();
		new File(cdir.getAbsolutePath() + File.separator +  "updatet.index").deleteOnExit();				
	}
	
	static void deleteFiles() {
		new File(cdir.getAbsolutePath() + File.separator + "updatet.data").delete();
		new File(cdir.getAbsolutePath() + File.separator + "updatet.index").delete();				
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		testChronicleUpdatedLongField();
	}
	
	static void testChronicleUpdatedLongField() {
		log("\n\t==================================\n\tRunning testChronicleUpdatedLongField\n\t==================================\n");		
		deleteFiles();
		int LOOPS = 100000;
		IndexedChronicle chronicle = null;
		try {

			chronicle = new IndexedChronicle(cdir.getAbsolutePath() + File.separator + "updatet", 2);
			chronicle.useUnsafe(false);
			for(int i = 0; i < LOOPS; i++) {				
				Excerpt<IndexedChronicle> ex = chronicle.createExcerpt();
				ex.startExcerpt(16);
				ex.writeLong(0);
				ex.writeLong(i);
				ex.finish();				
			}
			log("Writes complete");
			for(int i = 0; i < LOOPS; i++) {				
				Excerpt<IndexedChronicle> ex = chronicle.createExcerpt();
				ex.index(i);
				ex.writeLong(0, i);
			}
			log("Updates complete");
			chronicle.close();
			chronicle = new IndexedChronicle(cdir.getAbsolutePath() + File.separator + "updatet", 2);
			log("Chronicle Reopened");
			for(int i = 0; i < LOOPS; i++) {				
				Excerpt<IndexedChronicle> ex = chronicle.createExcerpt();
				ex.index(i);
				long value = ex.readLong(0);
				if(i!=value) throw new RuntimeException("Expecting [" + i + "] and got [" + value + "]", new Throwable());
			}
			log("Done");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try { chronicle.close(); } catch (Exception e) {}
		}
	}
	
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	

}
