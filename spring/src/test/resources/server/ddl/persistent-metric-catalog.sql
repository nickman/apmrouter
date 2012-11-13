;              
CREATE USER IF NOT EXISTS ALT SALT 'ade5e5446b42f2db' HASH 'f5235ffae303ebce74abae185f00bd160cc7ddda2e9ff3c4164b2b042356d58c'; 
CREATE USER IF NOT EXISTS SA SALT '2567a67c39fcfcc6' HASH '894a8db82f0eed53461e978337107599a20b594e588a4cd3939e6866c3987448' ADMIN;

CREATE  TABLE IF NOT EXISTS PUBLIC.AGENT(
    AGENT_ID INTEGER NOT NULL IDENTITY COMMENT 'The unqiue agent identifier',
    HOST_ID INTEGER NOT NULL COMMENT 'The id of the host this agent is running om.',
    NAME VARCHAR2(120) NOT NULL COMMENT 'The name of the agent.',
    MIN_LEVEL SMALLINT NOT NULL COMMENT 'The lowest level of metrics for this agent.',
    FIRST_CONNECTED TIMESTAMP NOT NULL COMMENT 'The first time the agent was seen.',
    LAST_CONNECTED TIMESTAMP NOT NULL COMMENT 'The last time the agent connected.',
    CONNECTED TIMESTAMP NULL COMMENT 'The time the agent connected or null if not connected.',
    URI VARCHAR2(120) NULL COMMENT 'The listening URI of the connected agent.'
) ; 
 

CREATE TABLE IF NOT EXISTS  PUBLIC.HOST(
    HOST_ID INTEGER NOT NULL IDENTITY COMMENT 'The primary key for the host',
    NAME VARCHAR2(255) NOT NULL COMMENT 'The short or preferred host name',
    DOMAIN VARCHAR2(255) NOT NULL COMMENT 'The domain that the host is in',
    IP VARCHAR2(15) COMMENT 'The ip address of the host',
    FQN VARCHAR2(255)  COMMENT 'The fully qualified name of the host',
    FIRST_CONNECTED TIMESTAMP NOT NULL COMMENT 'The first time the host was seen.',
    LAST_CONNECTED TIMESTAMP NOT NULL COMMENT 'The last time connected.',
    AGENTS INTEGER NOT NULL DEFAULT 0 COMMENT 'The number of agents connected from this host.',
    CONNECTED TIMESTAMP NULL COMMENT 'The time an agent from this host connected or null if not connected.'
) ;      
    

CREATE TABLE IF NOT EXISTS  PUBLIC.METRIC(
    METRIC_ID LONG  NOT NULL IDENTITY COMMENT 'The unique id of the metric.',
    AGENT_ID INTEGER NOT NULL COMMENT 'The  agent identifier for this metric',
    TYPE_ID SMALLINT NOT NULL COMMENT 'The metric type of the metric',
    NAMESPACE VARCHAR2(200) COMMENT 'The namespace of the metric',
    NARR ARRAY NOT NULL COMMENT 'The namespace array items of the metric',
    LEVEL SMALLINT NOT NULL COMMENT 'The number of namespaces in the namespace',
    NAME VARCHAR2(60) COMMENT 'The point of the metric name',
    FIRST_SEEN TIMESTAMP NOT NULL COMMENT 'The first time this metric was seen',
    LAST_SEEN TIMESTAMP COMMENT 'The last time this metric was seen'
) ;       
              

CREATE TABLE IF NOT EXISTS  PUBLIC.TRACE_TYPE(
    TYPE_ID SMALLINT NOT NULL COMMENT 'The unique id of the trace type.',
    TYPE_NAME VARCHAR2(30) COMMENT 'The name of the trace type'
) ;            
ALTER TABLE PUBLIC.TRACE_TYPE ADD CONSTRAINT IF NOT EXISTS PUBLIC.TRACE_TYPE_PK PRIMARY KEY(TYPE_ID);        


ALTER TABLE PUBLIC.AGENT ADD CONSTRAINT IF NOT EXISTS PUBLIC.AGENT_HOST_FK FOREIGN KEY(HOST_ID) REFERENCES PUBLIC.HOST(HOST_ID) NOCHECK;     
ALTER TABLE PUBLIC.METRIC ADD CONSTRAINT IF NOT EXISTS PUBLIC.METRIC_TRACE_TYPE_FK FOREIGN KEY(TYPE_ID) REFERENCES PUBLIC.TRACE_TYPE(TYPE_ID) NOCHECK;       
ALTER TABLE PUBLIC.METRIC ADD CONSTRAINT IF NOT EXISTS PUBLIC.METRIC_AGENT_FK FOREIGN KEY(AGENT_ID) REFERENCES PUBLIC.AGENT(AGENT_ID) NOCHECK;

CREATE UNIQUE INDEX IF NOT EXISTS TRACE_TYPE_AK ON TRACE_TYPE(TYPE_NAME);
CREATE UNIQUE INDEX IF NOT EXISTS HOST_AK ON HOST(NAME);
CREATE UNIQUE INDEX IF NOT EXISTS AGENT_AK ON AGENT(HOST_ID, NAME);
CREATE UNIQUE INDEX IF NOT EXISTS METRIC_AK ON METRIC(AGENT_ID, TYPE_ID, NAMESPACE, NAME);
CREATE  INDEX IF NOT EXISTS METRIC_NAMES ON METRIC(AGENT_ID, NAMESPACE, NAME);

CREATE SEQUENCE IF NOT EXISTS SEQ_HOST START WITH 0 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_AGENT START WITH 0 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_METRIC START WITH 0 INCREMENT BY 1 CACHE 128;

CREATE ALIAS IF NOT EXISTS GET_ID FOR "org.helios.apmrouter.catalog.jdbc.h2.H2StoredProcedure.getID";
CREATE ALIAS IF NOT EXISTS TOUCH FOR "org.helios.apmrouter.catalog.jdbc.h2.H2StoredProcedure.touch";
CREATE ALIAS IF NOT EXISTS HOSTAGENTSTATE FOR "org.helios.apmrouter.catalog.jdbc.h2.H2StoredProcedure.hostAgentState";
CREATE ALIAS IF NOT EXISTS PARENT FOR "org.helios.apmrouter.catalog.jdbc.h2.H2StoredProcedure.parent";
CREATE ALIAS IF NOT EXISTS ROOT FOR "org.helios.apmrouter.catalog.jdbc.h2.H2StoredProcedure.root";



UPDATE HOST SET CONNECTED = NULL, AGENTS = 0;
UPDATE AGENT SET CONNECTED = NULL, URI = NULL;

-- =============================================================================
--    Time Series
-- =============================================================================

CREATE ALIAS IF NOT EXISTS MAKE_MV FOR "org.helios.apmrouter.destination.h2timeseries.H2TimeSeries.make";
CREATE ALIAS IF NOT EXISTS UPSERT_MV FOR "org.helios.apmrouter.destination.h2timeseries.H2TimeSeries.make_and_add";

CREATE ALIAS IF NOT EXISTS IS_MV FOR "org.helios.apmrouter.destination.h2timeseries.H2TimeSeries.isType";
CREATE ALIAS IF NOT EXISTS UPDATE_MV FOR "org.helios.apmrouter.destination.h2timeseries.H2TimeSeries.add";
CREATE DOMAIN IF NOT EXISTS METRIC_VALUE AS OTHER CHECK IS_MV(VALUE);

CREATE TABLE IF NOT EXISTS PUBLIC.METRIC_VALUES  (
	ID LONG NOT NULL  COMMENT 'The ID of the metric that these values are for',
	V METRIC_VALUE NOT NULL COMMENT 'The live time-series values for the referenced metric'  
);

ALTER TABLE PUBLIC.METRIC_VALUES ADD CONSTRAINT IF NOT EXISTS PUBLIC.METRIC_VALUES_PK PRIMARY KEY(ID);
ALTER TABLE PUBLIC.METRIC_VALUES ADD CONSTRAINT IF NOT EXISTS PUBLIC.METRIC_VALUES_FK FOREIGN KEY(ID) REFERENCES PUBLIC.METRIC(METRIC_ID);

CREATE ALIAS IF NOT EXISTS ALL_VALUES FOR "org.helios.apmrouter.destination.h2timeseries.H2TimeSeries.allvalues";
CREATE ALIAS IF NOT EXISTS MV FOR "org.helios.apmrouter.destination.h2timeseries.H2TimeSeries.getValues";

CREATE VIEW IF NOT EXISTS METRIC_DATA AS SELECT * FROM MV(-1);
CREATE VIEW IF NOT EXISTS RICH_METRIC_DATA AS SELECT AGENT_ID, TYPE_ID, NAMESPACE, NARR, NAME, D.* FROM METRIC_DATA D, METRIC M WHERE M.METRIC_ID = D.ID;
