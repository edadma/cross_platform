package io.github.edadma.cross_platform

import java.io.{BufferedReader, InputStreamReader, OutputStreamWriter, PrintWriter}
import java.net.{StandardProtocolFamily, UnixDomainSocketAddress}
import java.nio.channels.{ServerSocketChannel, SocketChannel, Channels}
import java.nio.file.{Files, Paths}

// Uses Java NIO Unix domain sockets — available in Scala Native 0.5+ via javalib

def createSocketServer(path: String): SocketServer =
  val socketPath = Paths.get(path)
  Files.deleteIfExists(socketPath)
  val channel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
  channel.bind(UnixDomainSocketAddress.of(socketPath))

  new SocketServer:
    def accept(): SocketConnection =
      val client = channel.accept()
      new SocketConnection:
        private val in  = new BufferedReader(new InputStreamReader(Channels.newInputStream(client)))
        private val out = new PrintWriter(new OutputStreamWriter(Channels.newOutputStream(client)), true)
        def readLine(): Option[String] = Option(in.readLine())
        def writeLine(s: String): Unit = out.println(s)
        def close(): Unit = client.close()
    def close(): Unit =
      channel.close()
      Files.deleteIfExists(socketPath)

def connectSocket(path: String): SocketConnection =
  val channel = SocketChannel.open(UnixDomainSocketAddress.of(Paths.get(path)))
  new SocketConnection:
    private val in  = new BufferedReader(new InputStreamReader(Channels.newInputStream(channel)))
    private val out = new PrintWriter(new OutputStreamWriter(Channels.newOutputStream(channel)), true)
    def readLine(): Option[String] = Option(in.readLine())
    def writeLine(s: String): Unit = out.println(s)
    def close(): Unit = channel.close()
