package com.xeiam.sundial.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SimpleTrigger {

  long repeatInterval() default 0;

  TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

  // set to -1 for indefinite repeating
  int repeatCount() default -1;

  boolean isConcurrencyAllowed() default false;

  String[] jobDataMap() default {};

}
