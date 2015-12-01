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
