;              
CREATE USER IF NOT EXISTS ALT SALT 'ade5e5446b42f2db' HASH 'f5235ffae303ebce74abae185f00bd160cc7ddda2e9ff3c4164b2b042356d58c'; 
CREATE USER IF NOT EXISTS SA SALT '2567a67c39fcfcc6' HASH '894a8db82f0eed53461e978337107599a20b594e588a4cd3939e6866c3987448' ADMIN;
drop table IF EXISTS AGENT;
drop table IF EXISTS METRIC;
drop table IF EXISTS TRACE_TYPE;
drop table IF EXISTS HOST;
drop sequence IF EXISTS  SEQ_HOST;
drop sequence IF EXISTS  SEQ_AGENT;
drop sequence IF EXISTS  SEQ_METRIC;

CREATE MEMORY TEMPORARY TABLE PUBLIC.AGENT(
    AGENT_ID INTEGER NOT NULL IDENTITY COMMENT 'The unqiue agent identifier',
    HOST_ID INTEGER NOT NULL COMMENT 'The id of the host this agent is running om.',
    NAME VARCHAR2(120) NOT NULL COMMENT 'The name of the agent.',
    FIRST_CONNECTED TIMESTAMP NOT NULL COMMENT 'The first time the agent was seen.',
    LAST_CONNECTED TIMESTAMP NOT NULL COMMENT 'The last time the agent connected.'
) NOT PERSISTENT; 
 

CREATE MEMORY TEMPORARY TABLE PUBLIC.HOST(
    HOST_ID INTEGER NOT NULL IDENTITY COMMENT 'The primary key for the host',
    NAME VARCHAR2(255) NOT NULL COMMENT 'The short or preferred host name',
    IP VARCHAR2(15) COMMENT 'The ip address of the host',
    FQN VARCHAR2(255)  COMMENT 'The fully qualified name of the host',
    FIRST_CONNECTED TIMESTAMP NOT NULL COMMENT 'The first time the host was seen.',
    LAST_CONNECTED TIMESTAMP NOT NULL COMMENT 'The last time connected.'
) NOT PERSISTENT;      
    

CREATE MEMORY TEMPORARY TABLE PUBLIC.METRIC(
    METRIC_ID LONG  NOT NULL IDENTITY COMMENT 'The unique id of the metric.',
    AGENT_ID INTEGER NOT NULL COMMENT 'The  agent identifier for this metric',
    TYPE_ID SMALLINT NOT NULL COMMENT 'The metric type of the metric',
    NAMESPACE VARCHAR2(200) COMMENT 'The namespace of the metric',
    NAME VARCHAR2(60) COMMENT 'The point of the metric name',
    FIRST_SEEN TIMESTAMP NOT NULL COMMENT 'The first time this metric was seen',
    LAST_SEEN DATE COMMENT 'The last time this metric was seen'
) NOT PERSISTENT;       
              

CREATE MEMORY TEMPORARY TABLE PUBLIC.TRACE_TYPE(
    TYPE_ID SMALLINT NOT NULL COMMENT 'The unique id of the trace type.',
    TYPE_NAME VARCHAR2(30) COMMENT 'The name of the trace type'
) NOT PERSISTENT;            
ALTER TABLE PUBLIC.TRACE_TYPE ADD CONSTRAINT PUBLIC.TRACE_TYPE_PK PRIMARY KEY(TYPE_ID);        


ALTER TABLE PUBLIC.AGENT ADD CONSTRAINT PUBLIC.AGENT_HOST_FK FOREIGN KEY(HOST_ID) REFERENCES PUBLIC.HOST(HOST_ID) NOCHECK;     
ALTER TABLE PUBLIC.METRIC ADD CONSTRAINT PUBLIC.METRIC_TRACE_TYPE_FK FOREIGN KEY(TYPE_ID) REFERENCES PUBLIC.TRACE_TYPE(TYPE_ID) NOCHECK;       
ALTER TABLE PUBLIC.METRIC ADD CONSTRAINT PUBLIC.METRIC_AGENT_FK FOREIGN KEY(AGENT_ID) REFERENCES PUBLIC.AGENT(AGENT_ID) NOCHECK;

CREATE UNIQUE INDEX TRACE_TYPE_AK ON TRACE_TYPE(TYPE_NAME);
CREATE UNIQUE INDEX HOST_AK ON HOST(NAME);
CREATE UNIQUE INDEX AGENT_AK ON AGENT(NAME);
CREATE UNIQUE INDEX METRIC_AK ON METRIC(AGENT_ID, TYPE_ID, NAMESPACE, NAME);
CREATE  INDEX METRIC_NAMES ON METRIC(AGENT_ID, NAMESPACE, NAME);

CREATE SEQUENCE SEQ_HOST START WITH 0 INCREMENT BY 1;
CREATE SEQUENCE SEQ_AGENT START WITH 0 INCREMENT BY 1;
CREATE SEQUENCE SEQ_METRIC START WITH 0 INCREMENT BY 1 CACHE 128;

CREATE ALIAS GET_ID FOR "org.helios.apmrouter.catalog.jdbc.h2.H2StoredProcedure.getID";