package first.robot.subsystems.drive.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.littletonrobotics.junction.LogTable;
import org.wpilib.math.geometry.Rotation2d;

class GyroIOInputsTest {

  @Test
  void inputsSurviveALogRoundTrip() {
    var inputs = new GyroIOInputsAutoLogged();
    inputs.connected = true;
    inputs.yawPosition = new Rotation2d(0.75);
    inputs.yawVelocityRadPerSec = -1.5;
    inputs.odometryYawTimestamps = new double[] {12.484, 12.488};
    inputs.odometryYawPositions = new Rotation2d[] {new Rotation2d(0.7), new Rotation2d(0.75)};

    var table = new LogTable(0);
    inputs.toLog(table);
    var copy = new GyroIOInputsAutoLogged();
    copy.fromLog(table);

    assertTrue(copy.connected);
    assertEquals(new Rotation2d(0.75), copy.yawPosition);
    assertEquals(-1.5, copy.yawVelocityRadPerSec);
    assertArrayEquals(new double[] {12.484, 12.488}, copy.odometryYawTimestamps);
    assertArrayEquals(
        new Rotation2d[] {new Rotation2d(0.7), new Rotation2d(0.75)}, copy.odometryYawPositions);
  }
}
