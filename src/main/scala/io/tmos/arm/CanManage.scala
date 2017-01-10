package io.tmos.arm

/**
 * A resource that may become managed.
 *
 * @tparam R the underlying type of the resource passed to the managed body
 */
trait CanManage[R] {

  /**
   * Releases the resource.
   *
   * From Java's [[java.lang.AutoCloseable]] but also applicable to any implementation of this trait:
   *
   * "Implementers of this interface are also strongly advised to not have the
   * method throw [[InterruptedException]]. This exception interacts with a thread's
   * interrupted status, and runtime misbehavior is likely to occur if an [[InterruptedException]]
   * is suppressed. More generally, if it would cause problems for an exception to be suppressed,
   * the AutoCloseable.close method should not throw it."
   */
  def withFinally(r: R): Unit
}

/**
 * Companion object to the Resource type trait.
 *
 * This contains all the default implicits in appropriate priority order.
 */
object CanManage {
  import scala.language.reflectiveCalls

  type ReflectiveCloseable = {
    def close()
  }

  /**
   * This is the type class implementation for reflectively assuming a class with a close method is
   * a resource.
   */
  implicit def reflectiveCloseableResource[R <: ReflectiveCloseable]: CanManage[R] = new CanManage[R] {
    override def withFinally(handle: R) = handle.close()
  }

  implicit def autoCloseableResource[R <: AutoCloseable]: CanManage[R] = new CanManage[R] {
    override def withFinally(handle: R) = handle.close()
  }

}
