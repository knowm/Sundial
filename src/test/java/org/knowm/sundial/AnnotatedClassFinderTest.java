package org.knowm.sundial;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.quartz.classloading.CascadingClassLoadHelper;

/**
 * @author timmolter
 */
public class AnnotatedClassFinderTest {

  private static final Set<String> EXPECTED_JOBS =
      new HashSet(
          Arrays.asList(
              new String[] {
                "SampleJob1",
                "SampleJob2",
                "SampleJob3",
                "SampleJob4",
                "SampleJob5",
                "SampleJob6",
                "SampleJob7",
                "SampleJob8",
                "SampleJob9",
                "Concrete"
              }));

  private CascadingClassLoadHelper classLoadHelper;

  @Before
  public void before() {
    classLoadHelper = new CascadingClassLoadHelper();
    classLoadHelper.initialize();
  }

  @Test
  public void testJobsAreLoadedAndCommaWorksAsPackageSeparator() {

    Set<Class<? extends Job>> jobClasses =
        classLoadHelper.getJobClasses("org.knowm.sundial.jobs,org.knowm.sundial.jobs2");
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

    Set<Class<? extends Job>> jobClasses =
        classLoadHelper.getJobClasses("org.knowm.sundial.jobs:org.knowm.sundial.jobs2");
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
