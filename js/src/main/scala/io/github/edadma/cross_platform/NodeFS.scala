package io.github.edadma.cross_platform

import scala.scalajs.js
import scala.scalajs.js.annotation.*
import scala.scalajs.js.typedarray.Uint8Array

@js.native
@JSImport("fs", JSImport.Namespace)
object NodeFS extends js.Object {
  // String/text operations
  def readFileSync(path: String, enc: String): String    = js.native
  def readFileSync(path: String): Uint8Array             = js.native
  def appendFileSync(path: String, data: String): String = js.native
  def writeFileSync(path: String, data: String): Unit    = js.native

  // Buffer/binary operations
//  def readFileSync(path: String): js.Any              = js.native // Returns Buffer
  def writeFileSync(path: String, data: js.Any): Unit = js.native // Accepts Buffer

  // File system queries
  def existsSync(path: String): Boolean           = js.native
  def accessSync(path: String, mode: Int): Unit   = js.native
  def readdirSync(path: String): js.Array[String] = js.native

  // File stats
  def statSync(path: String): NodeStats  = js.native
  def lstatSync(path: String): NodeStats = js.native

  // Directory operations
  def mkdirSync(path: String): Unit                  = js.native
  def mkdirSync(path: String, options: js.Any): Unit = js.native
  def rmdirSync(path: String): Unit                  = js.native
  def mkdtempSync(prefix: String): String            = js.native

  // File operations
  def unlinkSync(path: String): Unit                     = js.native
  def copyFileSync(source: String, target: String): Unit = js.native
  def renameSync(source: String, target: String): Unit   = js.native

  // fd-based operations
  def openSync(path: String, flags: String): Int                                                    = js.native
  def readSync(fd: Int, buffer: Uint8Array, offset: Int, length: Int, position: Double): Int        = js.native
  def writeSync(fd: Int, buffer: Uint8Array, offset: Int, length: Int, position: Double): Int       = js.native
  def ftruncateSync(fd: Int, len: Double): Unit                                                     = js.native
  def fstatSync(fd: Int): NodeStats                                                                 = js.native
  def fsyncSync(fd: Int): Unit                                                                       = js.native
  def closeSync(fd: Int): Unit                                                                       = js.native

  // Constants
  val constants: NodeFSConstants = js.native
}

@js.native
trait NodeFSConstants extends js.Object {
  val R_OK: Int = js.native
  val W_OK: Int = js.native
  val X_OK: Int = js.native
}

@js.native
trait NodeStats extends js.Object {
  def isFile(): Boolean         = js.native
  def isDirectory(): Boolean    = js.native
  def isSymbolicLink(): Boolean = js.native
  val dev: Double               = js.native
  val ino: Double               = js.native
  val size: Double              = js.native
  val mtime: NodeDate           = js.native
}

@js.native
trait NodeDate extends js.Object {
  def getTime(): Double = js.native
}
