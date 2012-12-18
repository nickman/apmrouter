import javax.management.*;


mbs = ChannelSessions.getMBeanServerConnection("udp", "jboss-default", "njw810", "DefaultDomain");
println mbs.getDefaultDomain();
f = new File("/home/nwhitehead/hprojects/apmrouter/apmrouter-agent/src/test/resources/aop/byteman/sockets/SocketMonitor.btm");
name = f.getName();
text = f.getText();
on = new ObjectName("org.jboss.byteman.agent:service=Transformer");
installed = mbs.invoke(on, "installScript", [text, name] as Object[], [String.class.getName(), String.class.getName()] as String[]);

println "Installed Rules:";
installed.each() {
	println "\t[${it}]";
}

return null;