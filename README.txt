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


    ********************************************************
    *                     DISCLAIMER                       *
    *                                                      *
    * Use Sundial AT YOUR OWN RISK. Using this api in      *
    * production may result in data loss, data corruption, *
    * or other serious problems.                           *
    *                                                      *
    ********************************************************
    
Dependencies: 
	slf4j-log4j12-1.6.1.jar (required)
	slf4j-api-1.6.1.jar (required)
	servlet-api.jar (not required)
	log4j-1.2.16.jar (not required)