/**
 * Copyright 2015 Knowm Inc. (http://knowm.org) and contributors.
 * Copyright 2013-2015 Xeiam LLC (http://xeiam.com) and contributors.
 * Copyright 2001-2011 Terracotta Inc. (http://terracotta.org).
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
package org.quartz.plugins.xml;

import static org.quartz.builders.CronTriggerBuilder.cronTriggerBuilder;
import static org.quartz.builders.JobBuilder.newJobBuilder;
import static org.quartz.builders.SimpleTriggerBuilder.simpleTriggerBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.knowm.sundial.Job;
import org.quartz.builders.CronTriggerBuilder;
import org.quartz.builders.SimpleTriggerBuilder;
import org.quartz.classloading.ClassLoadHelper;
import org.quartz.core.Scheduler;
import org.quartz.exceptions.ObjectAlreadyExistsException;
import org.quartz.exceptions.SchedulerException;
import org.quartz.jobs.JobDetail;
import org.quartz.triggers.OperableTrigger;
import org.quartz.triggers.SimpleTrigger;
import org.quartz.triggers.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Parses an XML file that declares Jobs and their schedules (Triggers), and processes the related data.
 *
 * @author James House
 * @author Past contributions from <a href="mailto:bonhamcm@thirdeyeconsulting.com">Chris Bonham</a>
 * @author Past contributions from pl47ypus
 * @author timmolter
 * @since Quartz 1.8
 */
public class XMLSchedulingDataProcessor implements ErrorHandler {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constants.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private static final String QUARTZ_XSD_PATH_IN_JAR = "org/knowm/sundial/xml/job_scheduling_data.xsd";

  public static final String QUARTZ_XML_DEFAULT_FILE_NAME = "jobs.xml";

  /**
   * XML Schema dateTime datatype format.
   * <p>
   * See <a href="http://www.w3.org/TR/2001/REC-xmlschema-2-20010502/#dateTime"> http://www.w3.org/TR/2001/REC-xmlschema-2-20010502/#dateTime</a>
   */
  private static final String XSD_DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss";

  private static final SimpleDateFormat dateFormat = new SimpleDateFormat(XSD_DATE_FORMAT);

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  // scheduling commands
  private List<JobDetail> loadedJobs = new LinkedList<JobDetail>();
  private List<OperableTrigger> loadedTriggers = new LinkedList<OperableTrigger>();

  private Collection<Exception> validationExceptions = new ArrayList<Exception>();

  private ClassLoadHelper classLoadHelper = null;

  private DocumentBuilder docBuilder = null;
  private XPath xpath = null;

  private final Logger logger = LoggerFactory.getLogger(XMLSchedulingDataProcessor.class);

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * Constructor for JobSchedulingDataLoader.
   *
   * @param classLoadHelper
   * @param clh class-loader helper to share with digester.
   * @throws ParserConfigurationException if the XML parser cannot be configured as needed.
   */
  public XMLSchedulingDataProcessor(ClassLoadHelper classLoadHelper) throws ParserConfigurationException {

    this.classLoadHelper = classLoadHelper;

    initDocumentParser();
  }

  /**
   * Initializes the XML parser.
   *
   * @throws ParserConfigurationException
   */
  private void initDocumentParser() throws ParserConfigurationException {

    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

    docBuilderFactory.setNamespaceAware(false);
    docBuilderFactory.setValidating(true);

    docBuilderFactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");

    docBuilderFactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource", resolveSchemaSource());

    docBuilder = docBuilderFactory.newDocumentBuilder();

    docBuilder.setErrorHandler(this);

    xpath = XPathFactory.newInstance().newXPath();
  }

  private Object resolveSchemaSource() {

    InputSource inputSource = null;

    InputStream is = null;

    // try {
    is = classLoadHelper.getResourceAsStream(QUARTZ_XSD_PATH_IN_JAR);

    if (is == null) {
      logger.warn("Could not load jobs schema from classpath!");
    } else {
      inputSource = new InputSource(is);
    }

    return inputSource;
  }

  /**
   * Process the xml file in the given location, and schedule all of the jobs defined within it.
   *
   * @param fileName meta data file name.
   */
  public void processFile(String fileName, boolean failOnFileNotFound) throws Exception {

    boolean fileFound = false;
    InputStream f = null;
    try {
      String furl = null;

      File file = new File(fileName); // files in filesystem
      if (!file.exists()) {
        URL url = classLoadHelper.getResource(fileName);
        if (url != null) {
          try {
            furl = URLDecoder.decode(url.getPath(), "UTF-8");
          } catch (UnsupportedEncodingException e) {
            furl = url.getPath();
          }
          file = new File(furl);
          try {
            f = url.openStream();
          } catch (IOException ignor) {
            // Swallow the exception
          }
        }
      } else {
        try {
          f = new java.io.FileInputStream(file);
        } catch (FileNotFoundException e) {
          // ignore
        }
      }

      if (f == null) {
        fileFound = false;
      } else {
        fileFound = true;
      }
    } finally {
      try {
        if (f != null) {
          f.close();
        }
      } catch (IOException ioe) {
        logger.warn("Error closing jobs file " + fileName, ioe);
      }
    }

    if (!fileFound) {
      if (failOnFileNotFound) {
        throw new SchedulerException("File named '" + fileName + "' does not exist.");
      } else {
        logger.warn("File named '" + fileName + "' does not exist. This is OK if you don't want to use an XML job config file.");
      }
    } else {
      processFile(fileName);
    }
  }

  /**
   * Process the xmlfile named <code>fileName</code> with the given system ID.
   *
   * @param fileName meta data file name.
   * @param systemId system ID.
   */
  private void processFile(String fileName) throws ValidationException, ParserConfigurationException, SAXException, IOException, SchedulerException,
      ClassNotFoundException, ParseException, XPathException {

    prepForProcessing();

    logger.info("Parsing XML file: " + fileName);
    InputSource is = new InputSource(getInputStream(fileName));
    // is.setSystemId(systemId);

    process(is);

    maybeThrowValidationException();
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private void prepForProcessing() {

    clearValidationExceptions();

    loadedJobs.clear();
    loadedTriggers.clear();
  }

  private void process(InputSource is) throws SAXException, IOException, ParseException, XPathException, ClassNotFoundException {

    // load the document
    Document document = docBuilder.parse(is);

    //
    // Extract Job definitions...
    //

    NodeList jobNodes = (NodeList) xpath.evaluate("/job-scheduling-data/schedule/job", document, XPathConstants.NODESET);

    logger.debug("Found " + jobNodes.getLength() + " job definitions.");

    for (int i = 0; i < jobNodes.getLength(); i++) {

      Node jobDetailNode = jobNodes.item(i);

      String jobName = getTrimmedToNullString(xpath, "name", jobDetailNode);
      String jobDescription = getTrimmedToNullString(xpath, "description", jobDetailNode);
      String jobClassName = getTrimmedToNullString(xpath, "job-class", jobDetailNode);
      boolean isConcurrencyAllowed = getBoolean(xpath, "concurrency-allowed", jobDetailNode);
      Class<? extends Job> jobClass = classLoadHelper.loadClass(jobClassName);

      JobDetail jobDetail = newJobBuilder(jobClass).withIdentity(jobName).isConcurrencyAllowed(isConcurrencyAllowed).withDescription(jobDescription)
          .build();

      NodeList jobDataEntries = (NodeList) xpath.evaluate("job-data-map/entry", jobDetailNode, XPathConstants.NODESET);

      for (int k = 0; k < jobDataEntries.getLength(); k++) {
        Node entryNode = jobDataEntries.item(k);
        String key = getTrimmedToNullString(xpath, "key", entryNode);
        String value = getTrimmedToNullString(xpath, "value", entryNode);
        jobDetail.getJobDataMap().put(key, value);
      }

      logger.debug("Parsed job definition: " + jobDetail);

      loadedJobs.add(jobDetail);
    }

    //
    // Extract Trigger definitions...
    //

    NodeList triggerEntries = (NodeList) xpath.evaluate("/job-scheduling-data/schedule/trigger/*", document, XPathConstants.NODESET);

    logger.debug("Found " + triggerEntries.getLength() + " trigger definitions.");

    for (int j = 0; j < triggerEntries.getLength(); j++) {

      Node triggerNode = triggerEntries.item(j);
      String triggerName = getTrimmedToNullString(xpath, "name", triggerNode);
      String triggerDescription = getTrimmedToNullString(xpath, "description", triggerNode);
      String triggerMisfireInstructionConst = getTrimmedToNullString(xpath, "misfire-instruction", triggerNode);
      String triggerPriorityString = getTrimmedToNullString(xpath, "priority", triggerNode);
      String triggerCalendarRef = getTrimmedToNullString(xpath, "calendar-name", triggerNode);
      String triggerJobName = getTrimmedToNullString(xpath, "job-name", triggerNode);

      int triggerPriority = Trigger.DEFAULT_PRIORITY;
      if (triggerPriorityString != null) {
        triggerPriority = Integer.valueOf(triggerPriorityString);
      }

      String startTimeString = getTrimmedToNullString(xpath, "start-time", triggerNode);
      String startTimeFutureSecsString = getTrimmedToNullString(xpath, "start-time-seconds-in-future", triggerNode);
      String endTimeString = getTrimmedToNullString(xpath, "end-time", triggerNode);

      Date triggerStartTime = null;
      if (startTimeFutureSecsString != null) {
        triggerStartTime = new Date(System.currentTimeMillis() + (Long.valueOf(startTimeFutureSecsString) * 1000L));
      } else {
        triggerStartTime = (startTimeString == null || startTimeString.length() == 0 ? new Date() : dateFormat.parse(startTimeString));
      }
      Date triggerEndTime = endTimeString == null || endTimeString.length() == 0 ? null : dateFormat.parse(endTimeString);

      OperableTrigger trigger;

      if (triggerNode.getNodeName().equals("simple")) {

        String repeatCountString = getTrimmedToNullString(xpath, "repeat-count", triggerNode);
        String repeatIntervalString = getTrimmedToNullString(xpath, "repeat-interval", triggerNode);

        int repeatCount = repeatCountString == null ? SimpleTrigger.REPEAT_INDEFINITELY : Integer.parseInt(repeatCountString);
        long repeatInterval = repeatIntervalString == null ? 0 : Long.parseLong(repeatIntervalString);

        trigger = simpleTriggerBuilder().withRepeatCount(repeatCount).withIntervalInMilliseconds(repeatInterval).withIdentity(triggerName)
            .withDescription(triggerDescription).forJob(triggerJobName).startAt(triggerStartTime).endAt(triggerEndTime).withPriority(triggerPriority)
            .modifiedByCalendar(triggerCalendarRef).build();

        if (triggerMisfireInstructionConst != null && triggerMisfireInstructionConst.length() != 0) {
          if (triggerMisfireInstructionConst.equals("MISFIRE_INSTRUCTION_FIRE_NOW")) {
            ((SimpleTriggerBuilder) trigger).withMisfireHandlingInstructionFireNow();
          } else if (triggerMisfireInstructionConst.equals("MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT")) {
            ((SimpleTriggerBuilder) trigger).withMisfireHandlingInstructionNextWithExistingCount();
          } else if (triggerMisfireInstructionConst.equals("MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT")) {
            ((SimpleTriggerBuilder) trigger).withMisfireHandlingInstructionNextWithRemainingCount();
          } else if (triggerMisfireInstructionConst.equals("MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT")) {
            ((SimpleTriggerBuilder) trigger).withMisfireHandlingInstructionNowWithExistingCount();
          } else if (triggerMisfireInstructionConst.equals("MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT")) {
            ((SimpleTriggerBuilder) trigger).withMisfireHandlingInstructionNowWithRemainingCount();
          } else if (triggerMisfireInstructionConst.equals("MISFIRE_INSTRUCTION_SMART_POLICY")) {
            // do nothing.... (smart policy is default)
          } else {
            throw new ParseException(
                "Unexpected/Unhandlable Misfire Instruction encountered '" + triggerMisfireInstructionConst + "', for trigger: " + triggerName, -1);
          }
        }
      } else if (triggerNode.getNodeName().equals("cron")) {

        String cronExpression = getTrimmedToNullString(xpath, "cron-expression", triggerNode);
        String timezoneString = getTrimmedToNullString(xpath, "time-zone", triggerNode);

        TimeZone tz = timezoneString == null ? null : TimeZone.getTimeZone(timezoneString);

        trigger = cronTriggerBuilder(cronExpression).inTimeZone(tz).withIdentity(triggerName).withDescription(triggerDescription)
            .forJob(triggerJobName).startAt(triggerStartTime).endAt(triggerEndTime).withPriority(triggerPriority)
            .modifiedByCalendar(triggerCalendarRef).build();

        if (triggerMisfireInstructionConst != null && triggerMisfireInstructionConst.length() != 0) {
          if (triggerMisfireInstructionConst.equals("MISFIRE_INSTRUCTION_DO_NOTHING")) {
            ((CronTriggerBuilder) trigger).withMisfireHandlingInstructionDoNothing();
          } else if (triggerMisfireInstructionConst.equals("MISFIRE_INSTRUCTION_FIRE_ONCE_NOW")) {
            ((CronTriggerBuilder) trigger).withMisfireHandlingInstructionFireAndProceed();
          } else if (triggerMisfireInstructionConst.equals("MISFIRE_INSTRUCTION_SMART_POLICY")) {
            // do nothing.... (smart policy is default)
          } else {
            throw new ParseException(
                "Unexpected/Unhandlable Misfire Instruction encountered '" + triggerMisfireInstructionConst + "', for trigger: " + triggerName, -1);
          }
        }
      }

      else {
        throw new ParseException("Unknown trigger type: " + triggerNode.getNodeName(), -1);
      }

      NodeList jobDataEntries = (NodeList) xpath.evaluate("job-data-map/entry", triggerNode, XPathConstants.NODESET);

      for (int k = 0; k < jobDataEntries.getLength(); k++) {
        Node entryNode = jobDataEntries.item(k);
        String key = getTrimmedToNullString(xpath, "key", entryNode);
        String value = getTrimmedToNullString(xpath, "value", entryNode);
        trigger.getJobDataMap().put(key, value);
      }

      logger.debug("Parsed trigger definition: " + trigger);

      loadedTriggers.add(trigger);
    }
  }

  private String getTrimmedToNullString(XPath xpath, String elementName, Node parentNode) throws XPathExpressionException {

    String str = (String) xpath.evaluate(elementName, parentNode, XPathConstants.STRING);

    if (str != null) {
      str = str.trim();
    }

    if (str != null && str.length() == 0) {
      str = null;
    }

    return str;
  }

  protected Boolean getBoolean(XPath xpathToElement, String elementName, Node parentNode) throws XPathExpressionException {

    String str = (String) xpath.evaluate(elementName, parentNode, XPathConstants.STRING);

    return str.equalsIgnoreCase("true");
  }

  /**
   * Returns a <code>List</code> of jobs loaded from the xml file.
   * <p/>
   *
   * @return a <code>List</code> of jobs.
   */
  private List<JobDetail> getLoadedJobs() {

    return Collections.unmodifiableList(loadedJobs);
  }

  /**
   * Returns a <code>List</code> of triggers loaded from the xml file.
   * <p/>
   *
   * @return a <code>List</code> of triggers.
   */
  private List<OperableTrigger> getLoadedTriggers() {

    return Collections.unmodifiableList(loadedTriggers);
  }

  /**
   * Returns an <code>InputStream</code> from the fileName as a resource.
   *
   * @param fileName file name.
   * @return an <code>InputStream</code> from the fileName as a resource.
   */
  private InputStream getInputStream(String fileName) {

    return this.classLoadHelper.getResourceAsStream(fileName);
  }

  /**
   * Schedules the given sets of jobs and triggers.
   *
   * @param sched job scheduler.
   * @exception SchedulerException if the Job or Trigger cannot be added to the Scheduler, or there is an internal Scheduler error.
   */
  public void scheduleJobs(Scheduler sched) throws SchedulerException {

    List<JobDetail> jobs = new LinkedList<JobDetail>(getLoadedJobs());
    List<OperableTrigger> triggers = new LinkedList<OperableTrigger>(getLoadedTriggers());

    logger.info("Adding " + jobs.size() + " jobs, " + triggers.size() + " triggers.");

    for (JobDetail jobDetail : jobs) {
      logger.info("Scheduled job: {} ", jobDetail);

      sched.addJob(jobDetail);
    }

    for (OperableTrigger trigger : triggers) {

      logger.info("Scheduled trigger: {}", trigger);

      if (trigger.getStartTime() == null) {
        trigger.setStartTime(new Date());
      }

      Trigger dupeT = sched.getTrigger(trigger.getName());
      if (dupeT != null) { // if trigger with name already exists

        if (!dupeT.getJobName().equals(trigger.getJobName())) {
          logger.warn("Possibly duplicately named ({}) triggers in jobs xml file! ", trigger.getName());
        }

        sched.rescheduleJob(trigger.getName(), trigger);
      } else {
        logger.debug("Scheduling job: " + trigger.getJobName() + " with trigger: " + trigger.getName());

        try {
          sched.scheduleJob(trigger);
        } catch (ObjectAlreadyExistsException e) {
          logger.debug("Adding trigger: " + trigger.getName() + " for job: " + trigger.getJobName() + " failed because the trigger already existed.");
        }
      }
    }

  }

  /**
   * ErrorHandler interface. Receive notification of a warning.
   *
   * @param e The error information encapsulated in a SAX parse exception.
   * @exception SAXException Any SAX exception, possibly wrapping another exception.
   */
  @Override
  public void warning(SAXParseException e) throws SAXException {

    addValidationException(e);
  }

  /**
   * ErrorHandler interface. Receive notification of a recoverable error.
   *
   * @param e The error information encapsulated in a SAX parse exception.
   * @exception SAXException Any SAX exception, possibly wrapping another exception.
   */
  @Override
  public void error(SAXParseException e) throws SAXException {

    addValidationException(e);
  }

  /**
   * ErrorHandler interface. Receive notification of a non-recoverable error.
   *
   * @param e The error information encapsulated in a SAX parse exception.
   * @exception SAXException Any SAX exception, possibly wrapping another exception.
   */
  @Override
  public void fatalError(SAXParseException e) throws SAXException {

    addValidationException(e);
  }

  /**
   * Adds a detected validation exception.
   *
   * @param e SAX exception.
   */
  private void addValidationException(SAXException e) {

    validationExceptions.add(e);
  }

  /**
   * Resets the the number of detected validation exceptions.
   */
  private void clearValidationExceptions() {

    validationExceptions.clear();
  }

  /**
   * Throws a ValidationException if the number of validationExceptions detected is greater than zero.
   *
   * @exception ValidationException DTD validation exception.
   */
  private void maybeThrowValidationException() throws ValidationException {

    if (validationExceptions.size() > 0) {
      throw new ValidationException("Encountered " + validationExceptions.size() + " validation exceptions.", validationExceptions);
    }
  }
}
