package io.tmos.arm

/**
 * The default implementation of a [[ManagedResource]].
 *
 * @param r the resource passed by name
 * @tparam R the type of the resource to pass to the main body
 * @tparam S the type of the resource under management
 */
class DefaultManagedResource[R, -S >: R](r: => R)(
  implicit canManage: CanManage[S]
) extends ManagedResource[R] {

  override def map[B](f: R => B) : B = {
    val resource = r // construct resource
    var throwing: Throwable = null
    try {
      f(resource)
    } catch {
      case e1: Throwable =>
        throwing = e1
        try {
          canManage.onException(resource)
        } catch {
          case e2: Throwable =>
            if (e2 != e1) {
              e1.addSuppressed(e2)
            }
        }
        throw e1
    } finally {
      if (throwing != null) {
        try {
          canManage.onFinally(resource)
        } catch {
          case e: Throwable =>
            // We differ in Java's implementation in that we silently drop
            // attempts to self-suppress exceptions rather than throw
            // IllegalArgumentException since it is almost always not the users
            // fault that an underlying resource uses a cached exception
            // rather than generate a new exception/stacktrace etc. I consider this
            // an oversight in Java's implementation; changing it however was floated
            // http://mail.openjdk.java.net/pipermail/core-libs-dev/2014-May/026742.html
            if (e != throwing) {
              throwing.addSuppressed(e)
            }
        }
      } else {
        canManage.onFinally(resource)
      }
    }
  }

}
