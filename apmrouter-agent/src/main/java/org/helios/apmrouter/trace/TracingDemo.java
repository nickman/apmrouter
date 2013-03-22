package org.helios.apmrouter.trace;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.nativex.APMSigar;
import org.helios.apmrouter.sender.ISender;
import org.helios.apmrouter.sender.SenderFactory;
import org.helios.apmrouter.util.SystemClock;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.snmp4j.PDU;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

public class TracingDemo {

	public static void main(String[] args) {	
		System.setProperty("theice.agent.name", "tracing-demo");
		final int LOOPS = 100000;
		final int SLEEP = 1000;
		BasicConfigurator.configure();
		final ITracer tracer = TracerFactory.getTracer();
		Logger traceLogger = Logger.getLogger(TracingDemo.class);
		traceLogger.setLevel(Level.DEBUG);
		traceLogger.removeAllAppenders();
		traceLogger.addAppender(new LogTracer());
		APMSigar sigar = APMSigar.getInstance();
		TXContext.rollContext();
		MetricType.setCompress(true);
		ISender sender = SenderFactory.getInstance().getDefaultSender();
		log("Basic Tracing Test: [" +  tracer.getHost() + "/" + tracer.getAgent() + "]");
		traceTotalCpuUsage(tracer, sigar);
		while(true) {
			traceTotalCpuUsage(tracer, sigar);
			SystemClock.sleep(1000);
		}
		//DefaultMonitorBoot.boot();
//		for(int i = 0; i < LOOPS; i++) {
//			if(i>0 && i%10==0) {
//				//TXContext.rollContext();
//			}
//			SystemClock.startTimer();
//			//boolean success = sender.ping(2000);
//			ElapsedTime et = SystemClock.endTimer();
//			tracer.trace(System.currentTimeMillis(), "Foo", MetricType.LONG_COUNTER, "Bar");
//			//log("Ping [" + success + "]--  " + et );
//			//tracer.traceLong(i, "TXTest", "Foo", "Bar");
////			for(GarbageCollectorMXBean gc: ManagementFactory.getGarbageCollectorMXBeans()) {
////				tracer.traceDelta(gc.getCollectionCount(), "CollectionCount", "JVM", "Memory", "GC", gc.getName());
////				tracer.traceDelta(gc.getCollectionTime(), "CollectionTime", "JVM", "Memory", "GC", gc.getName());
////			}
////			traceCpuUsages(tracer, sigar);
////			traceTotalCpuUsage(tracer, sigar);
////			traceDiskUsage(tracer, sigar);
//			//traceMemorySpacesSNMP(tracer, sigar);
////			try {
////				traceLogger.info("Hello World [" + i + "]");
////				traceLogger.info("Hello Pluto [" + i + "]", new Throwable());
////			} catch (Exception e) {}
//			
//			if(i%100==0) {
//				long ns = sender.getAveragePingTime();
//				long ms = TimeUnit.MILLISECONDS.convert(ns, TimeUnit.NANOSECONDS);
////				log("Ping Time:" + ns + " ns.  " + ms + "  ms.");
//			}
//			TXContext.clearContext();
//			SystemClock.sleep(SLEEP);
//		}
//		SystemClock.sleep(Long.MAX_VALUE);
	}
	
	public static void traceCpuUsages(ITracer tracer, APMSigar sigar) {
		int cpuId = 0;
		for(CpuPerc cpu : sigar.getCpuPercList()) {
			tracer.traceGauge((long)(cpu.getCombined()*100), "Total", "CPU", "Usage", "Cpu" + cpuId);
			tracer.traceGauge((long)(cpu.getSys()*100), "Sys", "CPU", "Usage", "Cpu" + cpuId);
			tracer.traceGauge((long)(cpu.getUser()*100), "User", "CPU", "Usage", "Cpu" + cpuId);
			cpuId++;
		}
	}
	
	public static void traceTotalCpuUsage(ITracer tracer, APMSigar sigar) {
		CpuPerc perc = sigar.getCpuPerc();
		tracer.traceGauge((long)(perc.getCombined()*100), "Total", "CPU", "Usage", "Combined");
		tracer.traceGauge((long)(perc.getSys()*100), "Sys", "CPU", "Usage", "Combined");
		tracer.traceGauge((long)(perc.getUser()*100), "User", "CPU", "Usage", "Combined");
	}
	
	public static void traceMemorySpacesSNMP(ITracer tracer, APMSigar sigar) {
		MemoryUsage usage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
		tracer.tracePDUDirect(PDUBuilder.builder(PDU.NOTIFICATION, ".1.3.6.1.4.1.42.2.145.3.163.1.1.2.")
				.counter64("10", usage.getInit())
				.counter64("11", usage.getUsed())
				.counter64("12", usage.getCommitted())
				.counter64("13", usage.getMax())
				.build(), "HeapUsage", "JVM", "Memory"
		);
		usage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
		try {
			tracer.tracePDUDirect(PDUBuilder.builder(PDU.NOTIFICATION, ".1.3.6.1.4.1.42.2.145.3.163.1.1.2.")
					.counter64("20", usage.getInit())
					.counter64("21", usage.getUsed())
					.counter64("22", usage.getCommitted())
					.counter64("23", usage.getMax())
					.build(), "NonHeapUsage", "JVM", "Memory"
			);
		} catch (Exception e) {
			log("Direct Request Failed:" + e); 
		}
		
	}
	
	public static void traceDiskUsage(ITracer tracer, APMSigar sigar) {
		for(FileSystem fs: sigar.getFileSystemList()) {
			FileSystemUsage fsu = sigar.getFileSystemUsageOrNull(fs.getDevName().trim());
			if(fsu==null) {
				//log("No Usage for [" + fs.getDirName() + "]");
				continue;
			}
			tracer.traceGauge(fsu.getAvail(), "Available", "FileSystems", fs.getSysTypeName(), fs.getDirName().replace("\\", ""));
			tracer.traceGauge(fsu.getUsed(), "Used", "FileSystems", fs.getSysTypeName(), fs.getDirName().replace("\\", ""));
			tracer.traceGauge(fsu.getTotal(), "Total", "FileSystems", fs.getSysTypeName(), fs.getDirName().replace("\\", ""));
			tracer.traceGauge((long)(fsu.getUsePercent()*100), "Used%", "FileSystems", fs.getSysTypeName(), fs.getDirName().replace("\\", ""));
			tracer.traceGauge((long)(fsu.getDiskQueue()*100), "Queue", "FileSystems", fs.getSysTypeName(), fs.getDirName().replace("\\", ""));
			tracer.traceGauge((long)(fsu.getDiskServiceTime()*100), "ServiceTime", "FileSystems", fs.getSysTypeName(), fs.getDirName().replace("\\", ""));
			tracer.traceDeltaCounter(fsu.getDiskReadBytes(), "BytesRead", "FileSystems", fs.getSysTypeName(), fs.getDirName().replace("\\", ""));
			tracer.traceDeltaCounter(fsu.getDiskWriteBytes(), "BytesWritten", "FileSystems", fs.getSysTypeName(), fs.getDirName().replace("\\", ""));
			tracer.traceDeltaCounter(fsu.getDiskReads(), "Reads", "FileSystems", fs.getSysTypeName(), fs.getDirName().replace("\\", ""));
			tracer.traceDeltaCounter(fsu.getDiskWrites(), "Writes", "FileSystems", fs.getSysTypeName(), fs.getDirName().replace("\\", ""));
		}
	}
	
	

	public static void log(Object msg) {
		System.out.println(msg);
	}

}
