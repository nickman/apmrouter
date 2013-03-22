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
package org.helios.apmrouter.nativex;

import org.helios.apmrouter.util.IO;
import org.helios.apmrouter.util.ReadableWritableByteChannelBuffer;
import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarLoader;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * <p>Title: NativeLibLoader</p>
 * <p>Description: Utility class to load a native library acquired as a resource from the classpath.</p> 
 * <p>Attempts to load the named library as follows:<ol>
 * 		<li>Uses a straight load on the offchance that the library is in the <code>{java.library.path}</code></li>
 * 		<li>Attempts to read the lib file from <code>./src/main/resources/META-INF/native/</code> which is where it might be found when in dev mode</li>
 * 		<li>Looks in <code>{user.home}/.jzab/native</code></li>
 * 		<li>Writes out a temp file to <code>{java.io.tmpdir}</code>, loads it and schedules it for deletion on shutdown.</li>
 * </ol>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.NativeLibLoader</code></p>
 */
public class NativeLibLoader {
	
	/** The approximated location of the native libraries if not loaded from the JAR  (usually during dev) */
	private static final String NO_JAR_NATIVE_DIR = "./src/main/resources/META-INF/native/";
	/** The classloader resource prefix for loading the native lib from the jar */
	private static final String NATIVE_DIR_PREFIX = "META-INF/native/";
	

	/**
	 * Loads the native library for the current environment
	 */
	public static String loadLib() {
		String libName = getLibNameQuietly();
		final PrintStream out = System.out;
		final PrintStream err = System.err;
		// === Try lib path ===
		try {
			System.setErr(IO.NULL_PRINTSTREAM);
			System.setOut(IO.NULL_PRINTSTREAM);
			Sigar.load();
			return new Sigar().getNativeLibrary().getAbsolutePath();
		} catch (Throwable e) {
			/* No Op */
		} finally {
			System.setErr(err);
			System.setOut(out);			
		}
		
		//  ===  Look in jZab home ===
		File dir = new File(System.getProperty("user.home") + File.separator + ".apmrouter" + File.separator + "native" );
		if(dir.exists() && dir.isDirectory()) {
			File libFile = new File(dir.getAbsolutePath() + File.separator + libName);
			if(libFile.exists()) {
				try {
					System.load(libFile.getAbsolutePath());
					return libFile.getAbsolutePath();
				} catch (Throwable e) {}
			}
		}
		//  ===  Look in Dev Location ===
		File devFile = new File(NO_JAR_NATIVE_DIR + libName);
		try {
			System.load(devFile.getAbsolutePath());
			return devFile.getAbsolutePath();
		} catch (Throwable e) {			
		}
		//  ===  Extract and write tmp===
		String libFileName = extractNativeLib(libName);
		System.load(libFileName);
		return libFileName;
	}
	
	/**
	 * Reads the native library from the source and writes to the temp file.
	 * @return The file name where the lib was written
	 * @throws IOException
	 */
	private static String extractNativeLib(String nativeLibraryName) {
		FileOutputStream fos = null;
		InputStream is = null;
		try {
			byte[] buffer = new byte[4096];
			int bytesWritten = 0;
			int totalBytesWritten = 0;
//			ClassLoader ncl = NativeLibLoader.class.getClassLoader();
//			URL ncs = NativeLibLoader.class.getProtectionDomain().getCodeSource().getLocation();
//			System.out.println("\n\tNativeLibLoader:\n\t\tClassLoader:" + ncl + "\n\t\tCodeSource:" + ncs + "\n");
			if(NativeLibLoader.class.getClassLoader()==null) {
				is = NativeLibLoader.class.getResourceAsStream("META-INF/native/" + nativeLibraryName);
			} else {
				is = NativeLibLoader.class.getClassLoader().getResourceAsStream("META-INF/native/" + nativeLibraryName);
			}
			if(is==null) {
				try {
					String jarUrl = "jar:" + NativeLibLoader.class.getProtectionDomain().getCodeSource().getLocation().toString() + "!/META-INF/native/" + nativeLibraryName;
					URL jurl = new URL(jarUrl);
					is = jurl.openStream();
				} catch (Exception ex) {}
			}
			if(is==null) {
				try {
					is = ClassLoader.getSystemResourceAsStream("META-INF/native/" + nativeLibraryName);
				} catch (Exception ex) {}
			}
			if(is==null) {
				throw new RuntimeException("Failed to load resource [META-INF/native/" + nativeLibraryName + "]", new Throwable());
			}
			
			File tmpFile = File.createTempFile("jzabTmp", nativeLibraryName);
			SnipeFilesRepo.getInstance().bypass(tmpFile);
			fos = new FileOutputStream(tmpFile, true);
			while((bytesWritten=is.read(buffer))!=-1) {
				fos.write(buffer, 0, bytesWritten);
				totalBytesWritten += bytesWritten;
			}
			fos.close();
			is.close();
			
			//ByteBufferStreams buff = ByteBufferStreams.readInputStreamDirect(NativeLibLoader.class.getClassLoader().getResourceAsStream("META-INF/native/" + nativeLibraryName));
//			int size = buff.position();
//			LOG.debug("Read [{}] bytes into buffer [{}]", size, buff);
//			buff.flip();
						
			return tmpFile.getAbsolutePath();
		} catch (Exception e) {
			throw new RuntimeException("Unable to load native library [" + nativeLibraryName + "]", e);
		} finally {
			try { is.close(); } catch (Exception e) {}
			try { fos.close(); } catch (Exception e) {}
		}
	}
	
	
	private static String getLibNameQuietly() {
		final PrintStream err = System.err;
		final PrintStream out = System.out;
		// Redirects err to a null output stream
		System.setErr(IO.NULL_PRINTSTREAM);
		System.setOut(IO.NULL_PRINTSTREAM);
		try {
			return SigarLoader.getNativeLibraryName();
		} finally {
			System.setErr(err);
			System.setOut(out);
		}
	}
	
	/**
	 * Adds a file to be sniped
	 * @param name The name of the file
	 */
	static void deleteFile(CharSequence name) {
		SnipeFilesRepo.getInstance().bypass(new File(name.toString()));
	
	}
	
	/**
	 * Adds a file to be sniped
	 * @param file The file
	 */
	static void deleteFile(File file) {
		SnipeFilesRepo.getInstance().bypass(file);
	}
	
	public static void main(String[] args) {
//		LOG.info("Snipe Test");
//		try {
//			for(int i = 0; i < 5; i++) {
//				File f = File.createTempFile("Foo", "ffo.tmp");
//				SnipeFilesRepo.getInstance().bypass(f);
//			}
//		} catch (Exception e) {
//			LOG.error("Failed", e);
//		}
		
		
//		LOG.info("Test System Load");
//		loadLib();
		
		extractNativeLib(getLibNameQuietly());
		Cpu cpu = APMSigar.getInstance().getCpu();
		
	}
	
	
	/**
	 * <p>Title: SnipeFilesRepo</p>
	 * <p>Description: Maintains a persistent repository of files that failed to be deleted during that last shutdown and will be deleted on the next startup</p>
	 * <p>The serialized list of files is saved to <b><code>${user.home}/.heliosJzabSnipeFiles.ser</code></b>. 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jzab.plugin.nativex.NativeLibLoader.SnipeFilesRepo</code></p>
	 */
		static class SnipeFilesRepo  extends Thread {
			/** The names of the files that need to be snipped */
			private final Set<String> filesToSnipe = new HashSet<String>();
			/** The singleton instance */		
			private static volatile SnipeFilesRepo instance = null;
			/** The singleton instance ctor lock */
			private static final Object lock = new Object();
			/** The name of the file where this file will be saved and loaded from */
			public static final File serFile = new File(System.getProperty("user.home") + File.separator + ".heliosJzabSnipeFiles.ser");

			/**
			 * Acquires the singleton instance
			 * @return the singleton instance
			 */
			public static SnipeFilesRepo getInstance() {
				if(instance==null) {
					synchronized(lock) {
						if(instance==null) {
							instance = new SnipeFilesRepo();
						}
					}
				}
				return instance;
			}
			
			/**
			 * {@inheritDoc}
			 * @see java.lang.Thread#run()
			 */
			@Override
			public void run() {
				save();
			}

			private SnipeFilesRepo() {
				Runtime.getRuntime().addShutdownHook(this);
				load();
				onStart();
			}

			/**
			 * Deletes sniped files on startup
			 */
			private void onStart() {
				for(Iterator<String> iter = filesToSnipe.iterator(); iter.hasNext();) {
					File f = new File(iter.next());
					if(f.exists()) {
						if(!f.delete()) {
							continue;
						} 
					} 					
					iter.remove();					
				}					
			}

			/**
			 * Adds a file to be sniped
			 * @param name The name of the file
			 */
			public void addFile(CharSequence name) {
				if(name!=null) {
					String nm = name.toString();
					File f = new File(nm);
					if(f.exists()) {
						if(!f.delete()) {
							filesToSnipe.add(f.getAbsolutePath());
							save();
						}
					}
				}
			}
			
			/**
			 * Testing hook to add a file without attempting to delete
			 * @param f The file to add
			 */
			public void bypass(File f) {
				filesToSnipe.add(f.getAbsolutePath());
				save();
			}

			/**
			 * Adds a file to be sniped
			 * @param file The file
			 */
			public void addFile(File file) {
				if(file!=null) {
					if(file.exists()) {
						if(!file.delete()) {
							filesToSnipe.add(file.getAbsolutePath());
							save();
						}
					}
				}
			}


			/**
			 * Attempts to read the snipe file
			 */
			@SuppressWarnings("unchecked")
			private void load() {
				if(serFile.canRead()) {
					FileChannel fc = null;
					try {
						int size = (int)serFile.length();
						if(size<1) return;
						ReadableWritableByteChannelBuffer buff = ReadableWritableByteChannelBuffer.newDirectDynamic(size);
						fc = new RandomAccessFile(serFile, "rw").getChannel();
						ByteBuffer bb = ByteBuffer.allocate(size);
						long bt = fc.read(bb);
						bb.flip();
						buff.write(bb);
						Set<String> set = (Set<String>)new ObjectInputStream(buff.asInputStream()).readObject();
						for(String s: set) {
							if(s!=null && !s.trim().isEmpty()) {
								filesToSnipe.add(s);
							}
						}
					} catch (Exception e) {
						//serFile.delete();
					}  finally {
						try { if(fc.isOpen()) fc.close(); } catch (Exception e) {}
					}
				}			
			}
		
		
		/**
		 * Saves the snipe file. Yeah, it's not an XA transaction so if the write fails after the delete, then KAPUT.
		 */
		private synchronized void save() {
			if(filesToSnipe.isEmpty()) return;
			FileChannel fc = null;
			int size = 0;
			for(String s: filesToSnipe ) { size += s.getBytes().length; } 
			try {
				RandomAccessFile raf = new RandomAccessFile(serFile, "rw");
				fc = raf.getChannel();
				if(serFile.exists()) {
					fc.truncate(0);
				}
				ReadableWritableByteChannelBuffer buff = ReadableWritableByteChannelBuffer.newDirectDynamic(size);
				ObjectOutputStream oos = new ObjectOutputStream(buff.asOutputStream());
				oos.writeObject(filesToSnipe);
				oos.flush();
				buff.asOutputStream().flush();
				//long bt = fc.transferTo(0, buff.writerIndex(), buff);
				long bt = fc.write(buff.toByteBuffer());
				fc.force(true);
				fc.close();
			} catch (Exception e) {
			} finally {
				try { if(fc.isOpen()) fc.close(); } catch (Exception e) {}
			}
		}
	
	}
		
}
	

