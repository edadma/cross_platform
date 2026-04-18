package io.github.edadma.cross_platform

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

/** Socket tests — JS platform. Verifies stubs throw correctly. */
class SocketRoundtripTests extends AnyFreeSpec with Matchers {

  "Unix domain socket" - {
    "createSocketServer throws UnsupportedOperationException" in {
      assertThrows[UnsupportedOperationException] {
        createSocketServer("/tmp/test.sock")
      }
    }

    "connectSocket throws UnsupportedOperationException" in {
      assertThrows[UnsupportedOperationException] {
        connectSocket("/tmp/test.sock")
      }
    }
  }
}
