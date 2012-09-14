/**
 * 
 */
package test.org.helios.apmrouter.netty;

import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.BasicConfigurator;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.trace.DirectMetricCollection;
import org.helios.apmrouter.util.SystemClock;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.DirectChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Log4JLoggerFactory;

/**
 * <p>Title: UDPListener</p>
 * <p>Description: A test UDP listener for counting received messages</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.apmrouter.netty.UDPListener</code></p>
 */
public class UDPListener implements  ChannelPipelineFactory {
	private final InetSocketAddress isock = new InetSocketAddress(2094);
	private final NioDatagramChannelFactory channelFactory = new NioDatagramChannelFactory(Executors.newCachedThreadPool());
	private final ConnectionlessBootstrap bstrap = new ConnectionlessBootstrap(channelFactory);
	private final OneToOneDecoder handler = new TestHandler();
	private NioDatagramChannel serverChannel;
	private LoggingHandler loggingHandler;
	
	private AtomicLong receivedBytes = new AtomicLong(0);
	private AtomicLong receivedMetrics = new AtomicLong(0);

	
	public UDPListener() {		
		bstrap.setOption("broadcast", false);
		BasicConfigurator.configure();
		InternalLoggerFactory.setDefaultFactory(new Log4JLoggerFactory());		
		bstrap.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(1024));
		loggingHandler = new LoggingHandler(InternalLogLevel.INFO, true);
		bstrap.setPipelineFactory(this);
		
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
//		return Channels.pipeline(loggingHandler, handler);
		return Channels.pipeline(handler);
	}
	
	
	private class TestHandler extends OneToOneDecoder {

		@SuppressWarnings("unused")
		@Override
		protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {			
			if(msg instanceof ChannelBuffer) {
				ChannelBuffer buff = (ChannelBuffer)msg;				
				//log("Received Channel Buffer:" + buff.isDirect());
				DirectMetricCollection dmc = DirectMetricCollection.fromChannelBuffer(buff);
				//if(buff.readableBytes()<length) return null;
				receivedBytes.addAndGet(dmc.getSize());
				receivedMetrics.addAndGet(dmc.getMetricCount());
				int opCode = buff.getByte(0);
				int byteOrder = buff.getByte(1);
				int totalSize = buff.getInt(2);
				
				IMetric[] metrics = dmc.decode();
				dmc.destroy();
				return -1;
			} 
			return null;
		}
	}
	
	private static final ByteOrder RBO = ByteOrder.nativeOrder()==ByteOrder.LITTLE_ENDIAN ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN; 
	
	protected int reverse(int i) {
		return((i&0xff)<<24)+((i&0xff00)<<8)+((i&0xff0000)>>8)+((i>>24)&0xff);
	}
	
	

}
