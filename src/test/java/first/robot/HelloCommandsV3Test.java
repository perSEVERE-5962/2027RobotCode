package first.robot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.wpilib.command3.Command;
import org.wpilib.command3.Scheduler;

/**
 * Proves the v3 coroutine machinery runs under our test JVM. Continuation needs opened access into
 * jdk.internal.vm, so if the add-opens flags in build.gradle ever disappear this dies in
 * ExceptionInInitializerError at class load instead of failing an assert.
 */
class HelloCommandsV3Test {
  // Own scheduler so the default instance never leaks state between tests.
  private final Scheduler scheduler = Scheduler.createIndependentScheduler();

  @Test
  void coroutineCommandRunsToCompletion() {
    var mechanism = new DummyMechanism("Hello", scheduler);
    var ticks = new ArrayList<Integer>();

    Command hello =
        mechanism
            .run(
                coroutine -> {
                  for (int i = 0; i < 3; i++) {
                    ticks.add(i);
                    coroutine.yield();
                  }
                })
            .named("Hello World");

    assertTrue(scheduler.schedule(hello).successful());

    // Bounded so a scheduler bug fails the asserts instead of hanging the test.
    for (int i = 0; i < 5 && scheduler.isScheduledOrRunning(hello); i++) {
      scheduler.run();
    }

    assertEquals(List.of(0, 1, 2), ticks);
    assertFalse(scheduler.isScheduledOrRunning(hello));
  }
}
