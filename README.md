# ARM4S - Automatic Resource Management for Scala

[![Build Status](https://travis-ci.org/tmoschou/arm4s.svg?branch=develop)](https://travis-ci.org/tmoschou/arm4s)

This library provides a way of succinctly dealing with resources in an exception safe manner. The behaviour
is identical to Java's exception handling in `try`-with-resources statement and a substitute for scala which does not
have an equivalent construct natively.

Unlike Java's try-with-resource constructs, managed resources are not limited to `java.lang.AutoClosable`s.
Further types such as `java.util.concurrent.ExecutorService` can be supported with implicit converters in user code.
See Comprehensive Examples below. Also unlike try-with-resource constructs, ARM4S's managed block (the body) can
actually yield results, ensuring that resources are closed properly before returning.

For example: Parse multiple resources implicitly and yield a result

    import io.tmos.arm.implicits._
    import java.io._
    import java.net.{Socket, InetAddress, ServerSocket}

    val line = for {
      socket <- new Socket(InetAddress.getLoopbackAddress, port)
      out <- new PrintWriter(socket.getOutputStream, true)
      in <- new BufferedReader(new InputStreamReader(socket.getInputStream))
    } yield {
      val line = in.readLine()
      out.println(line.toUpperCase)
      return line
    }

For more examples see, the Examples section below.

## Exception Behaviour
This library differs from other Scala ARM libraries in that it has been designed with consideration for different
exception scenarios and with the following goals regarding exception safe behaviour:

1. The `withFinally` method (see `CanManage`) of a managed resource (e.g. delegates to `close` of `AutoCloseable`s)
   must be called even if the body throws _any_ `Throwable` exception including fatal ones. Example of fatal exceptions
   include anything not matched by `scala.util.control.NonFatal` such as `InterruptedException`, `ControlThrowable`
   and `VirtualMachineError`. Any exception must be immediately rethrown after withFinally,
   in fact this is a requirement of fatal exceptions.

2. The `withFinally` method in a finally clause may also throw any `Throwable` too. Unfortunately this is a possibility
   and permitted by `AutoCloseable` which can throw any `Throwable` (Any `Exception` plus `Error` which are
   unchecked).

3. Importantly, any `Throwable` thrown by `withFinally` must not mask (suppress) any exception thrown firstly by the body,
   if any. Instead it should catch and recorded as a suppressed exception against the original (currently throwing)
   exception, and certainly not vice-versa. This is what Java's try with resource construct effectively does; for more
   details, see Oracle's tech article on
   (Try-with-resources)[http://www.oracle.com/technetwork/articles/java/trywithresources-401775.html].


## Including ARM4S in your project

In SBT:

    libraryDependencies += "io.tmos" %% "arm4s" % VERSION

In Maven:

    <dependency>
        <groupId>io.tmos</groupId>
        <artifactId>arm4s_${scala.binary.version}</artifactId>
        <version>VERSION</version>
    </dependency>

Replace `VERSION` with the latest version.

## Using ARM4S

There are two ways you can import arm4s.

For explicit resource management

    import io.tmos.arm._
    for (r <- manage(resource))
      ...

Or implicitly

    import io.tmos.arm.implicits._
    for (r <- resource)
       ...

Any resource of type `T` for which an implicit `CanManage[T]` object is provided in scope can be managed.

By default the following `CanManage[T]` that are automatically provided in scope on import
  * `type T = java.lang.AutoClosable`
  * `type T = { def close() }` - via scala reflection

Managed resources may be composed together/chained in a monadic manner that allows for optionally yielding
results or imperatively using `for`-comprehensions.

The managed resources are automatically closed, and in reverse declaration order.

The resources are closed in a `finally` clause regardless of any exception thrown in the body of the
`for`-comprehension, or any prior `withFinally` called on other resources.

## Examples
Imperatively

    import io.tmos.arm._
    val lines: Seq[String] = for (inputStream <- managed(new FileInputStream("data.json")) yield {
      Json.parse(inputStream).as[Seq[String]]
    }

which translates the the following monadic style

    import io.tmos.arm._
    val lines = managed(new FileInputStream("data.json")) map { inputStream =>
       Json.parse(inputStream).as[Seq[String]]
    }

or if composing multiple resources this can be done easily too

    import io.tmos.arm._
    val result = for {
      a <- managed(new A)
      b <- managed(new B(a))
      c <- managed(new C)
    } yield {
      ...
    }

Note that this is NOT the same as

    val a : A = new A
    val b : B = new B(a)
    val c : C = new C

    val result try {
      ...
    } finally {
      c.close
      b.close
      a.close
    }

For example if `new B(a)` threw an exception then `a` would not be closed. Likewise if `c.close` threw an exception, then
`a` and `b` would not be closed. The equivalent code using multiple `try` statements gets messy very quickly.
See Oracle's tech article on
(Try-with-resources)[http://www.oracle.com/technetwork/articles/java/trywithresources-401775.html] for an example.

## Comprehensive Example

Here is a comprehensive example of managing multiple resources implcitly
including an `ExectorService` which we provide the custom `withFinally` logic for, that runs a tcp service in a separate
thread which echos back text in uppercase.

    import io.tmos.arm.implicits._
    import scala.collection.JavaConverters._

    implicit val canManageExectorService = new CanManage[ExecutorService] {
      override def withFinally(pool: ExecutorService): Unit = {
        pool.shutdown() // Disable new tasks from being submitted
        try {
          if (!pool.awaitTermination(10, TimeUnit.SECONDS)) { // wait for normal termination
            pool.shutdownNow() // force terminate
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) // wait for forced termination
              throw new RuntimeException("ExecutorService did not terminate")
          }
        } catch {
          case e: InterruptedException =>
            pool.shutdownNow()  // (Re-)Cancel if current thread also interrupted
            Thread.currentThread().interrupt()  // Preserve interrupt status
        }
      }
    }

    val port = new CompletableFuture[Int]

    val callable = new Callable[Unit] {
      override def call(): Unit = {
        for (ss <- new ServerSocket(0, 0, InetAddress.getLoopbackAddress)) {
          port.complete(ss.getLocalPort)
          for {
            connection <- ss.accept
            out <- new PrintWriter(connection.getOutputStream, true)
            in <- new BufferedReader(new InputStreamReader(connection.getInputStream))
            line <- in.lines().iterator().asScala // note not a resource, but now a traversable
          } out.println(line.toUpperCase)
        }
      }
    }

    // manage an executor service using the user defined canManageExectorService.
    // This will shutdown and wait termination
    val completedFuture = for (executorService <- Executors.newSingleThreadExecutor()) yield {
      val future = executorService.submit(callable)
      val upperPhrase = for {
        s <- new Socket(InetAddress.getLoopbackAddress, port.get)
        out <- new PrintWriter(s.getOutputStream, true)
        in <- new BufferedReader(new InputStreamReader(s.getInputStream))
      } yield {
        out.println("hello")
        out.println("world")
        in.readLine() + ' ' + in.readLine()
      }
      assert(upperPhrase === "HELLO WORLD")
      future
    }

    assert(completedFuture.isCancelled || completedFuture.isDone)
    completedFuture.get() // should not block

## Caveats

If a resource implements any of

  * `def map[B](f: A => B): B`
  * `def flatMap[B](f: A => B): B`
  * `def foreach(f: A => Unit): Unit`

then it is _not_ recommended to use to use the implicit management of the resource, as it may not be obvious to a reader
if the resource has become managed. For example

    import io.tmos.arm.implicits._

    class MappableResource extends AutoCloseable {
      def isClosed = closed
      protected var closed = false
      override def close(): Unit = closed = true
      def map[B](f: Int => B): Seq[B] = (0 to 3).map(f)
    }

    val resource1 = new MappableResource
    val result1 = for (i <- resource1) yield { // resource NOT managed
      i * 2     // Note i is of type Int, not MappableResource
    }
    println(result1)                // Vector(0, 2, 4, 6)
    println(resource1.isClosed)     // false

    val resource2 = new MappableResource
    val result2 = for {
      r <- resource2     // resource managed
      i <- r
    } yield {
      i * 2
    }
    println(result2)                // Vector(0, 2, 4, 6)
    println(resource2.isClosed)     // true

Please use the explicit `manage()` method.
