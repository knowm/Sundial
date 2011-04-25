Sundial is a lightweight Java job scheduling framework built on top
of Quartz (http://www.quartz-scheduler.org/). Sundial hides the 
nitty-gritty configuration details of Quartz, reducing the time
needed to get a simple RAM job scheduler up and running. Sundial
uses a ThreadLocal wrapper for each job containing a HashMap for
job key-value pairs. Convenience methods allow easy access to these
parameters. Built in logging methods are accessible from all jobs
and job actions. JobActions are reusable job snippets that also have
access to the context parameters and logging.


    ********************************************************
    *                     DISCLAIMER                       *
    *                                                      *
    * Use Sundial AT YOUR OWN RISK. Using this system in   *
    * production may result in data loss, data corruption, *
    * or other serious problems.                           *
    *                                                      *
    ********************************************************