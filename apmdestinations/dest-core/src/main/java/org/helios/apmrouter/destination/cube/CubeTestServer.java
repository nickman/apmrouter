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
package org.helios.apmrouter.destination.cube;

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.helios.apmrouter.util.SimpleLogger;
import org.helios.apmrouter.util.URLHelper;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ChildChannelStateEvent;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.ServerChannel;
import org.jboss.netty.channel.ServerChannelFactory;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.util.CharsetUtil;


/**
 * <p>Title: CubeTestServer</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.cube.CubeTestServer</code></p>
 */

public class CubeTestServer implements ChannelPipelineFactory, ServerChannelFactory {
	private static final int PORT = 8003;
	
	static final ChannelGroup CG = new DefaultChannelGroup();
	static final ChannelGroup WCG = new DefaultChannelGroup();
	
	private ChannelFactory cf = null;
	
	/**
	 * Creates a new CubeTestServer
	 */
	public CubeTestServer() {
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CubeTestServer testServer = new CubeTestServer();
		testServer.run();

	}

	
	public void run() {
		cf = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
		 // Configure the server.
		          ServerBootstrap bootstrap = new ServerBootstrap(this);
		  
		          // Set up the event pipeline factory.
		          bootstrap.setPipelineFactory(this);
		  
		          // Bind and start to accept incoming connections.
		          bootstrap.bind(new InetSocketAddress(PORT));
		  
		          System.out.println("Web socket server started at port " + PORT + '.');
		          System.out.println("Open your browser and navigate to http://localhost:" + PORT + '/');
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
	    // Create a default pipeline implementation.
      ChannelPipeline pipeline = Channels.pipeline();
      pipeline.addLast("decoder", new HttpRequestDecoder());
      pipeline.addLast("aggregator", new HttpChunkAggregator(65536));
      pipeline.addLast("encoder", new HttpResponseEncoder());
      pipeline.addLast("handler", new WebSocketServerHandler());
      return pipeline;	
	}
	
	protected static class WebSocketServerHandler  extends SimpleChannelUpstreamHandler {
	    private static final InternalLogger logger = InternalLoggerFactory.getInstance(WebSocketServerHandler.class);

	    private static final String WEBSOCKET_PATH = "/websocket";

	    private WebSocketServerHandshaker handshaker;
	    
	    static {
	    	
	    }

	    @Override
	    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
	        Object msg = e.getMessage();
	        if (msg instanceof HttpRequest) {
	            handleHttpRequest(ctx, (HttpRequest) msg);
	        } else if (msg instanceof WebSocketFrame) {
	            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
	        }
	    }

	    private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
	        // Allow only GET methods.
	        if (req.getMethod() != GET) {
	            sendHttpResponse(ctx, req, new DefaultHttpResponse(HTTP_1_1, FORBIDDEN));
	            return;
	        }

	        // /home/nwhitehead/hprojects/apmrouter/apmdestinations/dest-core/src/main/java/org/helios/apmrouter/destination/cube/CubeTestServer.java
	        // /home/nwhitehead/hprojects/apmrouter/apmrouter-server/src/main/resources/www/js/jquery-1.8.2.min.js
	        if (req.getUri().equals("/favicon.ico")) {
	            HttpResponse res = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND);
	            sendHttpResponse(ctx, req, res);
	            return;
	        } else if(req.getUri().startsWith("/jquery") || req.getUri().equals("/")) {
	        	ChannelBuffer content = null;
	        	String contentType = null;
	        	if (req.getUri().contains("/jquery")) {
	        		File f = new File("../../apmrouter-server/src/main/resources/www/js/jquery-1.8.2.min.js");
	        		if(!f.exists()) {
	        			throw new RuntimeException("File [" + f + "] was not found", new Throwable());	        			
	        		}	        		
	        		content = ChannelBuffers.wrappedBuffer(URLHelper.getBytesFromURL(URLHelper.toURL(f)));
	        		contentType = "application/x-javascript";
	        	} else {
	        		contentType = "text/html; charset=UTF-8";	        		
	        		content = ChannelBuffers.wrappedBuffer(URLHelper.getBytesFromURL(URLHelper.toURL(new File("./src/main/java/org/helios/apmrouter/destination/cube/CubeOutput.html"))));
	        	}
	        	HttpResponse res = new DefaultHttpResponse(HTTP_1_1, OK);
	        	res.setHeader(CONTENT_TYPE, contentType);
	        	setContentLength(res, content.readableBytes());
	            res.setContent(content);
	            sendHttpResponse(ctx, req, res);
	            return;
	        }

	        // Handshake
	        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
	                this.getWebSocketLocation(req), null, false);
	        this.handshaker = wsFactory.newHandshaker(req);
	        if (this.handshaker == null) {
	            wsFactory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel());
	        } else {
	            this.handshaker.handshake(ctx.getChannel(), req).addListener(WebSocketServerHandshaker.HANDSHAKE_LISTENER);
	            Channel channel = ctx.getChannel();
	            ctx.sendDownstream(new DownstreamMessageEvent(channel, Channels.future(channel), new TextWebSocketFrame("ChannelID:" + channel.getId()), channel.getRemoteAddress()));
	            WCG.add(channel);
		        for(Channel ch: WCG) {		  
		        	if(ch.isWritable()) {
		        		ch.write(new TextWebSocketFrame("ChannelCount:" + WCG.size()));
		        	}
		        }
	        }
	    }
	    
	    /**
	     * {@inheritDoc}
	     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelClosed(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
	     */
	    @Override
	    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {	    	
	    	super.channelClosed(ctx, e);
	        for(Channel ch: WCG) {		  
	        	if(ch.isWritable()) {
	        		ch.write(new TextWebSocketFrame("ChannelCount:" + WCG.size()));
	        	}
	        }
	    }

	    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {

	        // Check for closing frame
	        if (frame instanceof CloseWebSocketFrame) {
	            this.handshaker.close(ctx.getChannel(), (CloseWebSocketFrame) frame);
	            return;
	        } else if (frame instanceof PingWebSocketFrame) {
	            ctx.getChannel().write(new PongWebSocketFrame(frame.getBinaryData()));
	            return;
	        } else if (!(frame instanceof TextWebSocketFrame)) {
	            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass()
	                    .getName()));
	        }

	        // Send the uppercase string back.
	        String request = ((TextWebSocketFrame) frame).getText();
	        if (logger.isDebugEnabled()) {
	            logger.debug(String.format("Channel %s received %s", ctx.getChannel().getId(), request));
	        }
	        final Integer channelId = ctx.getChannel().getId();
	        for(Channel channel: WCG) {
	        	if(channel.getId().equals(channelId)) continue;
	        	channel.write(new TextWebSocketFrame(request));
	        }
	        
	    }

	    private void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req, HttpResponse res) {
	        // Generate an error page if response status code is not OK (200).
	        if (res.getStatus().getCode() != 200) {
	            res.setContent(ChannelBuffers.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8));
	            setContentLength(res, res.getContent().readableBytes());
	        }

	        // Send the response and close the connection if necessary.
	        ChannelFuture f = ctx.getChannel().write(res);
	        if (!isKeepAlive(req) || res.getStatus().getCode() != 200) {
	            f.addListener(ChannelFutureListener.CLOSE);
	        }
	    }

	    @Override
	    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
	        e.getCause().printStackTrace();
	        e.getChannel().close();
	    }
	    

	    
	    /**
	     * {@inheritDoc}
	     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelConnected(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
	     */
	    @Override
	    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
	    	Channel channel = e.getChannel();
	    	if(!(channel instanceof ServerChannel)) {
	    		CG.add(channel);
	    		SimpleLogger.info("Channel Connected \n\tLocal:[", channel.getLocalAddress(), "]\n\tRemote:[", channel.getRemoteAddress(), "]\n\tState:[", e.getState().name() , "/", e.getValue(),  "]\n\tID:[", channel.getId(), "]\n\tConnected Channels:", CG.size());
	    	} 
	    	super.channelConnected(ctx, e);
	    }

	    private String getWebSocketLocation(HttpRequest req) {
	        return "ws://" + req.getHeader(HttpHeaders.Names.HOST) + WEBSOCKET_PATH;
	    }
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelFactory#newChannel(org.jboss.netty.channel.ChannelPipeline)
	 */
	@Override
	public ServerChannel newChannel(ChannelPipeline pipeline) {
		Channel channel = cf.newChannel(pipeline);
		return (ServerChannel)channel;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelFactory#releaseExternalResources()
	 */
	@Override
	public void releaseExternalResources() {
		cf.releaseExternalResources();		
	}
}
