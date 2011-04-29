package org.quartz.core.jmx;

import static javax.management.openmbean.SimpleType.BOOLEAN;
import static javax.management.openmbean.SimpleType.DATE;
import static javax.management.openmbean.SimpleType.INTEGER;
import static javax.management.openmbean.SimpleType.LONG;
import static javax.management.openmbean.SimpleType.STRING;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.quartz.Calendar;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.quartz.spi.TriggerFiredBundle;

public class JobExecutionContextSupport {
	private static final String COMPOSITE_TYPE_NAME = "JobExecutionContext";
	private static final String COMPOSITE_TYPE_DESCRIPTION = "Job Execution Instance Details";
	private static final String[] ITEM_NAMES = new String[] { "schedulerName",
			"triggerName", "triggerGroup", "jobName", "jobGroup", "jobDataMap", "calendarName",
			"recovering", "refireCount", "fireTime", "scheduledFireTime",
			"previousFireTime", "nextFireTime", "jobRunTime" };
	private static final String[] ITEM_DESCRIPTIONS = new String[] {
			"schedulerName", "triggerName", "triggerGroup", "jobName", "jobGroup", "jobDataMap",
			"calendarName", "recovering", "refireCount", "fireTime",
			"scheduledFireTime", "previousFireTime", "nextFireTime", "jobRunTime" };
	private static final OpenType[] ITEM_TYPES = new OpenType[] { STRING,
			STRING, STRING, STRING, STRING, JobDataMapSupport.TABULAR_TYPE, STRING, BOOLEAN,
			INTEGER, DATE, DATE, DATE, DATE, LONG };
	private static final CompositeType COMPOSITE_TYPE;
	private static final String TABULAR_TYPE_NAME = "JobExecutionContextArray";
	private static final String TABULAR_TYPE_DESCRIPTION = "Array of composite JobExecutionContext";
	private static final String[] INDEX_NAMES = new String[] { "schedulerName",
			"triggerName", "triggerGroup", "jobName", "jobGroup", "fireTime" };
	private static final TabularType TABULAR_TYPE;

	static {
		try {
			COMPOSITE_TYPE = new CompositeType(COMPOSITE_TYPE_NAME,
					COMPOSITE_TYPE_DESCRIPTION, ITEM_NAMES, ITEM_DESCRIPTIONS,
					ITEM_TYPES);
			TABULAR_TYPE = new TabularType(TABULAR_TYPE_NAME,
					TABULAR_TYPE_DESCRIPTION, COMPOSITE_TYPE, INDEX_NAMES);
		} catch (OpenDataException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return composite data
	 */
	public static CompositeData toCompositeData(JobExecutionContext jec)
			throws SchedulerException {
		try {
			return new CompositeDataSupport(COMPOSITE_TYPE, ITEM_NAMES,
					new Object[] {
							jec.getScheduler().getSchedulerName(),
							jec.getTrigger().getKey().getName(),
							jec.getTrigger().getKey().getGroup(),
							jec.getJobDetail().getKey().getName(),
							jec.getJobDetail().getKey().getGroup(),
							JobDataMapSupport.toTabularData(jec
									.getMergedJobDataMap()),
							determineCalendarName(jec),
							Boolean.valueOf(jec.isRecovering()),
							Integer.valueOf(jec.getRefireCount()),
							jec.getFireTime(), jec.getScheduledFireTime(),
							jec.getPreviousFireTime(), jec.getNextFireTime(),
							Long.valueOf(jec.getJobRunTime()) });
		} catch (OpenDataException e) {
			throw new RuntimeException(e);
		}
	}

	private static String determineCalendarName(JobExecutionContext jec) {
		try {
			Calendar cal = jec.getCalendar();
			if(cal != null) {
				for (String name : jec.getScheduler().getCalendarNames()) {
					Calendar ocal = jec.getScheduler().getCalendar(name);
					if (ocal != null && ocal.equals(cal)) {
						return name;
					}
				}
			}
		} catch (SchedulerException se) {
			/**/
		}
		return "";
	}

	/**
	 * @param tabularData
	 * @return array of region statistics
	 */
	public static TabularData toTabularData(
			final List<JobExecutionContext> executingJobs)
			throws SchedulerException {
		List<CompositeData> list = new ArrayList<CompositeData>();
		for (final Iterator<JobExecutionContext> iter = executingJobs
				.iterator(); iter.hasNext();) {
			list.add(toCompositeData(iter.next()));
		}
		TabularData td = new TabularDataSupport(TABULAR_TYPE);
		td.putAll(list.toArray(new CompositeData[list.size()]));
		return td;
	}
}
