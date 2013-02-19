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
package org.helios.collector.webservice;

import java.io.UnsupportedEncodingException;

import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.helios.collector.core.CollectorException;
import org.helios.collector.url.URLCollector;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * <p>Title: WebServiceCollector</p>
 * <p>Description: Checks Web Service end points by calling defined web methods
 * 	  and traces availability and other statistics back to Helios OpenTrace.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
@ManagedResource
public class WebServiceCollector extends URLCollector {
	/** XML to be submitted for SOAP style endpoints */
	protected String requestXML = null;
	/** HTTPClient RequestEntity object */
	protected RequestEntity requestEntity = null; 
	/** MIME type of Web Service Request  */
	protected static final String REQUEST_MIME_TYPE = "text/xml; charset=ISO-8859-1";
	/** Web Service Collector Version */
	private static final String WEBSERVICE_COLLECTOR_VERSION="0.1";


	/**
	 * Constructor for REST style web service endpoint
	 */
	public WebServiceCollector(String url, String style){
		super(url);
		isWebServiceEndpoint = true;
		wsStyle = style;
	}	
	
	/**
	 * Constructor for SOAP style web service endpoint
	 */
	public WebServiceCollector(String url, String style, String requestXML){
		super(url);
		isWebServiceEndpoint = true;
		wsStyle = style;
		setRequestXML(requestXML);
	}	
	
	/**
	 * Additional tasks to be done before the first collection for the targeted web service
	 */
	public void postStart() throws CollectorException{
		// Set the appropriate Request entity for this HTTP Method
		if(getWsStyle()!=null && getWsStyle().equalsIgnoreCase("SOAP")){
			if(postMethod!=null){
				postMethod.setRequestEntity(requestEntity);
			}else{
				throw new CollectorException("HTTP Post method is not initialized properly for collector bean: "+this.getBeanName());
			}
		}
	}
	
	/**
	 * @return String version of Helios WebServiceCollector
	 */
	@ManagedAttribute
	public String getCollectorVersion() {
		return "WebServiceCollector v. "+ WEBSERVICE_COLLECTOR_VERSION;
	}
	
	@ManagedAttribute
	public String getRequestXML(){
		return requestXML;
	}
	/**
	 * @param the requestXML to set
	 */
	public void setRequestXML(String requestXML){
		this.requestXML = requestXML;
		if(requestXML!=null){
			try {
				requestEntity = new StringRequestEntity(requestXML, REQUEST_MIME_TYPE, null);
			}catch(UnsupportedEncodingException uex){
				if(logErrors)
					error("An error occured while setting HTTPClient RequestEntity for bean collector: " + this.getBeanName(), uex);
			}
		}
	}

	/**
	 * Constructs a <code>StringBuilder</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString()
	{
	    final String TAB = "    ";
	    StringBuilder retValue = new StringBuilder("");
	    retValue.append("WebServiceCollector ( " + super.toString() + TAB);
	    retValue.append("requestXML = " + this.requestXML + TAB);
	    retValue.append("requestEntity = " + this.requestEntity + TAB);
	    retValue.append(" )");
	
	    return retValue.toString();
	}


}
