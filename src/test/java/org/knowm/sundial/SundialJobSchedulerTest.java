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

import org.junit.Test;
import org.knowm.sundial.SundialJobScheduler;
import org.junit.BeforeClass;
import org.junit.Assert;

public class SundialJobSchedulerTest {
    public SundialJobSchedulerTest() {
    }

    @BeforeClass 
    public static void createScheduler() {
        SundialJobScheduler.createScheduler(10, "com.example.empty");
    }

    @Test
    public void shouldBeAbleToAddJobsByName() {
        SundialJobScheduler.addJob("jobByName", "org.knowm.sundial.jobs.SampleJob1");
        Assert.assertTrue(SundialJobScheduler.getAllJobNames().contains("jobByName"));
    }

    @Test
    public void shouldBeAbleToAddJobsByClass() {
        SundialJobScheduler.addJob("jobByClass", org.knowm.sundial.jobs.SampleJob1.class);
        Assert.assertTrue(SundialJobScheduler.getAllJobNames().contains("jobByClass"));
    }
}
