package org.quartz.core;

/**
 * @author lorban
 */
public class TriggerFiredResult {

  private TriggerFiredBundle triggerFiredBundle;

  private Exception exception;

  public TriggerFiredResult(TriggerFiredBundle triggerFiredBundle) {

    this.triggerFiredBundle = triggerFiredBundle;
  }

  public TriggerFiredBundle getTriggerFiredBundle() {

    return triggerFiredBundle;
  }

  public Exception getException() {

    return exception;
  }
}
