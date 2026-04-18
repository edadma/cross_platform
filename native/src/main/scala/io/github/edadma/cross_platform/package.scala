package io.github.edadma.cross_platform

import java.io.FileWriter
import java.nio.file.{Files, Paths, StandardCopyOption, StandardOpenOption}
import scala.Console.out
import scala.io.StdIn
import scala.jdk.CollectionConverters.*

import java.io.{RandomAccessFile => JRandomAccessFile}

import scalanative.unsafe.*
import scalanative.posix.unistd.{truncate => cTruncate}

def processArgs(a: Seq[String]): IndexedSeq[String] = a.toIndexedSeq

def nameSeparator: String = System.getProperty("file.separator")

def getCurrentDirectory: String = System.getProperty("user.dir")

def readFile(file: String): String = Files.readString(Paths.get(file))

def writeFile(file: String, data: String): Unit = {
  val f = new FileWriter(file)

  f.write(data)
  f.close()
}

def appendFile(file: String, data: String): Unit =
  Files.writeString(Paths.get(file), data, StandardOpenOption.CREATE, StandardOpenOption.APPEND)

def readableFile(file: String): Boolean = {
  val path = Paths.get(file)

  Files.isReadable(path) && Files.isRegularFile(path)
}

def listFiles(directory: String): Seq[String] = {
  val dirPath = Paths.get(directory)
  if (Files.isDirectory(dirPath)) {
    Files.list(dirPath)
      .iterator()
      .asScala
      .map(_.toAbsolutePath.normalize.toString)
      .toSeq
      .sorted
  } else {
    throw new IllegalArgumentException(s"$directory is not a directory or does not exist")
  }
}

def stdout(s: String): Unit = print(s)

def processExit(code: Int): Nothing = sys.exit(code)

def exists(path: String): Boolean =
  Files.exists(Paths.get(path))

def isFile(path: String): Boolean =
  Files.isRegularFile(Paths.get(path))

def isDirectory(path: String): Boolean =
  Files.isDirectory(Paths.get(path))

def isSymbolicLink(path: String): Boolean =
  Files.isSymbolicLink(Paths.get(path))

def isReadable(path: String): Boolean =
  Files.isReadable(Paths.get(path))

def isWritable(path: String): Boolean =
  Files.isWritable(Paths.get(path))

def isExecutable(path: String): Boolean =
  Files.isExecutable(Paths.get(path))

def isSameFile(path1: String, path2: String): Boolean = {
  val p1 = Paths.get(path1)
  val p2 = Paths.get(path2)
  if (Files.exists(p1) && Files.exists(p2)) {
    Files.isSameFile(p1, p2)
  } else {
    false
  }
}

def readBytes(path: String): Array[Byte] =
  Files.readAllBytes(Paths.get(path))

def writeBytes(path: String, data: Array[Byte]): Unit =
  Files.write(Paths.get(path), data)

def listDirectoryWithTypes(path: String): Vector[DirectoryEntry] = {
  val javaPath = Paths.get(path)
  if (!Files.isDirectory(javaPath)) {
    throw new IllegalArgumentException(s"Path is not a directory: $path")
  }

  Files.list(javaPath).iterator().asScala.toVector.map { entry =>
    val name     = entry.getFileName.toString
    val fileType = if (Files.isDirectory(entry)) FileType.Directory
    else if (Files.isSymbolicLink(entry)) FileType.SymbolicLink
    else if (Files.isRegularFile(entry)) FileType.File
    else FileType.Other
    DirectoryEntry(name, fileType)
  }
}

def createDirectory(path: String): Unit =
  Files.createDirectory(Paths.get(path))

def createDirectories(path: String): Unit =
  Files.createDirectories(Paths.get(path))

def deleteFile(path: String): Unit =
  Files.delete(Paths.get(path))

def copyFile(source: String, target: String): Unit =
  Files.copy(Paths.get(source), Paths.get(target), StandardCopyOption.REPLACE_EXISTING)

def moveFile(source: String, target: String): Unit =
  Files.move(Paths.get(source), Paths.get(target), StandardCopyOption.REPLACE_EXISTING)

def fileSize(path: String): Long =
  Files.size(Paths.get(path))

def lastModified(path: String): Long =
  Files.getLastModifiedTime(Paths.get(path)).toMillis

def createTempFile(prefix: String, suffix: String): String =
  val f = java.io.File.createTempFile(prefix, suffix)
  f.deleteOnExit()
  f.getPath

def openRandomAccessFile(path: String, mode: String): RandomAccessFile =
  val jraf = new JRandomAccessFile(path, mode)
  new RandomAccessFile:
    def seek(pos: Long): Unit                        = jraf.seek(pos)
    def getFilePointer: Long                         = jraf.getFilePointer
    def length: Long                                 = jraf.length()
    def setLength(newLength: Long): Unit =
      val currentLen = jraf.length()
      if newLength < currentLen then
        Zone { cTruncate(toCString(path), newLength.toSize) }
        if jraf.getFilePointer > newLength then jraf.seek(newLength)
      else if newLength > currentLen then
        val savedPos = jraf.getFilePointer
        jraf.seek(newLength - 1)
        jraf.write(0)
        jraf.seek(savedPos)
    def read: Int                                    = jraf.read()
    def fsync(): Unit                                = jraf.getFD.sync()
    def close(): Unit                                = jraf.close()
    def readFully(b: Array[Byte]): Unit              = jraf.readFully(b)
    def readFully(b: Array[Byte], off: Int, len: Int): Unit = jraf.readFully(b, off, len)
    def skipBytes(n: Int): Int                       = jraf.skipBytes(n)
    def readBoolean(): Boolean                       = jraf.readBoolean()
    def readByte(): Byte                             = jraf.readByte()
    def readUnsignedByte(): Int                      = jraf.readUnsignedByte()
    def readShort(): Short                           = jraf.readShort()
    def readUnsignedShort(): Int                     = jraf.readUnsignedShort()
    def readChar(): Char                             = jraf.readChar()
    def readInt(): Int                               = jraf.readInt()
    def readLong(): Long                             = jraf.readLong()
    def readFloat(): Float                           = jraf.readFloat()
    def readDouble(): Double                         = jraf.readDouble()
    def readLine(): String                           = jraf.readLine()
    def readUTF(): String                            = jraf.readUTF()
    def write(b: Int): Unit                          = jraf.write(b)
    def write(b: Array[Byte]): Unit                  = jraf.write(b)
    def write(b: Array[Byte], off: Int, len: Int): Unit = jraf.write(b, off, len)
    def writeBoolean(v: Boolean): Unit               = jraf.writeBoolean(v)
    def writeByte(v: Int): Unit                      = jraf.writeByte(v)
    def writeShort(v: Int): Unit                     = jraf.writeShort(v)
    def writeChar(v: Int): Unit                      = jraf.writeChar(v)
    def writeInt(v: Int): Unit                       = jraf.writeInt(v)
    def writeLong(v: Long): Unit                     = jraf.writeLong(v)
    def writeFloat(v: Float): Unit                   = jraf.writeFloat(v)
    def writeDouble(v: Double): Unit                 = jraf.writeDouble(v)
    def writeBytes(s: String): Unit                  = jraf.writeBytes(s)
    def writeChars(s: String): Unit                  = jraf.writeChars(s)
    def writeUTF(s: String): Unit                    = jraf.writeUTF(s)

def readLine(prompt: String = ""): String =
  print(prompt)
  out.flush()
  StdIn.readLine

// --- Unix Domain Sockets ---
// Uses POSIX C bindings since Scala Native may not support java.net.UnixDomainSocketAddress

import scalanative.posix.sys.socket.*
import scalanative.posix.sys.un.*
import scalanative.posix.unistd.{close => cClose, read => cRead, write => cWrite, unlink => cUnlink}
import scalanative.unsigned.*

def createSocketServer(path: String): SocketServer =
  val fd = socket(AF_UNIX, SOCK_STREAM, 0)
  if fd < 0 then throw new java.io.IOException("Failed to create socket")

  Zone { implicit z =>
    cUnlink(toCString(path)) // remove stale socket
    val addr = alloc[sockaddr_un]()
    addr._1 = AF_UNIX.toUShort
    val pathBytes = path.getBytes("UTF-8")
    val sunPath = addr.at2
    for i <- pathBytes.indices do !(sunPath + i) = pathBytes(i)
    !(sunPath + pathBytes.length) = 0.toByte

    if bind(fd, addr.asInstanceOf[Ptr[sockaddr]], sizeof[sockaddr_un].toUInt) < 0 then
      cClose(fd)
      throw new java.io.IOException(s"Failed to bind socket to $path")

    if listen(fd, 5) < 0 then
      cClose(fd)
      throw new java.io.IOException("Failed to listen on socket")
  }

  new SocketServer:
    def accept(): SocketConnection =
      val clientFd = scalanative.posix.sys.socket.accept(fd, null, null)
      if clientFd < 0 then throw new java.io.IOException("Failed to accept connection")
      fdToConnection(clientFd)
    def close(): Unit =
      cClose(fd)
      Zone { implicit z => cUnlink(toCString(path)) }

def connectSocket(path: String): SocketConnection =
  val fd = socket(AF_UNIX, SOCK_STREAM, 0)
  if fd < 0 then throw new java.io.IOException("Failed to create socket")

  Zone { implicit z =>
    val addr = alloc[sockaddr_un]()
    addr._1 = AF_UNIX.toUShort
    val pathBytes = path.getBytes("UTF-8")
    val sunPath = addr.at2
    for i <- pathBytes.indices do !(sunPath + i) = pathBytes(i)
    !(sunPath + pathBytes.length) = 0.toByte

    if connect(fd, addr.asInstanceOf[Ptr[sockaddr]], sizeof[sockaddr_un].toUInt) < 0 then
      cClose(fd)
      throw new java.io.IOException(s"Failed to connect to $path")
  }

  fdToConnection(fd)

private def fdToConnection(fd: Int): SocketConnection =
  new SocketConnection:
    private val buf = new Array[Byte](8192)
    private var bufPos = 0
    private var bufLen = 0

    def readLine(): Option[String] =
      val sb = new StringBuilder
      var done = false
      while !done do
        if bufPos >= bufLen then
          Zone { implicit z =>
            val cbuf = alloc[Byte](8192)
            val n = cRead(fd, cbuf, 8192.toULong).toInt
            if n <= 0 then
              done = true
            else
              for i <- 0 until n do buf(i) = !(cbuf + i)
              bufPos = 0
              bufLen = n
          }
        if !done && bufPos < bufLen then
          val b = buf(bufPos)
          bufPos += 1
          if b == '\n' then done = true
          else if b != '\r' then sb.append(b.toChar)
      if sb.isEmpty && bufPos >= bufLen then None
      else Some(sb.result())

    def writeLine(s: String): Unit =
      val bytes = (s + "\n").getBytes("UTF-8")
      Zone { implicit z =>
        val cbuf = alloc[Byte](bytes.length)
        for i <- bytes.indices do !(cbuf + i) = bytes(i)
        cWrite(fd, cbuf, bytes.length.toULong)
      }

    def close(): Unit = cClose(fd)
