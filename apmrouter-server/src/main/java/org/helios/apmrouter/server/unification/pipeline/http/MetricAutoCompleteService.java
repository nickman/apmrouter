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
package org.helios.apmrouter.server.unification.pipeline.http;

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.helios.apmrouter.catalog.api.impl.DataServiceInterceptor;
import org.helios.apmrouter.logging.APMLogLevel;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>Title: MetricAutoCompleteService</p>
 * <p>Description: Http service to provide autocomplete responses to UI components</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline.http.MetricAutoCompleteService</code></p>
 */

public class MetricAutoCompleteService extends AbstractHttpRequestHandler {
	/** The hibernate session factory from wot we's gettin the autoz complete */
	@Autowired(required=true)
	protected SessionFactory sessionFactory = null;
	/** The logging interceptor for hibernate queries, used if the logging level is DEBUG */
	protected final DataServiceInterceptor dsi = new DataServiceInterceptor();
	/**
	 * Creates a new MetricAutoCompleteService
	 */
	public MetricAutoCompleteService() {
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline.http.HttpRequestHandler#handle(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent, org.jboss.netty.handler.codec.http.HttpRequest, java.lang.String)
	 */
	@Override
	public void handle(ChannelHandlerContext ctx, MessageEvent e, HttpRequest request, String path) throws Exception {
        if (request.getMethod() != GET) {
            sendError(ctx, METHOD_NOT_ALLOWED);
            return;
        }
        
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
        Map<String, List<String>> params = queryStringDecoder.getParameters();

        byte[] resolved = resolveLookup(params).toString().getBytes();
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, resolved.length);
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/json");
        response.setContent(ChannelBuffers.wrappedBuffer(resolved));
        final Channel channel = ctx.getChannel();
        
        ChannelFuture writeFuture =  channel.write(response);

        // Decide whether to close the connection or not.
        if (writeFuture!=null && !isKeepAlive(request)) {
            // Close the connection when the whole content is written out.
            writeFuture.addListener(ChannelFutureListener.CLOSE);
        }
	}
	
	/** Forward slash splitter pattern */
	protected static final Pattern SLASH_SPLITTER = Pattern.compile("/");
	/** An empty string array constant */
	protected static final String[] EMPTY_STR_ARR = {};
	
	/**
	 * Returns the split and trimmed string array from splitting the passed value. 
	 * The array will not include any null or empty strings
	 * @param value The string value to split
	 * @return a [possibly empty] array of string
	 */
	protected static String[] trimmedSplit(CharSequence value) {
		if(value==null) return EMPTY_STR_ARR;
		String[] fragments = SLASH_SPLITTER.split(value);
		if(fragments.length==0) return EMPTY_STR_ARR;
		List<String> nonblank = new ArrayList<String>(fragments.length);
		for(String s: fragments) {
			if(s==null || s.trim().isEmpty()) continue;
			nonblank.add(s.trim());
		}
		if(nonblank.isEmpty()) return EMPTY_STR_ARR;
		return nonblank.toArray(new String[0]);
	}
	
	/**
	 * Reads the autocomplete query from the request and resolves the matching values from hibernate
	 * @param params The http request paramters
	 * @return A JSON object containing the resolved items
	 * @throws Exception thrown on any error
	 */
	protected JSONObject resolveLookup(Map<String, List<String>> params) throws Exception {
		Session session = null;
		try {
			String current = params.get("term").get(0);
			String path = (current==null || current.trim().isEmpty()) ? "" : current.trim();
			JSONObject resp = new JSONObject();
			String[] fragments = trimmedSplit(path);
			if(level.isEnabledFor(APMLogLevel.DEBUG )) {
				session = sessionFactory.openSession(dsi);
			} else {
				session = sessionFactory.openSession();
			}
			Query query = null;
			
			switch (fragments.length) {
				case 0:  	// looking for a domain
					query = session.getNamedQuery("allDomains");					
					break;					
				case 1:
					query = session.getNamedQuery("searchDomains");
					query.setString("domain", fragments[0] + "%");
					break;
				case 2:
					query = session.getNamedQuery("searchDomains");
					break;
				
				case 3:
					
					break;

				default:  // we have a domain, host and agent, and possible some namespaces
					
			}
			
			resp.put("path", path);
			return resp;
		} catch (Exception ex) {
			JSONObject err = new JSONObject();
			err.put("err", ex.toString());
			return err;
		} finally {
			if(session!=null) try { session.close(); } catch (Exception ex) {}
		}
	}

}
