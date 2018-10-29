package io.tmos.arm

/**
  * For encapsulating the management logic of a resource.
  *
  * Default logic for any `java.lang.AutoClosable` is provided by the companion object,
  * which may be imported into current scope as implicits.
  *
  * Other types may be provided in scope by the user. For example
  * {{{
  *   import java.util.concurrent._
  *   import io.tmos.arm.Implicits._
  *
  *   implicit val manager: CanManage[ExecutorService] = new CanManage[ExecutorService] {
  *     override def onFinally(pool: ExecutorService): Unit = {
  *       pool.shutdown() // Disable new tasks from being submitted
  *       try {
  *         if (!pool.awaitTermination(10, TimeUnit.SECONDS)) { // wait for normal termination
  *           pool.shutdownNow() // force terminate
  *           if (!pool.awaitTermination(10, TimeUnit.SECONDS)) // wait for forced termination
  *             throw new RuntimeException("ExecutorService did not terminate")
  *         }
  *       } catch {
  *         case _: InterruptedException =>
  *           pool.shutdownNow() // (Re-)Cancel if current thread also interrupted
  *           Thread.currentThread().interrupt() // Preserve interrupt status
  *       }
  *     }
  *     override def onException(r: ExecutorService): Unit = {}
  *   }
  *
  *   for (manage(executorService) <- Executors.newSingleThreadExecutor.manage) { ... }
  * }}}
  *
  * @tparam R the type of the resource to manage
  */
trait CanManage[-R] {

  /**
    * Execution hook called after the managed block.
    *
    * This execution hook is called regardless if an exception is thrown.
    *
    * Usually resources are released or closed in the lifecycle.
    *
    * Implementors are free to permit exceptions thrown from this method, however
    * it is strongly advised to not have the
    * method throw `java.lang.InterruptedException`. This exception interacts with a thread's
    * interrupted status, and runtime misbehavior is likely to occur if an `java.lang.InterruptedException`
    * is suppressed. More generally, if it would cause problems for an exception to be suppressed,
    * the AutoCloseable.close method should not throw it."
    *
    * @param r the resource being managed
    */
  def onFinally(r: R): Unit = {}

  /**
    * Execution hook called when an exception is thrown from the managed
    * block. This is executed prior to [onFinally].
    *
    * Implementors are free to permit exceptions thrown from this method, however
    * note that any new exceptions thrown will be added as
    * a suppressed exception of the currently throwing exception.
    * Thus it is strongly advised that implementors do not throw any exceptions
    * if it would cause problems for an exception  to be suppressed.
    *
    * @param r the resource being managed
    */
  def onException(r: R): Unit = {}
}


/**
 * Companion object to the CanManage type trait.
 *
 * Contains common implementations of CanManage for AutoClosable Resources
 */
object CanManage {

  /**
    * Always call close on a AutoClosable after applied block,
    * regardless if block throws an exception or not. This is identical to
    * Java's try-with-resources.
    */
  implicit object CloseOnFinally extends CanManage[AutoCloseable] {
    override def onFinally(r: AutoCloseable): Unit = if (r != null) r.close()
  }

  /**
    * Call close on a resource only if a exception is thrown in the applied block.
    * This is useful for instance if closing a resource needs to be delegated
    * elsewhere under normal circumstances, but abnormal circumstances should be
    * handled in the current scope. Such examples may include managing a resource
    * across threads.
    */
  object CloseOnException extends CanManage[AutoCloseable] {
    override def onException(r: AutoCloseable): Unit = if (r != null) r.close()
  }

}
