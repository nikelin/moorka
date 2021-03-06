package moorka.ui.components

import moorka.ui.element.{ElementBase, ElementExtension}
import moorka.ui.components.html.div
import moorka.rx._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * @author Aleksey Fomkin <aleksey.fomkin@gmail.com>
 */
class FutureElement(f: Future[ElementBase])
                   (implicit ec: ExecutionContext) extends ElementExtension {
  val tempEl = div()
  def start(target: ElementBase): Unit = {
    target.ref.appendChild(tempEl.ref)
    f onComplete {
      case Success(newEl) ⇒
        target.ref.replaceChild(
          newEl.ref,
          tempEl.ref
        )
        tempEl.kill()
      case Failure(ex) ⇒ 
        throw ex
    }
  }
}