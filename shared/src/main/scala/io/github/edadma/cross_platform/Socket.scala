package io.github.edadma.cross_platform

/** A connection to a Unix domain socket. Used by both client and server sides. */
trait SocketConnection extends AutoCloseable:
  def readLine(): Option[String]
  def writeLine(s: String): Unit
  def close(): Unit

/** A Unix domain socket server that listens for connections. */
trait SocketServer extends AutoCloseable:
  def accept(): SocketConnection
  def close(): Unit
