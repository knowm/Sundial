package org.knowm.sundial.xml;

import org.junit.Test;
import org.quartz.classloading.CascadingClassLoadHelper;
import org.quartz.classloading.ClassLoadHelper;
import org.quartz.plugins.xml.XMLSchedulingDataProcessor;

/**
 * @author timmolter
 */
public class TextXMLParser {

  @Test
  public void test0() throws Exception {

    ClassLoadHelper classLoadHelper = new CascadingClassLoadHelper();
    classLoadHelper.initialize();

    XMLSchedulingDataProcessor processor = new XMLSchedulingDataProcessor(classLoadHelper);
    // processor.addJobGroupToNeverDelete(JOB_INITIALIZATION_PLUGIN_NAME);
    // processor.addTriggerGroupToNeverDelete(JOB_INITIALIZATION_PLUGIN_NAME);
    processor.processFile(XMLSchedulingDataProcessor.QUARTZ_XML_DEFAULT_FILE_NAME, false);
  }
}
