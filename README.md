[![Sundial](http://xeiam.com/images/Sundial_64_64.png)](http://xeiam.com/sundial.jsp)

## Sundial

A Lightweight Job Scheduling Framework

## Description
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

Usage is very simple: create a Job, configure the Job's in jobs.xml, and start the scheduler.

## Example

    public class SampleJob1 extends Job {

        private final Logger logger = LoggerFactory.getLogger(SampleJob1.class);

        @Override
        public void doRun() throws JobInterruptException {

        logger.info("RUNNING!");

        // Do something interesting...

        logger.info("DONE!");
        }
    }
    
Now go ahead and [study some more examples](http://xeiam.com/sundial_examplecode.jsp), [download the thing](http://xeiam.com/sundial_changelog.jsp) and [provide feedback](https://github.com/timmolter/Sundial/issues).

## Features
* Depends only on slf4j
* ~175 KB Jar
* Apache 2.0 license
* Easy to use

## Getting Started
### Non-Maven
Download Jar: http://xeiam.com/sundial_changelog.jsp
#### Dependencies
* org.slf4j.slf4j-api-1.6.5

### Maven
The Sundial release artifacts are hosted on Maven Central.

Add the Sundial library as a dependency to your pom.xml file:

    <dependency>
        <groupId>com.xeiam</groupId>
        <artifactId>sundial</artifactId>
        <version>1.1.2</version>
    </dependency>

For snapshots, add the following to your pom.xml file:

    <repository>
      <id>sonatype-oss-snapshot</id>
      <snapshots/>
      <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    </repository>
    
    <dependency>
        <groupId>com.xeiam</groupId>
        <artifactId>sundial</artifactId>
        <version>1.1.3-SNAPSHOT</version>
    </dependency>

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
