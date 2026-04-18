package io.github.edadma.cross_platform

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

/** Socket roundtrip tests — JVM platform. */
class SocketRoundtripTests extends AnyFreeSpec with Matchers {

  private def withSocketPair(test: (SocketServer, String) => Unit): Unit =
    val path = createTempFile("test-sock-", ".sock")
    deleteFile(path)
    val server = createSocketServer(path)
    try test(server, path)
    finally server.close()

  "Unix domain socket" - {
    "server and client roundtrip" in withSocketPair { (server, path) =>
      val serverThread = new Thread(() => {
        val conn = server.accept()
        val line = conn.readLine()
        conn.writeLine(s"echo: ${line.getOrElse("")}")
        conn.close()
      })
      serverThread.setDaemon(true)
      serverThread.start()
      Thread.sleep(50)

      val client = connectSocket(path)
      client.writeLine("hello")
      val response = client.readLine()
      client.close()

      response shouldBe Some("echo: hello")
    }

    "multiple sequential connections" in withSocketPair { (server, path) =>
      val serverThread = new Thread(() => {
        for _ <- 1 to 3 do
          val conn = server.accept()
          val line = conn.readLine()
          conn.writeLine(line.getOrElse("").toUpperCase)
          conn.close()
      })
      serverThread.setDaemon(true)
      serverThread.start()
      Thread.sleep(50)

      for word <- List("alpha", "beta", "gamma") do
        val client = connectSocket(path)
        client.writeLine(word)
        val response = client.readLine()
        client.close()
        response shouldBe Some(word.toUpperCase)
    }

    "empty message" in withSocketPair { (server, path) =>
      val serverThread = new Thread(() => {
        val conn = server.accept()
        val line = conn.readLine()
        conn.writeLine(s"got:${line.getOrElse("")}")
        conn.close()
      })
      serverThread.setDaemon(true)
      serverThread.start()
      Thread.sleep(50)

      val client = connectSocket(path)
      client.writeLine("")
      val response = client.readLine()
      client.close()

      response shouldBe Some("got:")
    }

    "long message" in withSocketPair { (server, path) =>
      val longMsg = "x" * 10000
      val serverThread = new Thread(() => {
        val conn = server.accept()
        val line = conn.readLine()
        conn.writeLine(line.getOrElse(""))
        conn.close()
      })
      serverThread.setDaemon(true)
      serverThread.start()
      Thread.sleep(50)

      val client = connectSocket(path)
      client.writeLine(longMsg)
      val response = client.readLine()
      client.close()

      response shouldBe Some(longMsg)
    }

    "JSON message roundtrip" in withSocketPair { (server, path) =>
      val serverThread = new Thread(() => {
        val conn = server.accept()
        conn.readLine()
        conn.writeLine("""{"ok":true}""")
        conn.close()
      })
      serverThread.setDaemon(true)
      serverThread.start()
      Thread.sleep(50)

      val client = connectSocket(path)
      client.writeLine("""{"op":"ping"}""")
      val response = client.readLine()
      client.close()

      response shouldBe Some("""{"ok":true}""")
    }

    "connect to nonexistent socket throws" in {
      assertThrows[java.io.IOException] {
        connectSocket("/tmp/nonexistent-socket-12345.sock")
      }
    }
  }
}
