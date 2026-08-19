package first.robot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RobotIdentityTest {
  @TempDir Path tempDir;

  @Test
  void knownMarkersResolve() {
    assertEquals(RobotIdentity.COMP_BOT, RobotIdentity.fromMarker("COMP_BOT"));
    assertEquals(RobotIdentity.PRACTICE_BOT, RobotIdentity.fromMarker("PRACTICE_BOT"));
  }

  @Test
  void markerToleratesWhitespaceAndCase() {
    assertEquals(RobotIdentity.COMP_BOT, RobotIdentity.fromMarker(" comp_bot\n"));
  }

  @Test
  void unknownMarkerIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> RobotIdentity.fromMarker("COMP"));
  }

  @Test
  void simIsNotAHardwareIdentity() {
    assertThrows(IllegalArgumentException.class, () -> RobotIdentity.fromMarker("SIM"));
  }

  @Test
  void offHardwareResolvesToSim() {
    assertEquals(RobotIdentity.SIM, RobotIdentity.resolve());
  }

  @Test
  void hardwareReadsTheMarkerFile() throws IOException {
    Path marker = tempDir.resolve("robot_id");
    Files.writeString(marker, "PRACTICE_BOT\n");
    assertEquals(RobotIdentity.PRACTICE_BOT, RobotIdentity.resolve(true, marker));
  }

  @Test
  void missingMarkerOnHardwareFallsBackToComp() {
    assertEquals(RobotIdentity.COMP_BOT, RobotIdentity.resolve(true, tempDir.resolve("robot_id")));
  }

  @Test
  void garbageMarkerOnHardwareFallsBackToComp() throws IOException {
    Path marker = tempDir.resolve("robot_id");
    Files.writeString(marker, "polaris");
    assertEquals(RobotIdentity.COMP_BOT, RobotIdentity.resolve(true, marker));
  }
}
