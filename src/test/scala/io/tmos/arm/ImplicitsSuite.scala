package io.tmos.arm

import org.scalatest.funsuite.AnyFunSuite

class ImplicitsSuite extends AnyFunSuite {

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

  test("can supply different managers to different resource instances of the same type") {
    import Implicits._

    def onFinally[R](f: R => Unit): CanManage[R] = new CanManage[R] {
      override def onFinally(r: R): Unit = f(r)
    }

    var onFinally1 = false
    var onFinally2 = false

    for {
      _ <- ().manage(onFinally[Unit](_ => onFinally1 = true))
      _ <- ().manage(onFinally[Unit](_ => onFinally2 = true))
    } ()

    assert(onFinally1)
    assert(onFinally2)
  }

}
