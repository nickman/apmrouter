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
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.LoaderClassPath;

/**
 * <p>Title: NativeFactory</p>
 * <p>Description: The rhino class locations in Java bounce around so this class locates where they are and spins up a runtime compiled proxy to wrap the functions we need.</p> 
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
				ex.printStackTrace(System.err);
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
			cp.appendClassPath(new ClassClassPath(BaseNativeProxy.class));
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
			ex.printStackTrace(System.err);
			throw new RuntimeException("Failed to build rhino proxies", ex);
		}
	}
	
	/**
	 * <p>Title: BaseNativeProxy</p>
	 * <p>Description: Contains static helper methods for the actual proxy classes</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.monitor.script.rhino.NativeFactory.BaseNativeProxy</code></p>
	 */
	public static class BaseNativeProxy {
		
		/**
		 * Scans the passed array and converts any entries that are native objects into proxies.
		 * @param arr The array to convert
		 * @return the new array
		 */
		public static Object[] convertNatives(Object[] arr) {
			Object[] newArr = new Object[arr.length];
			for(int i = 0; i < arr.length; i++) {
				newArr[i] = convertNative(arr[i]);
			}
			return newArr;
		}
		/**
		 * Inspects the passed object and if it is a mozilla native object, returns the proxy wrapper, 
		 * otherwise returns the passed object
		 * @param obj The object to inspect
		 * @return a wrapper proxy or the passed object
		 */
		public static Object convertNative(Object obj) {
			if(obj==null) return null;
			if(OBJECT_CLASS.isInstance(obj)) {
				return newNativeObject(obj);
			} else if(ARRAY_CLASS.isInstance(obj)) {
				return newNativeArray(obj);
			} else {
				return obj;
			}
		}
		
		/**
		 * Tests the passed object to see if it a proxy, and if so, returns the underlying
		 * @param obj The object to test
		 * @return The underlying native or the passed value
		 */
		public static Object recoverNative(Object obj) {
			if(obj==null) return null;
			if(obj instanceof INativeObject) {
				return ((INativeObject)obj).getUnderlying();
			} else if(obj instanceof INativeArray) {
				return ((INativeArray)obj).getUnderlying();
			} else if(obj.getClass().isArray()) {
				int len = Array.getLength(obj);
				Object[] newArray = new Object[len];
				for(int i = 0; i < len; i++) {
					newArray[i] = recoverNative(Array.get(obj, i));
				}
				return newArray;
			}
			return obj;			
		}
		
		public static Object[] recoverNativeArr(Object obj) {
			return (Object[])recoverNative(obj);
		}
		
		/**
		 * Renders the contents of a map in a string.
		 * @param map The map to render
		 * @return the rendered string
		 */
		public static String mapToString(Map<?,?> map) {
			StringBuilder b = new StringBuilder(map.getClass().getSimpleName());
			for(Map.Entry<?, ?> m: map.entrySet()) {
				Object value = m.getValue();
				if(value instanceof INativeObject) {
					b.append("\n\t").append(m.getKey()).append(":").append(mapToString((Map)((INativeObject)value).getUnderlying()));
				} else {
					b.append("\n\t").append(m.getKey()).append(":").append(m.getValue());
				}
			}
			return b.toString();
		}
		
		/**
		 * Renders the contents of a list in a string.
		 * @param list The list to render
		 * @return the rendered string
		 */
		public static String listToString(List<?> list) {
			StringBuilder b = new StringBuilder(list.getClass().getSimpleName());
			for(Object value: list) {					
				if(value instanceof INativeArray) {
					b.append("\n\t").append(listToString((List)((INativeArray)value).getUnderlying()));
				} else {
					b.append("\n\t").append(value);
				}
			}
			return b.toString();
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
		CtClass clazz = cp.makeClass(packageName + "." + "NativeObject", cp.get(BaseNativeProxy.class.getName()));
		CtField internalField = new CtField(cp.get(OBJECT_CLASS.getName()), "internal", clazz);
		clazz.addField(internalField);
		CtConstructor zeroParamCtor = new CtConstructor(new CtClass[]{}, clazz);
		zeroParamCtor.setBody("internal = new " + OBJECT_CLASS.getName() + "();");
		clazz.addConstructor(zeroParamCtor);
		CtConstructor internalParamCtor = new CtConstructor(new CtClass[]{cp.get(IScriptableObject.class.getName())}, clazz);
		internalParamCtor.setBody("internal = (" + OBJECT_CLASS.getName() + ")$1.getUnderlying();");
		clazz.addConstructor(internalParamCtor);
		CtConstructor internalParamCtor2 = new CtConstructor(new CtClass[]{cp.get(Object.class.getName())}, clazz);		
		internalParamCtor2.setBody("internal = (" + OBJECT_CLASS.getName() + ")$1;");
		clazz.addConstructor(internalParamCtor2);
		
		
		CtMethod gp = new CtMethod(cp.get(Object.class.getName()), "getProperty", new CtClass[]{cp.get(String.class.getName())}, clazz);
		gp.setBody("return convertNative(" + SCRIPTABLE_CLASS.getName() + ".getProperty(internal,$1));");
		clazz.addMethod(gp);
		CtMethod pp = new CtMethod(CtClass.voidType, "putProperty", new CtClass[]{cp.get(String.class.getName()), cp.get(Object.class.getName())}, clazz);
		pp.setBody("return " + SCRIPTABLE_CLASS.getName() + ".putProperty(internal, $1, recoverNative($2));");
		clazz.addMethod(pp);
		CtMethod gi = new CtMethod(cp.get(Object.class.getName()), "getUnderlying", new CtClass[]{}, clazz);
		gi.setBody("return internal;");
		clazz.addMethod(gi);
		CtMethod gi2 = new CtMethod(cp.get(OBJECT_CLASS.getName()), "getNativeUnderlying", new CtClass[]{}, clazz);
		gi2.setBody("return internal;");
		clazz.addMethod(gi2);
		CtMethod ts = new CtMethod(cp.get(String.class.getName()), "toString", new CtClass[]{}, clazz);
		ts.setBody("return internal.toString();");
		clazz.addMethod(ts);
		CtMethod hp = new CtMethod(CtClass.booleanType, "hasProperty", new CtClass[]{cp.get(String.class.getName())}, clazz);
		hp.setBody("return " + SCRIPTABLE_CLASS.getName() + ".hasProperty(internal, $1);");
		clazz.addMethod(hp);
		CtMethod hp2 = new CtMethod(CtClass.booleanType, "hasProperty", new CtClass[]{CtClass.intType}, clazz);
		hp2.setBody("return " + SCRIPTABLE_CLASS.getName() + ".hasProperty(internal, $1);");
		clazz.addMethod(hp2);
		CtMethod gai = new CtMethod(cp.get(Object[].class.getName()), "getAllIds", new CtClass[]{}, clazz);
		gai.setBody("return convertNatives(internal.getAllIds());");
		clazz.addMethod(gai);
		
		
		
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
		CtClass clazz = cp.makeClass(packageName + "." + "NativeArray", cp.get(BaseNativeProxy.class.getName()));
		CtField internalField = new CtField(cp.get(ARRAY_CLASS.getName()), "internal", clazz);
		clazz.addField(internalField);
		CtConstructor arrayParamCtor = new CtConstructor(new CtClass[]{cp.get(Object[].class.getName())}, clazz);
		arrayParamCtor.setBody("internal = new " + ARRAY_CLASS.getName() + "(recoverNativeArr($1));");
		clazz.addConstructor(arrayParamCtor);
		CtConstructor internalParamCtor = new CtConstructor(new CtClass[]{cp.get(IScriptableObject.class.getName())}, clazz);
		internalParamCtor.setBody("internal = (" + ARRAY_CLASS.getName() + ")$1.getUnderlying();");
		clazz.addConstructor(internalParamCtor);
		
		CtConstructor internalParamCtor2 = new CtConstructor(new CtClass[]{cp.get(Object.class.getName())}, clazz);		
		internalParamCtor2.setBody("internal = (" + ARRAY_CLASS.getName() + ")$1;");
		clazz.addConstructor(internalParamCtor2);
		
		
		CtMethod gi = new CtMethod(cp.get(Object.class.getName()), "getUnderlying", new CtClass[]{}, clazz);
		gi.setBody("return internal;");
		clazz.addMethod(gi);
		CtMethod gi2 = new CtMethod(cp.get(ARRAY_CLASS.getName()), "getNativeUnderlying", new CtClass[]{}, clazz);
		gi2.setBody("return internal;");
		clazz.addMethod(gi2);
		CtMethod ap = new CtMethod(cp.get(Object.class.getName()), "get", new CtClass[]{CtClass.intType}, clazz);
		ap.setBody("return convertNative(" + SCRIPTABLE_CLASS.getName() + ".getProperty(internal,$1));");
		clazz.addMethod(ap);
		CtMethod sp = new CtMethod(CtClass.intType, "size", new CtClass[]{}, clazz);
		sp.setBody("return (int)internal.getLength();");
		clazz.addMethod(sp);
		CtMethod ts = new CtMethod(cp.get(String.class.getName()), "toString", new CtClass[]{}, clazz);
		ts.setBody("return internal.toString();");
		clazz.addMethod(ts);
		CtMethod hp = new CtMethod(CtClass.booleanType, "hasProperty", new CtClass[]{cp.get(String.class.getName())}, clazz);
		hp.setBody("return " + SCRIPTABLE_CLASS.getName() + ".hasProperty(internal, $1);");
		clazz.addMethod(hp);
		CtMethod hp2 = new CtMethod(CtClass.booleanType, "hasProperty", new CtClass[]{CtClass.intType}, clazz);
		hp2.setBody("return " + SCRIPTABLE_CLASS.getName() + ".hasProperty(internal, $1);");
		clazz.addMethod(hp2);
		CtMethod gai = new CtMethod(cp.get(Object[].class.getName()), "getAllIds", new CtClass[]{}, clazz);
		gai.setBody("return convertNatives(internal.getAllIds());");
		clazz.addMethod(gai);
		
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
		
		CtClass clazz = cp.makeClass(packageName + "." + "ProxyFactory", cp.get(BaseNativeProxy.class.getName()));
		CtMethod f = new CtMethod(cp.get(INativeObject.class.getName()), "newNativeObject", new CtClass[]{}, clazz);
		f.setBody("return new " + packageName + ".NativeObject();");
		clazz.addMethod(f);
		
		f = new CtMethod(cp.get(INativeObject.class.getName()), "newNativeObject", new CtClass[]{cp.get(IScriptableObject.class.getName())}, clazz);
		f.setBody("return new " + packageName + ".NativeObject($1);");
		clazz.addMethod(f);
		
		f = new CtMethod(cp.get(INativeObject.class.getName()), "newNativeObject", new CtClass[]{cp.get(Object.class.getName())}, clazz);
		f.setBody("return new " + packageName + ".NativeObject($1);");
		clazz.addMethod(f);		
		
		
		f = new CtMethod(cp.get(INativeArray.class.getName()), "newNativeArray", new CtClass[]{}, clazz);
		f.setBody("return new " + packageName + ".NativeArray(new Object[0]);");
		clazz.addMethod(f);
		
		f = new CtMethod(cp.get(INativeArray.class.getName()), "newNativeArray", new CtClass[]{cp.get(IScriptableObject.class.getName())}, clazz);
		f.setBody("return new " + packageName + ".NativeArray($1);");
		clazz.addMethod(f);
		
		f = new CtMethod(cp.get(INativeArray.class.getName()), "newNativeArray", new CtClass[]{cp.get(Object.class.getName())}, clazz);
		f.setBody("return new " + packageName + ".NativeArray(recoverNative($1));");
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
		INativeObject no2 = newNativeObject();
		no2.putProperty("foobar", "snafu");
		no.putProperty("me", no2);
		log("Foo:" + no.getProperty("foo"));
		log("Three:" + no.getProperty("three"));
		INativeArray na = newNativeArray();
		log("Arr:" + na.getClass().getName() + "  Internal:" + na.getUnderlying().getClass().getName());
		na = newNativeArray(new Object[]{"one", "two", "three", no});
		for(int i = 0; i < na.size(); i++) {
			log(na.get(i));
		}
		log("Arr toStr:" + na.toString());
		log("=======================================");
		Object foo = na.getUnderlying();
		newNativeArray(foo);
		
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
	public static INativeObject newNativeObject(Object internal) {
		return PROXY_FACTORY.newNativeObject(internal);
	}
	
	public static INativeArray newNativeArray() {
		return PROXY_FACTORY.newNativeArray();
	}
	public static INativeArray newNativeArray(IScriptableObject internal) {
		return PROXY_FACTORY.newNativeArray(internal);
	}
	
	public static INativeArray newNativeArray(Object internal) {
		return PROXY_FACTORY.newNativeArray(internal);
	}
	
	/**
	 * Determines if the passed object is an instance of the rhino NativeArray class
	 * @param obj The object to test
	 * @return true if the object is an instance of the rhino NativeArray class, false otherwise
	 */
	public static boolean isNativeArray(Object obj) {
		if(obj==null) return false;
		return ARRAY_CLASS.isInstance(obj);
	}
	
	public static INativeArray newNativeArray(Object[] elements) {
		return PROXY_FACTORY.newNativeArray(elements);
	}
	public static IScriptableObject newScriptable(Object obj) {
		if(obj==null) throw new IllegalArgumentException("The passed object was null", new Throwable());
		if(OBJECT_CLASS.isInstance(obj)) {
			return PROXY_FACTORY.newNativeObject(obj);
		} else if(ARRAY_CLASS.isInstance(obj)) {
			return PROXY_FACTORY.newNativeArray(obj);
		} else {
			throw new IllegalArgumentException("The passed object of type [" + obj.getClass().getName() + "] was not a rhino NativeObject or Native Array", new Throwable());
		}
		
	}
	

	
	public static IScriptableObject convertToNative(Object obj) {
		if(obj==null) throw new IllegalArgumentException("The passed object was null", new Throwable());
		if(OBJECT_CLASS.isInstance(obj)) {
			return newNativeObject(obj);
		} else if(ARRAY_CLASS.isInstance(obj)) {
			return newNativeArray(obj);
		} else if(obj instanceof IScriptableObject) {
			return (IScriptableObject)obj;
		} else {
			throw new IllegalArgumentException("The passed object of type [" + obj.getClass().getName() + "] was not a rhino NativeObject or Native Array", new Throwable());
		}
	}
	
	
}
