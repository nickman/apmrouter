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
package org.helios.apmrouter.wsclient;

import java.util.Collection;
import java.util.Map;
import java.util.Stack;

import org.json.JSONArray;
import org.json.JSONObject;



/**
 * <p>Title: JsonRequestBuilder</p>
 * <p>Description: A fluent style builder for JSON formatted requests to the server </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.wsclient.JsonRequestBuilder</code></p>
 */

public class JsonRequestBuilder {
	/** A stack of json objects that are being push into and popped out of focus */
	protected final Stack<Object> jsonStack = new Stack<Object>();	
	
	/**
	 * Returns a new JsonRequestBuilder initialized with a request id
	 * @return a new JsonRequestBuilder
	 */
	public static JsonRequestBuilder newBuilder() {
		return new JsonRequestBuilder();
	}
	
	/**
	 * Creates a new JsonRequestBuilder
	 */
	protected JsonRequestBuilder() {		
		jsonStack.push(new JsonRequest());
	}
	
	/**
	 * Adds a keyed value to the current json node
	 * @param key The entry key
	 * @param value The entry value
	 * @return this builder
	 */
	public JsonRequestBuilder put(String key, boolean value) {
		if(key==null || key.trim().isEmpty()) throw new IllegalArgumentException("The passed key was null or empty", new Throwable());
		try { ((JSONObject)jsonStack.peek()).put(key, value); } catch (Exception ex) { throw new RuntimeException(ex); }
		return this;
	}
	
	/**
	 * Adds a keyed value to the current json node
	 * @param key The entry key
	 * @param value The entry value
	 * @return this builder
	 */
	public JsonRequestBuilder put(String key, Collection<?> value) {
		if(key==null || key.trim().isEmpty()) throw new IllegalArgumentException("The passed key was null or empty", new Throwable());
		if(value==null) throw new IllegalArgumentException("The passed value was null", new Throwable());
		try { ((JSONObject)jsonStack.peek()).put(key, value); } catch (Exception ex) { throw new RuntimeException(ex); }
		return this;
	}
	
	/**
	 * Adds a keyed value to the current json node
	 * @param key The entry key
	 * @param value The entry value
	 * @return this builder
	 */
	public JsonRequestBuilder put(String key, double value) {
		if(key==null || key.trim().isEmpty()) throw new IllegalArgumentException("The passed key was null or empty", new Throwable());
		try { ((JSONObject)jsonStack.peek()).put(key, value); } catch (Exception ex) { throw new RuntimeException(ex); }
		return this;
	}

	/**
	 * Adds a keyed value to the current json node
	 * @param key The entry key
	 * @param value The entry value
	 * @return this builder
	 */
	public JsonRequestBuilder put(String key, long value) {
		if(key==null || key.trim().isEmpty()) throw new IllegalArgumentException("The passed key was null or empty", new Throwable());
		try { ((JSONObject)jsonStack.peek()).put(key, value); } catch (Exception ex) { throw new RuntimeException(ex); }
		return this;
	}
	
	/**
	 * Adds a keyed value to the current json node
	 * @param key The entry key
	 * @param value The entry value
	 * @return this builder
	 */
	public JsonRequestBuilder put(String key, int value) {
		if(key==null || key.trim().isEmpty()) throw new IllegalArgumentException("The passed key was null or empty", new Throwable());
		try { ((JSONObject)jsonStack.peek()).put(key, value); } catch (Exception ex) { throw new RuntimeException(ex); }
		return this;
	}
	
	/**
	 * Adds a keyed value to the current json node
	 * @param key The entry key
	 * @param value The entry value
	 * @return this builder
	 */
	public JsonRequestBuilder put(String key, Object value) {
		if(key==null || key.trim().isEmpty()) throw new IllegalArgumentException("The passed key was null or empty", new Throwable());
		if(value==null) throw new IllegalArgumentException("The passed value was null", new Throwable());
		try { ((JSONObject)jsonStack.peek()).put(key, value); } catch (Exception ex) { throw new RuntimeException(ex); }
		return this;
	}
	
	/**
	 * Adds a keyed value to the current json node
	 * @param key The entry key
	 * @param value The entry value
	 * @return this builder
	 */
	public JsonRequestBuilder put(String key, Map<?,?> value) {
		if(key==null || key.trim().isEmpty()) throw new IllegalArgumentException("The passed key was null or empty", new Throwable());
		if(value==null) throw new IllegalArgumentException("The passed value was null", new Throwable());
		try { ((JSONObject)jsonStack.peek()).put(key, value); } catch (Exception ex) { throw new RuntimeException(ex); }
		return this;
	}
	
	/**
	 * Adds a new JSONObject to the current content and pushes it onto the context stack
	 * @param key The key of the new JSONObject
	 * @return this builder
	 */
	public JsonRequestBuilder putJSONObject(String key) {
		if(key==null || key.trim().isEmpty()) throw new IllegalArgumentException("The passed key was null or empty", new Throwable());
		typeCheck(JSONObject.class);
		JSONObject value = new JSONObject();
		try { ((JSONObject)jsonStack.peek()).put(key, value); } catch (Exception ex) { throw new RuntimeException(ex); }
		jsonStack.push(value);
		return this;
	}
	
	/**
	 * Appends the passed value to the JSONArray in current context
	 * @param value The value to append to the array
	 * @return this builder
	 */
	public JsonRequestBuilder append(boolean value) {
		typeCheck(JSONArray.class);
		try { ((JSONArray)jsonStack.peek()).put(value); } catch (Exception ex) { throw new RuntimeException(ex); }
		return this;
	}
	
	/**
	 * Appends the passed value to the JSONArray in current context
	 * @param value The value to append to the array
	 * @return this builder
	 */
	public JsonRequestBuilder append(Collection<?> value) {
		typeCheck(JSONArray.class);
		if(value==null) throw new IllegalArgumentException("The passed value was null", new Throwable());
		try { ((JSONArray)jsonStack.peek()).put(value); } catch (Exception ex) { throw new RuntimeException(ex); }
		return this;
	}
	
	/**
	 * Appends the passed value to the JSONArray in current context
	 * @param value The value to append to the array
	 * @return this builder
	 */
	public JsonRequestBuilder append(double value) {
		typeCheck(JSONArray.class);
		try { ((JSONArray)jsonStack.peek()).put(value); } catch (Exception ex) { throw new RuntimeException(ex); }
		return this;
	}

	/**
	 * Appends the passed value to the JSONArray in current context
	 * @param value The value to append to the array
	 * @return this builder
	 */
	public JsonRequestBuilder append(long value) {
		typeCheck(JSONArray.class);
		try { ((JSONArray)jsonStack.peek()).put(value); } catch (Exception ex) { throw new RuntimeException(ex); }
		return this;
	}
	
	/**
	 * Appends the passed value to the JSONArray in current context
	 * @param value The value to append to the array
	 * @return this builder
	 */
	public JsonRequestBuilder append(int value) {
		typeCheck(JSONArray.class);
		try { ((JSONArray)jsonStack.peek()).put(value); } catch (Exception ex) { throw new RuntimeException(ex); }
		return this;
	}
	
	/**
	 * Appends the passed value to the JSONArray in current context
	 * @param value The value to append to the array
	 * @return this builder
	 */
	public JsonRequestBuilder append(Object value) {
		typeCheck(JSONArray.class);
		if(value==null) throw new IllegalArgumentException("The passed value was null", new Throwable());
		try { ((JSONArray)jsonStack.peek()).put(value); } catch (Exception ex) { throw new RuntimeException(ex); }
		return this;
	}
	
	/**
	 * Appends the passed value to the JSONArray in current context
	 * @param value The value to append to the array
	 * @return this builder
	 */
	public JsonRequestBuilder append(Map<?,?> value) {
		typeCheck(JSONArray.class);
		if(value==null) throw new IllegalArgumentException("The passed value was null", new Throwable());
		try { ((JSONArray)jsonStack.peek()).put(value); } catch (Exception ex) { throw new RuntimeException(ex); }
		return this;
	}
	
	
	/**
	 * Adds a new JSONArray to the current content and pushes it onto the context stack
	 * @param key The key of the new JSONArray
	 * @return this builder
	 */
	public JsonRequestBuilder putJSONArray(String key) {
		if(key==null || key.trim().isEmpty()) throw new IllegalArgumentException("The passed key was null or empty", new Throwable());
		typeCheck(JSONObject.class);
		JSONArray value = new JSONArray();
		try { ((JSONObject)jsonStack.peek()).put(key, value); } catch (Exception ex) { throw new RuntimeException(ex); }
		jsonStack.push(value);
		return this;
	}
	
	
	
	/**
	 * Pops the current element off the context stack
	 * @return this builder
	 */
	public JsonRequestBuilder pop() {
		jsonStack.pop();
		return this;
	}
	
	/**
	 * Completes the build and returns the root JSONObject
	 * @return the root JSONObject
	 */
	public JsonRequest build() {
		typeCheck(JsonRequest.class);
		if(jsonStack.size()!=1) throw new RuntimeException("Incomplete pop state. Expected context size of 1  but was " + jsonStack.size(), new Throwable());
		return (JsonRequest)jsonStack.pop();
	}
	
	
	/**
	 * Executes a type check against the current stack context
	 * @param expectedClass The expected class of the current context
	 */
	protected void typeCheck(Class<?> expectedClass) {
		if(!expectedClass.isInstance(jsonStack.peek())) throw new IllegalStateException("Unassignable Op. Expected context: [" + expectedClass.getSimpleName() + "] Actual Context: [" + jsonStack.peek().getClass().getSimpleName() + "]", new Throwable());
	}
	
	
	
	
	

	/*
	EXAMPLE:
	=======
			JSONObject request = new JSONObject();
			int reqId = client.requestSerial.incrementAndGet();
			request.put("rid", reqId);
			request.put("t", "req");
			request.put("svc", "sub");
			request.put("op", "start");
			JSONObject ags = new JSONObject();
			ags.put("es", "jmx");
			ags.put("esn", "service:jmx:local://DefaultDomain");
			ags.put("f", "org.helios.apmrouter.session:service=SharedChannelGroup");
			request.put("args", ags);
	
	 */
	
}
