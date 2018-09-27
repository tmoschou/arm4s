package io.tmos.arm

/**
 * A resource that is managed.
 *
 * Only one implementation is provided currently [[DefaultManagedResource]]. Subclasses only need to provide
 * [[ManagedResource.map]]
 *
 * @tparam A the type of the resource to manage
 */
trait ManagedResource[+A]{

  /**
   * Allows the resource to be used imperatively in `yield`-ing `for`-comprehensions.
   *
   * For example
   * {{{
   *   import io.tmos.arm._
   *   for (r <- managed(new Resource)) yield {
   *     ...
   *   }
   * }}}
   *
   * @param f the function to execute of which the resource is managed
   * @tparam B the return type of the function
   * @return the result of the function
   */
  def map[B](f: A => B): B

  /**
   * Allows the resource to be used imperatively in ''stacked'' `yield`-ing `for`-comprehensions.
   *
   * For example
   * {{{
   *   import io.tmos.arm._
   *   for {
   *     a <- managed(new Resource1)
   *     b <- managed(new Resource2)
   *   } yield {
   *     ...
   *   }
   * }}}
   * which translates to
   * {{{
   *   managed(new Resource1) flatMap { a =>
   *     managed(new Resource2) map { b =>
   *       ...
   *     }
   *   }
   * }}}
   *
   * Default implementation is to call [[map]]
   *
   * @param f the function to execute of which the resource is managed
   * @tparam B the return type of the function
   * @return the result of the function
   */
  def flatMap[B](f: A => B): B  = map(f)

  /**
   * Allows the resource to be used imperatively in `for`-comprehensions.
   *
   * For example
   * {{{
   *   import io.tmos.arm._
   *   for (a <- managed(new Resource1)) {
   *     ...
   *   }
   * }}}
   *
   * Default implementation is to call [[map]]
   *
   * @param f the function to execute of which the resource is managed
   */
  def foreach(f: A => Unit): Unit  = map(f)
}




