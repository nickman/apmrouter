import org.hibernate.*;
import org.helios.apmrouter.catalog.domain.Metric;
import org.helios.apmrouter.catalog.api.impl.DataServiceInterceptor;

session = null;
try {
    int agentId = 1;
    int level = 2;
    //String parent = "category=MemoryPools";
    //String parent = "category=Compilation";
    String parent = 'compiler=HotSpot64-BitTieredCompilers';

    session = sessionFactory.openSession(new DataServiceInterceptor());    
    query = session.createSQLQuery("""
        select {m.*} from metric m where m.agent_id = :agentId and m.level = :level+1 and m.narr[:level] = :parent
        
        
        
        
    """) //.addEntity("m", "metric").setMaxResults(10)
    query.setInteger("level", level);
    query.setInteger("agentId", agentId);    
    query.setString("parent", parent);
    
    query.list().each() {
            println it;
    }
} finally {
    try { session.close(); } catch (e) {}
}


return null;