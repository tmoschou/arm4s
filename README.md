# ARM4S - Automatic Resource Management for Scala

 |Branch|Status|
 |:-----|:----:|
 |*master*|[![Build Status](https://travis-ci.org/tmoschou/arm4s.svg?branch=master)](https://travis-ci.org/tmoschou/arm4s)|
 |*develop*|[![Build Status](https://travis-ci.org/tmoschou/arm4s.svg?branch=develop)](https://travis-ci.org/tmoschou/arm4s)|

[![Scaladocs](https://www.javadoc.io/badge/io.tmos/arm4s_2.12.svg?label=Scaladoc)](https://www.javadoc.io/doc/io.tmos/arm4s_2.12)

This library provides a way of succinctly dealing with resources in an exception
safe manner. This library can provided identical exception handling and
execution semantics to Java's `try`-with-resource statement and a substitute for Scala
which does not have an equivalent construct natively.

More generally, managed resources are not limited to `java.lang.AutoClosable`s.
Any type with a **on-finally** or **on-exception** lifecycle, can be supported with
user-defined implicit adapters. For instance a `java.util.concurrent.ExecutorService`
may have a user-defined shutdown hook executed on-finally. See Comprehensive Example below.

Managed resources are treated as a singleton enumerator whose managed lifecycle is
scoped to an applied expression (or block). Unlike Java's constructs, ARM4S's
applied block can yield results, ensuring that resources are closed properly
before returning.

For example: Manage multiple resources and yield a result

```scala
import io.tmos.arm.Implicits._
import java.io._
import java.net.{Socket, InetAddress, ServerSocket}

val line = for {
  socket <- new Socket(InetAddress.getLoopbackAddress, port).manage
  out <- new PrintWriter(socket.getOutputStream, true).manage
  in <- new BufferedReader(new InputStreamReader(socket.getInputStream)).manage
} yield {
  val line = in.readLine()
  out.println(line.toUpperCase)
  return line
}
```

For more examples see, the Examples section below.

# Rational

Manual management of resources have proven to be error prone, and when done
"correctly" - ugly.

Refer to
 * Joshua Bloch's Original
   [Proposal for ARM](http://mail.openjdk.java.net/pipermail/coin-dev/2009-February/000011.html).
 * Oracle's tech article on
   [Try-with-resources](http://www.oracle.com/technetwork/articles/java/trywithresources-401775.html).

For instance, if you are doing any of the following, then you should consider this library.

```scala
// don't do this
val r = new Resource(...)
try {
  ... // assume we throw an exception here
} finally {
  r.close() // what if we throw an exception here too?
}
```

Not good - we just masked (lost) the first 'important' exception!
You may be tempted to wrap the close in another try/catch and log it
so that the first exception isn't ever dropped.

```scala
// don't do this either
val r = new Resource(...)
try {
  ...
} finally {
  try {
    r.close() // close quietly
  } catch {
    case e => log.warn(e)
  }
}
```

Still Bad - If `close()` throws an exception, the application has no idea one was thrown and
with no opportunity to fail fast and safely. Especially so, if the main try clause didn't
throw any exception, in which case _no_ exception is propagated.

Instead we should utilise `Throwable.addSuppressed` to propagate the 'first' important exception
 with any subsequent exceptions attached as 'suppressed'.

Now, that was for one resource! - What if you needed to close multiple resources
 in a finally block, each of which could independently throw an exception on close.
Could you get it right? If you do - well done!
But the next developer who reads is unlikely to understand it.

This is where this library comes in and does things *correctly* and *succinctly*,
ensuring that the first exception thrown is the one that
is propagated, and any subsequent exceptions thrown are added to the head exception as suppressed.

## Exception Behaviour
This library differs from other Scala ARM libraries in that it has been designed with consideration for different
exception scenarios and with the following goals regarding exception safe behaviour:

1. The `onFinally` (by default delegates to `close` for `java.lang.AutoClosables`s) and `onException` management
   hooks of a resource method must be called even if the body throws _any_ `Throwable` exception
   including fatal ones to ensure that no resources are leaked. Example of fatal exceptions include anything not
   matched by `scala.util.control.NonFatal` such as `InterruptedException`, `ControlThrowable` and
   `VirtualMachineError`. Though you should not try to handle such fatal errors, finally logic should still
   (attempted to) be executed regardless.

2. The `onFinally` and `onException` execution hooks are permitted to throw any `Throwable` too,
   possibly additional to exceptions thrown from the main block.

3. Any `Throwable` thrown by `onFinally` or `onException` should not mask any exception thrown firstly by the
   body, if any. Instead the secondary exception(s) thrown should be caught and recorded as a
   suppressed exception against the primary (currently throwing) exception.

4. Lastly we differ slightly to Java's implementation of `try`-with-resources in which we permit
   the corner case where exceptions thrown `onFinally` or `onException` may be the same instance
   as thrown by the applied expression. Where Java will throw a new `IllegalArgumentException` on
   attempts to self suppress, we will silently drop repeated instances as usually it is not the
   users fault that an underlying or decorated resource used a cached exception rather than
   generate a new exception/stacktrace etc.

## Including ARM4S in your project

In SBT:
```scala
libraryDependencies += "io.tmos" %% "arm4s" % "1.0.0"
```
In Maven:
```xml
<dependency>
    <groupId>io.tmos</groupId>
    <artifactId>arm4s_${scala.binary.version}</artifactId>
    <version>1.0.0</version>
</dependency>
```
## Using ARM4S

There are two ways you can construct a managed resource

Explicitly
```scala
import io.tmos.arm.ArmMethods._
for (r <- manage(resource)) {
  ...
}
```

Or using implicit decorator methods
```scala
import io.tmos.arm.Implicits._
for (r <- resource.manage) {
   ...
}
```

Any resource of type `T` for which an implicit `CanManage[T]` adapter is provided in scope can be managed.

By default the implicit `CloseOnFincally extends CanManage[AutoClosable]`
manager defined in the CanManage companion object is used, which calls `close`
on-finally, unless a higher priority implicit is in scope.
Alternatively `closeOnFinally` method may be used in place of `manage` method,
To explicitly use this adapter.

```scala
import io.tmos.arm.ArmMethods._
for (r <- closeOnFinally(resource)) {...}
```

or using the implicit method
```scala
import io.tmos.arm.Implicits._
for (r <- resource.closeOnFinally) {...}
```

Managed resources may be composed together/chained in a monadic manner that allows for optionally yielding
results or imperatively using `for`-comprehensions.

## Examples
Using
[For-Comprehensions](https://www.scala-lang.org/files/archive/spec/2.12/06-expressions.html#for-comprehensions-and-for-loops)
```scala
import io.tmos.arm.ArmMethods._
val jsonMap: Map[String, Any] = for {
  inputStream <- manage(new FileInputStream("data.json"))
} yield {
  JsonMethods.parse(inputStream).extract[Map[String, Any]]
}
```

We could also write in the following monadic style
```scala
import io.tmos.arm.Implicits._
val jsonMap: Map[String, Any] = new FileInputStream("data.json")
 .manage
 .map(JsonMethods.parse(_))
 .extract[Map[String, Any]]
```
Or if composing multiple resources this can be done easily too
```scala
import io.tmos.arm.ArmMethods._
val result = for {
  a <- manage(new A)
  b <- manage(a.getB)
  c <- manage(new C(b))
} yield {
  // ...
}
```

Resources will be safely managed in reverse declaration order even if a later enumerator
declaration threw an exception prior to the main body. For example if `new C(b)`
thew an exception, then `b` followed by `a`'s on-exception/on-finally lifecycle
hooks will be executed.

There may be cases where you need to override the default behaviour for AutoClosable and
close resources safely _only_ on-exception, such as when needing to construct
multiple resources atomically or delegating close to a different thread/scope.
This can be achieved using the predefined `CloseOnException` manager.

```scala
import io.tmos.arm.ArmMethods._

// this implicit now has higher priority then the default CloseOnFinally
implicit val canManage: CanManage[AutoCloseable] = CanManage.CloseOnException

def openAll(): (A, B, C) = for {
  a <- manage(new A)
  b <- manage(new B)
  c <- manage(new C)
} yield (a,b,c)
```

We also provided `closeOnException` method similarly to `closeOnFinally`, for
explicit usage of this adapter without needing to import it as a higher priority
implicit.

## Comprehensive Example

Here is a comprehensive (hypothetical) example of managing multiple resources implicitly,
including an `ExectorService` which we define `onFinally` logic for.
This sample code runs a server socket in a separate thread echoing back text it
receives in uppercase.

```scala
import io.tmos.arm.Implicits._
import scala.collection.JavaConverters._

implicit val manager: CanManage[ExecutorService] = new CanManage[ExecutorService] {
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
```
