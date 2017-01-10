# arm4s - Automatic Resource Management for Scala

This package provides a way of succinctly dealing with resources in an exception safe manner,
exactly the same behaviour as Java's `try`-with-resources statement.

By importing this package object in scope with `import io.tmos.arm._`, any object
implementing the [[io.tmos.arm.CanManage]] trait may be used to construct
a [[io.tmos.arm.ManagedResource]] by passing it to the [[io.tmos.arm.manage(Resource)]] method.

Note that any [[java.lang.AutoCloseable]], else any object who's structural type
has the member `def close()` will implicitly be converted to a [[io.tmos.arm.CanManage]]

Managed resources may be composed together/chained in a monadic manner that allows for optionally yielding
results or imperatively using `for`-comprehensions.

The managed resources are automatically closed, and in reverse declaration order `for`-comprehension.

The resources are closed in a `finally` clause regardless of any exception thrown in the body of the
`for`-comprehension, or any prior `close()` called on other resources.

## Examples
Imperatively
```
  import io.tmos.arm._
  val lines: Seq[String] = for (inputStream <- managed(new FileInputStream("data.json")) yield {
    Json.parse(inputStream).as[Seq[String]]
  }
```

or if composing multiple resources this can be done easily too
```
  import io.tmos.arm._
  val lines: Seq[String] = for {
    x <- managed(new FileInputStream("dataset-1.json")
    y <- managed(new FileInputStream("dataset-2.json")
    z <- managed(new FileInputStream("dataset-3.json")
  } yield {
    Json.parse(x).as[Seq[String]] ++
    Json.parse(y).as[Seq[String]] ++
    Json.parse(z).as[Seq[String]]
  }
```

This `for`-comprehension translates by the compiler to the following monadic style that is also supported
(but more verbose).
```
   import io.tmos.arm._
   val lines: Seq[String] =
     managed(new FileInputStream("dataset-1.json") flatMap { x =>
       managed(new FileInputStream("dataset-2.json") flatMap { y =>
         managed(new FileInputStream("dataset-3.json") map { z =>
           Json.parse(x).as[Seq[String]] ++
           Json.parse(y).as[Seq[String]] ++
           Json.parse(z).as[Seq[String]]
         }
       }
     }
```

Note that the [[io.tmos.arm.ManagedResource.apply()]] method is also conveniently provided so as to drop
the need to write `map`, `flatMap` or `foreach` in the above.

Likewise a managed body need not return/yield a result
```
  import io.tmos.arm._
  for {
    readSocket <- managed(new Socket(...))
    writeSocket <- managed(new Socket(...))
  } Util.pipe(readSocket, writeSocket)
```

or
```
   import io.tmos.arm._
   managed(new Socket(...)) foreach { readSocket =>
     managed(new Socket(...)) forach { writeSocket =>
       Util.pipe(readSocket, writeSocket)
     }
   }
```
Again


## Exception Behaviour
The library has been designed with the following goals regarding exception safe behaviour and identical to
Java's `try`-with-resource statement behaviour.

1. The `close` method must be called even if the body throws any [[Throwable]] (possibly fatal) exception.
   Example of fatal exceptions include anything not matched by [[scala.util.control.NonFatal]] such as
   [[InterruptedException]], [[ControlThrowable]] and [[VirtualMachineError]]. Note that any fatal exception must
   be immediately rethrown after close (in fact we just throw everything); as such a `finally` clause is used to
   handle the `close`.

2. The close method in a finally clause may also throw any [[Throwable]] too. Unfortunately this is a possibility
   and permitted by [[java.lang.AutoCloseable]] which can throw any [[Exception]] plus any [[Error]] which are
   unchecked.

3. Any exception thrown in the `finally` clause must not mask (suppress) any exception thrown firstly by the body,
   if any. Instead it should caught and recorded as a suppressed exception against the original (currently throwing)
   exception, and certainly not vice-versa.

For more details on proper exception handling of resources, see
[[http://www.oracle.com/technetwork/articles/java/trywithresources-401775.html]].

Many libraries don't get this right, and result in suppressed exceptions making debugging difficult.
