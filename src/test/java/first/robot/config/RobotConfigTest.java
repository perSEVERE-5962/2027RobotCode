package first.robot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import first.robot.RobotIdentity;
import first.robot.config.RobotConfig.DriveConfig;
import first.robot.config.RobotConfig.ModuleConfig;
import first.robot.config.RobotConfig.SpeedCaps;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Translation2d;
import org.wpilib.math.kinematics.ChassisVelocities;

class RobotConfigTest {
  private static final double EPSILON = 1e-9;

  @Test
  void compConfigIsPinned() {
    var config = RobotConfig.forIdentity(RobotIdentity.COMP_BOT);
    var drive = config.drive();

    assertEquals(4, drive.modules().size());
    assertCorner(drive.modules().get(0), "FrontLeft", 10, 11, OptionalInt.of(12), 0.3, 0.3);
    assertCorner(drive.modules().get(1), "FrontRight", 20, 21, OptionalInt.of(22), 0.3, -0.3);
    assertCorner(drive.modules().get(2), "BackLeft", 30, 31, OptionalInt.of(32), -0.3, 0.3);
    assertCorner(drive.modules().get(3), "BackRight", 40, 41, OptionalInt.of(42), -0.3, -0.3);
    assertEquals(0, drive.gyroBusId());
    assertEquals(50, drive.gyroCanId());
    assertEquals(0.0508, drive.wheelRadiusMeters());
    assertEquals(6.75, drive.driveGearRatio());
    assertEquals(12.8, drive.turnGearRatio());
    assertEquals(new SpeedCaps(4.0, 3.0, 3.0), config.caps());
  }

  @Test
  void practiceConfigIsPinned() {
    var config = RobotConfig.forIdentity(RobotIdentity.PRACTICE_BOT);
    var drive = config.drive();

    assertEquals(4, drive.modules().size());
    assertCorner(drive.modules().get(0), "FrontLeft", 10, 11, OptionalInt.empty(), 0.3, 0.3);
    assertCorner(drive.modules().get(1), "FrontRight", 20, 21, OptionalInt.empty(), 0.3, -0.3);
    assertCorner(drive.modules().get(2), "BackLeft", 30, 31, OptionalInt.empty(), -0.3, 0.3);
    assertCorner(drive.modules().get(3), "BackRight", 40, 41, OptionalInt.empty(), -0.3, -0.3);
    assertEquals(0, drive.gyroBusId());
    assertEquals(50, drive.gyroCanId());
    assertEquals(0.0508, drive.wheelRadiusMeters());
    assertEquals(6.75, drive.driveGearRatio());
    assertEquals(12.8, drive.turnGearRatio());
    assertEquals(new SpeedCaps(4.0, 3.0, 3.0), config.caps());
  }

  @Test
  void simUsesTheCompConfig() {
    assertEquals(
        RobotConfig.forIdentity(RobotIdentity.COMP_BOT),
        RobotConfig.forIdentity(RobotIdentity.SIM));
  }

  @Test
  void modulesCannotBeChangedAfterConstruction() {
    var modules =
        new ArrayList<>(RobotConfig.forIdentity(RobotIdentity.COMP_BOT).drive().modules());
    var drive = new DriveConfig(modules, 0, 50, 0.0508, 6.75, 12.8);

    modules.remove(0);

    assertEquals(4, drive.modules().size());
  }

  @ParameterizedTest
  @EnumSource(RobotIdentity.class)
  void noDuplicateOrUnsetIds(RobotIdentity identity) {
    var drive = RobotConfig.forIdentity(identity).drive();
    Set<String> seen = new HashSet<>();

    for (var module : drive.modules()) {
      addId(seen, module.busId(), module.driveCanId());
      addId(seen, module.busId(), module.turnCanId());
      module.encoderCanId().ifPresent(id -> addId(seen, module.busId(), id));
    }
    addId(seen, drive.gyroBusId(), drive.gyroCanId());
  }

  @ParameterizedTest
  @EnumSource(RobotIdentity.class)
  void drivingForwardPointsEveryWheelForward(RobotIdentity identity) {
    var kinematics = RobotConfig.forIdentity(identity).drive().kinematics();

    var states = kinematics.toSwerveModuleVelocities(new ChassisVelocities(1.5, 0, 0));

    assertEquals(4, states.length);
    for (var state : states) {
      assertEquals(1.5, state.velocity, EPSILON);
      assertEquals(0.0, state.angle.getRadians(), EPSILON);
    }
  }

  // Equal speeds mean all four corners are the same distance from the center.
  @ParameterizedTest
  @EnumSource(RobotIdentity.class)
  void spinningGivesFourEqualSpeeds(RobotIdentity identity) {
    var kinematics = RobotConfig.forIdentity(identity).drive().kinematics();

    var states = kinematics.toSwerveModuleVelocities(new ChassisVelocities(0, 0, 1.0));

    Set<Double> angles = new HashSet<>();
    for (var state : states) {
      assertEquals(states[0].velocity, state.velocity, EPSILON);
      angles.add(state.angle.getRadians());
    }
    assertEquals(4, angles.size(), "a spin should point all four corners differently");
  }

  @ParameterizedTest
  @EnumSource(RobotIdentity.class)
  void teleopCapIsBelowMax(RobotIdentity identity) {
    var caps = RobotConfig.forIdentity(identity).caps();
    assertTrue(caps.maxSpeedMetersPerSec() > 0);
    assertTrue(caps.maxAngularRadPerSec() > 0);
    assertTrue(caps.teleopMetersPerSec() > 0);
    assertTrue(caps.teleopMetersPerSec() < caps.maxSpeedMetersPerSec());
  }

  private static void assertCorner(
      ModuleConfig module,
      String name,
      int driveCanId,
      int turnCanId,
      OptionalInt encoderCanId,
      double x,
      double y) {
    assertEquals(name, module.name());
    assertEquals(0, module.busId(), name);
    assertEquals(driveCanId, module.driveCanId(), name);
    assertEquals(turnCanId, module.turnCanId(), name);
    assertEquals(encoderCanId, module.encoderCanId(), name);
    assertEquals(new Translation2d(x, y), module.location(), name);
    assertEquals(Rotation2d.kZero, module.absoluteEncoderOffset(), name);
    assertFalse(module.driveInverted(), name);
  }

  private static void addId(Set<String> seen, int busId, int canId) {
    assertNotEquals(0, canId, "id 0 means nobody set it");
    assertTrue(seen.add(busId + ":" + canId), "duplicate device at can_s" + busId + " id " + canId);
  }
}
