/**
 * Copyright 2011 Xeiam LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xeiam.sundial;

import java.util.List;

import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.TriggerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author timothy.molter
 * @version $Revision: 1.3 $ $Date: 2010/09/04 22:39:01 $ $Author: xeiam $
 */
public class DefaultTriggerListener implements TriggerListener {

    /** slf4J logger wrapper */
    Logger logger = LoggerFactory.getLogger(DefaultTriggerListener.class);

    /**
     * The default behavior is to veto any job is currently running. However, conncurrent jobs can be created by setting the 'Concurrency' key in jobdatamap set to 'Y'.
     */
    @Override
    public boolean vetoJobExecution(Trigger pTrigger, JobExecutionContext pJobExecutionContext) {

        String lConcurrency = (String) pJobExecutionContext.getJobDetail().getJobDataMap().get("Concurrency");
        if (lConcurrency != null && lConcurrency.equals("Y")) {
            logger.debug("Concurrency allowed. Not Vetoing!");
            return false;
        }

        String newJobName = pJobExecutionContext.getJobDetail().getKey().getName();
        // logger.debug(JobClass);

        try {

            List<JobExecutionContext> currentlyExecutingJobs = DefaultJobScheduler.getScheduler().getCurrentlyExecutingJobs();
            for (JobExecutionContext lJobExecutionContext : currentlyExecutingJobs) {

                String alreadyRunningJobName = lJobExecutionContext.getJobDetail().getKey().getName();

                if (newJobName.equals(alreadyRunningJobName)) {
                    logger.debug("Already Running. Vetoing!");
                    return true;
                } else {
                    logger.debug("Non-matching Job found. Not Vetoing!");
                }
            }
            logger.debug("Not yet Running. Not Vetoing!");
            return false; // if we get here, it checked all running WFs and did not find a match.

        } catch (SchedulerException e) {
            logger.error("ERROR DURING VETO!!!" + e);
            return true;
        }
    }

    /**
     * For whatever reason, Quartz needs this here.
     * 
     * @param s
     * @return
     */
    public String setName(String s) {
        return s;
    }

    @Override
    public String getName() {
        return "DefaultTriggerListener";
    }

    @Override
    public void triggerFired(Trigger arg0, JobExecutionContext arg1) {
    }

    @Override
    public void triggerMisfired(Trigger arg0) {
    }

    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext context, CompletedExecutionInstruction triggerInstructionCode) {
    }
}
