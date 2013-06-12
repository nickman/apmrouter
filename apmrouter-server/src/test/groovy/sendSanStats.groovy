import java.util.zip.*;

// bzip2: Elapsed:23 ms, Avg:2.3 ms. per file
// gzip: Elapsed:25 ms, Avg:2.5 ms. per file
// raw: Elapsed:155 ms, Avg:15.5 ms. per file

// 100
// bzip2: Elapsed:1254 ms, Avg:12.54 ms. per file
// gzip: Elapsed:332 ms, Avg:3.32 ms. per file






fileDir = "c:/hprojects/apmrouter/apmrouter-server/src/test/resources/san";

compression  = "gzip";

if("raw".equals(compression)) {
    payload = new File("${fileDir}/statvlun.xml").getBytes();
} else if("gzip".equals(compression)) {
    payload = new File("${fileDir}/statvlun.xml.gz").getBytes();
} else if("bzip2".equals(compression)) {
    payload = new File("${fileDir}/statvlun.xml.bzip2").getBytes();
} else {
    throw new Exception("Invalid compression:[$compression]");
}
//



socket = null;
long sleepTime = 500;
int loops = 1;
long startTime = System.currentTimeMillis();
for(i in 0..loops-1) {
    try {
        long st = System.currentTimeMillis();
        socket = new Socket("localhost", 1089);
        //(socket<< payload).flush();
        socket.withObjectStreams() { is, os->
            os.writeDouble((Math.sqrt(Math.PI));
            os.flush();
        }
/*
        response = socket.withStreams() { is, os ->
            println "Writing payload...";
            os.write(payload);
            os.flush();
            println "Wrote payload";
            //(os << payload).flush();
            return is.readLines();
        }
*/        
        println "Response:${response}";
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

println "$compression: Elapsed:$elapsed ms, Avg:$avg ms. per file";