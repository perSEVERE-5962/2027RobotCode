package first.robot.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import first.robot.safety.DriveRequest.Source;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DriveRequestTest {
  @Test
  void stopIsZeroFromNobody() {
    var stop = DriveRequest.stop();
    assertEquals(0, stop.vxMetersPerSec());
    assertEquals(0, stop.vyMetersPerSec());
    assertEquals(0, stop.omegaRadPerSec());
    assertEquals(Source.NONE, stop.source());
    assertTrue(stop.isStop());
  }

  @ParameterizedTest
  @EnumSource(Source.class)
  void zeroVelocityIsAStopFromAnySource(Source source) {
    assertTrue(new DriveRequest(0, 0, 0, false, source).isStop());
  }

  @Test
  void movingRequestIsNotAStop() {
    assertFalse(new DriveRequest(1.0, 0, 0, true, Source.TELEOP).isStop());
    assertFalse(new DriveRequest(0, 0.5, 0, false, Source.AUTO).isStop());
    assertFalse(new DriveRequest(0, 0, 2.0, false, Source.UTILITY).isStop());
  }

  @Test
  void noneIsAStopEvenWithVelocity() {
    assertTrue(new DriveRequest(1.0, 1.0, 1.0, false, Source.NONE).isStop());
  }

  @Test
  void sourceIsRequired() {
    assertThrows(NullPointerException.class, () -> new DriveRequest(0, 0, 0, false, null));
  }
}
