package first.robot;

import org.wpilib.command3.Scheduler;

/** Owns the robot's scheduler. */
public class RobotContainer {
  // Our own scheduler, not Scheduler.getDefault(). Nothing in this repo runs the default one, so
  // anything that binds to it (RobotModeTriggers does) would never fire.
  private final Scheduler scheduler = Scheduler.createIndependentScheduler();

  private int schedulerFaults;
  private String lastSchedulerFault = "";

  public void periodic() {
    // v3 removes a failed command and rethrows its RuntimeException. Catch it so robotPeriodic()
    // can finish. Errors skip that cleanup, so they are left to escape.
    try {
      scheduler.run();
    } catch (RuntimeException e) {
      schedulerFaults++;
      lastSchedulerFault = e.getClass().getSimpleName() + ": " + e.getMessage();
      if (schedulerFaults == 1) {
        // Once per boot. A command that throws every loop would flood the console otherwise.
        e.printStackTrace();
      }
    }
  }

  Scheduler scheduler() {
    return scheduler;
  }

  public int schedulerFaults() {
    return schedulerFaults;
  }

  public String lastSchedulerFault() {
    return lastSchedulerFault;
  }
}
