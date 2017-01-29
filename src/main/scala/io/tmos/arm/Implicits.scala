package io.tmos.arm

/**
 * Allows for implicit management of resources.
 *
 * == Caveats ==
 *
 * If a resource implements any of
 *
 *   - `def map[B](f: A => B): B`
 *   - `def flatMap[B](f: A => B): B`
 *   - `def foreach(f: A => Unit): Unit`
 *
 * then it is ''not'' recommended to use to use the implicit management of the resource, as it may not be obvious to a reader
 * if the resource has become managed. For example
 * {{{
 *     import io.tmos.arm.Implicits._
 *
 *     class MappableResource extends AutoCloseable {
 *       def isClosed = closed
 *       protected var closed = false
 *       override def close(): Unit = closed = true
 *       def map[B](f: Int => B): Seq[B] = (0 to 3).map(f)
 *     }
 *
 *     val resource1 = new MappableResource
 *     val result1 = for (i <- resource1) yield { // resource NOT managed
 *       i * 2     // Note i is of type Int, not MappableResource
 *     }
 *     println(result1)                // Vector(0, 2, 4, 6)
 *     println(resource1.isClosed)     // false
 *
 *     val resource2 = new MappableResource
 *     val result2 = for {
 *       r <- resource2     // resource managed
 *       i <- r
 *     } yield {
 *       i * 2
 *     }
 *     println(result2)                // Vector(0, 2, 4, 6)
 *     println(resource2.isClosed)     // true
 * }}}
 * Please use the explicit [[io.tmos.arm.manage]] method instead.
 */
object Implicits {

  /**
   * Implicit class to convert a resource to one that is managed.
   *
   * @param r the resource passed by name
   * @tparam R
   */
  implicit class ImplicitDefaultManagedResource[R: CanManage](r: =>  R) extends DefaultManagedResource[R](r)
}
