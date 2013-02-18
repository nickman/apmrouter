/**
 * Monitoring script to monitor JBoss 4.x Transaction Stats
 * Whitehead, Oct 1, 2012
 */


	var on = 'jboss:service=TransactionManager';
	var ns = ["platform=JBoss", "category=JTA", "service=TXManager" ];
	if(jmx.isRegistered('jboss', on)) {
		tracer.traceDeltaGauge(jmx.getNumericAttribute("jboss", on, "TransactionCount") , "TransactionCount", ns);
		tracer.traceDeltaGauge(jmx.getNumericAttribute("jboss", on, "CommitCount") , "CommitCount", ns);
		tracer.traceDeltaGauge(jmx.getNumericAttribute("jboss", on, "RollbackCount") , "RollbackCount", ns);
	}

	
	
 