package org.knowm.sundial.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Job to be registered with the scheduler on startup with no automatic trigger. The job
 * will only run when explicitly started via {@code SundialJobScheduler.startJob()} or the admin
 * task endpoint.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ManualTrigger {

  boolean isConcurrencyAllowed() default false;

  String[] jobDataMap() default {};
}
