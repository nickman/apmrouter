import javax.management.*;


mbs = ChannelSessions.getMBeanServerConnection("udp", "jboss-default", "njw810", "DefaultDomain");
println mbs.getDefaultDomain();
f = new File("/tmp/socketmon.btm");
name = f.getName();
text = f.getText();
on = new ObjectName("org.jboss.byteman.agent:service=Transformer");
installed = mbs.invoke(on, "installScript", [text, name] as Object[], [String.class.getName(), String.class.getName()] as String[]);

println "Installed Rules:";
installed.each() {
    println "\t[${it}]";
}

/*
mbs = ChannelSessions.getMBeanServerConnection("udp", "jboss-default", "njw810", "jboss");
jndiOn = new ObjectName("jboss:service=Naming");
mbs.invoke(jndiOn, "stop", [] as Object[], [] as String[]);
println "Stopped JNDI Server";
mbs.invoke(jndiOn, "start", [] as Object[], [] as String[]);
println "Started JNDI Server";

bytesRead = new URL("http://localhost:8080/jmx-console").getText().length();
println "Read $bytesRead bytes from URL";
*/
return null;