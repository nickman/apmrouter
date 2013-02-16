import org.hibernate.*;
import org.helios.apmrouter.catalog.domain.AgentMetricSet;
import com.google.gson.*;

session = null;
try {
    session = sessionFactory.openSession();
    ams = AgentMetricSet.newInstance(session, 1);
    gson = new GsonBuilder().setPrettyPrinting().create();
    println gson.toJson(ams);
    
} finally {
    try { session.close(); } catch (e) {}
}


return null;