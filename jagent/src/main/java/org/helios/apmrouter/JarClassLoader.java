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
package org.helios.apmrouter;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * <p>Title: JarClassLoader</p>
 * <p>Description: Extension of {@link URLClassLoader} that classloads from a jar within a jar.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author qdolan.blogspot.com
 * <p><code>org.helios.apmrouter.JarClassLoader</code></p>
 */

public class JarClassLoader extends URLClassLoader {
	 
	 private static void close(Closeable closeable) {
	  if (closeable != null) {
	   try {
	    closeable.close();
	   } catch (IOException e) {
	    e.printStackTrace();
	   }
	  }
	 }
	 
	 private static boolean isJar(String fileName) {
	  return fileName != null && fileName.toLowerCase().endsWith(".jar");
	 }
	 
	 private static File jarEntryAsFile(JarFile jarFile, JarEntry jarEntry) throws IOException {
	  InputStream input = null;
	  OutputStream output = null;
	  try {
	   String name = jarEntry.getName().replace('/', '_');
	   int i = name.lastIndexOf(".");
	   String extension = i > -1 ? name.substring(i) : "";
	   File file = File.createTempFile(name.substring(0, name.length() - extension.length()) + ".", extension);
	   file.deleteOnExit();
	   input = jarFile.getInputStream(jarEntry);
	   output = new FileOutputStream(file);
	   int readCount;
	   byte[] buffer = new byte[4096];
	   while ((readCount = input.read(buffer)) != -1) {
	    output.write(buffer, 0, readCount);
	   }
	   return file;
	  } finally {
	   close(input);
	   close(output);
	  }
	 }

	 /**
	 * Creates a new JarClassLoader
	 * @param urls
	 * @param parent
	 */
	public JarClassLoader(URL[] urls, ClassLoader parent) {
	  super(urls, parent);
	  try {
	   ProtectionDomain protectionDomain = getClass().getProtectionDomain();
	   CodeSource codeSource = protectionDomain.getCodeSource();
	   URL rootJarUrl = codeSource.getLocation();
	   String rootJarName = rootJarUrl.getFile();
	   if (isJar(rootJarName)) {
	    addJarResource(new File(rootJarUrl.getPath()));
	   }
	  } catch (IOException e) {
	   e.printStackTrace();
	  }
	 }
	 
	 private void addJarResource(File file) throws IOException {
	  JarFile jarFile = new JarFile(file);
	  addURL(file.toURI().toURL());
	  Enumeration<JarEntry> jarEntries = jarFile.entries();
	  while (jarEntries.hasMoreElements()) {
	   JarEntry jarEntry = jarEntries.nextElement();
	   if (!jarEntry.isDirectory() && isJar(jarEntry.getName())) {
	    addJarResource(jarEntryAsFile(jarFile, jarEntry));
	   }
	  }
	 }
	 
	 /**
	 * {@inheritDoc}
	 * @see java.net.URLClassLoader#addURL(java.net.URL)
	 */
	public void addURL(URL url) {
		 super.addURL(url);
	 }
	  
	 /**
	 * {@inheritDoc}
	 * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
	 */
	@Override
	 protected synchronized Class<?> loadClass(String name, boolean resolve)  throws ClassNotFoundException {
	  try {
	   Class<?> clazz = findLoadedClass(name);
	   if (clazz == null) {
	    clazz = findClass(name);
	    if (resolve)
	     resolveClass(clazz);
	   }
	   return clazz;
	  } catch (ClassNotFoundException e) {
	   return super.loadClass(name, resolve);
	  }
	 }
	}

