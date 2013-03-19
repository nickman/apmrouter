import javax.management.*;

mbs = ChannelSessions.getMBeanServerConnection("udp", "marginservice", "com.cpex.ne-wk-nwhi-01", "DefaultDomain");
println mbs.getDefaultDomain();
f = new File("c:/hprojects/byteman/jboss-byteman/sample/scripts/ThreadMonitor.btm");
name = f.getName();
text = f.getText();
on = new ObjectName("org.jboss.byteman.agent:service=Transformer");
installed = mbs.invoke(on, "installScript", [text, name] as Object[], [String.class.getName(), String.class.getName()] as String[]);
println "Installed:${installed}";