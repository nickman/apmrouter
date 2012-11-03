import org.hibernate.*;

session = null;
try {
    session = sessionFactory.openSession();
    session.createQuery("from metric").setMaxResults(3)
        .list().each() {
        	println it;
        }
    }
    
} finally {
    try { session.close(); } catch (e) {}
}


return null;