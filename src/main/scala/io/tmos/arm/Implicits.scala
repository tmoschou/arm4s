package io.tmos.arm

/**
  * Implicit Methods for management of a resource.
  *
  * For explicit management see `io.tmos.arm.ArmMethods`.
  */
object Implicits {

  /**
    * Provide implicit methods to manage a generic resource.
    *
    * Also requires an implicit `CanManage[S]` in scope. A default manager for
    * `AutoClosable`s is provided (See `CanManage.CloseOnFinally`) and the
    * default behaviour is equivalently the same as `closeOnFinally`
    *
    * @param r the resource to managed passed by-name
    * @tparam R the type of the resource passed to the applied expression
    */
  implicit class ImplicitManageable[R](r: =>  R) {

    /**
      * Converts this resource into a generic managed resource.
      *
      * @tparam S the type of the resource passed to the manager
      * @return the managed resource
      */
    def manage[S >: R](implicit canManage: CanManage[S]): ManagedResource[R] = ArmMethods.manage(r)
  }

  /**
    * Provide implicit methods to manage an AutoClosable resource.
    *
    * @param r the resource to managed passed by-name
    * @tparam R the type of the resource passed to the applied expression
    */
  implicit class ImplicitAutoClosable[R <: AutoCloseable](r: => R) {

    /**
      * Convert this AutoClosable into a managed resource. Will call
      * `AutoClosable.close` during the on-finally execution lifecycle.
      * The on-exception lifecycle hook is a no-op.
      *
      * @return the managed resource
      */
    def closeOnFinally: ManagedResource[R] = ArmMethods.closeOnFinally(r)

    /**
      * Convert this AutoClosable into a managed resource. Will call
      * `AutoClosable.close` during the on-exception execution lifecycle.
      * The on-finally lifecycle hook is a no-op.
      *
      * @return the managed resource
      */
    def closeOnException: ManagedResource[R] = ArmMethods.closeOnException(r)
  }

}
