package io.tmos.arm

import org.scalatest.FunSuite

class ImplicitsSuite extends FunSuite {

  test("has implicit manage converter") {
    import Implicits._
    val resource = new SimpleAutoCloseableTest("msg")
    for (r <- resource.manage) {}
    assert(resource.isClosed)
  }

  test("has implicit closeOnFinally converter") {
    import Implicits._
    val resource = new SimpleAutoCloseableTest("msg")
    for (r <- resource.closeOnFinally) {}
    assert(resource.isClosed)
  }

  test("has implicit closeOnException converter") {
    import Implicits._
    val resource = new SimpleAutoCloseableTest("msg")
    try {
      for (r <- resource.closeOnException) {
        r.except()
      }
    } catch {
      case _: Throwable =>
    }
    assert(resource.isClosed)
  }

}

