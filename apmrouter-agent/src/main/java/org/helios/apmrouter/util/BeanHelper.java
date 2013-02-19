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
package org.helios.apmrouter.util;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.Expression;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>Title: BeanHelper</p>
 * <p>Description: A set of generic bean helper operations.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision: 222 $
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-utils/trunk/src/org/helios/helpers/BeanHelper.java $
 * $Id: BeanHelper.java 222 2008-07-08 21:05:12Z nwhitehead $
 */
public class BeanHelper {
	/**
	 * Locates the PropertyDescriptor for the passed name in the passed class.
	 * @param targetType
	 * @param name
	 * @return A PropertyDescriptor
	 */
	public static PropertyDescriptor getPropertyDescriptor(Class<?> targetType, String name)  {
		String dName = Introspector.decapitalize(name);
		try {
			BeanInfo bi = Introspector.getBeanInfo(targetType);
			for(PropertyDescriptor pd: bi.getPropertyDescriptors()) {
				if(pd.getName().equals(dName)) {
					return pd;
				}
			}
			throw new Exception("No PropertyDescriptor found for " + name);
		} catch (Exception e) {
			throw new RuntimeException("Failed to Acquire PropertyDescriptor for [" + targetType.getName() + "/" + name + "]", e);
		}
		
	}
	
	/**
	 * Determines the type of the named parameter for the passed type.
	 * @param targetType The target type to invoke against.
	 * @param name The attribute name.
	 * @return The class of the parameter type
	 */
	public static Class<?> getAttributeType(Class<?> targetType, String name)  {		
		PropertyDescriptor pd = getPropertyDescriptor(targetType, name);
		return pd.getPropertyType();				
	}
	
	
	/**
	 * Sets an attribute on the passed object instance.
	 * @param name The name of the attribute.
	 * @param value The string value.
	 * @param target The object instance to set on.
	 */
	public static void setAttribute(String name, String value, Object target) {
		try {
			Class<?> targetType = getAttributeType(target.getClass(), name);
			PropertyEditor propertyEditor = PropertyEditorManager.findEditor(targetType);
			if(propertyEditor==null) throw new RuntimeException("No PropertyEditor Found for type [" + targetType.getName() + "] in object of type + [" + target.getClass().getName() + "]" );
			propertyEditor.setAsText(value);
			Object typedValue = propertyEditor.getValue();
			Expression expression = new Expression(target, getPropertyDescriptor(target.getClass(), name).getWriteMethod().getName(), new Object[]{typedValue});
			expression.execute();
		} catch (Exception e) {
			throw new RuntimeException("Failed to set attribute [" + name + "] on instance of  [" + target.getClass().getName() + "]");
		}
	}
	
	/**
	 * Returns the value of the named attribute from the passed pojo.
	 * @param name The name of the attribute.
	 * @param target The target pojo to retrieve the value from.
	 * @return the value of the named attribute.
	 */
	public static Object getAttribute(String name, Object target) {
		try {
			Class<?> targetType = getAttributeType(target.getClass(), name);
			PropertyEditor propertyEditor = PropertyEditorManager.findEditor(targetType);
			if(propertyEditor==null) throw new RuntimeException("No PropertyEditor Found for type [" + targetType.getName() + "] in object of type + [" + target.getClass().getName() + "]" );
			
			Expression expression = new Expression(target, getPropertyDescriptor(target.getClass(), name).getReadMethod().getName(), new Object[]{});
			expression.execute();
			return expression.getValue();
			
		} catch (Exception e) {
			throw new RuntimeException("Failed to get attribute [" + name + "] on instance of  [" + target.getClass().getName() + "]");
		}
		
	}
	
	public static Object getInstance(String className, Object...args) {
		try {			
			Class<?> clazz = Class.forName(className);
			Class<?>[] sig = new Class[args.length];
			for(int i = 0; i < args.length; i++) {
				sig[i] = args[i].getClass();
			}
			Constructor<?> ctor = clazz.getConstructor(sig);
			return ctor.newInstance(args);			
		} catch (Throwable t) {
			throw new RuntimeException("Failed to create test pojo:" + className, t);
		}
	}
	
	
	public static BeanInfo getBeanInfo(Object pojo) {
		try {
			return Introspector.getBeanInfo(pojo.getClass());
		} catch (Exception e) {
			throw new RuntimeException("Failed to create bean info", e);
		}
	}
	
	public static BeanDescriptor getBeanDescriptor(Object pojo) {
		try {
			return new BeanDescriptor(pojo.getClass());
		} catch (Exception e) {
			throw new RuntimeException("Failed to create bean descriptor", e);
		}
	}	
	
	public static String[] repeatingArr(String value, int times) {
		String[] arr = new String[times];
		for(int i =0; i < times; i++) {
			arr[i] = value;
		}
		return arr;
	}
	

	
	/**
	 * Gets a complete list of all a pojo's attribute names.
	 * <code>@JMXAttribute</code> annotations are ignored.
	 * @param pojo
	 * @param exclude attributes defined in Object class.
	 * @return A list of pojo attribute names.
	 */
	public static List<String> getPojoBeanAttributes(Object pojo, boolean declared) {
		BeanInfo bi = getBeanInfo(pojo);
		List<String> list = new ArrayList<String>();		
		for(PropertyDescriptor pd : bi.getPropertyDescriptors()) {
			list.add(pd.getName());
		}
		if(pojo.getClass()!=Object.class) {
			list.removeAll(getPojoBeanAttributes(new Object(), false));
		}
		return list;
	}
	
	/**
	 * Gets a complete list of all a pojo's attribute names.
	 * <code>@JMXAttribute</code> annotations are ignored.
	 * Declared defaults to true.
	 * @param pojo
	 * @return A list of pojo attribute names.
	 */
	public static List<String> getPojoBeanAttributes(Object pojo) {
		return getPojoBeanAttributes(pojo, true);
	}
	
	
	
	/**
	 * Returns an alpha sorted string array of a pojo's attribute names.
	 * @param pojo
	 * @return A string array of attribute names.
	 */
	public static String[] getPojoAttributeTypes(Object pojo) {
		List<String> list = new ArrayList<String>();		
		for(PropertyDescriptor pd : getPropertyDescriptors(pojo)) {
			if(pd.getReadMethod()!=null) {
				list.add(pd.getReadMethod().getReturnType().getName());
			} else {
				list.add(pd.getWriteMethod().getReturnType().getName());
			}
		}
		Collections.sort(list);
		return list.toArray(new String[list.size()]);
	}
	
	/**
	 * Returns a list of all of a pojo's <code>PropertyDescriptor</code>s
	 * @param pojo
	 * @return A list of <code>PropertyDescriptor</code>s.
	 */
	public static List<PropertyDescriptor> getAllPropertyDescriptors(Object pojo) {
		List<PropertyDescriptor> list = new ArrayList<PropertyDescriptor>();
		BeanInfo bi = getBeanInfo(pojo);
		for(PropertyDescriptor pd : bi.getPropertyDescriptors()) {
			list.add(pd);
		}
		return list;
	}
	
	/**
	 * Sorts an array of objects that implement the comparable interface.
	 * @param args An array of objects.
	 * @return An array of objects sorted by their natural order.
	 */
	@SuppressWarnings("unchecked")
	public static Object[] sortArray(Object...args) {
		List<Comparable> list = new ArrayList<Comparable>(args.length);
		for(Object o: args) {list.add((Comparable)o);}
		Collections.sort(list);
		return list.toArray(new Object[list.size()]);
	}
	
	/**
	 * Sorts an array of objects that implement the comparable interface and returns a typed array specified by the class passed.
	 * @param arrayType
	 * @param args
	 * @return An array of objects sorted by their natural order.
	 */
	@SuppressWarnings("unchecked")
	public static Object sortArray(Class arrayType, Object...args) {
		List<Comparable> list = new ArrayList<Comparable>(args.length);
		for(Object o: args) {list.add((Comparable)o);}
		Collections.sort(list);
		Object arrObject = Array.newInstance(arrayType, list.size());
		for(int i = 0; i < list.size(); i++) {
			Array.set(arrObject, i, list.get(i));
		}
		return arrObject;
	}
	
	
	/**
	 * Returns a list of a pojo's <code>PropertyDescriptor</code>s minus the same for <code>java.lang.Object</code>.
	 * @param pojo
	 * @return A list of <code>PropertyDescriptor</code>s.
	 */
	public static List<PropertyDescriptor> getPropertyDescriptors(Object pojo) {
		List<PropertyDescriptor> list = getAllPropertyDescriptors(pojo);
		List<PropertyDescriptor> objList = getAllPropertyDescriptors(new Object());
		list.removeAll(objList);
		return list;
	}

	
	/**
	 * Constructs an object of the passed class name from the passed string.
	 * @param targetClassName The class name of the object to be created.
	 * @param value The string to be used to construct the return object.
	 * @return The constructed object.
	 */
	public static Object stringToObject(String targetClassName, String value) {
		Object object = null;
		String arrayType = null;
		String[] arrayElements = null;
		Object convertedArrayElement = null;
		Object returnArray = null;
		Class<?> arrayClass = null;
		try {			
			if(targetClassName.startsWith("[L") && targetClassName.endsWith(";")) {
				arrayType = targetClassName.replace("[L", "");				
				arrayType = arrayType.replace(";", "");
				arrayClass = Class.forName(targetClassName, false, BeanHelper.class.getClassLoader()).getComponentType();
				
				arrayElements = value.split(",");
				returnArray = Array.newInstance(arrayClass, arrayElements.length);
				int index=0; 
				for(String s: arrayElements) {
					convertedArrayElement = stringToObject(arrayType, s);
					Array.set(returnArray, index, convertedArrayElement);	
					index++;
				}
				object = returnArray;
			} else if("java.lang.String".equals(targetClassName) || "java.lang.Object".equals(targetClassName)) {
				object = value;
			} else if("java.lang.Boolean".equals(targetClassName) || "boolean".equals(targetClassName)) {
				object = Boolean.valueOf(value);
			} else if("java.lang.Byte".equals(targetClassName) || "byte".equals(targetClassName)) {
				object = new Byte(value);
			}  else if("java.lang.Character".equals(targetClassName) || "char".equals(targetClassName)) {
				object = Character.valueOf(value.charAt(0));
			}  else if("java.lang.Class".equals(targetClassName)) {
				object = Class.forName(value);
			}  else if("java.lang.Double".equals(targetClassName) || "double".equals(targetClassName)) {
				object = new Double(value);
			}  else if("java.lang.Float".equals(targetClassName) || "float".equals(targetClassName)) {
				object = new Float(value);
			}  else if("java.lang.Integer".equals(targetClassName) || "int".equals(targetClassName)) {
				object = new Integer(value);
			}  else if("java.lang.Long".equals(targetClassName) || "long".equals(targetClassName)) {
				object = new Long(value);
			} else {
				throw new Exception("Unsupported Conversion Type[" +  targetClassName + "]. Please Update " + BeanHelper.class.getName() + ".stringToObject Method");
			}
			return object;
		} catch (Exception e) {
			throw new RuntimeException("Failed to convert value [" + value + "] to an object of type [" + targetClassName + "]", e);
		}
		
	}
	

}
