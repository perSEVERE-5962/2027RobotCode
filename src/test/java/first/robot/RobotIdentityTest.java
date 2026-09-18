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

  /** Makes a fake marker file that says PRACTICE_BOT, so tests can check if it gets read. */
  private Path practiceBotMarker() throws IOException {
    Path marker = tempDir.resolve("robot_id");
    Files.writeString(marker, "PRACTICE_BOT\n");
    return marker;
  }

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
  void realReadsTheMarkerFile() throws IOException {
    assertEquals(
        RobotIdentity.PRACTICE_BOT,
        RobotIdentity.resolve(Constants.Mode.REAL, practiceBotMarker()));
  }

  @Test
  void missingMarkerInRealFallsBackToComp() {
    assertEquals(
        RobotIdentity.COMP_BOT,
        RobotIdentity.resolve(Constants.Mode.REAL, tempDir.resolve("robot_id")));
  }

  @Test
  void garbageMarkerInRealFallsBackToComp() throws IOException {
    Path marker = tempDir.resolve("robot_id");
    Files.writeString(marker, "polaris");
    assertEquals(RobotIdentity.COMP_BOT, RobotIdentity.resolve(Constants.Mode.REAL, marker));
  }

  /** SIM returns SIM, even when there is a marker file it could read. */
  @Test
  void simReturnsSimWithoutReadingTheMarker() throws IOException {
    assertEquals(RobotIdentity.SIM, RobotIdentity.resolve(Constants.Mode.SIM, practiceBotMarker()));
  }

  /** REPLAY returning SIM is temporary until issue #77 reads the robot's identity from the log. */
  @Test
  void replayReturnsSimUntilIssue77() throws IOException {
    assertEquals(
        RobotIdentity.SIM, RobotIdentity.resolve(Constants.Mode.REPLAY, practiceBotMarker()));
  }
}
