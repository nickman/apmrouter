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
package org.helios.apmrouter.nash;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.helios.apmrouter.nash.codecs.NashRequestDecoder;
import org.helios.apmrouter.nash.streams.ConnectTimeoutPipedInputStream;
import org.helios.apmrouter.nash.util.Banner;
import org.helios.apmrouter.nash.DefaultNashRequestImpl;
import org.helios.apmrouter.nash.NashConstants;
import org.helios.apmrouter.nash.NashRequest;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;

/**
 * <p>Title: DefaultNashRequestImpl</p>
 * <p>Description: Instances of this class are created by the {@link NashRequestDecoder} when a nash request is received. It represents the contents of the request.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.nash.DefaultNashRequestImpl</code></p>
 */

public class DefaultNashRequestImpl implements Serializable, NashRequest {
	/**  */
	private static final long serialVersionUID = -3507580873475441885L;
	/** Static class logger */
	protected static final InternalLogger log = InternalLoggerFactory.getInstance(NashRequest.class);	

	/** The command or specified invocation target */
	private String command;
	/** The caller's command line working directory  */
	private String workingDirectory;
	/** The caller's environment  */
	private final Properties environment = new Properties();
	/** The caller's command line arguments */
	private final List<String> arguments = new ArrayList<String>();
    /** The netty channel through which the client is communicating */
    private transient Channel channel = null;
    /** The presumed exit code set based on the last output */
    private transient int exitCode = 0;
	/** The input stream used when the command handler wishes to read the nash input stream as an output stream */
	protected transient PipedInputStream pipeIn = null;
	/** The output stream used by the NashRequestDecoder to write to the STDIN inout stream read by the command handler */
	protected transient PipedOutputStream pipeOut = null;
	/** The latch that guards the STDIN readers when STDIN inout is requested until the nash client responds */
	protected transient volatile CountDownLatch stdInLatch = null;
    
	/** The name of the response encoding channel handler */
	public static final String RESP_HANDLER = "response-encoder";
	
	/** The signal to send the client when we're ready to handle the stream input */
	private static final ChannelBuffer STREAM_IN_READY = ChannelBuffers.buffer(5);
	
	static {
		// Prep the stream in readiness buffer
		STREAM_IN_READY.writeInt(0);
		STREAM_IN_READY.writeByte(NashConstants.CHUNKTYPE_STARTINPUT);
	}
    
	
	
	/**
	 * Creates a new DefaultNashRequestImpl
	 * @param command The command or specified invocation target
	 * @param workingDirectory The caller's command line working directory
	 * @param environment The caller's environment
	 * @param arguments The caller's command line arguments
	 * @return a new DefaultNashRequestImpl
	 */
	public static NashRequest newInstance(String command, String workingDirectory, Properties environment, String...arguments) {
		return new DefaultNashRequestImpl(command, workingDirectory, environment, arguments);
	}
	
	
	/**
	 * Creates a new DefaultNashRequestImpl
	 * @param command The command or specified invocation target
	 * @param workingDirectory The caller's command line working directory
	 * @param environment The caller's environment
	 * @param arguments The caller's command line arguments
	 */
	public DefaultNashRequestImpl(String command, String workingDirectory, Properties environment, String...arguments) {
		this.command = command;
		this.workingDirectory = workingDirectory;
		this.environment.putAll(environment);
		for(String arg: arguments) {
			if(arg!=null && !arg.isEmpty()) {
				this.arguments.add(arg);
			}
		}
	}
    
	
	/**
	 * Creates a new DefaultNashRequestImpl
	 */
	public DefaultNashRequestImpl() {
		
	}
	



	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.nash.NashRequest#getCommand()
	 */
	@Override
	public String getCommand() {
		return command;
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.nash.NashRequest#getWorkingDirectory()
	 */
	@Override
	public String getWorkingDirectory() {
		return workingDirectory;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.nash.NashRequest#getEnvironment()
	 */
	@Override
	public Properties getEnvironment() {
		return environment;
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.nash.NashRequest#getArguments()
	 */
	@Override
	public String[] getArguments() {
		return arguments.toArray(new String[arguments.size()]);
	}



	/**
	 * Sets the command
	 * @param command the command to set
	 */
	public void setCommand(String command) {
		this.command = command;
	}

	/**
	 * Sets the working directory
	 * @param workingDirectory the workingDirectory to set
	 */
	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	/**
	 * Sets the environment
	 * @param environment the environment to set
	 */
	public void setEnvironment(Properties environment) {
		this.environment.putAll(environment);
	}
	
	/**
	 * Adds a property to the remote environment
	 * @param key The property key
	 * @param value The property value
	 */
	public void addToEnvironment(String key, String value) {
		this.environment.setProperty(key, value);
	}
	
	/**
	 * Adds a property to the remote environment
	 * @param line The property line (i.e. <code>KEY=VALUE</code>)
	 */
	public void addToEnvironment(String line) {
		int equalsIndex = line.indexOf('=');
		if (equalsIndex > 0) {
			environment.setProperty(line.substring(0, equalsIndex),line.substring(equalsIndex + 1));
		}		
	}
	

	/**
	 * Sets the arguments
	 * @param arguments the arguments to set
	 */
	public void setArguments(String[] arguments) {
		Collections.addAll(this.arguments, arguments);		
	}
	
	/**
	 * Adds an argument to the request
	 * @param arg the argument to add
	 */
	public void addArgument(String arg) {
		this.arguments.add(arg);
	}
	



	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.nash.NashRequest#getRemoteAddress()
	 */
	@Override
	public InetAddress getRemoteAddress() {
		return channel==null ? null : ((InetSocketAddress)channel.getRemoteAddress()).getAddress();
	}
	
	public String printEnvironment() {
		StringBuilder b = new StringBuilder(environment.size()*30);
		TreeMap<?, ?> env = new TreeMap<Object, Object>(environment); 
		for(Map.Entry<?, ?> entry: env.entrySet()) {
			b.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
		}
		return b.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.nash.NashRequest#getRemotePort()
	 */
	@Override
	public int getRemotePort() {
		return channel==null ? -1: ((InetSocketAddress)channel.getRemoteAddress()).getPort();
	}
	
	/**
	 * Returns the channel
	 * @return the channel
	 */
	public Channel getChannel() {
		return channel;
	}


	/**
	 * Sets the channel
	 * @param channel the channel to set
	 */
	public void setChannel(Channel channel) {
		this.channel = channel;
	}
	
	/*
	 * OnInputStreamStart
	 * OnInoutStreamEOF
	 * getAvailableBytes()
	 * 
	 * 
	 * 
	 * InputStream  (reads from ng client), returns -1 on end of stream
	 * onChannelClosed
	 * -- How do we timeout when the ng client is not sending any input ?
	 */
	
	/*
	 *             writeInt(len);
            writeByte(streamCode);
			out.write(b, offset, len);

	 */
	
	

	
	/**
	 * Initializes the STDIN stream processing pipe when the nash client starts streaming STDIN 
	 * @param initialSize The initial size of the input stream pipe
	 * @return the output stream that the NashRequestDecoder will use to write STDIN
	 * @throws IOException thrown if the pipe connect setup fails
	 */
	public OutputStream setStdInStream(int initialSize) throws IOException {
		if(stdInLatch.getCount()<1) {
			return pipeOut;
		}
		pipeOut = new PipedOutputStream();
		pipeIn = new PipedInputStream(pipeOut, initialSize);
		stdInLatch.countDown();
		log.debug(Banner.banner("Dropped Latch for STDIN Stream"));
		return pipeOut;
	}
	
	/**
	 * Cancels STDIN processing if the nash client reports no STDIN or the STDIN stream is EOFed. 
	 */
	public void cancelStdInStream() {
		stdInLatch.countDown();
		if(pipeOut!=null) {
			log.debug("Closing PipeOut");
			try { pipeOut.flush(); } catch (Exception e) {}
			try { pipeOut.close(); } catch (Exception e) {}
			log.debug("Closed PipeOut");
		}
		pipeOut = null;
		if(pipeIn!=null) try { 
			log.debug("Closing PipeIn");
			pipeIn.close(); 
			log.debug("Closed PipeIn");
		} catch (Exception e) {}
		pipeIn = null;		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.nash.NashRequest#getInputStream(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public InputStream getInputStream(long timeout, TimeUnit unit) {
		if(stdInLatch == null) stdInLatch = new CountDownLatch(1);
		sendStartStdInSignal();
		try {
			stdInLatch.await(timeout, unit);
			return pipeIn;
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Sends a signal back to the nash client indicating that we're ready to accept the input stream
	 * @param ctx The ChannelHandlerContext
	 * @param channel The channel
	 */
	protected void sendStartStdInSignal() {
		DownstreamMessageEvent dme = new DownstreamMessageEvent(channel, Channels.future(channel), STREAM_IN_READY, channel.getRemoteAddress());
		//channel.getPipeline().sendDownstream(dme);
		System.out.println("Sending STDIN READY");
		channel.write(STREAM_IN_READY).awaitUninterruptibly();
		System.out.println("Sent STDIN READY");
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.nash.NashRequest#out(java.lang.CharSequence)
	 */
	@Override
	public NashRequest out(CharSequence message) {
		exitCode = 0;
		ChannelBuffer header = ChannelBuffers.buffer(5);
		header.writeInt(message.length());
		header.writeByte(NashConstants.CHUNKTYPE_STDOUT);
		
		ChannelBuffer response = ChannelBuffers.wrappedBuffer(
				header,
				ChannelBuffers.copiedBuffer(message, Charset.defaultCharset())
		);
		channel.getPipeline().sendDownstream(new DownstreamMessageEvent(channel, Channels.future(channel), response, channel.getRemoteAddress()));
		return this;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.nash.NashRequest#err(java.lang.CharSequence)
	 */
	@Override
	public NashRequest err(CharSequence message) {
		exitCode = 1;
		ChannelBuffer header = ChannelBuffers.buffer(5);
		header.writeInt(message.length());
		header.writeByte(NashConstants.CHUNKTYPE_STDERR);
		
		ChannelBuffer response = ChannelBuffers.wrappedBuffer(
				header,
				ChannelBuffers.copiedBuffer(message, Charset.defaultCharset())
		);
		channel.getPipeline().sendDownstream(new DownstreamMessageEvent(channel, Channels.future(channel), response, channel.getRemoteAddress()));
		return this;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.nash.NashRequest#end()
	 */
	@Override
	public void end() {
		end(exitCode);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.nash.NashRequest#end(int)
	 */
	@Override
	public void end(int exitCode) {
		byte[] msg = ("" + exitCode + "\n").getBytes();
		ChannelBuffer header = ChannelBuffers.buffer(msg.length + 5);
		header.writeInt(msg.length);
		header.writeByte(NashConstants.CHUNKTYPE_EXIT);
		header.writeBytes(msg);
		
		//channel.getPipeline().sendDownstream(new DownstreamMessageEvent(channel, Channels.future(channel), header, channel.getRemoteAddress()));
		channel.write(header).addListener(ChannelFutureListener.CLOSE);
		//channel.close();		
	}



	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	@Override
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("DefaultNashRequestImpl [")
	    	.append(TAB).append("channel:").append(this.channel)
	        .append(TAB).append("command:").append(this.command)
	        .append(TAB).append("workingDirectory:").append(this.workingDirectory)
	        .append(TAB).append("environment:").append(this.environment.size()).append(" properties")
	        .append(TAB).append("arguments:").append(this.arguments)
	        .append(TAB).append("remoteAddress:").append(getRemoteAddress())
	        .append(TAB).append("remotePort:").append(getRemotePort())
	        .append("\n]");    
	    return retValue.toString();
	}


	
}
