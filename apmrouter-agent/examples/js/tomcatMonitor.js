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

var workerUnformatter;
if(workerUnformatter==null) {
	workerUnformatter = jmx.unformatter("(.*?)-(.*?)-(.*?)", ["protocol", "address", "port"]);
}


if(jmx.isRegistered('jboss', 'jboss.web:type=RequestProcessor,*')) {
	var r = jmx.jmxCalc(tomcatCalcs);    
	if(r!=null) {
		for(var i = 0, m = r.length; i < m; i++) {
			var c = r[i];
			for(key in c.calcs) {
				var val = c.calcs[key];
				var worker = workerUnformatter.jsunformat(key);
				var ns = ["platform=JBoss", "category=Tomcat", "service=RequestProcessors", "protocol=" + worker.protocol, "listener=" + worker.address, "port=" + worker.port];				
				tracer.traceDeltaGauge(val, "RequestTimeRate", ns);
				
			}
		}
	} else {
		pout.println("R was null");
	}
} else {
	//pout.println("No MBeans matching jboss.web:type=RequestProcessor,*");
}


