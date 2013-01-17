/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2012, Helios Development Group and individual contributors
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
package org.helios.apmrouter.subscription.impls.jmx;

import java.util.HashSet;
import java.util.Set;

import javax.management.NotificationFilter;
import javax.management.NotificationFilterSupport;
import javax.management.ObjectName;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.helios.apmrouter.dataservice.json.JsonRequest;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.subscription.criteria.SubscriptionCriteria;
import org.helios.apmrouter.subscription.criteria.builder.AbstractSubscriptionCriteriaBuilder;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * <p>Title: JMXSubscriptionCriteriaBuilder</p>
 * <p>Description: A subscription criteria builder for JMX</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.impls.jmx.JMXSubscriptionCriteriaBuilder</code></p>
 */
public class JMXSubscriptionCriteriaBuilder extends AbstractSubscriptionCriteriaBuilder<String, ObjectName, NotificationFilter> {
	/** The script engine manager for compiling notification filters */
	protected static final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
	/** The script engine for compiling notification filters */
	protected static final ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("JavaScript");  
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.criteria.builder.SubscriptionCriteriaBuilder#build(org.helios.apmrouter.dataservice.json.JsonRequest)
	 */
	@Override
	public SubscriptionCriteria<String, ObjectName, NotificationFilter> build(JsonRequest subRequest)  {
		String eventSource = subRequest.getArgument(JSON_EVENT_SOURCE, ""); // e.g. "jmx"		
		if(eventSource.isEmpty()) throw new RuntimeException("No event source type provided", new Throwable());
		String eventSourceName = subRequest.getArgument(JSON_EVENT_SOURCE_NAME, ""); // e.g. "service:jmx:local://DefaultDomain"		
		if(eventSource.isEmpty()) throw new RuntimeException("No event source name provided", new Throwable());
		
		ObjectName objectName = JMXHelper.objectName(subRequest.getArgument(JSON_EVENT_FILTER, ""));
		String filterExpression = subRequest.getArgumentOrNull(JSON_EXTENDED_EVENT_FILTER, String.class);
		JSONArray simpleFilterExpression = subRequest.getArgumentOrNull(JSON_SIMPLE_TYPE_FILTER, JSONArray.class);
		
		NotificationFilter filter = null;
		Set<String> enabledTypes = new HashSet<String>();
		if(simpleFilterExpression!=null) {
			filter = new NotificationFilterSupport();
			for(int i = 0; i < simpleFilterExpression.length(); i++) {
				try {
					((NotificationFilterSupport)filter).enableType(simpleFilterExpression.getString(i).trim());
					enabledTypes.add(simpleFilterExpression.getString(i).trim());
				} catch (IllegalArgumentException iex) { 
					throw new RuntimeException("Failed to parse simple filter expression", iex);
				} catch (JSONException jex) {
					throw new RuntimeException("Failed to parse simple filter expression", jex);
				}
			}

		} else if(filterExpression!=null && !filterExpression.trim().isEmpty()) {
			filter = compileFilter(filterExpression);
		}
		JMXSubscriptionCriteria criteria = new JMXSubscriptionCriteria(this, eventSourceName, objectName, filter);
		if(!enabledTypes.isEmpty()) {
			criteria.setSubcriptionKey(enabledTypes.toArray(new String[enabledTypes.size()]));
		}
		return criteria;
	}
	
	/**
	 * Compiles the filter expression into a NotificationFilter
	 * @param filterExpression the supplied expression
	 * @return a NotificationFilter
	 */
	protected NotificationFilter compileFilter(String filterExpression) {
		try {
			scriptEngine.eval(filterExpression);
			Invocable inv = (Invocable)scriptEngine;
			return inv.getInterface(NotificationFilter.class);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to compile filter [" + filterExpression + "]", ex);
			
		}
	}

}
