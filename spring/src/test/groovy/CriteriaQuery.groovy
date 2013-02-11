import org.hibernate.*;
import org.helios.apmrouter.dataservice.json.catalog.MetricURI;
import org.helios.apmrouter.metric.*;
import org.helios.apmrouter.catalog.api.impl.DataServiceInterceptor;
session = null;
try {
    interceptor = new DataServiceInterceptor();
    //mu = new MetricURI("DefaultDomain/njw810/APMRouterServer/platform=os/resource=netstat");
    mu = new MetricURI("DefaultDomain/njw810/APMRouterServer/platform=JVM/category=Memory/type=Heap?st=0,1,2");
    
    //mu = new MetricURI("DefaultDomain/njw810/APMRouterServer/platform=os/resource=netstat?maxd=3&name=*Bound");
    //mu = new MetricURI("DefaultDomain/njw810/APMRouterServer/platform=os/resource=netstat?name=*Bound&type=${MetricType.LONG_COUNTER.ordinal()}");
    session = sessionFactory.openSession(interceptor);
    criteria = mu.toCriteriaQuery(session);
    println "Acquired Criteria:$criteria";
    criteria.list().each() {
        println it;
    }
} finally {
    try { session.close(); } catch (e) {}
}


return null;