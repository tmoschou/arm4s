package io.tmos.arm

import java.io.Closeable

class SimpleAutoCloseableTest(val msg: String) extends Closeable {
  protected var closed = false
  def except(m: String = msg) = throw new RuntimeException(m)
  def isClosed: Boolean = closed
  override def close(): Unit = closed = true
}
