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
package org.helios.apmrouter.util;

import java.util.Arrays;
import java.util.Random;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

/**
 * <p>Title: ByteSequenceIndexFinderTestCase</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.ByteSequenceIndexFinderTestCase</code></p>
 */

public class ByteSequenceIndexFinderTestCase {

	
	/**
	 * Some quick test cases and rough performance measurements.
	 * @param args None.
	 */
	public static void main(String[] args) {
		final byte MINUS_ONE = -1; 
		try {
			log("ByteSequenceIndexFinder Testing");			
			
			Random random = new Random(System.currentTimeMillis());
			int bufferCount = 1000;
			int bufferSize = 10000;
			int sequenceSize = 300;
			// create n ChannelBuffers, each y bytes long
			ChannelBuffer[] buffers = new ChannelBuffer[bufferCount];
			// create n byte sequences, each y bytes long
			// even ones will have a match in the ChannelBuffer, odd ones will not
			byte[][] sequences = new byte[bufferCount][];
			// create n finders, one for each sequence
			ByteSequenceIndexFinder[] finders = new ByteSequenceIndexFinder[bufferCount]; 
			// populate the buffers and the sequences
			byte[] randomBytes = new byte[bufferSize];
			for(int i = 0; i < bufferCount; i++) {
				random.nextBytes(randomBytes);
				buffers[i] = ChannelBuffers.copiedBuffer(randomBytes);
				sequences[i] = new byte[sequenceSize];
				if(i%2==0) {
					buffers[i].getBytes(Math.abs(random.nextInt(bufferSize-sequenceSize)), sequences[i]);					
				} else {
					Arrays.fill(sequences[i], MINUS_ONE);					
				}
				
				finders[i] = new ByteSequenceIndexFinder(sequences[i]);
				//log("Loaded #" + i + " seq:" + Arrays.toString(sequences[i]) + "  and buffer:" + Arrays.toString(buffers[i].array()));
			}
			log("Sample Data Populated. Starting test...");
			for(int i = 0; i < bufferCount; i++) {
				boolean odd = (i%2!=0);
				//log("Testing #" + i + " seq:" + Arrays.toString(sequences[i]) + "  and buffer:" + Arrays.toString(buffers[i].array()));
				int pos = finders[i].findIn(buffers[i]);
						//finders[i].findIn(buffers[i]);  // buffers[i].bytesBefore(finders[i]);
				if(odd) {
					if(pos!=-1) {
						throw new Exception("Test failed. Expected -1, got " + pos);
					}
				} else {
					if(pos==-1) {
						throw new Exception("Test failed. Expected >-1 , got " + pos +" with seq:" + Arrays.toString(sequences[i]) + "  and buffer:" + Arrays.toString(buffers[i].array()));
					}
					//log("Extracting #" + i + " pos:" + pos); 
					byte[] extract = new byte[sequenceSize];
					buffers[i].getBytes(pos, extract);
					if(!Arrays.equals(extract, sequences[i])) {
						throw new Exception("Test failed. Got  " + Arrays.toString(extract) + " with seq:" + Arrays.toString(sequences[i]) + "  and buffer:" + Arrays.toString(buffers[i].array()));
					}
					
					
				}
				log("Test #" + i + ": Pass");
			}
			log("Done");
			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
		
		
		
	}
	
	private static void log(Object msg) {
		System.out.println(msg);
	}	

}
