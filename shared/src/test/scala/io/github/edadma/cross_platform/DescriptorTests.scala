package io.github.edadma.cross_platform

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

/** Every call that opens a file or a directory gives the descriptor back before it returns.
 *
 * `listFiles` and `listDirectoryWithTypes` read a directory through `Files.list`, whose stream holds
 * an open handle until it is closed — and the garbage collector never closes one. So a program that
 * lists thousands of directories in one process, as a compiler walking its source trees does, ran
 * out of descriptors and saw every later file operation fail with *"Too many open files"*.
 *
 * The count is read off `/dev/fd`, which lists this process's open descriptors on macOS and Linux.
 * Other suites open and close files beside this one, so the bound is loose on purpose: a leak of one
 * descriptor per call is thousands over it, and that noise is not.
 */
class DescriptorTests extends AnyFreeSpec with Matchers:

  private val calls = 3000

  private def openDescriptors: Int = listFiles("/dev/fd").length

  /** Asserts that `calls` repetitions of `body` leave the descriptor count where it was. */
  private def givesBack(body: String => Unit): Unit =
    assume(isDirectory("/dev/fd"), "this machine has no /dev/fd to count descriptors with")

    val dir = createTempDirectory("cross-platform-fd-")

    writeFile(s"$dir/a", "")
    createDirectory(s"$dir/b")

    val before = openDescriptors

    for _ <- 1 to calls do body(dir)

    (openDescriptors - before) should be < (calls / 3)

  "listing a directory" - {
    "answers its entries as absolute paths, sorted" in {
      val dir = createTempDirectory("cross-platform-fd-")

      writeFile(s"$dir/b", "")
      writeFile(s"$dir/a", "")
      createDirectory(s"$dir/c")

      listFiles(dir).map(_.split('/').last) shouldBe Seq("a", "b", "c")
      listFiles(dir).forall(_.startsWith("/")) shouldBe true
    }

    "with types names each entry's kind" in {
      val dir = createTempDirectory("cross-platform-fd-")

      writeFile(s"$dir/f", "")
      createDirectory(s"$dir/d")

      listDirectoryWithTypes(dir).sortBy(_.name) shouldBe Vector(
        DirectoryEntry("d", FileType.Directory),
        DirectoryEntry("f", FileType.File),
      )
    }

    "refuses something that is not a directory" in {
      val dir = createTempDirectory("cross-platform-fd-")

      writeFile(s"$dir/f", "")
      an[IllegalArgumentException] should be thrownBy listFiles(s"$dir/f")
      an[IllegalArgumentException] should be thrownBy listDirectoryWithTypes(s"$dir/f")
    }
  }

  "giving descriptors back, however many times it is asked" - {
    "listFiles" in givesBack(dir => listFiles(dir))

    "listDirectoryWithTypes" in givesBack(dir => listDirectoryWithTypes(dir))

    "writeFile and readFile" in givesBack { dir =>
      writeFile(s"$dir/a", "x")
      readFile(s"$dir/a")
    }
  }
