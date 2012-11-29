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
package org.helios.apmrouter.monitor.script.rhino;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.LoaderClassPath;

/**
 * <p>Title: NativeFactory</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.monitor.script.rhino.NativeFactory</code></p>
 */

public class NativeFactory {

//	import sun.org.mozilla.javascript.internal.NativeArray;
//	import sun.org.mozilla.javascript.internal.NativeObject;
//	import sun.org.mozilla.javascript.internal.ScriptableObject;
	
	/** The mozilla NativeArray class */
	public static final Class<?> ARRAY_CLASS;
	/** The mozilla NativeObject class */
	public static final Class<?> OBJECT_CLASS;
	/** The mozilla ScriptableObject class */
	public static final Class<?> SCRIPTABLE_CLASS;
	/** The mozilla NativeArray ctor */
	public static final Constructor<?> ARRAY_CTOR;
	/** The mozilla NativeObject ctor */
	public static final Constructor<?> OBJECT_CTOR;
	/** The mozilla Native package name */
	public static final String MOZ_PACKAGE;
	/** The javassist classloader for the mozilla classes */
	public static final LoaderClassPath MOZ_CLASSLOADER;
	/** The tmp dir to write classes to */
	public static final File TMP = new File(System.getProperty("java.io.tmpdir") + File.separator + "mozclasses");
	/** The proxy object factory instance */
	protected static final INativeFactory PROXY_FACTORY;
	
	static {
		Class<?> arr = null;
		Class<?> obj = null;
		Class<?> scr = null;
		Constructor<?> arrCtor = null;
		Constructor<?> objCtor = null;
		String packageName = "sun.org.mozilla.javascript.internal.";
		try {
			arr = Class.forName(packageName + "NativeArray");
			obj = Class.forName(packageName + "NativeObject");
			scr = Class.forName(packageName + "ScriptableObject");
			arrCtor = arr.getConstructor(Object[].class);
			objCtor = obj.getConstructor();
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			packageName = "sun.org.mozilla.javascript.";
			try {
				arr = Class.forName(packageName + "NativeArray");
				obj = Class.forName(packageName + "NativeObject");
				scr = Class.forName(packageName + "ScriptableObject");
				arrCtor = arr.getConstructor();
				objCtor = obj.getConstructor();				
			} catch (Exception ex2) {
				throw new RuntimeException("Failed to locate the rhino classes", ex2);
			}
		}
		MOZ_PACKAGE = packageName + ".*";
		ARRAY_CLASS = arr;
		OBJECT_CLASS = obj;
		SCRIPTABLE_CLASS = scr;
		ARRAY_CTOR = arrCtor;
		OBJECT_CTOR = objCtor;
		MOZ_CLASSLOADER = new LoaderClassPath(OBJECT_CLASS.getClassLoader());
		if(!TMP.exists()) {
			TMP.mkdirs();
		}
		PROXY_FACTORY = buildProxies();
	}
	
	private static INativeFactory buildProxies() {
		try {
			String packageName = NativeFactory.class.getPackage().getName();
			ClassPool cp = new ClassPool();
			cp.appendClassPath(MOZ_CLASSLOADER);
			cp.appendClassPath(new ClassClassPath(INativeObject.class));
			cp.appendClassPath(new ClassClassPath(INativeArray.class));
			cp.appendClassPath(new ClassClassPath(IScriptableObject.class));
			cp.importPackage(MOZ_PACKAGE);
			return buildProxyFactory(packageName, cp,  
					buildNativeObject(packageName, cp), 
					buildNativeArray(packageName, cp)
			);
			
			
			/*
	public Object getProperty(String name);
	public void putProperty(String name, Object obj);
	public IScriptableObject getUnderlying();
			 */
		} catch (Exception ex) {
			throw new RuntimeException("Failed to build rhino proxies", ex);
		}
	}
	
	/**
	 * Builds the native NativeObject proxy class
	 * @param packageName The package name
	 * @param cp The class pool
	 * @return the native NativeObject proxy class
	 * @throws Exception thrown on any errors building the class
	 */
	protected static Class<?> buildNativeObject(String packageName, ClassPool cp) throws Exception {
		CtClass clazz = cp.makeClass(packageName + "." + "NativeObject");
		CtField internalField = new CtField(cp.get(OBJECT_CLASS.getName()), "internal", clazz);
		clazz.addField(internalField);
		CtConstructor zeroParamCtor = new CtConstructor(new CtClass[]{}, clazz);
		zeroParamCtor.setBody("internal = new " + OBJECT_CLASS.getName() + "();");
		clazz.addConstructor(zeroParamCtor);
		CtConstructor internalParamCtor = new CtConstructor(new CtClass[]{cp.get(IScriptableObject.class.getName())}, clazz);
		internalParamCtor.setBody("internal = (" + OBJECT_CLASS.getName() + ")$1.getUnderlying();");
		clazz.addConstructor(internalParamCtor);
		
		CtMethod gp = new CtMethod(cp.get(Object.class.getName()), "getProperty", new CtClass[]{cp.get(String.class.getName())}, clazz);
		gp.setBody("return internal.get($1);");
		clazz.addMethod(gp);
		CtMethod pp = new CtMethod(CtClass.voidType, "putProperty", new CtClass[]{cp.get(String.class.getName()), cp.get(Object.class.getName())}, clazz);
		pp.setBody("return internal.put($1, $2);");
		clazz.addMethod(pp);
		CtMethod gi = new CtMethod(cp.get(Object.class.getName()), "getUnderlying", new CtClass[]{}, clazz);
		gi.setBody("return internal;");
		clazz.addMethod(gi);
		CtMethod gi2 = new CtMethod(cp.get(OBJECT_CLASS.getName()), "getNativeUnderlying", new CtClass[]{}, clazz);
		gi2.setBody("return internal;");
		clazz.addMethod(gi2);
		
		clazz.addInterface(cp.get(INativeObject.class.getName()));
		if(TMP.exists() && TMP.isDirectory()) {
			clazz.writeFile(TMP.getAbsolutePath());
		}
		return clazz.toClass();
		
	}
		
	/**
	 * Builds the native NativeArray proxy class
	 * @param packageName The package name
	 * @param cp The class pool
	 * @return the native NativeArray proxy class
	 * @throws Exception thrown on any errors building the class
	 */
	protected static Class<?> buildNativeArray(String packageName, ClassPool cp) throws Exception {
		CtClass clazz = cp.makeClass(packageName + "." + "NativeArray");
		CtField internalField = new CtField(cp.get(ARRAY_CLASS.getName()), "internal", clazz);
		clazz.addField(internalField);
		CtConstructor arrayParamCtor = new CtConstructor(new CtClass[]{cp.get(Object[].class.getName())}, clazz);
		arrayParamCtor.setBody("internal = new " + ARRAY_CLASS.getName() + "($1);");
		clazz.addConstructor(arrayParamCtor);
		CtConstructor internalParamCtor = new CtConstructor(new CtClass[]{cp.get(IScriptableObject.class.getName())}, clazz);
		internalParamCtor.setBody("internal = (" + ARRAY_CLASS.getName() + ")$1.getUnderlying();");
		clazz.addConstructor(internalParamCtor);
		
		CtMethod gi = new CtMethod(cp.get(Object.class.getName()), "getUnderlying", new CtClass[]{}, clazz);
		gi.setBody("return internal;");
		clazz.addMethod(gi);
		CtMethod gi2 = new CtMethod(cp.get(ARRAY_CLASS.getName()), "getNativeUnderlying", new CtClass[]{}, clazz);
		gi2.setBody("return internal;");
		clazz.addMethod(gi2);
		CtMethod ap = new CtMethod(cp.get(Object.class.getName()), "get", new CtClass[]{CtClass.intType}, clazz);
		ap.setBody("return internal.get($1);");
		clazz.addMethod(ap);
		CtMethod sp = new CtMethod(CtClass.intType, "size", new CtClass[]{}, clazz);
		sp.setBody("return internal.size();");
		clazz.addMethod(sp);
		
		// public Object get(int index);
		// public int size();
		
		clazz.addInterface(cp.get(INativeArray.class.getName()));
		
		if(TMP.exists() && TMP.isDirectory()) {
			clazz.debugWriteFile(TMP.getAbsolutePath());
		}
		return clazz.toClass();
	}
	
	/**
	 * Builds the proxy factory class
	 * @param packageName The package name
	 * @param cp The class pool
	 * @param noClass The native object proxy class
	 * @param naClass The native array proxy class
	 * @return the proxy factory
	 * @throws Exception thrown on any error
	 */
	protected static INativeFactory buildProxyFactory(String packageName, ClassPool cp, Class<?> noClass, Class<?> naClass) throws Exception {
		cp.appendClassPath(new ClassClassPath(noClass));
		cp.appendClassPath(new ClassClassPath(naClass));
		cp.importPackage(new StringBuilder(packageName).deleteCharAt(packageName.length()-1).toString());
		
		CtClass clazz = cp.makeClass(packageName + "." + "ProxyFactory");
		CtMethod f = new CtMethod(cp.get(INativeObject.class.getName()), "newNativeObject", new CtClass[]{}, clazz);
		f.setBody("return new " + packageName + ".NativeObject();");
		clazz.addMethod(f);
		
		f = new CtMethod(cp.get(INativeObject.class.getName()), "newNativeObject", new CtClass[]{cp.get(IScriptableObject.class.getName())}, clazz);
		f.setBody("return new " + packageName + ".NativeObject($1);");
		clazz.addMethod(f);		
		
		f = new CtMethod(cp.get(INativeArray.class.getName()), "newNativeArray", new CtClass[]{}, clazz);
		f.setBody("return new " + packageName + ".NativeArray(new Object[0]);");
		clazz.addMethod(f);
		
		f = new CtMethod(cp.get(INativeArray.class.getName()), "newNativeArray", new CtClass[]{cp.get(IScriptableObject.class.getName())}, clazz);
		f.setBody("return new " + packageName + ".NativeArray($1);");
		clazz.addMethod(f);		
		
		f = new CtMethod(cp.get(INativeArray.class.getName()), "newNativeArray", new CtClass[]{cp.get(Object[].class.getName())}, clazz);
		f.setBody("return new " + packageName + ".NativeArray($1);");
		clazz.addMethod(f);		

		clazz.addInterface(cp.get(INativeFactory.class.getName()));
		if(TMP.exists() && TMP.isDirectory()) {
			clazz.writeFile(TMP.getAbsolutePath());
		}
		return (INativeFactory) clazz.toClass().newInstance();		
	}
	
//	public interface INativeFactory {
//		public INativeObject newNativeObject();
//		public INativeObject newNativeObject(Object internal);
//		public INativeArray newNativeArray();
//		public INativeArray newNativeArray(Object internal);
//		public INativeArray newNativeArray(Object[] elements);
//
//	}	
	
	
	
	public static void main(String[] args) {
		log("Moz Classes Test");
		INativeObject no = newNativeObject();
		log("Obj:" + no.getClass().getName() + "  Internal:" + no.getUnderlying().getClass().getName());
		no.putProperty("foo", "foo");
		no.putProperty("three", 3);
		log("Foo:" + no.getProperty("foo"));
		log("Three:" + no.getProperty("three"));
		INativeArray na = newNativeArray();
		log("Arr:" + na.getClass().getName() + "  Internal:" + na.getUnderlying().getClass().getName());
		
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

	
	public static INativeObject newNativeObject() {
		return PROXY_FACTORY.newNativeObject();
	}
	public static INativeObject newNativeObject(IScriptableObject internal) {
		return PROXY_FACTORY.newNativeObject(internal);
	}
	public static INativeArray newNativeArray() {
		return PROXY_FACTORY.newNativeArray();
	}
	public static INativeArray newNativeArray(IScriptableObject internal) {
		return PROXY_FACTORY.newNativeArray(internal);
	}
	public static INativeArray newNativeArray(Object[] elements) {
		return PROXY_FACTORY.newNativeArray(elements);
	}
	
}
