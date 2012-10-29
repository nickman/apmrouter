import org.hibernate.*;
import org.json.*;



// "ent" : "agent", "fs" : 20, "t" : 5, "c" : false, "mr" : 100, "fr" : 0, "fmd" : ["foo", "JOIN"]
jsonQuery = """
{ "q" {
    "named" : "findAgentsByHost", {
        "hostId" : 1
    }
}}
""";

exec = jqp.parse(jsonQuery);



session = null;

try {
    session = sessionFactory.openSession();
    dom4jSession =  session.getSession(EntityMode.DOM4J);
    exec.execute(dom4jSession).each() {
        println XML.toJSONObject(it.asXML()).toString(2);
    }

} finally {
    try { session.close(); } catch (e) {}

}
return null;