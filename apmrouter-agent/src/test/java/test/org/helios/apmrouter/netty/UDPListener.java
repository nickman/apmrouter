/**
 * 
 */
package test.org.helios.apmrouter.netty;

import org.apache.log4j.BasicConfigurator;
import org.helios.apmrouter.OpCode;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.metric.catalog.ICEMetricCatalog;
import org.helios.apmrouter.metric.catalog.IMetricCatalog;
import org.helios.apmrouter.sender.netty.UDPSender;
import org.helios.apmrouter.sender.netty.handler.ChannelStateAware;
import org.helios.apmrouter.sender.netty.handler.ChannelStateListener;
import org.helios.apmrouter.trace.DirectMetricCollection;
import org.helios.apmrouter.util.SystemClock;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.DirectChannelBufferFactory;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioDatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Log4JLoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: UDPListener</p>
 * <p>Description: A test UDP listener for counting received messages</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.apmrouter.netty.UDPListener</code></p>
 */
public class UDPListener implements  ChannelPipelineFactory, ChannelStateAware {
	private final InetSocketAddress isock = new InetSocketAddress(2094);
	private final NioDatagramChannelFactory channelFactory = new NioDatagramChannelFactory(Executors.newCachedThreadPool());
	private final ConnectionlessBootstrap bstrap = new ConnectionlessBootstrap(channelFactory);
	private final ChannelHandler handler = new TestHandler();
	private NioDatagramChannel serverChannel;
	private LoggingHandler loggingHandler;
	private final ChannelStateListener channelStateListener = new ChannelStateListener(); 
	
	private AtomicLong receivedBytes = new AtomicLong(0);
	private AtomicLong receivedMetrics = new AtomicLong(0);

	private final IMetricCatalog metricCatalog;
	
	
	/**
	 * Creates a new UDPListener
	 */
	public UDPListener() {		
		channelStateListener.addChannelStateAware(this);
		bstrap.setOption("broadcast", false);
		BasicConfigurator.configure();
		InternalLoggerFactory.setDefaultFactory(new Log4JLoggerFactory());		
		bstrap.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(UDPSender.MAXSIZE));
		loggingHandler = new LoggingHandler(InternalLogLevel.DEBUG, true);
		bstrap.setPipelineFactory(this);
		metricCatalog = ICEMetricCatalog.getInstance();		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.netty.handler.ChannelStateAware#getInterestedChannelStates()
	 */
	@Override
	public ChannelState[] getInterestedChannelStates() {
		return new ChannelState[]{ChannelState.CONNECTED};
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.netty.handler.ChannelStateAware#onChannelStateEvent(boolean, org.jboss.netty.channel.ChannelStateEvent)
	 */
	@Override
	public void onChannelStateEvent(boolean upstream, ChannelStateEvent stateEvent) {
		if(upstream && stateEvent.getValue().equals(Boolean.FALSE)) {
			
		}
	}
	
	
	public void start() {
		serverChannel = (NioDatagramChannel)bstrap.bind(isock);
		serverChannel.getConfig().setBufferFactory(new DirectChannelBufferFactory());
		new Thread("ActivityPrinter") {
			long lastBytes = 0, lastMetrics = 0, bytes = 0, metrics = 0;
			public void run() {
				while(true) {
					try {
						for(int i = 0; i < 12; i++) {
							SystemClock.sleep(5, TimeUnit.SECONDS);
							bytes = receivedBytes.get();
							metrics = receivedMetrics.get();
							if(bytes!=lastBytes || metrics!=lastMetrics) {				
								log("Totals:  Bytes:" + bytes + "  Metrics:" + metrics);
							}
							lastBytes = bytes; lastMetrics = metrics;
						}
						log("\n\t====\n\tTotals:  Bytes:" + bytes + "  Metrics:" + metrics + "\n\t====");
					} catch (Exception e) {}
				}
			}
		}.start();
		
		log("UDP Test Listener Started on [" + isock + "]");
	}
	
	

	/**
	 * Boots the UDP listener
	 * @param args None
	 */
	public static void main(String[] args) {
		log("UDP Test Listener");
		new UDPListener().start();
	}
	
	/**
	 * Out log
	 * @param msg the message to log
	 */
	public static void log(Object msg) {
		System.out.println(msg);
	}


	@Override
	public ChannelPipeline getPipeline() throws Exception {		
//		return Channels.pipeline(loggingHandler, channelStateListener, handler);
		return Channels.pipeline(channelStateListener, handler);
	}
	
	
	private class TestHandler extends SimpleChannelUpstreamHandler {
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
			e.getCause().printStackTrace(System.err);
			super.exceptionCaught(ctx, e);
		}
		
		@SuppressWarnings("unused")
		@Override
		public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent me) throws Exception {	
			Object msg = me.getMessage();
			if(msg instanceof ChannelBuffer) {
				ChannelBuffer buff = (ChannelBuffer)msg;				
				//log("Received Channel Buffer:" + buff.isDirect());
				DirectMetricCollection dmc = DirectMetricCollection.fromChannelBuffer(buff);
				OpCode opCode = dmc.getOpCode();
				//if(buff.readableBytes()<length) return null;
				receivedBytes.addAndGet(dmc.getSize());
				receivedMetrics.addAndGet(dmc.getMetricCount());				
				int byteOrder = buff.getByte(1);
				int totalSize = buff.getInt(2);
				IMetric[] metrics = null;
				try {
					metrics = dmc.decode();
				} catch (Exception e) {
					e.printStackTrace(System.err);					
				}				
				//dmc.destroy();
				for(final IMetric metric: metrics) {
					if(opCode==OpCode.SEND_METRIC_DIRECT) {
						sendConfirm(me.getChannel(), me.getRemoteAddress(),  metric);
					}
					if(metric.getToken()==-1) {						
						sendToken(me.getChannel(), me.getRemoteAddress(),  metric);
					}
				}
			} 
		}

	}
	
	/**
	 * Confirms the receipt of a direct metric
	 * @param channel The channel on which the metric was received
	 * @param address The remote address of the sender
	 * @param metric The direct metric
	 */
	protected static void sendConfirm(Channel channel, SocketAddress remoteAddress, final IMetric metric) {
		String key = new StringBuilder(metric.getFQN()).append(metric.getTime()).toString();
		byte[] bytes = key.getBytes();
		// Buffer size:  OpCode, key size, key bytes
		ChannelBuffer cb = ChannelBuffers.directBuffer(1 + 4 + bytes.length);
		cb.writeByte(OpCode.CONFIRM_METRIC.op());
		cb.writeInt(bytes.length);
		cb.writeBytes(bytes);		
		
		if(!channel.isConnected()) channel.connect(remoteAddress).awaitUninterruptibly(); 
		channel.write(cb, remoteAddress).addListener(new ChannelFutureListener() {
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess()) {
					//System.out.println("Sent Token for [" + metric.getFQN() + "]");
				} else {
					System.err.println("Failed to send confirm for direct metric [" + metric + "]");
					future.getCause().printStackTrace(System.err);
				}
				
			}
		});
		
		
	}
	
	
	/**
	 * When an untokenized metric is received, the token is generated from the metric catalog
	 * and returned to the caller in this protocol:<ol>
	 * 	<li>0 for the op-code (1 byte)</li>
	 *  <li>The size of the metric FQN (1 int)</li>
	 *  <li>The metric FQN's bytes  (n bytes)</li>
	 *  <li>The metric token (1 long)</li>
	 * </ol>
	 * @param channel The channel on which the untokenized metric was received
	 * @param address The remote address of the sender
	 * @param metric The untokenized metric
	 */
	protected void sendToken(final Channel channel, final SocketAddress address, final IMetric metric) {
		final long token = metricCatalog.setToken(metric);		
		byte[] bytes = metric.getFQN().getBytes();
		// Buffer size:  OpCode, fqn size, fqn bytes, token
		ChannelBuffer cb = ChannelBuffers.directBuffer(1 + 4 + bytes.length + 8 );
		cb.writeByte(OpCode.SEND_METRIC_TOKEN.op());
		cb.writeInt(bytes.length);
		cb.writeBytes(bytes);
		cb.writeLong(token);
		
		if(!channel.isConnected()) channel.connect(address).awaitUninterruptibly(); 
		channel.write(cb, address).addListener(new ChannelFutureListener() {
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess()) {
					//System.out.println("Sent Token for [" + metric.getFQN() + "]");
				} else {
					System.err.println("Failed to send token for [" + metric.getFQN() + "]");
					future.getCause().printStackTrace(System.err);
				}
				
			}
		});
		
	}

	

}
