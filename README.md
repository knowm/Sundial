## [![Sundial](http://xeiam.com/wp-content/uploads/sundiallogo.png)](http://xeiam.com/sundial) Sundial 

A Lightweight Job Scheduling Framework

## In a Nutshell

Sundial makes adding cron jobs to your Java application a walk in the park. Simply define jobs, define cron triggers, and start the Sundial scheduler. 

## Long Description

Sundial is a lightweight Java job scheduling framework forked from
Quartz (http://www.quartz-scheduler.org/) stripped down to the bare essentials. Sundial also hides the 
nitty-gritty configuration details of Quartz, reducing the time
needed to get a simple RAM job scheduler up and running. Sundial
uses a ThreadLocal wrapper for each job containing a HashMap for
job key-value pairs. Convenience methods allow easy access to these
parameters. JobActions are reusable components that also have
access to the context parameters. If you are looking 
for an all-Java job scheduling framework that is easy to integrate
into your applications, Sundial is for you.

Usage is very simple: create a Job, configure the Job in jobs.xml, and start the scheduler.

## Example

### Create a Job class With Your Custom Logic

```java
public class SampleJob1 extends Job {

    private final Logger logger = LoggerFactory.getLogger(SampleJob1.class);

    @Override
    public void doRun() throws JobInterruptException {

    logger.info("RUNNING!");

    // Do something interesting...

    logger.info("DONE!");
    }
}
```
   
### Put an XML File on the Classpath Defining the Jobs and Cron Triggers

```xml
    <?xml version='1.0' encoding='utf-8'?>
    <job-scheduling-data>
    
        <schedule>
            <job>
                <name>SampleJob1</name>
                <job-class>com.xeiam.sundial.SampleJob1</job-class>
            </job>
            <trigger>
                <cron>
                    <name>SampleJob1-Trigger</name>
                    <job-name>SampleJob1</job-name>
                    <cron-expression>0/45 * * * * ?</cron-expression>
                </cron>
            </trigger>
    
        </schedule>
    
    </job-scheduling-data>
```
    
### Run your App

```java
    public static void main(String[] args) {
  
      SundialJobScheduler.startScheduler();
    }
```
    
### More Functions

```java
    // start a job by name
    SundialJobScheduler.startJob("SampleJob1");
    
    // start a job by name with data
    Map<String, Object> params = new HashMap<>();
    params.put("MY_KEY", new Integer(660));
    SundialJobScheduler.startJob("SampleJob1", params);
    
    // interrupt a running job
    SundialJobScheduler.stopJob("SampleJob1");
    
    // remove a job from the scheduler
    SundialJobScheduler.removeJob("SampleJob1");
```
    
Now go ahead and [study some more examples](http://xeiam.com/sundial-example-code), [download the thing](http://xeiam.com/sundial-change-log) and [provide feedback](https://github.com/timmolter/Sundial/issues).

## Features

 * [x] Depends only on slf4j
 * [x] ~175 KB Jar
 * [x] Apache 2.0 license
 * [x] Easy to use

## Getting Started
### Non-Maven
Download Jar: http://xeiam.com/sundial-change-log
#### Dependencies
* org.slf4j.slf4j-api-1.7.10

### Maven

The Sundial release artifacts are hosted on Maven Central.

Add the Sundial library as a dependency to your pom.xml file:

```xml
    <dependency>
        <groupId>com.xeiam</groupId>
        <artifactId>sundial</artifactId>
        <version>1.1.3</version>
    </dependency>
```

For snapshots, add the following to your pom.xml file:

```xml
    <repository>
      <id>sonatype-oss-snapshot</id>
      <snapshots/>
      <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    </repository>
    
    <dependency>
        <groupId>com.xeiam</groupId>
        <artifactId>sundial</artifactId>
        <version>2.0.0-SNAPSHOT</version>
    </dependency>
```

## Building

    mvn clean package  
    mvn javadoc:javadoc  

## Cron Expressions in jobs.xml

See the Cron Trigger tutorial over at [quartz-scheduler.org](http://www.quartz-scheduler.org/documentation/quartz-2.2.x/tutorials/crontrigger).
Here are a few examples:  

Expression | Meaning 
------------- | -------------
0 0 12 * * ? | Fire at 12pm (noon) every day
0 15 10 * * ? | Fire at 10:15am every day
0 15 10 ? * MON-FRI | Fire at 10:15am every Monday, Tuesday, Wednesday, Thursday and Friday
0 0/10 * * * ? | Fire every 10 mintes starting at 12 am (midnight) every day

## Bugs
Please report any bugs or submit feature requests to [Sundial's Github issue tracker](https://github.com/timmolter/Sundial/issues).  

## Continuous Integration
[![Build Status](https://travis-ci.org/timmolter/Sundial.png?branch=develop)](https://travis-ci.org/timmolter/Sundial.png)  
[Build History](https://travis-ci.org/timmolter/Sundial/builds)  

## Donations
15MvtM8e3bzepmZ5vTe8cHvrEZg6eDzw2w  
