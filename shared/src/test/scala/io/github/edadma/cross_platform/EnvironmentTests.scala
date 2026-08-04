package io.github.edadma.cross_platform

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

/** The environment and the two directories derived from it.
 *
 * These are asserted about *shape* rather than value, because the value is the machine's: a home is
 * whatever this user's is, and a cache root differs by platform on purpose. What can be pinned
 * without knowing the machine is that the answers are absolute paths, that an unset variable is
 * `None` rather than an empty string, and that the cache root sits under the home when it was derived
 * from it — which is the part a caller relies on when it appends its own name.
 */
class EnvironmentTests extends AnyFreeSpec with Matchers:

  /** A home that is nobody's, so an assertion below cannot pass by resembling this machine. */
  private val someHome = Some("/home/someone")

  /** The two answers the filesystem probe can give, as the thing `cacheRoot` actually takes. */
  private val keepsCachesInLibrary: String => Boolean = _ => true
  private val doesNot: String => Boolean              = _ => false

  "a variable the process was started with is readable" in {
    // `PATH` is the one variable every platform this builds for sets, including Node.
    envVar("PATH") shouldBe defined
    envVar("PATH").get should not be empty
  }

  "a variable that is not set is absent, rather than an empty string" in {
    // The distinction the whole function turns on: a caller chaining `.orElse` needs the absent case
    // to actually be absent, or it builds a path out of nothing and gets a plausible wrong answer.
    envVar("CROSS_PLATFORM_SURELY_NOBODY_HAS_THIS_SET") shouldBe None
  }

  "a home directory is found, and it is an absolute path" in {
    homeDirectory shouldBe defined

    val home = homeDirectory.get

    home should not be empty
    // Absolute on every platform this targets: a leading separator, or a Windows drive letter.
    withClue(s"home was '$home': ") {
      (home.startsWith("/") || home.matches("""^[A-Za-z]:[\\/].*""")) shouldBe true
    }
  }

  "the home directory is one that exists" in {
    isDirectory(homeDirectory.get) shouldBe true
  }

  "a cache directory is found, and it is an absolute path" in {
    cacheDirectory shouldBe defined

    val cache = cacheDirectory.get

    withClue(s"cache was '$cache': ") {
      (cache.startsWith("/") || cache.matches("""^[A-Za-z]:[\\/].*""")) shouldBe true
    }
  }

  "and where it was derived from the home rather than named outright, it sits under it" in {
    // Only checkable when no variable overrode the derivation — with `XDG_CACHE_HOME` or
    // `%LOCALAPPDATA%` set, the answer is deliberately somewhere else entirely, and asserting it sat
    // under the home would fail for a machine that is configured exactly as intended.
    assume(envVar("XDG_CACHE_HOME").isEmpty && envVar("LOCALAPPDATA").isEmpty,
           "the cache root was named outright rather than derived")

    cacheDirectory.get should startWith(homeDirectory.get)
  }

  /* Everything above is what this machine happens to answer, so between them those tests can only
   * ever exercise the one branch this machine falls into. The precedence itself is asked of
   * `cacheRoot`, which takes what it depends on rather than reading it — so every branch is reachable
   * from any machine, including the Windows one nobody here has. */

  "the order the cache root is chosen in" - {

    "an explicitly named XDG_CACHE_HOME wins over everything" in {
      // Including over macOS's own convention: somebody who set the variable has said where their
      // caches go, and deriving a path anyway would write somewhere the platform's housekeeping does
      // not know to look.
      cacheRoot(Some("/named"), Some("/local"), someHome, keepsCachesInLibrary)
        .shouldBe(Some("/named"))
    }

    "then LOCALAPPDATA, which is the Windows one" in {
      cacheRoot(None, Some("/Users/someone/AppData/Local"), someHome, doesNot)
        .shouldBe(Some("/Users/someone/AppData/Local"))
    }

    "a machine that keeps caches in Library uses that" in {
      cacheRoot(None, None, someHome, keepsCachesInLibrary)
        .shouldBe(Some("/home/someone/Library/Caches"))
    }

    "and one that does not falls back on the XDG default" in {
      cacheRoot(None, None, someHome, doesNot).shouldBe(Some("/home/someone/.cache"))
    }

    "with no home there is nowhere for it to be, and it says so" in {
      // Rather than answering a relative path, or `/.cache`, either of which a caller would happily
      // create somewhere nobody meant.
      cacheRoot(None, None, None, keepsCachesInLibrary).shouldBe(None)
    }

    "though a named root needs no home at all" in {
      cacheRoot(Some("/named"), None, None, keepsCachesInLibrary).shouldBe(Some("/named"))
    }
  }
