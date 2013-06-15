;              
CREATE USER IF NOT EXISTS ALT SALT 'ade5e5446b42f2db' HASH 'f5235ffae303ebce74abae185f00bd160cc7ddda2e9ff3c4164b2b042356d58c'; 
CREATE USER IF NOT EXISTS SA SALT '2567a67c39fcfcc6' HASH '894a8db82f0eed53461e978337107599a20b594e588a4cd3939e6866c3987448' ADMIN;
CREATE USER IF NOT EXISTS TSDB PASSWORD '' ADMIN; 
CREATE SCHEMA IF NOT EXISTS OPENTSDB AUTHORIZATION TSDB;


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
    

-- Make sure to update org.helios.apmrouter.catalog.jdbc.h2.MetricTrigger if you change this table.
CREATE TABLE IF NOT EXISTS  PUBLIC.METRIC(
    METRIC_ID LONG  NOT NULL COMMENT 'The unique id of the metric.',
    AGENT_ID INTEGER NOT NULL COMMENT 'The  agent identifier for this metric',
    TYPE_ID SMALLINT NOT NULL COMMENT 'The metric type of the metric',
    NAMESPACE VARCHAR2(200) COMMENT 'The namespace of the metric',
    NARR ARRAY NOT NULL COMMENT 'The namespace array items of the metric',
    LEVEL SMALLINT NOT NULL COMMENT 'The number of namespaces in the namespace',
    NAME VARCHAR2(60) COMMENT 'The point of the metric name',
    FIRST_SEEN TIMESTAMP NOT NULL COMMENT 'The first time this metric was seen',
    STATE TINYINT DEFAULT 0 NOT NULL COMMENT 'The status of this metric (ACTIVE, STALE, OFFLINE) Decode byte with org.helios.apmrouter.destination.chronicletimeseries.EntryStatus',
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
CREATE  INDEX IF NOT EXISTS METRIC_NAMESPACE_IND ON METRIC(AGENT_ID, NAMESPACE);

CREATE SEQUENCE IF NOT EXISTS SEQ_HOST START WITH 0 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_AGENT START WITH 0 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_METRIC START WITH 0 INCREMENT BY 1 CACHE 128;

CREATE ALIAS IF NOT EXISTS GET_ID FOR "org.helios.apmrouter.catalog.jdbc.h2.H2StoredProcedure.getID";
CREATE ALIAS IF NOT EXISTS TOUCH FOR "org.helios.apmrouter.catalog.jdbc.h2.H2StoredProcedure.touch";
CREATE ALIAS IF NOT EXISTS HOSTAGENTSTATE FOR "org.helios.apmrouter.catalog.jdbc.h2.H2StoredProcedure.hostAgentState";
CREATE ALIAS IF NOT EXISTS PARENT FOR "org.helios.apmrouter.catalog.jdbc.h2.H2StoredProcedure.parent";
CREATE ALIAS IF NOT EXISTS ROOT FOR "org.helios.apmrouter.catalog.jdbc.h2.H2StoredProcedure.root";
CREATE ALIAS IF NOT EXISTS ASSIGNED FOR "org.helios.apmrouter.catalog.jdbc.h2.H2StoredProcedure.getAssigned";
CREATE ALIAS IF NOT EXISTS STATUS FOR "org.helios.apmrouter.catalog.jdbc.h2.H2StoredProcedure.decode";

-- =============================================================================
--    New element triggers
-- =============================================================================

CREATE TRIGGER IF NOT EXISTS HOST_TRG  AFTER INSERT ON HOST FOR EACH ROW CALL "org.helios.apmrouter.catalog.jdbc.h2.HostTrigger";
CREATE TRIGGER IF NOT EXISTS AGENT_TRG  AFTER INSERT ON AGENT FOR EACH ROW CALL "org.helios.apmrouter.catalog.jdbc.h2.AgentTrigger";
CREATE TRIGGER IF NOT EXISTS AGENT_UPDATE_TRG  AFTER UPDATE ON AGENT FOR EACH ROW CALL "org.helios.apmrouter.catalog.jdbc.h2.AgentTrigger";

CREATE TRIGGER IF NOT EXISTS METRIC_TRG  AFTER INSERT ON METRIC FOR EACH ROW CALL "org.helios.apmrouter.catalog.jdbc.h2.MetricTrigger";
CREATE TRIGGER IF NOT EXISTS METRIC_TRG_UPDATE AFTER UPDATE ON METRIC FOR EACH ROW CALL "org.helios.apmrouter.catalog.jdbc.h2.MetricTrigger";

-- =============================================================================
--    Time Series
-- =============================================================================

--CREATE ALIAS IF NOT EXISTS MAKE_MV FOR "org.helios.apmrouter.destination.h2timeseries.H2TimeSeries.make";
--CREATE ALIAS IF NOT EXISTS UPSERT_MV FOR "org.helios.apmrouter.destination.h2timeseries.H2TimeSeries.make_and_add";
--
--CREATE ALIAS IF NOT EXISTS IS_MV FOR "org.helios.apmrouter.destination.h2timeseries.H2TimeSeries.isType";
--CREATE ALIAS IF NOT EXISTS UPDATE_MV FOR "org.helios.apmrouter.destination.h2timeseries.H2TimeSeries.add";
--CREATE DOMAIN IF NOT EXISTS METRIC_VALUE AS OTHER CHECK IS_MV(VALUE);
--
--CREATE TABLE IF NOT EXISTS PUBLIC.METRIC_VALUES  (
--	ID LONG NOT NULL  COMMENT 'The ID of the metric that these values are for',
--	V METRIC_VALUE NOT NULL COMMENT 'The live time-series values for the referenced metric'  
--);
--
--ALTER TABLE PUBLIC.METRIC_VALUES ADD CONSTRAINT IF NOT EXISTS PUBLIC.METRIC_VALUES_PK PRIMARY KEY(ID);
--ALTER TABLE PUBLIC.METRIC_VALUES ADD CONSTRAINT IF NOT EXISTS PUBLIC.METRIC_VALUES_FK FOREIGN KEY(ID) REFERENCES PUBLIC.METRIC(METRIC_ID);
--
--CREATE ALIAS IF NOT EXISTS ALL_VALUES FOR "org.helios.apmrouter.destination.h2timeseries.H2TimeSeries.allvalues";
--CREATE ALIAS IF NOT EXISTS MV FOR "org.helios.apmrouter.destination.h2timeseries.H2TimeSeries.getValues";
--
--CREATE VIEW IF NOT EXISTS METRIC_DATA AS SELECT * FROM MV(0, -1);
--CREATE VIEW IF NOT EXISTS RICH_METRIC_DATA AS SELECT AGENT_ID, TYPE_ID, NAMESPACE, NARR, NAME, D.* FROM METRIC_DATA D, METRIC M WHERE M.METRIC_ID = D.ID;

-- =============================================================================
--    Unsafe Time Series
-- =============================================================================

--CREATE ALIAS IF NOT EXISTS UNSAFE_MAKE_MV FOR "org.helios.apmrouter.destination.h2timeseries.UnsafeH2TimeSeries.make";
--CREATE ALIAS IF NOT EXISTS UNSAFE_UPSERT_MV FOR "org.helios.apmrouter.destination.h2timeseries.UnsafeH2TimeSeries.make_and_add";
--CREATE ALIAS IF NOT EXISTS UNSAFE_IS_MV FOR "org.helios.apmrouter.destination.h2timeseries.UnsafeH2TimeSeries.isType";
--CREATE ALIAS IF NOT EXISTS UNSAFE_UPDATE_MV FOR "org.helios.apmrouter.destination.h2timeseries.UnsafeH2TimeSeries.add";
--
--CREATE DOMAIN IF NOT EXISTS UNSAFE_METRIC_VALUE AS BINARY CHECK UNSAFE_IS_MV(VALUE);
--
--CREATE TABLE IF NOT EXISTS UNSAFE_METRIC_VALUES( ID LONG NOT NULL  COMMENT 'The ID of the metric that these values are for', V UNSAFE_METRIC_VALUE NOT NULL COMMENT 'The time series for this metric');
--ALTER TABLE PUBLIC.UNSAFE_METRIC_VALUES ADD CONSTRAINT IF NOT EXISTS PUBLIC.UNSAFE_METRIC_VALUES_PK PRIMARY KEY(ID);
--ALTER TABLE PUBLIC.UNSAFE_METRIC_VALUES ADD CONSTRAINT IF NOT EXISTS PUBLIC.UNSAFE_METRIC_VALUES_FK FOREIGN KEY(ID) REFERENCES PUBLIC.METRIC(METRIC_ID);
--
--CREATE ALIAS IF NOT EXISTS UNSAFE_ALL_VALUES FOR "org.helios.apmrouter.destination.h2timeseries.UnsafeH2TimeSeries.allvalues";
--CREATE ALIAS IF NOT EXISTS UNSAFE_MV FOR "org.helios.apmrouter.destination.h2timeseries.UnsafeH2TimeSeries.getValues";
--
--CREATE VIEW IF NOT EXISTS UNSAFE_METRIC_DATA AS SELECT * FROM UNSAFE_MV(0, -1);
--CREATE VIEW IF NOT EXISTS UNSAFE_RICH_METRIC_DATA AS SELECT AGENT_ID, TYPE_ID, NAMESPACE, NARR, NAME, D.* FROM UNSAFE_METRIC_DATA D, METRIC M WHERE M.METRIC_ID = D.ID;

-- =============================================================================
--    Chronicle Time Series
-- =============================================================================


CREATE ALIAS IF NOT EXISTS CV FOR "org.helios.apmrouter.catalog.jdbc.h2.adapters.chronicle.ChronicleTSAdapter.getValues";

CREATE VIEW IF NOT EXISTS CMETRIC_DATA AS SELECT * FROM CV(0, -1);
CREATE VIEW IF NOT EXISTS METRIC_STATUS AS SELECT ID, STATUS, MAX(TS) FROM CMETRIC_DATA  GROUP BY ID, STATUS;
CREATE VIEW IF NOT EXISTS CRICH_METRIC_DATA AS SELECT AGENT_ID, TYPE_ID, NAMESPACE, NARR, NAME, D.* FROM CMETRIC_DATA D, METRIC M WHERE M.METRIC_ID = D.ID;

-- =============================================================================
--    Incrementors
-- =============================================================================

-- CREATE TABLE INCR (	METRIC_ID LONG NOT NULL,INC_VALUE LONG DEFAULT INCR_SEQ.NEXTVAL NOT NULL,LAST_INC TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL)

CREATE TABLE IF NOT EXISTS PUBLIC.INCREMENTOR (
	METRIC_ID LONG NOT NULL COMMENT 'The metric ID of the incrementor',
	INC_VALUE LONG NOT NULL COMMENT 'The value of this incrementor',
	LAST_INC TIMESTAMP NOT NULL COMMENT 'The last time this incrementor was updated'
);

CREATE TABLE IF NOT EXISTS PUBLIC.INTERVAL_INCREMENTOR (
	METRIC_ID LONG NOT NULL COMMENT 'The metric ID of the incrementor',
	INC_VALUE LONG NOT NULL COMMENT 'The value of this incrementor',
	LAST_INC TIMESTAMP NOT NULL COMMENT 'The last time this incrementor was updated'
);

ALTER TABLE PUBLIC.INCREMENTOR ADD CONSTRAINT IF NOT EXISTS PUBLIC.INCREMENTOR_PK PRIMARY KEY(METRIC_ID);
ALTER TABLE PUBLIC.INTERVAL_INCREMENTOR ADD CONSTRAINT IF NOT EXISTS PUBLIC.INTERVAL_INCREMENTOR_PK PRIMARY KEY(METRIC_ID);

ALTER TABLE PUBLIC.INCREMENTOR ADD CONSTRAINT IF NOT EXISTS PUBLIC.INCREMENTOR_FK FOREIGN KEY(METRIC_ID) REFERENCES PUBLIC.METRIC(METRIC_ID);
ALTER TABLE PUBLIC.INTERVAL_INCREMENTOR ADD CONSTRAINT IF NOT EXISTS PUBLIC.INTERVAL_INCREMENTOR_FK FOREIGN KEY(METRIC_ID) REFERENCES PUBLIC.METRIC(METRIC_ID);

DELETE FROM METRIC  WHERE AGENT_ID IN  (SELECT AGENT_ID FROM AGENT WHERE NAME LIKE 'WebSock%');
DELETE FROM AGENT WHERE NAME LIKE 'WebSock%';
COMMIT;



----select * from metric order by last_seen desc
--select namespace, name, last_seen, max(ts)
--from metric m, cmetric_data c
--where m.metric_id  = c.id  
--and last_seen > sysdate -1
--group by namespace, name, last_seen
--order by last_seen desc
