package first.robot.config;

import first.robot.RobotIdentity;
import java.util.List;
import java.util.OptionalInt;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Translation2d;
import org.wpilib.math.kinematics.SwerveDriveKinematics;

/**
 * All the numbers that change between robots. Built once from the identity and passed down by
 * constructor so nothing else has to check which robot it is on.
 *
 * <p>Most of these numbers are placeholders right now.
 */
public record RobotConfig(DriveConfig drive, SpeedCaps caps) {

  // encoderCanId is empty on R2 because the steer controller reads the absolute encoder itself.
  // The comp modules get their own CANcoders.
  public record ModuleConfig(
      String name,
      int busId,
      int driveCanId,
      int turnCanId,
      OptionalInt encoderCanId,
      Rotation2d absoluteEncoderOffset,
      boolean driveInverted,
      Translation2d location) {}

  public record DriveConfig(
      List<ModuleConfig> modules,
      int gyroBusId,
      int gyroCanId,
      double wheelRadiusMeters,
      double driveGearRatio,
      double turnGearRatio) {

    // Copy so nobody can change the modules after the kinematics are built from them.
    public DriveConfig {
      modules = List.copyOf(modules);
    }

    // Module order everywhere else comes from this list.
    public SwerveDriveKinematics kinematics() {
      return new SwerveDriveKinematics(
          modules.stream().map(ModuleConfig::location).toArray(Translation2d[]::new));
    }
  }

  // Provisional speed limits. Teleop leaves headroom for assists.
  public record SpeedCaps(
      double maxSpeedMetersPerSec, double teleopMetersPerSec, double maxAngularRadPerSec) {}

  // Sim uses the comp config so it tests the numbers the real robot will run.
  public static RobotConfig forIdentity(RobotIdentity identity) {
    return switch (identity) {
      case PRACTICE_BOT -> build(false);
      case COMP_BOT, SIM -> build(true);
    };
  }

  // TODO: all guesses until the chassis gets measured (#52). Gear ratios come from the module spec
  // once we know which modules we have (#81).
  private static final double HALF_TRACK_METERS = 0.3;
  private static final double WHEEL_RADIUS_METERS = 0.0508;
  private static final double DRIVE_GEAR_RATIO = 6.75;
  private static final double TURN_GEAR_RATIO = 12.8;

  // Each corner gets a decade. x0 is drive, x1 is steer, x2 is the encoder.
  private static final int FRONT_LEFT = 10;
  private static final int FRONT_RIGHT = 20;
  private static final int BACK_LEFT = 30;
  private static final int BACK_RIGHT = 40;

  // Swerve and its gyro get can_s2, the one bus with its own SPI lane. can_s0 is the PDH and
  // the robot level sensors.
  private static final int GYRO_CAN_ID = 50;
  private static final int BUS = 2;

  // The placeholder configs differ only in their encoder ids.
  private static RobotConfig build(boolean separateEncoder) {
    return new RobotConfig(
        new DriveConfig(
            List.of(
                corner("FrontLeft", FRONT_LEFT, separateEncoder, 1, 1),
                corner("FrontRight", FRONT_RIGHT, separateEncoder, 1, -1),
                corner("BackLeft", BACK_LEFT, separateEncoder, -1, 1),
                corner("BackRight", BACK_RIGHT, separateEncoder, -1, -1)),
            BUS,
            GYRO_CAN_ID,
            WHEEL_RADIUS_METERS,
            DRIVE_GEAR_RATIO,
            TURN_GEAR_RATIO),
        new SpeedCaps(4.0, 3.0, 3.0));
  }

  private static ModuleConfig corner(
      String name, int decade, boolean separateEncoder, int xSign, int ySign) {
    return new ModuleConfig(
        name,
        BUS,
        decade,
        decade + 1,
        separateEncoder ? OptionalInt.of(decade + 2) : OptionalInt.empty(),
        Rotation2d.kZero, // TODO: capture the real offsets (#49)
        false, // TODO: check which side runs backwards on the first drive (#50)
        new Translation2d(xSign * HALF_TRACK_METERS, ySign * HALF_TRACK_METERS));
  }
}
