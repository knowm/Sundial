package org.knowm.sundial;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class JobActionTest {

  @Test
  public void testCleanupCalledWhenDoRunThrows() {
    FailingJobAction action = new FailingJobAction();

    IllegalStateException thrown = assertThrows(IllegalStateException.class, action::run);

    assertSame(action.runFailure, thrown);
    assertTrue(action.cleanedUp);
  }

  private static class FailingJobAction extends JobAction {

    private final IllegalStateException runFailure = new IllegalStateException("run failed");
    private boolean cleanedUp;

    @Override
    public void doRun() {
      throw runFailure;
    }

    @Override
    public void cleanup() {
      cleanedUp = true;
    }
  }
}
