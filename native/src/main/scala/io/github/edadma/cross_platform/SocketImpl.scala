package io.github.edadma.cross_platform

import scalanative.unsafe.*
import scalanative.unsigned.*
import scalanative.posix.sys.socket.*
import scalanative.posix.sys.un.*
import scalanative.posix.unistd.{close => cClose, read => cRead, write => cWrite, unlink => cUnlink}
import scalanative.posix.string.strncpy

def createSocketServer(path: String): SocketServer =
  val fd = socket(AF_UNIX, SOCK_STREAM, 0)
  if fd < 0 then throw new java.io.IOException("Failed to create socket")

  Zone {
    cUnlink(toCString(path))
    val addr = alloc[sockaddr_un]()
    addr._1 = AF_UNIX.toUShort
    strncpy(addr._2.at(0), toCString(path), 107.toCSize)

    if bind(fd, addr.asInstanceOf[Ptr[sockaddr]], sizeof[sockaddr_un].toUInt) < 0 then
      cClose(fd)
      throw new java.io.IOException(s"Failed to bind socket to $path")

    if listen(fd, 5) < 0 then
      cClose(fd)
      throw new java.io.IOException("Failed to listen on socket")
  }

  val savedPath = path
  new SocketServer:
    def accept(): SocketConnection =
      val clientFd = scalanative.posix.sys.socket.accept(fd, null, null)
      if clientFd < 0 then throw new java.io.IOException("Failed to accept connection")
      fdToConnection(clientFd)
    def close(): Unit =
      cClose(fd)
      Zone { cUnlink(toCString(savedPath)) }

def connectSocket(path: String): SocketConnection =
  val fd = socket(AF_UNIX, SOCK_STREAM, 0)
  if fd < 0 then throw new java.io.IOException("Failed to create socket")

  Zone {
    val addr = alloc[sockaddr_un]()
    addr._1 = AF_UNIX.toUShort
    strncpy(addr._2.at(0), toCString(path), 107.toCSize)

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
      var eof = false
      while !done do
        if bufPos >= bufLen then
          val cbuf = stackalloc[Byte](8192)
          val n = cRead(fd, cbuf, 8192.toCSize).toInt
          if n <= 0 then
            done = true
            eof = n < 0 || sb.isEmpty
          else
            var i = 0
            while i < n do
              buf(i) = cbuf(i)
              i += 1
            bufPos = 0
            bufLen = n
        if !done && bufPos < bufLen then
          val b = buf(bufPos)
          bufPos += 1
          if b == '\n' then done = true
          else if b != '\r' then sb.append(b.toChar)
      if eof then None
      else Some(sb.result())

    def writeLine(s: String): Unit =
      val bytes = (s + "\n").getBytes("UTF-8")
      val cbuf = stackalloc[Byte](bytes.length)
      var i = 0
      while i < bytes.length do
        cbuf(i) = bytes(i)
        i += 1
      cWrite(fd, cbuf, bytes.length.toCSize)

    def close(): Unit = cClose(fd)
