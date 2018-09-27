package io.tmos.arm

import org.scalatest.FunSuite

class DefaultManagedResourceSuite extends FunSuite {

  case object SimpleResource

  class Manager[T] extends CanManage[T] {
    var onExceptionCalled: Boolean = false
    var onFinallyCalled: Boolean = false
    override def onException(r: T): Unit = {
      assert(!onFinallyCalled)
      onExceptionCalled = true
    }
    override def onFinally(r: T): Unit = {
      onFinallyCalled = true
    }
  }

  test("under normal operation, only onFinally should be called") {
    implicit val manager: Manager[SimpleResource.type] = new Manager[SimpleResource.type]
    val managedResource = new DefaultManagedResource(SimpleResource)
    managedResource.map(_ => Unit)
    assert(!manager.onExceptionCalled)
    assert(manager.onFinallyCalled)
  }

  test("if an exception is thrown by the applied expr onException should be called") {
    implicit val manager: Manager[SimpleResource.type] = new Manager[SimpleResource.type]
    val managedResource = new DefaultManagedResource(SimpleResource)
    try {
      managedResource.map(_ => throw new IllegalArgumentException)
      throw new IllegalStateException
    } catch {
      case e: IllegalArgumentException =>
    }
    assert(manager.onExceptionCalled)
    assert(manager.onFinallyCalled)
  }

  test("if exceptions are thrown onException and onFinally, they should be marked as suppressed") {
    implicit val manager: CanManage[SimpleResource.type] = new CanManage[SimpleResource.type] {
      override def onException(r: SimpleResource.type): Unit = throw new RuntimeException("onException")
      override def onFinally(r: SimpleResource.type): Unit = throw new RuntimeException("onFinally")
    }
    val managedResource = new DefaultManagedResource(SimpleResource)
    try {
      managedResource.map(_ => throw new IllegalArgumentException)
      throw new IllegalStateException
    } catch {
      case e: IllegalArgumentException =>
        assert(e.getMessage == null)
        val suppressed = e.getSuppressed
        assert(suppressed(0).getMessage == "onException")
        assert(suppressed(1).getMessage == "onFinally")
    }
  }

  test("cached exception should be silently dropped") {
    val e = new IllegalArgumentException
    implicit val manager: CanManage[SimpleResource.type] = new CanManage[SimpleResource.type] {
      override def onException(r: SimpleResource.type): Unit = throw e
      override def onFinally(r: SimpleResource.type): Unit = throw e
    }
    val managedResource = new DefaultManagedResource(SimpleResource)
    try {
      managedResource.map(_ => throw e)
      throw new IllegalStateException
    } catch {
      case e: RuntimeException =>
    }
    assert(e.getSuppressed.isEmpty)
    assert(e.getCause == null)
  }

}
