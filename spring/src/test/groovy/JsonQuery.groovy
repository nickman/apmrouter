import org.hibernate.*;
import org.json.*;

xmlQuery = """
<Query name="GetAllEmps">
    <Class name="Emp" prefix="org.aa4h.samples"
        rowCountOnly="false">
    </Class>
    <Alias name="job" value="j"/>
    <Alias name="dept" value="d"/>
    <Projections name="DepartmentJobCount">
        <Projection type="groupProperty" name="j.jobName" alias="jobname"/>
        <Projection type="groupProperty" name="d.dname" alias="department"/>
        <Projection type="count" name="empno" alias="count"/>
    </Projections>
</Query>
""";
jsonQuery = """
{ "q" {
    "ent" : "agent", "fs" : 20, "t" : 5, "c" : false, "mr" : 100, "fr" : 0, "fmd" : ["foo", "JOIN"]
}}
""";

jqp.parse(jsonQuery);
obj = jqp.parsed.get().pop();
edc =  obj.get();
println "EDC:${edc}";


session = null;
stSession = null;
try {
    session = sessionFactory.openSession();
    stSession = sessionFactory.openStatelessSession() ;
    dom4jSession =  session.getSession(EntityMode.DOM4J);
    criteria = edc.getExecutableCriteria(dom4jSession);

    criteria.list().each() {
        println XML.toJSONObject(it.asXML()).toString(2);
        //println json.toString(2);
        //println it;
    }
} finally {
    try { session.close(); } catch (e) {}
    try { stSession.close(); } catch (e) {}
}
return null;