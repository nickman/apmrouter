import java.util.zip.*;

// Raw Avg:  191.4 ms.     //  1,310,086
// GZipped Avg:  ? ms.     // 103006

//payload = new File("/home/nwhitehead/hprojects/apmrouter/apmrouter-server/src/test/resources/san/statvlun.xml").getBytes();
USEGZIP = true;
socket = null;
long sleepTime = 5000;
int loops = 10;
long startTime = System.currentTimeMillis();
for(i in 0..loops-1) {
    try {
        long st = System.currentTimeMillis();
        socket = new Socket("localhost", 1089);
        if(USEGZIP) {
            payload = new File("/home/nwhitehead/hprojects/apmrouter/apmrouter-server/src/test/resources/san/statvlun.xml.gz").getBytes();
            (socket<< payload).flush();
            /*
            sos = socket.getOutputStream();
            gzip = new GZIPOutputStream(sos);
            (gzip<< payload).flush();
            gzip.finish();
            sos.flush();
            */
        } else {
            payload = new File("/home/nwhitehead/hprojects/apmrouter/apmrouter-server/src/test/resources/san/statvlun.xml").getBytes();
            (socket<< payload).flush();
        }
        long el = System.currentTimeMillis()-st;
        println "\tComplete: $el ms.";
    } finally {
        if(socket!=null) try { 
            socket.close(); 
            //println "Socket Closed"; 
        } catch (e) {};
        socket = null;
    }
    Thread.sleep(sleepTime);
}
long elapsed = System.currentTimeMillis()-startTime-(loops * sleepTime);
float avg = elapsed/loops;
title = USEGZIP ? "GZipped" : "Raw";
println "$title: Elapsed:$elapsed ms, Avg:$avg ms. per file";
