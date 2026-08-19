package first.robot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.wpilib.driverstation.Alert;
import org.wpilib.framework.RobotBase;

/** Which robot this code woke up on: comp bot, practice bot, or sim. */
public enum RobotIdentity {
  COMP_BOT,
  PRACTICE_BOT,
  SIM;

  // Marker file instead of a serial-number map so a controller swap keeps the right identity.
  private static final Path MARKER = Path.of("/home/systemcore/robot_id");

  /**
   * Always SIM off hardware. On hardware the marker file decides. A missing or unreadable marker
   * alerts and falls back to COMP_BOT rather than crash-looping at an event.
   */
  public static RobotIdentity resolve() {
    return resolve(RobotBase.isReal(), MARKER);
  }

  // Split out so tests can fake being on hardware and point at their own marker files.
  static RobotIdentity resolve(boolean onHardware, Path marker) {
    if (!onHardware) {
      return SIM;
    }
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
