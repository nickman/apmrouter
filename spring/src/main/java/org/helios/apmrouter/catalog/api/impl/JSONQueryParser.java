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
package org.helios.apmrouter.catalog.api.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;
import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * <p>Title: JSONQueryParser</p>
 * <p>Description: An event driven parser to transform a JSON object into a hibernate query</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.api.impl.JSONQueryParser</code></p>
 */

public class JSONQueryParser implements ContentHandler {
	/** Static class logger  */
	private static final Logger LOG = Logger.getLogger(JSONQueryParser.class);
	
	protected final AtomicInteger ind = new AtomicInteger(0);
	protected final ThreadLocal<Stack<String>> currentKey = new ThreadLocal<Stack<String>>(){
		@Override
		protected Stack<String> initialValue() {
			return new Stack<String>();
		}		
	};
	protected final ThreadLocal<Stack<Parsed>> parsed = new ThreadLocal<Stack<Parsed>>(){
		@Override
		protected Stack<Parsed> initialValue() {
			return new Stack<Parsed>();
		}
	};
	
	private void log(Object msg) {
		StringBuilder b = new StringBuilder();
		for(int i = 0; i < ind.get(); i++) { b.append("\t"); }
		b.append(msg);
		System.out.println(b);
	}
	
	public ExtendedDetachedCriteria parse(String jsonText) {
		JSONParser parser = new JSONParser();
		ind.set(0);		
		try {
			parser.parse(jsonText, this);
			return (ExtendedDetachedCriteria)parsed.get().pop();
		} catch (Exception ex) {
			throw new RuntimeException("Parser failure", ex);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.json.simple.parser.ContentHandler#endArray()
	 */
	@Override
	public boolean endArray() throws ParseException, IOException {
		ind.decrementAndGet();
		log("End Array");		
		Parsed<Object[]> arrayAccumulator = parsed.get().pop();
		parsed.get().peek().applyPrimitive(currentKey.get().peek(), arrayAccumulator.get());
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see org.json.simple.parser.ContentHandler#endJSON()
	 */
	@Override
	public void endJSON() throws ParseException, IOException {
		ind.decrementAndGet();
		log("End JSON");		
	}

	/**
	 * {@inheritDoc}
	 * @see org.json.simple.parser.ContentHandler#endObject()
	 */
	@Override
	public boolean endObject() throws ParseException, IOException {
		ind.decrementAndGet();
		log("End Object");				
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see org.json.simple.parser.ContentHandler#endObjectEntry()
	 */
	@Override
	public boolean endObjectEntry() throws ParseException, IOException {
		ind.decrementAndGet();
		log("End ObjectEntry");		
		currentKey.get().pop();
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see org.json.simple.parser.ContentHandler#primitive(java.lang.Object)
	 */
	@Override
	public boolean primitive(Object value) throws ParseException, IOException {
		log("Primitive:" + value);
		parsed.get().peek().applyPrimitive(currentKey.get().peek(), value);
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see org.json.simple.parser.ContentHandler#startArray()
	 */
	@Override
	public boolean startArray() throws ParseException, IOException {
		log("Start Array");
		ind.incrementAndGet();
		parsed.get().push(new ArrayAccumulator());
		
		
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see org.json.simple.parser.ContentHandler#startJSON()
	 */
	@Override
	public void startJSON() throws ParseException, IOException {
		log("Start JSON");
		ind.incrementAndGet();		
	}

	/**
	 * {@inheritDoc}
	 * @see org.json.simple.parser.ContentHandler#startObject()
	 */
	@Override
	public boolean startObject() throws ParseException, IOException {
		log("Start Object");
		ind.incrementAndGet();
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see org.json.simple.parser.ContentHandler#startObjectEntry(java.lang.String)
	 */
	@Override
	public boolean startObjectEntry(String entryName) throws ParseException, IOException {
		log("Start Object Entry [" + entryName + "]");
		ind.incrementAndGet();
		if(parsed.get().size()==0) {
			parsed.get().push(ParsedFactory.createPrimary(entryName));
			log("=== Created [" + parsed.get().peek().getClass().getSimpleName() + "]");		
		}
		currentKey.get().push(entryName);
		return true;
	}

}
