package org.knowm.sundial.jobs2;

import org.knowm.sundial.Job;
import org.knowm.sundial.annotations.SimpleTrigger;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 *
 */
@SimpleTrigger(repeatInterval = 30, timeUnit = TimeUnit.SECONDS, isConcurrencyAllowed = true)
public class SampleJob8 extends Job {

    private final Logger logger = LoggerFactory.getLogger(org.knowm.sundial.jobs.SampleJob7.class);

    @Override
    public void doRun() throws JobInterruptException {

        logger.info("Running SampleJob7.");

        // Do something interesting...
    }
}
