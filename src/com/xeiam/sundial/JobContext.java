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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.quartz.CronTrigger;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xeiam.sundial.exceptions.RequiredParameterException;

/**
 * The JobContext is a Map that contains key value pairs from the Quartz Job's JobDataMap object and any key/value pairs the user wishes to add.
 * 
 * @author timothy.molter
 */
public class JobContext {

    Logger logger = LoggerFactory.getLogger(JobContext.class);

    private static final String KEY_JOB_NAME = "KEY_JOB_NAME";

    private static final String KEY_TRIGGER_NAME = "KEY_TRIGGER_NAME";

    private static final String KEY_TRIGGER_CRON_EXPRESSION = "KEY_TRIGGER_CRON_EXPRESSION";

    /** The Map holding key/value pairs */
    public Map<String, Object> mMap = new HashMap<String, Object>();

    /**
     * Add all the mappings from the JobExecutionContext to the JobContext
     * 
     * @param pJobExecutionContext
     */
    public void addQuartzContext(JobExecutionContext pJobExecutionContext) {

        for (Object lMapKey : pJobExecutionContext.getMergedJobDataMap().keySet()) {
            logger.debug("added key: " + (String) lMapKey);
            logger.debug("added value: " + (String) pJobExecutionContext.getMergedJobDataMap().get(lMapKey));
            mMap.put((String) lMapKey, pJobExecutionContext.getMergedJobDataMap().get(lMapKey));
        }
        mMap.put(KEY_JOB_NAME, pJobExecutionContext.getJobDetail().getKey().getName());
        mMap.put(KEY_TRIGGER_NAME, (pJobExecutionContext.getTrigger().getKey().getName()));
        if (pJobExecutionContext.getTrigger() instanceof CronTrigger) {
            mMap.put(KEY_TRIGGER_CRON_EXPRESSION, ((CronTrigger) pJobExecutionContext.getTrigger()).getCronExpression());
        }

    }

    /**
     * Add a key/value pair to the JobContext
     * 
     * @param pKey
     * @param pValue
     */
    public void put(String pKey, Object pValue) {
        mMap.put(pKey, pValue);
    }

    /**
     * Get a value from a key out of the JobContext
     * 
     * @param pKey
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String pKey) {
        T value = (T) mMap.get(pKey);
        return value;
    }

    /**
     * Get a required value from a key out of the Job Context
     * 
     * @param pKey
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getRequiredValue(String pKey) {
        T value = (T) mMap.get(pKey);
        if (value == null) {
            throw new RequiredParameterException();
        }
        return value;
    }

    /**
     * Get a String value out of the JobContext
     * 
     * @param pKey
     * @return
     */
    public String getStringValue(String pKey) {
        try {
            return (String) get(pKey);
        } catch (Exception e) {
            logger.error("COULD NOT CAST CONTEXT VALUE INTO A STRING FOR KEY: " + pKey);
            return null;
        }
    }

    /**
     * Get a Date value out of the JobContext matching the given Date pattern String
     * 
     * @param pKey
     * @param pDatePattern
     * @return
     */
    public Date getDateValue(String pKey, String pDatePattern) {
        DateFormat lDateFormat = new SimpleDateFormat(pDatePattern);
        try {
            return lDateFormat.parse((String) get(pKey));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get a Boolean value out of the JobContext
     * 
     * @param pKey
     * @return
     */
    public Boolean getBooleanValue(String pKey) {
        try {
            return Boolean.parseBoolean((String) get(pKey));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get a Long value out of the JobContext
     * 
     * @param pKey
     * @return
     */
    public Long getLongValue(String pKey) {
        try {
            return new Long((String) get(pKey));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get a Double value out of the JobContext
     * 
     * @param pKey
     * @return
     */
    public Double getDoubleValue(String pKey) {
        try {
            return new Double((String) get(pKey));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get a Integer value out of the JobContext
     * 
     * @param pKey
     * @return
     */
    public Integer getIntegerValue(String pKey) {
        try {
            return new Integer((String) get(pKey));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Convenience method to get the Job Name
     * 
     * @return
     */
    public String getJobName() {
        return getStringValue(KEY_JOB_NAME);
    }

    /**
     * Convenience method to get the Trigger Name
     * 
     * @return
     */
    public String getTriggerName() {
        return getStringValue(KEY_TRIGGER_NAME);
    }

    /**
     * Convenience method to get the Cron Expression
     * 
     * @return
     */
    public String getCronExpressionName() {
        return getStringValue(KEY_TRIGGER_CRON_EXPRESSION);
    }

}
