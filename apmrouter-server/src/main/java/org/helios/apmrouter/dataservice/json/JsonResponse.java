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
package org.helios.apmrouter.dataservice.json;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.helios.apmrouter.OpCode;
import org.helios.apmrouter.dataservice.json.marshalling.netty.ChannelBufferizable;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.SerializedName;

/**
 * <p>Title: JsonResponse</p>
 * <p>Description: The standard object container for sending a response to a JSON data service caller</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.JsonResponse</code></p>
 */

public class JsonResponse implements ChannelBufferizable  {
	/** The client provided request ID that this response is being sent for */
	@SerializedName("rerid")
	protected final long reRequestId;
	/** The response type */
	@SerializedName("t")
	protected final String type;
	/** The response instance id */
	@SerializedName("id")
	protected final int id = System.identityHashCode(this);
	
	/** The content payload */
	@SerializedName("msg")
	protected Object content = null;
	/** The response op code */
	@SerializedName("op")
	protected OpCode opCode = null;
	
	/** The gson serializer */
	protected static final Gson gson = new GsonBuilder().create();
	
	
	/** Response flag for an error message */
	public static final String RESP_TYPE_ERR = "err";
	/** Response flag for a request response */
	public static final String RESP_TYPE_RESP = "resp";
	/** Response flag for a subscription event delivery */
	public static final String RESP_TYPE_SUB = "sub";
	/** Response flag for a subscription start confirm */
	public static final String RESP_TYPE_SUB_STARTED = "subst";
	/** Response flag for a subscription stop notification */
	public static final String RESP_TYPE_SUB_STOPPED = "xsub";
	/** Response flag for a growl */
	public static final String RESP_TYPE_GROWL = "growl";
	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#clone()
	 */
	@Override
	public JsonResponse clone() {
		return new JsonResponse(reRequestId, type);
	}
	
	/**
	 * Clones this json response with a new type
	 * @param type the new type
	 * @return an updated type clone of this response
	 */
	public JsonResponse clone(String type) {
		return new JsonResponse(reRequestId, type);
	}
	
	/**
	 * Creates a new JsonResponse
	 * @param reRequestId The client provided request ID that this response is being sent for
	 * @param type The type flag. Currently "err" for an error message, "resp" for a response, "sub" for subcription event
	 */
	public JsonResponse(long reRequestId, String type) {
		super();
		this.reRequestId = reRequestId;
		this.type = type;
	}
	
	public static void main(String[] args) {
		log("GSON Test");
		JsonResponse resp = new JsonResponse(3, RESP_TYPE_RESP);
		Map<Integer, String> hosts = new HashMap<Integer, String>();
		int cnt = 0;
		List<String> sp = new ArrayList<String>(System.getProperties().stringPropertyNames());
		while(cnt < 10) {
			hosts.put(cnt, sp.get(cnt) + ":" + System.getProperty(sp.get(cnt)));
			cnt++;
		}
		resp.setContent(hosts);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		log(gson.toJson(resp));
	}
	
	private static class MapSerializer implements JsonSerializer<Map<?,?>> {
		@Override
		public JsonElement serialize(Map<?,?> src, Type typeOfSrc, JsonSerializationContext context) {
			if(!src.isEmpty()) {
				Map.Entry<?, ?> entry = src.entrySet().iterator().next();
				boolean keysAreNumbers = isNumber(entry.getKey());
				boolean valuesAreNumbers = isNumber(entry.getValue());
				if(keysAreNumbers || valuesAreNumbers) {
					JsonObject map = new JsonObject();
				}
			}
			return context.serialize(src, typeOfSrc);
		}
		
		private static boolean isNumber(Object obj) {
			if(obj==null) return false;
			if(obj instanceof Number) return true;
			try {
				Double.parseDouble(obj.toString());
				return true;
			} catch (Exception ex) {
				return false;
			}
		}
	}

	public static void log(Object msg) {
		System.out.println(msg);
	}

	/**
	 * Returns the content payload
	 * @return the content
	 */
	public Object getContent() {
		return content;
	}

	/**
	 * Sets the payload content
	 * @param content the content to set
	 * @return this json response
	 */
	public JsonResponse setContent(Object content) {
		this.content = content;
		return this;
	}

	/**
	 * Returns the in reference to request id
	 * @return the in reference to request id
	 */
	public long getReRequestId() {
		return reRequestId;
	}


	/**
	 * Returns the type flag
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns the response op code
	 * @return the response op code
	 */
	public OpCode getOpCode() {
		return opCode;
	}

	/**
	 * Sets the response op code
	 * @param opCode the response op code
	 * @return this response
	 */
	public JsonResponse setOpCode(OpCode opCode) {
		this.opCode = opCode;
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.marshalling.netty.ChannelBufferizable#toChannelBuffer()
	 */
	@Override
	public ChannelBuffer toChannelBuffer() {
		return ChannelBuffers.wrappedBuffer(gson.toJson(this).getBytes());
	}
	
	/** An empty ChannelFuture const. */
	private static final ChannelFuture[] EMPTY_CHANNEL_FUTURE_ARR = {};
	
	/**
	 * Sends this response to all the passed channels as a {@link TextWebSocketFrame}
	 * @param listener A channel future listener to attach to each channel future. Ignored if null.
	 * @param channels The channels to send this response to
	 * @return An array of the futures for the write of this response to each channel written to
	 */
	public ChannelFuture[] send(ChannelFutureListener listener, Channel...channels) {		
		if(channels!=null && channels.length>0) {
			Set<ChannelFuture> futures = new HashSet<ChannelFuture>(channels.length);
			TextWebSocketFrame frame = new TextWebSocketFrame(this.toChannelBuffer());
			for(Channel channel: channels) {
				if(channel!=null && channel.isWritable()) {
					ChannelFuture cf = Channels.future(channel);
					if(listener!=null) cf.addListener(listener);
					channel.getPipeline().sendDownstream(new DownstreamMessageEvent(channel, cf, frame, channel.getRemoteAddress()));
					futures.add(cf);
				}
			}
			return futures.toArray(new ChannelFuture[futures.size()]);
		}
		return EMPTY_CHANNEL_FUTURE_ARR;
	}
	
	/**
	 * Sends this response to all the passed channels as a {@link TextWebSocketFrame}
	 * @param channels The channels to send this response to
	 * @return An array of the futures for the write of this response to each channel written to
	 */
	public ChannelFuture[] send(Channel...channels) {
		return send(null, channels);
	}
	
	
}
