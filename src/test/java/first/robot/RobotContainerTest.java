package first.robot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.hardware.hal.HAL;

class RobotContainerTest {
  @BeforeAll
  static void initHal() {
    assertTrue(HAL.initialize(500, 0));
  }

  @Test
  void aThrowingCommandDoesNotStopTheLoop() {
    var container = new RobotContainer();
    var scheduler = container.scheduler();
    var ticks = new ArrayList<Integer>();

    Command victim =
        new Mechanism("A", scheduler).runRepeatedly(() -> ticks.add(1)).named("Victim");
    Command thrower =
        new Mechanism("B", scheduler)
            .run(
                coroutine -> {
                  coroutine.yield();
                  throw new IllegalStateException("boom");
                })
            .named("Thrower");

    scheduler.schedule(victim);
    scheduler.schedule(thrower);

    // First cycle runs both, second cycle is the throw.
    container.periodic();
    container.periodic();
    int ticksAtTheThrow = ticks.size();

    for (int i = 0; i < 3; i++) {
      container.periodic();
    }

    assertEquals(1, container.schedulerFaults());
    assertEquals("IllegalStateException: boom", container.lastSchedulerFault());
    assertFalse(scheduler.isScheduledOrRunning(thrower));
    assertTrue(scheduler.isScheduledOrRunning(victim));
    assertTrue(ticks.size() > ticksAtTheThrow, "victim should keep running after the throw");
  }

  @Test
  void aCleanLoopCountsNothing() {
    var container = new RobotContainer();
    var scheduler = container.scheduler();
    var ticks = new ArrayList<Integer>();

    scheduler.schedule(
        new Mechanism("A", scheduler).runRepeatedly(() -> ticks.add(1)).named("Fine"));
    for (int i = 0; i < 3; i++) {
      container.periodic();
    }

    assertEquals(3, ticks.size());
    assertEquals(0, container.schedulerFaults());
    assertEquals("", container.lastSchedulerFault());
  }

  @Test
  void anErrorStillEscapes() {
    var container = new RobotContainer();
    var scheduler = container.scheduler();

    scheduler.schedule(
        new Mechanism("A", scheduler)
            .run(
                coroutine -> {
                  coroutine.yield();
                  throw new AssertionError("fatal");
                })
            .named("Fatal"));

    container.periodic();
    assertThrows(AssertionError.class, container::periodic);
    assertEquals(0, container.schedulerFaults());
  }
}
