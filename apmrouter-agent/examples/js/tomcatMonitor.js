/**
 * Monitoring script to monitor JBoss 4.x Tomcat Stats
 * Whitehead, Oct 1, 2012
 */


var tomcatCalcs = [ {
	  name: "TotalRequestProcessingTime",
    group: ["worker"],
    aggr: "SUM",
    query: {
    		mbs:'jboss',
        on: 'jboss.web:type=RequestProcessor,*',
        attrs: 'requestProcessingTime'
    }
}];


if(jmx.isRegistered('jboss.web:type=RequestProcessor,*')) {
	var r = jmx.jmxCalc(tomcatCalcs);    
	if(r!=null) {
		pout.println("R:" + r.length);
		for(var i = 0, m = r.length; i < m; i++) {
			pout.println("Processing " + i);
			var c = r[i];
			pout.println("\tName:" + c.name);
			for(key in c.calcs) {
				var val = c.calcs[key];
				var splits = key.split("-");
				var port = splits[2];
				var protocol = splits[0];
				var bindAddress = splits[1];
				pout.println("\t\tProtocol:" + protocol + " BindAddress:" + bindAddress + " Port:" + port + " Value:" + val);
				tracer.traceDeltaGauge(val, "RequestTimeRate", ["platform=JBoss", "category=Tomcat", "service=RequestProcessors", "protocol=" + protocol, "listener=" + bindAddress, "port=" + port]);
				
			}
			pout.println("Processed " + i); 
		}
	} else {
		pout.println("R was null");
	}
}

pout.println("Tomcat Monitor OK" );  
