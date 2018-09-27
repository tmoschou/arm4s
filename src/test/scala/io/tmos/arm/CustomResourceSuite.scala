package io.tmos.arm

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{InetAddress, ServerSocket, Socket, SocketException}
import java.util.concurrent.{Callable, CompletableFuture, ExecutorService, Executors, TimeUnit}

import org.scalatest.FunSuite

class CustomResourceSuite extends FunSuite {

  import Implicits._

  implicit val execServiceManager: CanManage[ExecutorService] = new CanManage[ExecutorService] {
    override def onFinally(pool: ExecutorService): Unit = {
      pool.shutdown() // Disable new tasks from being submitted
      try {
        if (!pool.awaitTermination(10, TimeUnit.SECONDS)) { // wait for normal termination
          pool.shutdownNow() // force terminate
          if (!pool.awaitTermination(10, TimeUnit.SECONDS)) // wait for forced termination
            throw new RuntimeException("ExecutorService did not terminate")
        }
      } catch {
        case _: InterruptedException =>
          pool.shutdownNow() // (Re-)Cancel if current thread also interrupted
          Thread.currentThread().interrupt() // Preserve interrupt status
          // It is very important that we do not propagate InterruptedException
          // as it may be added as it may be suppressed if an earlier
          // exception trumps its. This is the same generally for any close method.
          // See https://docs.oracle.com/javase/8/docs/api/java/lang/AutoCloseable.html#close--
          // for details.
      }
    }
    override def onException(r: ExecutorService): Unit = {}
  }

  test("complex example of custom manager for Executor service and delegated resource management") {

    import scala.collection.JavaConverters._

    val serverSocketFuture = new CompletableFuture[ServerSocket]

    val callable = new Callable[Unit] {

      override def call(): Unit = {
        // Note that ServerSocket.accept() blocks but, does not throw
        // InterruptedException. Instead, to terminate the event loop, we need
        // to close server socket asynchronously. We delegate closing of server
        // socket to the main thread under normal circumstances, but in the
        // event this thread has thrown an exception, we will close.
        for (ss <- new ServerSocket(0, 0, InetAddress.getLoopbackAddress).closeOnException) {
          serverSocketFuture.complete(ss)
          while (!Thread.interrupted()) { // main event loop
            try {
              for {
                connection <- ss.accept.closeOnFinally // block here
                out <- new PrintWriter(connection.getOutputStream, true).closeOnFinally
                in <- new BufferedReader(new InputStreamReader(connection.getInputStream)).closeOnFinally
                line <- in.lines().iterator().asScala
              } out.println(line.toUpperCase)
            } catch {
              case _: SocketException if ss.isClosed =>
                // at this point server socket has been closed via the main thread
                // we set interrupt status and terminate the event loop / thread
                Thread.currentThread().interrupt()
            }
          }
        }
      }
    }

    // manage an executor service using the user defined CanManage
    val completedFuture = for (
      executorService <- Executors.newSingleThreadExecutor().manage
    ) yield {
      val future = executorService.submit(callable)
      for (ss <- serverSocketFuture.get.closeOnFinally) {
        val upperPhrase = for {
          s <- new Socket(InetAddress.getLoopbackAddress, ss.getLocalPort).closeOnFinally
          out <- new PrintWriter(s.getOutputStream, true).closeOnFinally
          in <- new BufferedReader(new InputStreamReader(s.getInputStream)).closeOnFinally
        } yield {
          out.println("hello")
          out.println("world")
          in.readLine() + ' ' + in.readLine()
        }
        assert(upperPhrase === "HELLO WORLD")
      }
      // at this point the callable event loop is terminating
      future
    }
    // at this point the callable event loop has been terminated

    assert(!completedFuture.isCancelled)
    assert(completedFuture.isDone)
    completedFuture.get // should not block or throw any error

  }

}
