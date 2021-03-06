package moorka.rx.binding

import moorka.rx._

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Aleksey Fomkin <aleksey.fomkin@gmail.com>
 */
class ExpressionBinding[A](dependencies: Seq[Channel[_]])
                          (expression: => A)
                          (implicit executor: ExecutionContext)

  extends Channel[A] with State[A] {

  private var value = expression

  private var _valid = true

  private val subscriptions = dependencies.map {
    _ subscribe { _ =>
      if (_valid) {
        _valid = false
        // Deferred event emit cause more
        // than one dependency could be changed
        Future(emit(apply()))
      }
    }
  }

  def apply(): A = {
    if (!_valid) {
      value = expression
      _valid = true
    }
    value
  }

  override def kill(): Unit = {
    subscriptions.foreach(_.kill())
    super.kill()
  }
}
