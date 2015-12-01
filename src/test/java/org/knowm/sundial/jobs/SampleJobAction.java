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
package org.knowm.sundial.jobs;

import org.knowm.sundial.JobAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A sample Job Action that simply logs a message every time it's called.
 * 
 * @author timmolter
 */
public class SampleJobAction extends JobAction {

  private final Logger logger = LoggerFactory.getLogger(SampleJobAction.class);

  @Override
  public void doRun() {

    Integer myValue = getJobContext().get("MyValue");
    logger.info("myValue: " + myValue);

  }

}
