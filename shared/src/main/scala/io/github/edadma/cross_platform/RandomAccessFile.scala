package io.github.edadma.cross_platform

trait RandomAccessFile extends java.io.DataInput with java.io.DataOutput with AutoCloseable:
  def seek(pos: Long): Unit
  def getFilePointer: Long
  def length: Long
  def setLength(newLength: Long): Unit
  def read: Int
  def fsync(): Unit
  def close(): Unit
