package org.helios.apmrouter.nash;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.helios.apmrouter.nash.NashRequest;
import org.jboss.netty.channel.Channel;

/**
 * <p>Title: NashRequest</p>
 * <p>Description: Defines a read-only nash request</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.nash.NashRequest</code></p>
 */
public interface NashRequest {

	/**
	 * Returns the command or specified invocation target
	 * @return the command
	 */
	public abstract String getCommand();

	/**
	 * Returns the caller's command line working directory
	 * @return the workingDirectory
	 */
	public abstract String getWorkingDirectory();

	/**
	 * Returns the caller's environment
	 * @return the environment
	 */
	public abstract Properties getEnvironment();

	/**
	 * Returns the caller's command line arguments
	 * @return the arguments
	 */
	public abstract String[] getArguments();


	/**
	 * Returns the caller's IP address
	 * @return the remoteAddress
	 */
	public abstract InetAddress getRemoteAddress();

	/**
	 * Returns the caller's port
	 * @return the remotePort
	 */
	public abstract int getRemotePort();
	
	/**
	 * Returns an input stream that provides the streamed data feed from a nash client.
	 * The timeout specified is the amount of time to wait for the nash client to respond with its STDIN.
	 * @param timeout The availability timeout
	 * @param unit The availability timeout unit	 
	 * @return an InputStream feeding the nash client streamed input
	 */
	public abstract InputStream getInputStream(long timeout, TimeUnit unit);
	
	/**
	 * Returns a message back to the nail gun client's StdOut
	 * @param message The message to return
	 * @return this request
	 */
	public abstract NashRequest out(CharSequence message);
	
	/**
	 * Returns a message back to the nail gun client's StdErr
	 * @param message The message to return
	 * @return this request
	 */
	public abstract NashRequest err(CharSequence message);
	
	/**
	 * Terminates the response stream
	 * @param exitCode The exit code to send the nash client
	 */
	public abstract void end(int exitCode);

	/**
	 * Terminates the response stream with an exit code of 0
	 */
	public abstract void end();
	
	/**
	 * Returns the request's channel 
	 * @return the request's channel
	 */
	public abstract Channel getChannel();

	
	

}