Description
===============

Sundial is a lightweight Java job scheduling framework forked from
Quartz (http://www.quartz-scheduler.org/). Sundial hides the 
nitty-gritty configuration details of Quartz, reducing the time
needed to get a simple RAM job scheduler up and running. Sundial
uses a ThreadLocal wrapper for each job containing a HashMap for
job key-value pairs. Convenience methods allow easy access to these
parameters. Built in logging methods are accessible from all jobs
and job actions. JobActions are reusable job snippets that also have
access to the context parameters and logging. If you are looking 
for an all-Java job scheduling framework, that is easy to integrate
into your applications, Sundial is for you.


Getting Started
===============

Non-Maven
---------
Download Jar: http://xeiam.com/sundial.jsp

Maven
-----
The Sundial artifacts are currently hosted on the Xeiam Nexus repository here:

    <repositories>
      <repository>
        <id>xchange-release</id>
        <releases/>
        <url>http://nexus.xeiam.com/content/repositories/releases</url>
      </repository>
      <repository>
        <id>xchange-snapshot</id>
        <snapshots/>
        <url>http://nexus.xeiam.com/content/repositories/snapshots/</url>
      </repository>
    </repositories>
  
Add this to dependencies in pom.xml:

    <dependency>
      <groupId>com.xeiam</groupId>
      <artifactId>sundial</artifactId>
      <version>1.1.1</version>
    </dependency>

Building
===============
mvn clean package  
mvn javadoc:javadoc  
    
