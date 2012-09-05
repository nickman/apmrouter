package test.org.helios.apmrouter.performance;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;
import org.junit.Ignore;

import test.org.helios.apmrouter.metric.BaseTestCase;

/**
 * <p>Title: BasePerformanceTestCase</p>
 * <p>Description: Base class for performance tests</p> 
 * <p>Some useful JVM init options:<b><code>-server -XX:+PrintCompilation -XX:CompileThreshold=1000 -Xbatch -XX:CICompilerCount=1</code></b>
 * <p>Useful links:<ul>
 * 	<li><a href="https://wikis.oracle.com/display/HotSpotInternals/MicroBenchmarks">Oracle's Java MicroBenchmarks Wiki</a></li>
 *  <li><a href="http://www.ibm.com/developerworks/java/library/j-jtp02225/index.html">Brian Goetz's Java MicroBenchmark article</a></li>
 *  <li><a href="http://blog.joda.org/2011/08/printcompilation-jvm-flag.html">Blog explaining compilation printouts</a></li>
 *  <li><a href="https://gist.github.com/1165804#file_notes.md">Gist with more info on compilation printouts</a></li>  
 * </ul></p>
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.apmrouter.performance.BasePerformanceTestCase</code></p>
 */
@Ignore
public class BasePerformanceTestCase extends BaseTestCase {
	
	/** Indicates if this JVM supports current cpu time per thread */
	public static final boolean CPU_TIME_SUPPORTED = ManagementFactory.getThreadMXBean().isCurrentThreadCpuTimeSupported();
	/** Indicates if this JVM supports thread contention monitoring */
	public static final boolean THREAD_CONTENTION_SUPPORTED = ManagementFactory.getThreadMXBean().isThreadContentionMonitoringSupported();
	
	
	/** Numbers and letters */
	protected static final char[] WORD_CHARS;
	/** The number of Numbers and letters */
	protected static final int WORD_CHARS_LENGTH;
	
	/** Tets random instance */
	protected static final Random RANDOM = new Random(System.nanoTime());
	
	static {
		List<Character> chars = new ArrayList<Character>();
		for(int i = 48; i < 122; i++) {
			if((i >= 58 && i <=64) || (i >= 91 && i <=96)) continue;
			chars.add((char)i);
		}
		WORD_CHARS = new char[chars.size()];
		for(int i = 0; i < chars.size(); i++) {
			WORD_CHARS[i] = chars.get(i);			
		}
		WORD_CHARS_LENGTH = WORD_CHARS.length;
	}
	
	protected static void log(Object obj) {
		System.out.println(obj);
	}
	protected static void loge(Object obj) {
		System.err.println(obj);
	}
	
	/**
	 * Generates a random character
	 * @return a random character
	 */
	protected char randomChar() {
		return WORD_CHARS[Math.abs(RANDOM.nextInt(WORD_CHARS_LENGTH-1))];
	}
	
	/**
	 * Generates an array of random words
	 * @param numberOfWords The number of words to generate
	 * @param wordLength The length of ther words to generate
	 * @return an array of random words
	 */
	protected String[] randomWords(int numberOfWords, int wordLength) {
		String[] words = new String[numberOfWords];
		for(int i = 0; i < numberOfWords; i++) {
			char[] wordChars = new char[wordLength];
			for(int x = 0; x < wordLength; x++) {
				wordChars[x] = randomChar();
			}
			words[i] = new String(wordChars);
		}
		return words;
	}
	
	/**
	 * Collects the memory usage, optionally calling a gc first
	 * @param gcFirst If true, gc will called first
	 * @return an array of heap memory and non-heap memory usages
	 */
	public MemoryUsage[] memoryUsage(boolean gcFirst) {		
		if(gcFirst) {
			ManagementFactory.getMemoryMXBean().gc();
			sleep(500);
			ManagementFactory.getMemoryMXBean().gc();
		}
		return new MemoryUsage[]{ManagementFactory.getMemoryMXBean().getHeapMemoryUsage(), ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage()};
	}
	
	/**
	 * Causes the calling thread to join for the specified time
	 * @param time The time to join
	 * @param unit The unit of the time
	 */
	public void sleep(long time, TimeUnit unit) {
		try {
			Thread.currentThread().join(TimeUnit.MILLISECONDS.convert(time, unit));
		} catch (Exception e) {
			throw new RuntimeException("Failed to sleep", e);
		}
	}
	
	/**
	 * Causes the calling thread to join for the specified time
	 * @param time The time to join in ms.
	 */
	public void sleep(long time) {
		sleep(time, TimeUnit.MILLISECONDS);
	}
	
	public static class ExceptionCountingThreadFactory implements ThreadFactory, Thread.UncaughtExceptionHandler {
		/** Serial number for thread names */
		private static final AtomicLong serial = new AtomicLong(0);
		/** The name of the thread pool */
		private final String name;
		/** Counter for exceptions */
		private static final AtomicInteger exceptionCount = new AtomicInteger(0);
		
		/**
		 * Creates a new ExceptionCountingThreadFactory
		 * @param name The name of the pool we're working for
		 */
		public ExceptionCountingThreadFactory(String name) {
			this.name = name;
		}
		
		/**
		 * {@inheritDoc} 
		 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
		 */
		@Override
		public void uncaughtException(Thread t, Throwable e) {
			exceptionCount.incrementAndGet();
		}

		/**
		 * {@inheritDoc} 
		 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
		 */
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, name + "#" + serial.incrementAndGet());
			t.setDaemon(true);
			t.setUncaughtExceptionHandler(this);
			return t;
		}
		
		/**
		 * Returns the number of unhandled exception counts from this factory's threads
		 * @return the number of unhandled exception counts from this factory's threads
		 */
		public int getExceptionCount() {
			return exceptionCount.get();
		}		
	}
	
	/**
	 * Creates a benchmark thread pool
	 * @param name The name of the thread pool
	 * @param threadCount The number of threads
	 * @return a fully started thread pool executor
	 */
	public ThreadPoolExecutor newExecutorService(final String name, int threadCount) {
		ExceptionCountingThreadFactory ectf = new ExceptionCountingThreadFactory(name);
		ThreadPoolExecutor executorService =  new ThreadPoolExecutor(threadCount, threadCount, 1L, TimeUnit.NANOSECONDS, new ArrayBlockingQueue<Runnable>(threadCount, false), ectf);
		executorService.prestartAllCoreThreads();		
		return executorService;
	}
	
	/**
	 * Returns the exception count for the passed thread pool executor
	 * @param es The thread pool executor to get the exception count for
	 * @return the number of exceptions counted
	 */
	public int getThreadPoolExceptionCount(ThreadPoolExecutor es) {
		return ((ExceptionCountingThreadFactory)es.getThreadFactory()).getExceptionCount();
	}
	
	
	public static void main(String[] args) {
		BasePerformanceTestCase tc = new BasePerformanceTestCase();
		for(int i = 0; i < 2000; i++) {
			tc.testRandomWords();
		}
	}
	
	protected void testRandomWords() {
		log("Testing RandomWords");
		int loopCount = 100000;
		int wordCount = 20;
		int wordSize = 12;
		long dummy = 0L;
		for(int i = 0; i < loopCount; i++) {
			dummy += randomWords(wordCount, wordSize).length;
		}
		log("Warmup Complete. Dummy:" + dummy);
		dummy = 0L;
		SystemClock.startTimer();
		for(int i = 0; i < loopCount; i++) {
			dummy += randomWords(wordCount, wordSize).length;
		}
		ElapsedTime et = SystemClock.endTimer();
		log("Dummy:" + dummy + "\nElapsed Time:" + et);
		log("Ns per array:" + et.avgNs(loopCount));
	}

}
