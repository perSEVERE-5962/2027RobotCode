package first.robot.subsystems.drive.io;

import org.littletonrobotics.junction.AutoLog;
import org.wpilib.math.geometry.Rotation2d;

public interface GyroIO {
  @AutoLog
  public static class GyroIOInputs {
    public boolean connected = false;
    public Rotation2d yawPosition = new Rotation2d();
    public double yawVelocityRadPerSec = 0.0;

    public double[] odometryYawTimestamps = new double[] {};
    public Rotation2d[] odometryYawPositions = new Rotation2d[] {};
  }

  default void updateInputs(GyroIOInputs inputs) {}

  // For a manual yaw zero or odometry reset when needed
  default void setYaw(Rotation2d yaw) {}
}
