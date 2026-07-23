package io.github.edadma.cross_platform

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class ExecTests extends AnyFreeSpec with Matchers:

  "a successful command captures stdout and a zero exit code" in {
    val r = exec(Seq("echo", "hello"))

    r.exitCode shouldBe 0
    r.stdout.trim shouldBe "hello"
  }

  "a command's exit code is reported" in {
    // `false` exits non-zero on every supported platform.
    exec(Seq("false")).exitCode should not be 0
  }

  "a missing executable yields a non-zero exit code" in {
    // The exact code varies by platform — JVM/Node surface start-failure as -1, while a
    // posix_spawn platform lets the child exit 127 — but it is never a success.
    exec(Seq("this-command-does-not-exist-hopefully")).exitCode should not be 0
  }
