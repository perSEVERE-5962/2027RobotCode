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

  public DriveRequest {
    Objects.requireNonNull(source, "source");
  }

  public static DriveRequest stop() {
    return new DriveRequest(0, 0, 0, false, Source.NONE);
  }

  // NONE is a stop no matter what the numbers say.
  public boolean isStop() {
    return source == Source.NONE
        || (vxMetersPerSec == 0 && vyMetersPerSec == 0 && omegaRadPerSec == 0);
  }
}
