package io.tmos.arm

/**
 * The default implementation of a [[ManagedResource]].
 *
 * This implementation has identical to Java's try-with-resource constructs. See the package documentation for
 * a description on the exception behaviour of this implementation.
 *
 * @param r the resource passed by name
 * @tparam A the type of the resource to manage
 * @tparam U a supertype of `A` for which an implicit CanManage[U] object is in scope, defining the `finally`
 *           logic of the resource.
 */
final class DefaultManagedResource[A, U >: A : CanManage](r: => A) extends ManagedResource[A] {

  override def apply[B](f: A => B) : B = {
    val resource = r // construct resource
    val canManage = implicitly[CanManage[U]]
    var throwing: Throwable = null
    try {
      f(resource)
    } catch {
      case e: Throwable =>
        throwing = e
        throw e
    } finally {
      if (resource != null) {
        if (throwing != null) {
          try {
            canManage.withFinally(resource)
          } catch {
            case e: Throwable =>
              throwing.addSuppressed(e)
          }
        } else {
          canManage.withFinally(resource)
        }
      }
    }
  }
}
