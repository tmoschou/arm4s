package io.tmos.arm

import java.io.Closeable

import io.tmos.arm.ArmMethods._
import org.scalatest.WordSpec

import scala.collection.mutable

class ArmMethodsSuite extends WordSpec {

  "AutoClosable resources" when {

    "using default CanManage manager" should  {

      "be the CloseOnFinally implementation" in {
        val manager: CanManage[Closeable] = implicitly[CanManage[Closeable]]
        assert(manager == CanManage.CloseOnFinally)
      }

      "be closed in reverse order when foreach-ing over them" in {
        var resource1: SimpleAutoCloseableTest = null
        var resource2: SimpleAutoCloseableTest = null
        var resource3: SimpleAutoCloseableTest = null

        resource1 = new SimpleAutoCloseableTest("1") {
          override def close(): Unit = {
            super.close()
            assert(resource2.isClosed)
            assert(resource3.isClosed)
          }
        }

        resource2 = new SimpleAutoCloseableTest("2") {
          override def close(): Unit = {
            super.close()
            assert(!resource1.isClosed)
            assert(resource3.isClosed)
          }
        }

        resource3 = new SimpleAutoCloseableTest("3") {
          override def close(): Unit = {
            super.close()
            assert(!resource1.isClosed)
            assert(!resource2.isClosed)
          }
        }

        for {
          r1 <- manage(resource1)
          r2 <- manage(resource2)
          r3 <- manage(resource3)
        } {
          assert(!r1.isClosed)
          assert(!r2.isClosed)
          assert(!r3.isClosed)
        }

        assert(resource1.isClosed)
        assert(resource2.isClosed)
        assert(resource3.isClosed)
      }

      "be closed in reverse order when yield-ing over them" in {
        var resource1: SimpleAutoCloseableTest = null
        var resource2: SimpleAutoCloseableTest = null
        var resource3: SimpleAutoCloseableTest = null

        resource1 = new SimpleAutoCloseableTest("1") {
          override def close(): Unit = {
            super.close()
            assert(resource2.isClosed)
            assert(resource3.isClosed)
          }
        }

        resource2 = new SimpleAutoCloseableTest("2") {
          override def close(): Unit = {
            super.close()
            assert(!resource1.isClosed)
            assert(resource3.isClosed)
          }
        }

        resource3 = new SimpleAutoCloseableTest("3") {
          override def close(): Unit = {
            super.close()
            assert(!resource1.isClosed)
            assert(!resource2.isClosed)
          }
        }

        val value = for {
          r1 <- manage(resource1)
          r2 <- manage(resource2)
          r3 <- manage(resource3)
        } yield r1.msg + r2.msg + r3.msg

        assert(resource1.isClosed)
        assert(resource2.isClosed)
        assert(resource3.isClosed)
        assert(value === "123")
      }

      "propagate exception thrown in main body and be closed" in {
        val resource = new SimpleAutoCloseableTest("msg")
        try {
          for (r <- manage(resource))
            r.except("from main body")
        } catch {
          case e: RuntimeException =>
            assert(e.getMessage === "from main body")
        }
        assert(resource.isClosed)
      }

      "propagate exception thrown on close" in {
        val resource = new SimpleAutoCloseableTest("msg") {
          override def close(): Unit = {
            super.close()
            except("onClose")
          }
        }
        try {
          for (r <- manage(resource)) yield {
            r.msg
          }
        } catch {
          case e: RuntimeException =>
            assert(e.getMessage === "onClose")
        }
        assert(resource.isClosed)
      }

      "propagate first exception thrown in body with subsequent exception thrown on close attached" in {
        val resource = new SimpleAutoCloseableTest("msg") {
          override def close(): Unit = {
            super.close()
            except("second")
          }
        }
        try {
          for (r <- manage(resource))
            r.except("first")
        } catch {
          case e: RuntimeException =>
            assert(e.getMessage === "first")
            val suppressed = e.getSuppressed
            assert(suppressed.length === 1)
            assert(suppressed(0).getMessage === "second")
        }
        assert(resource.isClosed)
      }

      "handle orthogonal type variance in CanManage and body lambda" in {

        trait SimpleTraitTest {
          val msg: String
        }

        class PolyCloseableTest(override val msg: String)
          extends SimpleAutoCloseableTest(msg)
          with SimpleTraitTest {
        }

        val resource: PolyCloseableTest = new PolyCloseableTest("msg")

        // there is no type lineage between SimpleTestTrait and AutoClosable

        // use map so we can pass lambda accepting SimpleTraitTest super class
        val msg = manage(resource).map((myTrait: SimpleTraitTest) => myTrait.msg)

        assert(msg == "msg")
        assert(resource.isClosed)

      }

    }

    "using closeOnException CanManage implicit" should {

      implicit val canManage: CanManage[AutoCloseable] = CanManage.CloseOnException

      "not close when not excepting" in {

        val (r1, r2, r3) = for {
          r1 <- manage(new SimpleAutoCloseableTest("1"))
          r2 <- manage(new SimpleAutoCloseableTest("2"))
          r3 <- manage(new SimpleAutoCloseableTest("3"))
        } yield (r1, r2, r3)

        assert(!r1.isClosed)
        assert(!r2.isClosed)
        assert(!r3.isClosed)

        // because we should always clean up resources
        for {
          _ <- closeOnFinally(r1)
          _ <- closeOnFinally(r2)
          _ <- closeOnFinally(r3)
        } {}

        assert(r1.isClosed)
        assert(r2.isClosed)
        assert(r3.isClosed)

      }

      "close all if exception thrown at any point" in {

        // only for asserting that resources are closed
        val resources = mutable.ArrayBuffer.empty[SimpleAutoCloseableTest]

        def buildResource(msg: String): SimpleAutoCloseableTest = {
          resources += new SimpleAutoCloseableTest(msg)
          resources.last
        }

        try {
          for {
            _ <- manage(buildResource("1"))
            _ <- manage(buildResource("2"))
            _ <- manage(buildResource("3"))
          } {
            throw new RuntimeException("ops")
          }
        } catch {
          case e: Throwable =>
            assert(e.getMessage == "ops")
        }

        assert(resources.nonEmpty)
        for (r <- resources) {
          assert(r.isClosed)
        }

      }

    }

  }

  "different resources" when {
    "of the same type" should {
      "be able to use different managers" in {

        def onFinally[R](f: R => Unit): CanManage[R] = new CanManage[R] {
          override def onFinally(r: R): Unit = f(r)
        }

        var onFinally1 = false
        var onFinally2 = false

        for {
          _ <- manage(())(onFinally[Unit](_ => onFinally1 = true))
          _ <- manage(())(onFinally[Unit](_ => onFinally2 = true))
        } ()

        assert(onFinally1)
        assert(onFinally2)
      }

    }

  }
}
