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

import com.xeiam.sundial.exceptions.JobInterruptException;

/**
 * @author timmolter
 * @version $Revision: $ $Date: $ $Author: $
 */
public class SampleJob extends Job {

    @Override
    public void doRun() throws JobInterruptException {

        JobContext lContext = getJobContext();

        System.out.println("RUNNING!");

        int counter = 0;
        while (counter < 3) {

            checkTerminated();

            new SampleJobAction().run();

            Thread t = Thread.currentThread();
            String name = t.getName();

            // System.out.println(lContext.getValue("timestamp") + " TName: " + name);
            counter++;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // e.printStackTrace();
            }
        }
        // setTerminate();
        // checkTerminated();

        System.out.println("DONE!");
    }
}
