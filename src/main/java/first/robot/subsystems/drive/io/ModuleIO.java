package first.robot.subsystems.drive.io;

import org.littletonrobotics.junction.AutoLog;
import org.wpilib.math.geometry.Rotation2d;

public interface ModuleIO {
  @AutoLog
  public static class ModuleIOInputs {
    public boolean driveConnected = false;
    public double drivePositionRad = 0.0;
    public double driveVelocityRadPerSec = 0.0;
    public double driveAppliedVolts = 0.0;
    public double driveCurrentAmps = 0.0;

    public boolean turnConnected = false;
    public Rotation2d turnPosition = new Rotation2d();

    /**
     * Absolute angle in radians before the configured offset is subtracted. Offset capture has to
     * read this one, because turnPosition already has the offset removed, so a wrong offset hides
     * in it.
     */
    public double turnAbsoluteRad = 0.0;

    public double turnVelocityRadPerSec = 0.0;
    public double turnAppliedVolts = 0.0;
    public double turnCurrentAmps = 0.0;

    public double[] odometryTimestamps = new double[] {};
    public double[] odometryDrivePositionsRad = new double[] {};
    public Rotation2d[] odometryTurnPositions = new Rotation2d[] {};
  }

  default void updateInputs(ModuleIOInputs inputs) {}

  default void setDriveOpenLoop(double volts) {}

  default void setDriveVelocity(double velocityRadPerSec) {}

  default void setTurnPosition(Rotation2d rotation) {}
}
