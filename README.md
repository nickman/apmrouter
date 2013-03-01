APMRouter
=========

APRouter is a performance and availability monitoring solution. The focus is the acquisition of useful metrics from all the functional components of an application stack and collecting them to a single
logical repository to provide a unified view of the performance environment. The goal is to provide you the data you want, the way you want it. 

The primary components are as follows:

APMRouter Server
================

This is the primary hub to which acquired metrics are delivered to. The server acts as a routing hub, distributing incoming metrics to configured "destinations" that have specified the characteristics
of the data they are interested in receiving. For example, incoming raw numeric data might be routed to a Graphite or OpenTSDB server, while incoming SNMP PDUs might be routed to an SNMP Manager such as
Spectrum or OpenView, and logging events might be routed to a log indexing service such as LogStash or Splunk. The server also contains its own internal default destinations such as the APMRouter Catalog which is a repository of the meta-data of all known metrics, the hosts/devices they came from 
and the on-line status of those devices. This allows you to implement multiple destinations for your collected data, while retaining a consolidated view of your metric dictionary.

* Multi-protocol listeners that can accept submitted metrics over several protocols such as HTTP, TCP, UDP, File Scraping or JMS. The listener API is well demarcated from the core routing engine, 
so it is straight forward to implement new listener protocols, both in terms of the transport protocol and the data format of the submitted data, and the roadmap includes several additional protocols to
enhance the flexibility of the server as much as possible. Collectd and Thrift are planned. If you're intrested in others, or want to write one yourself, post a github issue and we'll be all over it.

* Destinations are server resident adapters you can configure to forward all, or filtered selections of incoming metrics, to external services that we've intergated into APMRouter. Current supported 
destinations include:
  * OpenTSDB
  * Embedded OpenTSDB ( a fork of OpenTSDB that you can run inside the APMRouter )
  * Graphite
  * Wily Introscope
  * Cube (http://square.github.com/cube/)
  * Seriesly ( A Go based, document oriented time-series database)  (https://github.com/dustin/seriesly)
  * SNMP Gateway (forwards received SNMP PDUs to configured SNMP Managers
  * Additional destinations are on the roadmap, including Complex Event Processors and an Apache Camel Destination which will significantly simplify the creation of customized metric data handling.
  * The Destination API makes destinations easy to implement, so perhaps your Destination is out there too.

* The server's internal default destinations are:
  * The APMRouter Metric Catalog, a database which maintains a [near] real-time representation of all connected or monitored hosts and agents, as well as a canonical dictionary of all known metrics. 
    The metric dictionary simplifies correlating data from multiple destinations, and while you will most likely find it useful, it does not introduce any dependencies if you simply want to implement
    APMRouter as a collection funnel for another performance system you're in love with or just tied to. 
  * In order to support realtime data visualization and reporting, as well as general data interval smoothing, APMRouter implements it's own internal time-series database implemented using 
    the collaboration of the H2 database and the absurdly fast Java-Chronicle persistence library. This database only maintains the "live-tier" of data, which by default is the last 5 minutes split into 
    15 second intervals. This is configurable and we're looking to extend this component to support additional roll-up tiers. So how do you get data after the last 5 minutes ? We're implementing a common
    data query API which will be implemented for most of the supported destinations, so if you want to visualize historical data in the APMRouter Console, you will be able to pick the destination[s] you
    want to see data for and go to town. We're hoping to have the first few of these available in about 4 weeks. What day is it today ? According to my watch, it's February 29, 2013, but I happen to know
    that it is actually March 1.

* JMXUYY is the view of what's going on in the server. It stands for "JMX Up the Ying Yang", which is to say, there's a lot of internal details exposed about the internals of the APMRouter Server. You may 
  need or be interested in this, but that's how we make sure everything is running smoothly (or how we figure out what isn't running so smoothly, ahem.) .