package first.robot.subsystems.drive.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.littletonrobotics.junction.LogTable;
import org.wpilib.math.geometry.Rotation2d;

/**
 * Round-trips the inputs through a LogTable and checks every field survives. Compiling against
 * ModuleIOInputsAutoLogged is what proves the annotation processor ran; the round trip proves
 * replay reads back exactly what logging wrote.
 */
class ModuleIOInputsTest {
  @Test
  void inputsSurviveALogRoundTrip() {
    var inputs = new ModuleIOInputsAutoLogged();
    inputs.driveConnected = true;
    inputs.drivePositionRad = 1.25;
    inputs.driveVelocityRadPerSec = -2.5;
    inputs.driveAppliedVolts = 6.0;
    inputs.driveCurrentAmps = 12.5;
    inputs.turnConnected = true;
    inputs.turnPosition = new Rotation2d(0.5);
    inputs.turnAbsoluteRad = 3.0;
    inputs.turnVelocityRadPerSec = 0.25;
    inputs.turnAppliedVolts = -3.0;
    inputs.turnCurrentAmps = 4.5;
    inputs.odometryTimestamps = new double[] {1.0, 2.0};
    inputs.odometryDrivePositionsRad = new double[] {0.1, 0.2};
    inputs.odometryTurnPositions = new Rotation2d[] {new Rotation2d(0.1), new Rotation2d(0.2)};

    var table = new LogTable(0);
    inputs.toLog(table);
    var copy = new ModuleIOInputsAutoLogged();
    copy.fromLog(table);

    assertTrue(copy.driveConnected);
    assertEquals(1.25, copy.drivePositionRad);
    assertEquals(-2.5, copy.driveVelocityRadPerSec);
    assertEquals(6.0, copy.driveAppliedVolts);
    assertEquals(12.5, copy.driveCurrentAmps);
    assertTrue(copy.turnConnected);
    assertEquals(new Rotation2d(0.5), copy.turnPosition);
    assertEquals(3.0, copy.turnAbsoluteRad);
    assertEquals(0.25, copy.turnVelocityRadPerSec);
    assertEquals(-3.0, copy.turnAppliedVolts);
    assertEquals(4.5, copy.turnCurrentAmps);
    assertArrayEquals(new double[] {1.0, 2.0}, copy.odometryTimestamps);
    assertArrayEquals(new double[] {0.1, 0.2}, copy.odometryDrivePositionsRad);
    assertArrayEquals(
        new Rotation2d[] {new Rotation2d(0.1), new Rotation2d(0.2)}, copy.odometryTurnPositions);
  }
}
