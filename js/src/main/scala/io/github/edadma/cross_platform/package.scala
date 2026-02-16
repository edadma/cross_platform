package io.github.edadma.cross_platform

import scala.scalajs.js
import scala.scalajs.js.DynamicImplicits.*
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.typedarray.Uint8Array

def processArgs(a: Seq[String]): IndexedSeq[String] =
  js.Dynamic.global.process.argv.asInstanceOf[js.Array[String]] drop 2 toIndexedSeq

private val process = js.Dynamic.global.process

def nameSeparator: String = NodePath.sep

def getCurrentDirectory: String = process.cwd().toString

def readFile(file: String): String = NodeFS.readFileSync(file, "utf8")

def writeFile(file: String, data: String): Unit = NodeFS.writeFileSync(file, data)

def appendFile(file: String, data: String): Unit = NodeFS.appendFileSync(file, data)

def readableFile(file: String): Boolean = {
  NodeFS.accessSync(file, NodeFS.constants.R_OK)
  true
}

def listFiles(directory: String): Seq[String] = {
  if (NodeFS.existsSync(directory)) {
    NodeFS.readdirSync(directory)
      .asInstanceOf[js.Array[String]]
      .map(file => NodePath.resolve(directory, file).toString)
      .sort()
      .toList
  } else {
    throw new IllegalArgumentException(s"$directory is not a directory or does not exist")
  }
}

def stdout(s: String): Unit = process.stdout.write(s)

def processExit(code: Int): Nothing =
  process.exit(code)
  throw new RuntimeException("Unreachable code after process.exit")

def exists(path: String): Boolean =
  NodeFS.existsSync(path)

def isFile(path: String): Boolean = {
  if (!NodeFS.existsSync(path)) false
  else {
    val stats = NodeFS.statSync(path)
    stats.isFile()
  }
}

def isDirectory(path: String): Boolean = {
  if (!NodeFS.existsSync(path)) false
  else {
    val stats = NodeFS.statSync(path)
    stats.isDirectory()
  }
}

def isSymbolicLink(path: String): Boolean = {
  if (!NodeFS.existsSync(path)) false
  else {
    val stats = NodeFS.lstatSync(path) // Use lstat to get symlink info
    stats.isSymbolicLink()
  }
}

def isReadable(path: String): Boolean = {
  NodeFS.accessSync(path, NodeFS.constants.R_OK)
  true
}

def isWritable(path: String): Boolean = {
  NodeFS.accessSync(path, NodeFS.constants.W_OK)
  true
}

def isExecutable(path: String): Boolean = {
  NodeFS.accessSync(path, NodeFS.constants.X_OK)
  true
}

def isSameFile(path1: String, path2: String): Boolean = {
  if (!exists(path1) || !exists(path2)) false
  else {
    val stats1 = NodeFS.statSync(path1)
    val stats2 = NodeFS.statSync(path2)

    // Compare device and inode numbers (Unix-like) or use other unique identifiers
    val dev1 = stats1.dev
    val ino1 = stats1.ino
    val dev2 = stats2.dev
    val ino2 = stats2.ino

    dev1 == dev2 && ino1 == ino2
  }
}

def readBytes(path: String): Array[Byte] = {
  // Read as Buffer and convert to Array manually
  val buffer = NodeFS.readFileSync(path)
  val length = buffer.asInstanceOf[js.Dynamic].length.asInstanceOf[Int]
  val array  = new Array[Byte](length)
  for (i <- 0 until length) {
    array(i) = buffer.asInstanceOf[js.Dynamic].selectDynamic(i.toString).asInstanceOf[Int].toByte
  }
  array
}

def writeBytes(path: String, data: Array[Byte]): Unit = {
  // Convert Array[Byte] to Node.js Buffer
  val jsArray = js.Array(data.map(_ & 0xff)*)
  val buffer  = NodeBuffer.from(jsArray)
  NodeFS.writeFileSync(path, buffer)
}

def listDirectoryWithTypes(path: String): Vector[DirectoryEntry] = {
  if (!NodeFS.existsSync(path)) {
    throw new IllegalArgumentException(s"Path does not exist: $path")
  }

  val files = NodeFS.readdirSync(path)
  files.toVector.map { name =>
    val fullPath = NodePath.join(path, name)
    val stats    = NodeFS.lstatSync(fullPath) // Use lstat to detect symlinks properly
    val fileType = if (stats.isSymbolicLink()) FileType.SymbolicLink
    else if (stats.isDirectory()) FileType.Directory
    else if (stats.isFile()) FileType.File
    else FileType.Other
    DirectoryEntry(name, fileType)
  }
}

def createDirectory(path: String): Unit =
  NodeFS.mkdirSync(path)

def createDirectories(path: String): Unit =
  NodeFS.mkdirSync(path, js.Dynamic.literal(recursive = true))

def deleteFile(path: String): Unit = {
  if (isDirectory(path)) {
    NodeFS.rmdirSync(path)
  } else {
    NodeFS.unlinkSync(path)
  }
}

def copyFile(source: String, target: String): Unit =
  NodeFS.copyFileSync(source, target)

def moveFile(source: String, target: String): Unit =
  NodeFS.renameSync(source, target)

def fileSize(path: String): Long = {
  val stats = NodeFS.statSync(path)
  stats.size.toLong
}

def lastModified(path: String): Long = {
  val stats = NodeFS.statSync(path)
  stats.mtime.getTime().toLong
}

def openRandomAccessFile(path: String, mode: String): RandomAccessFile =
  val jsFlags = mode match
    case "r" => "r"
    case _ =>
      if !NodeFS.existsSync(path) then NodeFS.writeFileSync(path, "")
      mode match
        case "rws" | "rwd" => "rs+"
        case _             => "r+"
  val fd = NodeFS.openSync(path, jsFlags)
  new RandomAccessFile:
    private var pointer: Long = 0

    def seek(pos: Long): Unit    = pointer = pos
    def getFilePointer: Long     = pointer
    def length: Long             = NodeFS.fstatSync(fd).size.toLong
    def setLength(newLength: Long): Unit = NodeFS.ftruncateSync(fd, newLength.toDouble)

    def read: Int =
      val buf = new Uint8Array(1)
      val n = NodeFS.readSync(fd, buf, 0, 1, pointer.toDouble)
      if n <= 0 then -1
      else
        pointer += 1
        buf(0) & 0xff

    def close(): Unit = NodeFS.closeSync(fd)

    def readFully(b: Array[Byte]): Unit = readFully(b, 0, b.length)

    def readFully(b: Array[Byte], off: Int, len: Int): Unit =
      val buf = new Uint8Array(len)
      var totalRead = 0
      while totalRead < len do
        val n = NodeFS.readSync(fd, buf, totalRead, len - totalRead, (pointer + totalRead).toDouble)
        if n <= 0 then throw new java.io.EOFException
        totalRead += n
      for i <- 0 until len do b(off + i) = buf(i).toByte
      pointer += len

    def skipBytes(n: Int): Int =
      val skipped = n.toLong.min(length - pointer).toInt
      pointer += skipped
      skipped

    def readBoolean(): Boolean = readByte() != 0
    def readByte(): Byte =
      val v = read
      if v < 0 then throw new java.io.EOFException
      v.toByte
    def readUnsignedByte(): Int =
      val v = read
      if v < 0 then throw new java.io.EOFException
      v

    def readShort(): Short =
      val b = new Array[Byte](2)
      readFully(b)
      (((b(0) & 0xff) << 8) | (b(1) & 0xff)).toShort

    def readUnsignedShort(): Int =
      val b = new Array[Byte](2)
      readFully(b)
      ((b(0) & 0xff) << 8) | (b(1) & 0xff)

    def readChar(): Char = readUnsignedShort().toChar

    def readInt(): Int =
      val b = new Array[Byte](4)
      readFully(b)
      ((b(0) & 0xff) << 24) | ((b(1) & 0xff) << 16) | ((b(2) & 0xff) << 8) | (b(3) & 0xff)

    def readLong(): Long =
      val b = new Array[Byte](8)
      readFully(b)
      ((b(0).toLong & 0xff) << 56) | ((b(1).toLong & 0xff) << 48) |
        ((b(2).toLong & 0xff) << 40) | ((b(3).toLong & 0xff) << 32) |
        ((b(4).toLong & 0xff) << 24) | ((b(5).toLong & 0xff) << 16) |
        ((b(6).toLong & 0xff) << 8) | (b(7).toLong & 0xff)

    def readFloat(): Float  = java.lang.Float.intBitsToFloat(readInt())
    def readDouble(): Double = java.lang.Double.longBitsToDouble(readLong())

    def readLine(): String =
      val sb = new StringBuilder
      var c = read
      if c == -1 then return null
      while c != -1 do
        if c == '\n' then return sb.toString
        if c == '\r' then
          val next = read
          if next != '\n' && next != -1 then pointer -= 1
          return sb.toString
        sb.append(c.toChar)
        c = read
      sb.toString

    def readUTF(): String = throw new UnsupportedOperationException("readUTF not supported")

    def write(b: Int): Unit =
      val buf = new Uint8Array(1)
      buf(0) = (b & 0xff).toShort
      NodeFS.writeSync(fd, buf, 0, 1, pointer.toDouble)
      pointer += 1

    def write(b: Array[Byte]): Unit = write(b, 0, b.length)

    def write(b: Array[Byte], off: Int, len: Int): Unit =
      val buf = new Uint8Array(len)
      for i <- 0 until len do buf(i) = (b(off + i) & 0xff).toShort
      var totalWritten = 0
      while totalWritten < len do
        val n = NodeFS.writeSync(fd, buf, totalWritten, len - totalWritten, (pointer + totalWritten).toDouble)
        totalWritten += n
      pointer += len

    def writeBoolean(v: Boolean): Unit = write(if v then 1 else 0)
    def writeByte(v: Int): Unit        = write(v)

    def writeShort(v: Int): Unit =
      write(Array(((v >> 8) & 0xff).toByte, (v & 0xff).toByte))

    def writeChar(v: Int): Unit = writeShort(v)

    def writeInt(v: Int): Unit =
      write(Array(
        ((v >> 24) & 0xff).toByte,
        ((v >> 16) & 0xff).toByte,
        ((v >> 8) & 0xff).toByte,
        (v & 0xff).toByte,
      ))

    def writeLong(v: Long): Unit =
      write(Array(
        ((v >> 56) & 0xff).toByte,
        ((v >> 48) & 0xff).toByte,
        ((v >> 40) & 0xff).toByte,
        ((v >> 32) & 0xff).toByte,
        ((v >> 24) & 0xff).toByte,
        ((v >> 16) & 0xff).toByte,
        ((v >> 8) & 0xff).toByte,
        (v & 0xff).toByte,
      ))

    def writeFloat(v: Float): Unit   = writeInt(java.lang.Float.floatToIntBits(v))
    def writeDouble(v: Double): Unit  = writeLong(java.lang.Double.doubleToLongBits(v))

    def writeBytes(s: String): Unit =
      for c <- s do write(c.toInt & 0xff)

    def writeChars(s: String): Unit =
      for c <- s do writeChar(c.toInt)

    def writeUTF(s: String): Unit = throw new UnsupportedOperationException("writeUTF not supported")

@js.native
@JSImport("fs", JSImport.Namespace)
object fs extends js.Object:
  def openSync(path: String, flags: String): Int = js.native
  def readSync(fd: Int, buffer: Uint8Array, offset: Int, length: Int, position: js.UndefOr[Int] = js.undefined): Int =
    js.native
  def closeSync(fd: Int): Unit = js.native

@js.native
@JSImport("util", "TextDecoder")
class TextDecoder(encoding: String = "utf-8", options: js.UndefOr[js.Object] = js.undefined) extends js.Object:
  def decode(input: js.UndefOr[Uint8Array] = js.undefined, options: js.UndefOr[js.Object] = js.undefined): String =
    js.native

def readLine(prompt: String = ""): String =
  // write prompt
  js.Dynamic.global.process.stdout.write(prompt)

  // open controlling terminal
  val isWin   = js.Dynamic.global.process.platform.asInstanceOf[String] == "win32"
  val ttyPath = if isWin then "CONIN$" else "/dev/tty"

  // Try to open TTY; if that fails (rare), fall back to fd=0 (stdin)
  val fd: Int =
    try fs.openSync(ttyPath, "rs")
    catch case _: Throwable => 0 // stdin

  val needClose = fd != 0 // don't close real stdin
  val decoder   = new TextDecoder("utf-8")
  val buf       = new Uint8Array(1024)
  val sb        = new StringBuilder

  var done = false
  while !done do
    val n = fs.readSync(fd, buf, 0, buf.length)
    if n <= 0 then
      // EOF before newline: finish whatever we decoded so far
      val tail = decoder.decode()
      if tail.nonEmpty then sb.append(tail)
      done = true
    else
      // scan for newline among the bytes we got
      var nlIdx = -1
      var i     = 0
      while i < n && nlIdx == -1 do
        if buf(i) == 0x0a then nlIdx = i // '\n'
        i += 1

      if nlIdx >= 0 then
        // Handle optional '\r' before '\n'
        val endExclusive = if nlIdx > 0 && buf(nlIdx - 1) == 0x0d then nlIdx - 1 else nlIdx
        val slice        = buf.subarray(0, endExclusive)
        // Final chunk: stream=false to flush decoder
        val part = decoder.decode(slice, js.Dynamic.literal(stream = false).asInstanceOf[js.Object])
        if part.nonEmpty then sb.append(part)
        done = true
      else
        // No newline yet: stream this chunk
        val slice = buf.subarray(0, n)
        val part  = decoder.decode(slice, js.Dynamic.literal(stream = true).asInstanceOf[js.Object])
        if part.nonEmpty then sb.append(part)

  if needClose then
    try fs.closeSync(fd)
    catch case _: Throwable => ()

  sb.result()
end readLine
