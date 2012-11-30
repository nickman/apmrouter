//if(!state.get().get('inited')) 

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


if(jmx.isRegistered('jboss', 'jboss.web:type=RequestProcessor,*')) {
	var r = jmx.jmxCalc(tomcatCalcs);    
	if(r!=null) {
		for(var i = 0, m = r.length; i < m; i++) {
			var c = r[i];
			for(key in c.calcs) {
				var val = c.calcs[key];
				var splits = key.split("-");
				var port = splits[2];
				var protocol = splits[0];
				var bindAddress = splits[1];
				//pout.println("\t\tProtocol:" + protocol + " BindAddress:" + bindAddress + " Port:" + port + " Value:" + val);
				tracer.traceDeltaGauge(val, "RequestTimeRate", ["platform=JBoss", "category=Tomcat", "service=RequestProcessors", "protocol=" + protocol, "listener=" + bindAddress, "port=" + port]);
				
			}
		}
	} else {
		pout.println("R was null");
	}
} else {
	pout.println("No MBeans matching jboss.web:type=RequestProcessor,*");
}

pout.println("Tomcat Monitor OK" );  
