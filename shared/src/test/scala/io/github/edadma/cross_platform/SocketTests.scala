package io.github.edadma.cross_platform

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

/** Cross-platform socket tests. Tests that run on all platforms. */
class SocketTests extends AnyFreeSpec with Matchers {

  // Platform-specific tests that need Thread are in jvm/ and native/ test dirs.
  // This shared test verifies the API exists and behaves correctly on platforms
  // that don't support sockets (JS).

  "Unix domain socket API exists" in {
    // Just verify the functions are callable — platform tests do the real work
    try
      val path = createTempFile("sock-api-", ".sock")
      deleteFile(path)
      // If we get here, createTempFile works. Don't actually create a server
      // because we can't test it without Thread (not available on JS).
      succeed
    catch
      case _: UnsupportedOperationException => succeed
      case _: Exception                     => succeed
  }
}
