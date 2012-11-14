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
package org.helios.apmrouter.dataservice.json.marshalling;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.helios.apmrouter.server.ServerComponentBean;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.json.JSONObject;
import org.springframework.jmx.export.annotation.ManagedAttribute;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;

/**
 * <p>Title: GSONJSONMarshaller</p>
 * <p>Description: JSON marshalling service, implemented using <a href="https://sites.google.com/site/gson/">Google-gson</a>to centralize the marshalling of outgoing objects into json.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.marshalling.GSONJSONMarshaller</code></p>
 */

public class GSONJSONMarshaller extends ServerComponentBean implements JSONMarshaller {
	/** Sets the pretty-print attribute of the gson builder */
	protected boolean prettyPrint = false;
	/** Sets the HTML escaping attribute of the gson builder */
	protected boolean disableHtmlEscaping = false;
	/** Sets the null serialization attribute of the gson builder */
	protected boolean serializeNulls = false;
	
	/** Custom type adapters */
	protected final Map<Type, Object> adapterInstances = new ConcurrentHashMap<Type, Object>();
	/** Custom type adapter factories */
	protected final Set<TypeAdapterFactory> adapterFactoryInstances = new CopyOnWriteArraySet<TypeAdapterFactory>();
	
	/** The gson instance */
	protected Gson gson = null;
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.marshalling.JSONMarshaller#marshallToChannel(java.lang.Object)
	 */
	public ChannelBuffer marshallToChannel(Object obj) {
		if(obj==null) return ChannelBuffers.buffer(0);
		byte[] bytes = null;
		if(obj instanceof CharSequence || obj instanceof JSONObject) {
			bytes = obj.toString().getBytes();
		} else {
			bytes = gson.toJson(obj).getBytes();
		}				
		ChannelBuffer cb = ChannelBuffers.directBuffer(bytes.length);
		cb.writeBytes(bytes);
		bytes = null;
		return cb;
	}
	
	/**
	 * Marshalls the passed object into JSON and then writes the JSON to a channel buffer and then writes the channel buffer to the passed channel
	 * @param obj The object to marshall
	 * @param channel The channel to write to
	 */
	public void marshallToChannel(Object obj, Channel channel) {
		channel.write(marshallToChannel(obj));
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.marshalling.JSONMarshaller#marshallToText(java.lang.Object)
	 */
	public String marshallToText(Object obj) {
		if(obj==null) return "";
		if(obj instanceof CharSequence || obj instanceof JSONObject) {
			return obj.toString();
		} 
		return gson.toJson(obj);		
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		super.doStart();
		update();
	}
	
	/**
	 * Creates a new Gson instance based on the settings in this bean.
	 */
	protected void update() {
		GsonBuilder builder = new GsonBuilder();
		if(prettyPrint) builder.setPrettyPrinting();
		if(disableHtmlEscaping) builder.disableHtmlEscaping();
		if(!adapterInstances.isEmpty()) {
			for(Map.Entry<Type, Object> entry: adapterInstances.entrySet()) {
				builder.registerTypeAdapter(entry.getKey(), entry.getValue());
			}
		}
		if(!adapterFactoryInstances.isEmpty()) {
			for(TypeAdapterFactory taf: adapterFactoryInstances) {
				builder.registerTypeAdapterFactory(taf);
			}
		}
		gson = builder.create();
	}
	


	/**
	 * Returns the pretty print attribute
	 * @return the pretty print attribute
	 */
	@ManagedAttribute(description="The JSON pretty print attribute")
	public boolean isPrettyPrint() {
		return prettyPrint;
	}


	/**
	 * Sets the pretty print attribute
	 * @param prettyPrint true to pretty-print, false for compact 
	 */
	@ManagedAttribute(description="The JSON pretty print attribute")
	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
		update();
	}


	/**
	 * Returns the html escaping disabled attribute
	 * @return true if html escaping is enabled, false if it disabled.
	 */
	@ManagedAttribute(description="The html escaping disabled attribute")
	public boolean isDisableHtmlEscaping() {
		return disableHtmlEscaping;
	}


	/**
	 * Sets the html escaping disabled attribute
	 * @param disableHtmlEscaping true to disable html escaping, false to enable
	 */
	@ManagedAttribute(description="The html escaping disabled attribute")
	public void setDisableHtmlEscaping(boolean disableHtmlEscaping) {
		this.disableHtmlEscaping = disableHtmlEscaping;
		update();
	}


	/**
	 * Returns the serialize nulls attribute
	 * @return true if nulls are being serialized, false otherwise
	 */
	@ManagedAttribute(description="The null serialization attribute")
	public boolean isSerializeNulls() {
		return serializeNulls;
	}


	/**
	 * Sets the null serialization attribute
	 * @param serializeNulls true to serialize nulls, false otherwise
	 */
	@ManagedAttribute(description="The null serialization attribute")
	public void setSerializeNulls(boolean serializeNulls) {
		this.serializeNulls = serializeNulls;
	}

	/**
	 * Adds the passed map of adapter instances to the gson builder
	 * @param adapterInstances the adapterInstances to set
	 */
	public void setAdapterInstances(Map<Type, Object> adapterInstances) {
		this.adapterInstances.putAll(adapterInstances);
	}


	/**
	 * Adds the passed set of adapter factory instances to the gson builder
	 * @param adapterFactoryInstances the adapterFactoryInstances to set
	 */
	public void setAdapterFactoryInstances(Set<TypeAdapterFactory> adapterFactoryInstances) {
		this.adapterFactoryInstances.addAll(adapterFactoryInstances);
	}
	
	
}
