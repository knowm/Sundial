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
package org.knowm.sundial;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.knowm.sundial.Job;
import org.quartz.classloading.CascadingClassLoadHelper;

import static org.junit.Assert.assertEquals;

/**
 * @author timmolter
 */
public class AnnotatedClassFinderTest {

  private static final Set<String> EXPECTED_JOBS = new HashSet(Arrays.asList(new String[]{
          "SampleJob1", "SampleJob2", "SampleJob3", "SampleJob4",
          "SampleJob5", "SampleJob8", "SampleJob6", "SampleJob7", "Concrete"
  }));

  private CascadingClassLoadHelper classLoadHelper;

  @Before
  public void before(){
    classLoadHelper = new CascadingClassLoadHelper();
    classLoadHelper.initialize();
  }

  @Test
  public void testJobsAreLoadedAndCommaWorksAsPackageSeparator() {

    Set<Class<? extends Job>> jobClasses = classLoadHelper.getJobClasses("org.knowm.sundial.jobs,org.knowm.sundial.jobs2");
    Set<String> jobClassNames = new HashSet(jobClasses.size());
    for (Class<? extends Job> jobClass : jobClasses) {
      Assert.assertTrue(jobClass.getPackage().getName().startsWith("org.knowm.sundial.jobs"));
      jobClassNames.add(jobClass.getSimpleName());
    }

    assertEquals(EXPECTED_JOBS, jobClassNames);
  }

  @Test
  public void testThatColonWorksAsPackageSeparator() {

    CascadingClassLoadHelper classLoadHelper = new CascadingClassLoadHelper();
    classLoadHelper.initialize();

    Set<Class<? extends Job>> jobClasses = classLoadHelper.getJobClasses("org.knowm.sundial.jobs:org.knowm.sundial.jobs2");
    Set<String> jobClassNames = new HashSet(jobClasses.size());
    for (Class<? extends Job> jobClass : jobClasses) {
      Assert.assertTrue(jobClass.getPackage().getName().startsWith("org.knowm.sundial.jobs"));
      jobClassNames.add(jobClass.getSimpleName());
    }

    assertEquals(EXPECTED_JOBS, jobClassNames);
  }

  @Test
  public void testOnePackage() {

    CascadingClassLoadHelper classLoadHelper = new CascadingClassLoadHelper();
    classLoadHelper.initialize();

    Set<Class<? extends Job>> jobClasses = classLoadHelper.getJobClasses("org.knowm.sundial.jobs2");

    assertEquals(1, jobClasses.size());

    Class<? extends Job> theClass = jobClasses.iterator().next();

    assertEquals("org.knowm.sundial.jobs2", theClass.getPackage().getName());

    assertEquals("SampleJob8", theClass.getSimpleName());
  }
}
