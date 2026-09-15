package first.robot.safety;

import java.util.Objects;

/** How fast to drive and where the request came from. */
public record DriveRequest(
    double vxMetersPerSec,
    double vyMetersPerSec,
    double omegaRadPerSec,
    boolean fieldRelative,
    Source source) {

  public enum Source {
    NONE,
    TELEOP,
    AUTO,
    UTILITY
  }

  // Reject NaN and infinity. NONE requests must have zero speeds.
  public DriveRequest {
    Objects.requireNonNull(source, "source");
    requireFinite(vxMetersPerSec, "vx");
    requireFinite(vyMetersPerSec, "vy");
    requireFinite(omegaRadPerSec, "omega");
    if (source == Source.NONE
        && !(vxMetersPerSec == 0 && vyMetersPerSec == 0 && omegaRadPerSec == 0)) {
      throw new IllegalArgumentException("a NONE request must be a stop");
    }
  }

  public static DriveRequest stop() {
    return new DriveRequest(0, 0, 0, false, Source.NONE);
  }

  public boolean isStop() {
    return vxMetersPerSec == 0 && vyMetersPerSec == 0 && omegaRadPerSec == 0;
  }

  private static void requireFinite(double value, String name) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(name + " is " + value);
    }
  }
}
