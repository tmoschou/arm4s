package io.tmos.arm

package object implicits {
  import scala.language.implicitConversions
  implicit def manageImplicit[R: CanManage](r: => R): ManagedResource[R] = new DefaultManagedResource(r)
}
