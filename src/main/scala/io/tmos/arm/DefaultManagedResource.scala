package io.tmos.arm


final class DefaultManagedResource[A, U >: A : CanManage](r: => A) extends ManagedResource[A] {

  def apply[B](f: A => B) : B = {
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
