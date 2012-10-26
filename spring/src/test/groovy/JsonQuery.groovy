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
    "ent" : "agent", "rc" : "false"
}}
""";

jqp.parse(jsonQuery);