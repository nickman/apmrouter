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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.nio.ByteOrder;
import java.util.TreeSet;

import vanilla.java.chronicle.Excerpt;
import vanilla.java.chronicle.impl.IndexedChronicle;

/**
 * <p>Title: UTFReadWriteTest</p>
 * <p>Description: Defect reproduction for UTF write and reads.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.metric.catalog.direct.chronicle.UTFReadWriteTest</code></p>
 */

public class UTFReadWriteTest {
	static final File cdir = new File(System.getProperty("java.io.tmpdir") + File.separator + "utftest");
	
	static {
		cdir.mkdir();
		log("Chronicle Directory:" + cdir.getAbsolutePath());
		new File(cdir.getAbsolutePath() + File.separator +  "utftest.data").deleteOnExit();
		new File(cdir.getAbsolutePath() + File.separator +  "utftest.index").deleteOnExit();				
	}
	
	static void deleteFiles() {
		new File(cdir.getAbsolutePath() + File.separator + "utftest.data").delete();
		new File(cdir.getAbsolutePath() + File.separator + "utftest.index").delete();				
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("UTFReadWriteTest");
		testUTFWriteToChronicle();
//		testCharSequenceWriteToChronicle();
		testUTFWriteToByteArray();
//		testUTFWriteWithDOSToChronicle();
	}
	
	static void testUTFWriteToChronicle() {
		log("\n\t==================================\n\tRunning testUTFWriteToChronicle\n\t==================================\n");		
		deleteFiles();
		IndexedChronicle chronicle = null;
		try {
			TreeSet<Long> indexes = new TreeSet<Long>();
			chronicle = new IndexedChronicle(cdir.getAbsolutePath() + File.separator + "utftest", 2);
			chronicle.useUnsafe(false);
			
			for(int i = 0; i < 10; i++) {				
				Excerpt<IndexedChronicle> ex = chronicle.createExcerpt();
				
				log("Ex is DOS:" + (ex instanceof DataOutputStream));
				ex.startExcerpt(12);
				String foo = "foo" + i;
				String bar = "bar" + i;				
				ex.writeUTF(foo);
				ex.writeUTF(bar);
				short b1 = ex.readShort(0);				
				log("Length:" + ex.length() + "  b1:" + b1);				
				ex.finish();
				indexes.add(ex.index());
			}
			log("Writes complete");
			for(Long index: indexes) {
				Excerpt<IndexedChronicle> ex = chronicle.createExcerpt();
				ex.index(index);
				String foo = ex.readUTF();
				String bar = ex.readUTF();				
				ex.finish();				
				if(!("foo" + index).equals(foo)) throw new RuntimeException("Mismatch on [" + foo + "]"); 				
				if(!("bar" + index).equals(bar)) throw new RuntimeException("Mismatch on [" + foo + "]");
				
				
			}
			log("Done");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try { chronicle.close(); } catch (Exception e) {}
		}
	}
	
	static void testUTFWriteToByteArray() {
		log("\n\t==================================\n\tRunning testUTFWriteToByteArray\n\t==================================\n");		
		deleteFiles();
		ByteArrayOutputStream baos = null;
		DataOutputStream daos = null;
		ByteArrayInputStream bais = null;
		DataInputStream dais = null;
		
		try {
			baos = new ByteArrayOutputStream(70);
			daos = new DataOutputStream(baos);
			for(int i = 0; i < 10; i++) {				
				String foo = "foo" + i;
				String bar = "bar" + i;				
				daos.writeUTF(foo);
				daos.writeUTF(bar);
			}
			log("Writes complete");
			bais = new ByteArrayInputStream(baos.toByteArray());
			dais = new DataInputStream(bais);
			log("First Short:" + dais.readShort());
			log("Second Short:" + dais.readShort());
			
			bais = new ByteArrayInputStream(baos.toByteArray());
			dais = new DataInputStream(bais);
			for(int i = 0; i < 10; i++) {
				String foo = dais.readUTF();
				String bar = dais.readUTF();
				if(!("foo" + i).equals(foo)) throw new RuntimeException("Mismatch on [" + foo + "]"); 				
				if(!("bar" + i).equals(bar)) throw new RuntimeException("Mismatch on [" + foo + "]");
				log("Index " + i + ":[" + foo + "/" + bar + "]");
			}
			log("Done");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	static void testCharSequenceWriteToChronicle() {
		log("\n\t==================================\n\tRunning testCharSequenceWriteToChronicle\n\t==================================\n");
		deleteFiles();
		IndexedChronicle chronicle = null;
		try {
			TreeSet<Long> indexes = new TreeSet<Long>();
			chronicle = new IndexedChronicle(cdir.getAbsolutePath() + File.separator + "utftest", 2);
			chronicle.useUnsafe(true);
			for(int i = 0; i < 10; i++) {				
				Excerpt<IndexedChronicle> ex = chronicle.createExcerpt();
				ex.startExcerpt(20);
				String foo = "foo" + i;
				String bar = "bar" + i;
				ex.writeChars(foo);
				ex.writeChars(bar);
				ex.finish();
				log("Length " + i + "#:" + ex.length());
				indexes.add(ex.index());
			}
			log("Writes complete");
			for(Long index: indexes) {
				Excerpt<IndexedChronicle> ex = chronicle.createExcerpt();
				ex.index(index);
				
				String foo = ex.readChars();
				String bar = ex.readChars();
				if(!("foo" + index).equals(foo)) throw new RuntimeException("Mismatch on [" + foo + "]"); 				
				if(!("bar" + index).equals(bar)) throw new RuntimeException("Mismatch on [" + foo + "]");				
				log("Index " + index + ":[" + foo + "/" + bar + "]");
				ex.finish();
			}
			log("Done");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try { chronicle.close(); } catch (Exception e) {}
		}
	}

	static void testUTFWriteWithDOSToChronicle() {
		log("\n\t==================================\n\tRunning testUTFWriteWithDOSToChronicle\n\t==================================\n");		
		deleteFiles();
		IndexedChronicle chronicle = null;
		try {
			TreeSet<Long> indexes = new TreeSet<Long>();
			chronicle = new IndexedChronicle(cdir.getAbsolutePath() + File.separator + "utftest", 2);
			chronicle.useUnsafe(true);
			for(int i = 0; i < 10; i++) {				
				Excerpt<IndexedChronicle> ex = chronicle.createExcerpt();
				ex.startExcerpt(12);
				DataOutputStream daos = new DataOutputStream(ex.outputStream());
				String foo = "foo" + i;
				String bar = "bar" + i;				
				daos.writeUTF(foo);
				daos.writeUTF(bar);
				daos.flush();
				ex.finish();
				log("Length:" + ex.length());
				indexes.add(ex.index());
			}
			log("Writes complete");
			for(Long index: indexes) {
				Excerpt<IndexedChronicle> ex = chronicle.createExcerpt();
				ex.index(index);
				String foo = ex.readUTF();
				String bar = ex.readUTF();				
				ex.finish();				
				if(!("foo" + index).equals(foo)) throw new RuntimeException("Mismatch on [" + foo + "]"); 				
				if(!("bar" + index).equals(bar)) throw new RuntimeException("Mismatch on [" + foo + "]");
				
				
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
