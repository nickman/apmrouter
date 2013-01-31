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
package org.helios.apmrouter.dataservice.json.catalog;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helios.apmrouter.catalog.MetricCatalogService;
import org.helios.apmrouter.catalog.api.impl.DataServiceInterceptor;
import org.helios.apmrouter.catalog.domain.AgentMetricSet;
import org.helios.apmrouter.catalog.domain.DomainObject;
import org.helios.apmrouter.dataservice.json.JSONRequestHandler;
import org.helios.apmrouter.dataservice.json.JsonRequest;
import org.helios.apmrouter.dataservice.json.JsonResponse;
import org.helios.apmrouter.dataservice.json.marshalling.JSONMarshaller;
import org.helios.apmrouter.server.ServerComponentBean;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.NamedQueryDefinition;
import org.hibernate.impl.SessionFactoryImpl;
import org.jboss.netty.channel.Channel;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>Title: CatalogJSONDataService</p>
 * <p>Description: Data services for the metric catalog service</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.catalog.CatalogJSONDataService</code></p>
 */
@JSONRequestHandler(name="catalog")
public class CatalogJSONDataService extends ServerComponentBean {
	/** The metric catalog service */
	protected MetricCatalogService catalog = null;
	/** The hibernate session factory */
	protected SessionFactory sessionFactory = null;
	/** The Json marshaller */
	protected JSONMarshaller marshaller = null;
	/** The named queries map */
	protected final Map<String, NamedQueryDefinition> namedQueries = new HashMap<String, NamedQueryDefinition>();
	
	/** The namedQueries map field */
	private static final Field namedQueriesField;
	/** The namedSqlQueries map field */
	private static final Field namedSqlQueriesField;
	
	static {
		try {
			namedQueriesField = SessionFactoryImpl.class.getDeclaredField("namedQueries");
			namedQueriesField.setAccessible(true);
			namedSqlQueriesField = SessionFactoryImpl.class.getDeclaredField("namedSqlQueries");
			namedSqlQueriesField.setAccessible(true);			
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	protected void doStart() throws Exception {
		super.doStart();
		namedQueries.putAll((Map<String, NamedQueryDefinition>) namedQueriesField.get(sessionFactory));
		namedQueries.putAll((Map<String, NamedQueryDefinition>) namedSqlQueriesField.get(sessionFactory));
	}

	/**
	 * Sets the metric catalog service
	 * @param catalog the metric catalog service
	 */
	@Autowired(required=true)
	public void setCatalog(MetricCatalogService catalog) {
		this.catalog = catalog;
	}
	
	/**
	 * Sets the hibernate session factory
	 * @param sessionFactory the hibernate session factory
	 */
	@Autowired(required=true)
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	
	/**
	 * Returns a JSON list of hosts
	 * @param request The JSON request
	 * @param channel The channel to respond on
	 */
	@JSONRequestHandler(name="listhosts")
	public void listHosts(JsonRequest request, Channel channel)   {
		boolean onlineOnly = request.getArgument(0, false);
		Map<Integer, String> hosts = catalog.listHosts(onlineOnly);
		JsonResponse response = request.response();
		response.setContent(hosts);
		channel.write(response);
	}
	
	/**
	 * Dumps a JSON stream describing all the named queries available to a JSON client
	 * @param request The json request
	 * @param channel The chennel to respond on
	 */
	@JSONRequestHandler(name="namedqueries")
	public void listNamedQueries(JsonRequest request, Channel channel)   {
		JsonResponse response = request.response();
		Map<String, NamedQueryDefinitionContainer> nqs = new HashMap<String, NamedQueryDefinitionContainer>();
		for(Map.Entry<String, NamedQueryDefinition> entry: namedQueries.entrySet()) {
			nqs.put(entry.getKey(), new NamedQueryDefinitionContainer(entry.getKey(), entry.getValue()));
		}
		response.setContent(nqs);
		channel.write(response);
	}
	
	
	/**
	 * Processes a named query
	 * @param request The JSON encoded named query request
	 * @param channel The channel to write the response to
	 */
	@JSONRequestHandler(name="nq")
	public void processNamedQuery(JsonRequest request, Channel channel) {
		String name = request.getArgumentOrNull("name", String.class);
		if(name==null) throw new RuntimeException("The named-query name was null", new Throwable());
		Map<String, ?> params = request.getArgument("p", new HashMap<String, Object>());

		Session session = null;
		try {
			session = sessionFactory.openSession(new DataServiceInterceptor());
			Query query = session.getNamedQuery(name);
			NamedQueryDefinition nqd = namedQueries.get(name);
			
			
			for(Map.Entry<String, ?> param: params.entrySet()) {
				String type = nqd.getParameterTypes().get(param.getKey()).toString();
				PropertyEditor pe = PropertyEditorManager.findEditor(Class.forName(type));
				if(param.getValue().toString().equals("null")) {
					warn("Param with null value [", param.getKey(), "]");
				}
				pe.setAsText(param.getValue().toString());
				query.setParameter(param.getKey(), pe.getValue());
			}
			List<?> results = query.list();
			if(!results.isEmpty() && results.iterator().next() instanceof DomainObject) {
				Object obj = results.toArray(new DomainObject[0]);
				channel.write(request.response().setContent(obj));
			} else {
				channel.write(request.response().setContent(results));
			}
		} catch (Exception ex) {
			error("Failed to execute named query [", name, "] with params [" + params + "]", ex);
		} finally {
			if(session!=null && session.isOpen()) try { session.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Returns an {@link AgentMetricSet} for the agent with the passed agent Id
	 * @param request The JSON encoded named query request
	 * @param channel The channel to write the response to
	 */
	@JSONRequestHandler(name="ams")
	public void agentMetricSet(JsonRequest request, Channel channel) {		
		Session session = null;
		try {
			int agentId = Integer.parseInt(request.getArgument("agentId"));
			session = sessionFactory.openSession();
			channel.write(request.response().setContent(AgentMetricSet.newInstance(session, agentId)));
		} catch (Exception ex) {
			error("Failed to execute agentMetricSet [" + request + "]", ex);
		} finally {
			if(session!=null && session.isOpen()) try { session.close(); } catch (Exception e) {/* No Op */}
		}
 
	}
	

	/**
	 * Sets the object Json marshaller
	 * @param marshaller the object Json marshaller
	 */
	@Autowired(required=true)
	public void setMarshaller(JSONMarshaller marshaller) {
		this.marshaller = marshaller;
	}
	
	
}
