package io.github.edadma.cross_platform

import java.io.File

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class RandomAccessFileTests extends AnyFreeSpec with Matchers:

  def withTempFile(test: String => Unit): Unit =
    val f = File.createTempFile("raf-test", ".dat")
    f.deleteOnExit()
    try test(f.getPath)
    finally f.delete()

  "single byte read/write" in withTempFile { path =>
    val raf = openRandomAccessFile(path, "rw")
    raf.write(42)
    raf.seek(0)
    raf.read shouldBe 42
    raf.close()
  }

  "short read/write" in withTempFile { path =>
    val raf = openRandomAccessFile(path, "rw")
    raf.writeShort(12345)
    raf.seek(0)
    raf.readShort() shouldBe 12345
    raf.close()
  }

  "int read/write" in withTempFile { path =>
    val raf = openRandomAccessFile(path, "rw")
    raf.writeInt(0x12345678)
    raf.seek(0)
    raf.readInt() shouldBe 0x12345678
    raf.close()
  }

  "long read/write" in withTempFile { path =>
    val raf = openRandomAccessFile(path, "rw")
    raf.writeLong(0x123456789abcdef0L)
    raf.seek(0)
    raf.readLong() shouldBe 0x123456789abcdef0L
    raf.close()
  }

  "double read/write" in withTempFile { path =>
    val raf = openRandomAccessFile(path, "rw")
    raf.writeDouble(3.14159265358979)
    raf.seek(0)
    raf.readDouble() shouldBe 3.14159265358979
    raf.close()
  }

  "seek and getFilePointer" in withTempFile { path =>
    val raf = openRandomAccessFile(path, "rw")
    raf.writeLong(100L)
    raf.writeLong(200L)
    raf.getFilePointer shouldBe 16
    raf.seek(8)
    raf.getFilePointer shouldBe 8
    raf.readLong() shouldBe 200L
    raf.seek(0)
    raf.readLong() shouldBe 100L
    raf.close()
  }

  "length and setLength" in withTempFile { path =>
    val raf = openRandomAccessFile(path, "rw")
    raf.length shouldBe 0
    raf.writeLong(42L)
    raf.length shouldBe 8
    raf.setLength(16)
    raf.length shouldBe 16
    raf.setLength(4)
    raf.length shouldBe 4
    raf.close()
  }

  "readFully" in withTempFile { path =>
    val raf = openRandomAccessFile(path, "rw")
    val data = Array[Byte](1, 2, 3, 4, 5, 6, 7, 8)
    raf.write(data)
    raf.seek(0)
    val buf = new Array[Byte](8)
    raf.readFully(buf)
    buf shouldBe data
    raf.close()
  }

  "writeBytes and readFully" in withTempFile { path =>
    val raf = openRandomAccessFile(path, "rw")
    raf.writeBytes("B+ Tree v1  ")
    raf.seek(0)
    val header = new Array[Byte](12)
    raf.readFully(header)
    new String(header) shouldBe "B+ Tree v1  "
    raf.close()
  }
