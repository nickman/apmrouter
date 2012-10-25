import org.hibernate.*;
import org.json.*;

session = null;
try {
    session = sessionFactory.openSession();
    dom4jSession =  session.getSession(EntityMode.DOM4J);
    dom4jSession
        .createQuery("from metric").setMaxResults(3)
        .list().each() {
        //println it.element("name").getStringValue();    
        //println it.asXML();    
        json = XML.toJSONObject(it.asXML());
        println json.toString(2);
    }
    
} finally {
    try { session.close(); } catch (e) {}
}
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
println "\n==========================================================\n";
//println XML.toJSONObject(xmlQuery).toString(2);
println "\n==========================================================\n";
//println XML.toString(XML.toJSONObject(xmlQuery));


return null;