package io.tmos.arm

/**
  * Methods for management of a resource.
  *
  * For implicit management see `io.tmos.arm.Implicits`.
  */
object ArmMethods {

  /**
    * Manages a generic resource.
    *
    * Requires an implicit `CanManage[S]` in scope. A default manager for
    * `AutoClosable`s is provided (See `CanManage.CloseOnFinally`) and the
    * default behaviour is equivalently the same as `closeOnFinally`
    *
    * @param r the resource to managed passed by-name
    * @tparam R the type of the resource passed to the applied expression
    * @tparam S the type of the resource passed to the manager
    *
    * @return a managed resource
    */
  def manage[R, S >: R : CanManage](r: => R): ManagedResource[R] = new DefaultManagedResource[R, S](r)

  /**
    * Manage an `AutoClosable`. This will call `AutoClosable.close` during the
    * on-finally execution lifecycle. The on-exception lifecycle hook is a no-op.
    *
    * @param r the resource the manage
    * @tparam R the type of the resource passed to the applied expression
    * @return a managed resource
    */
  def closeOnFinally[R <: AutoCloseable](r: => R): ManagedResource[R] = new DefaultManagedResource(r)

  /**
    * Manage an `AutoClosable`. This will call `AutoClosable.close` during the
    * on-exception execution lifecycle. The on-finally lifecycle hook is a no-op.
    *
    * @param r the resource the manage
    * @tparam R the type of the resource passed to the applied expression
    * @return a managed resource
    */
  def closeOnException[R <: AutoCloseable](r: => R): ManagedResource[R] = {
    implicit val manager: CanManage[AutoCloseable] = CanManage.CloseOnException
    new DefaultManagedResource(r)
  }

}
