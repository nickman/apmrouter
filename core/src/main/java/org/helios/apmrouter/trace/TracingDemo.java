package org.helios.apmrouter.trace;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.sender.ISender;
import org.helios.apmrouter.sender.Sender;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;
import org.helios.jzab.plugin.nativex.HeliosSigar;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.snmp4j.PDU;

public class TracingDemo {

	public static void main(String[] args) {		
		final int LOOPS = 100000;
		final int SLEEP = 10;
		BasicConfigurator.configure();
		final ITracer tracer = TracerFactory.getTracer();
		Logger traceLogger = Logger.getLogger(TracingDemo.class);
		traceLogger.setLevel(Level.DEBUG);
		traceLogger.removeAllAppenders();
		traceLogger.addAppender(new LogTracer());
		HeliosSigar sigar = HeliosSigar.getInstance();
		//TXContext.rollContext();
		MetricType.setCompress(true);
		ISender sender = Sender.getInstance().getDefaultSender();
		log("Basic Tracing Test: [" +  tracer.getHost() + "/" + tracer.getAgent() + "]");
		for(int i = 0; i < LOOPS; i++) {
			SystemClock.startTimer();
			boolean success = sender.ping(2000);
			ElapsedTime et = SystemClock.endTimer();
			//log("Ping [" + success + "]--  " + et );
			//tracer.traceLong(i, "TXTest", "Foo", "Bar");
//			for(GarbageCollectorMXBean gc: ManagementFactory.getGarbageCollectorMXBeans()) {
//				tracer.traceDelta(gc.getCollectionCount(), "CollectionCount", "JVM", "Memory", "GC", gc.getName());
//				tracer.traceDelta(gc.getCollectionTime(), "CollectionTime", "JVM", "Memory", "GC", gc.getName());
//			}
//			traceCpuUsages(tracer, sigar);
//			traceTotalCpuUsage(tracer, sigar);
//			traceDiskUsage(tracer, sigar);
//			traceMemorySpacesSNMP(tracer, sigar);
//			try {
//				traceLogger.info("Hello World [" + i + "]");
//				traceLogger.info("Hello Pluto [" + i + "]", new Throwable());
//			} catch (Exception e) {}
			
			if(i%100==0) {
				long ns = sender.getAveragePingTime();
				long ms = TimeUnit.MILLISECONDS.convert(ns, TimeUnit.NANOSECONDS);
				log("Ping Time:" + ns + " ns.  " + ms + "  ms.");
			}
			SystemClock.sleep(SLEEP);
		}
		SystemClock.sleep(5000);
	}
	
	public static void traceCpuUsages(ITracer tracer, HeliosSigar sigar) {
		int cpuId = 0;
		for(CpuPerc cpu : sigar.getCpuPercList()) {
			tracer.traceLong((long)(cpu.getCombined()*100), "Total", "CPU", "Usage", "Cpu" + cpuId);
			tracer.traceLong((long)(cpu.getSys()*100), "Sys", "CPU", "Usage", "Cpu" + cpuId);
			tracer.traceLong((long)(cpu.getUser()*100), "User", "CPU", "Usage", "Cpu" + cpuId);
			cpuId++;
		}
	}
	
	public static void traceTotalCpuUsage(ITracer tracer, HeliosSigar sigar) {
		CpuPerc perc = sigar.getCpuPerc();
		tracer.traceLong((long)(perc.getCombined()*100), "Total", "CPU", "Usage", "Combined");
		tracer.traceLong((long)(perc.getSys()*100), "Sys", "CPU", "Usage", "Combined");
		tracer.traceLong((long)(perc.getUser()*100), "User", "CPU", "Usage", "Combined");
	}
	
	public static void traceMemorySpacesSNMP(ITracer tracer, HeliosSigar sigar) {
		MemoryUsage usage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
		tracer.tracePDU(PDUBuilder.builder(PDU.NOTIFICATION, ".1.3.6.1.4.1.42.2.145.3.163.1.1.2.")
				.counter64("10", usage.getInit())
				.counter64("11", usage.getUsed())
				.counter64("12", usage.getCommitted())
				.counter64("13", usage.getMax())
				.build(), "HeapUsage", "JVM", "Memory"
		);
		usage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
		try {
			tracer.tracePDU(PDUBuilder.builder(PDU.NOTIFICATION, ".1.3.6.1.4.1.42.2.145.3.163.1.1.2.")
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
	
	public static void traceDiskUsage(ITracer tracer, HeliosSigar sigar) {
		for(FileSystem fs: sigar.getFileSystemList()) {
			FileSystemUsage fsu = sigar.getFileSystemUsageOrNull(fs.getDevName().trim());
			if(fsu==null) {
				//log("No Usage for [" + fs.getDirName() + "]");
				continue;
			}
			tracer.traceLong(fsu.getAvail(), "Available", "FileSystems", fs.getSysTypeName(), fs.getDirName().replace("\\", ""));
			tracer.traceLong(fsu.getUsed(), "Used", "FileSystems", fs.getSysTypeName(), fs.getDirName().replace("\\", ""));
			tracer.traceLong(fsu.getTotal(), "Total", "FileSystems", fs.getSysTypeName(), fs.getDirName().replace("\\", ""));
			tracer.traceLong((long)(fsu.getUsePercent()*100), "Used%", "FileSystems", fs.getSysTypeName(), fs.getDirName().replace("\\", ""));
			tracer.traceLong((long)(fsu.getDiskQueue()*100), "Queue", "FileSystems", fs.getSysTypeName(), fs.getDirName().replace("\\", ""));
			tracer.traceLong((long)(fsu.getDiskServiceTime()*100), "ServiceTime", "FileSystems", fs.getSysTypeName(), fs.getDirName().replace("\\", ""));
			tracer.traceDelta(fsu.getDiskReadBytes(), "BytesRead", "FileSystems", fs.getSysTypeName(), fs.getDirName().replace("\\", ""));
			tracer.traceDelta(fsu.getDiskWriteBytes(), "BytesWritten", "FileSystems", fs.getSysTypeName(), fs.getDirName().replace("\\", ""));
			tracer.traceDelta(fsu.getDiskReads(), "Reads", "FileSystems", fs.getSysTypeName(), fs.getDirName().replace("\\", ""));
			tracer.traceDelta(fsu.getDiskWrites(), "Writes", "FileSystems", fs.getSysTypeName(), fs.getDirName().replace("\\", ""));
		}
	}
	
	

	public static void log(Object msg) {
		System.out.println(msg);
	}

}
