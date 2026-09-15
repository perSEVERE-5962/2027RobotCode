package first.robot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.wpilib.driverstation.Alert;

/** Which robot this code woke up on: comp bot, practice bot, or sim. */
public enum RobotIdentity {
  COMP_BOT,
  PRACTICE_BOT,
  SIM;

  // Marker file instead of a serial-number map so a controller swap keeps the right identity.
  private static final Path MARKER = Path.of("/home/systemcore/robot_id");

  /**
   * The mode is passed in from Constants, so this doesn't check for hardware on its own. REAL reads
   * the marker file. SIM and REPLAY return SIM. REPLAY returning SIM is temporary until issue #77
   * reads the robot's identity from the log.
   */
  public static RobotIdentity resolve(Constants.Mode mode) {
    return resolve(mode, MARKER);
  }

  // Split out so tests can pick the mode and point at their own marker files.
  static RobotIdentity resolve(Constants.Mode mode, Path marker) {
    return switch (mode) {
      case REAL -> readMarker(marker);
      case SIM, REPLAY -> SIM;
    };
  }

  /**
   * A missing or unreadable marker alerts and falls back to COMP_BOT rather than crash-looping at
   * an event.
   */
  private static RobotIdentity readMarker(Path marker) {
    try {
      return fromMarker(Files.readString(marker));
    } catch (Exception e) {
      new Alert(
              "Robot identity unknown, assuming COMP_BOT. Write COMP_BOT or PRACTICE_BOT to "
                  + marker
                  + " ("
                  + e.getMessage()
                  + ")",
              Alert.Level.HIGH)
          .set(true);
      return COMP_BOT;
    }
  }

  /** SIM gets rejected here on purpose: hardware claiming to be sim means the file is wrong. */
  static RobotIdentity fromMarker(String contents) {
    return switch (contents.trim().toUpperCase(Locale.ROOT)) {
      case "COMP_BOT" -> COMP_BOT;
      case "PRACTICE_BOT" -> PRACTICE_BOT;
      default -> throw new IllegalArgumentException("unrecognized robot_id: " + contents.trim());
    };
  }
}
