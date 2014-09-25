package com.tenderowls.moorka.mkml.dom

import com.tenderowls.moorka.core._
import com.tenderowls.moorka.mkml.engine._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

import scala.concurrent.Future
import scala.scalajs.js

sealed trait ElementExtension extends SyntheticDomNode with Mortal {

  protected var element:ComponentBase = null

  def assignElement(element: ComponentBase): Unit = {
    this.element = element
  }

  override def kill(): Unit = {

  }
}

case class ElementAttributeName(name: String) {

  def :=(x: String) = ElementAttributeExtension(name, x)

  def :=(x: Bindable[String]) = ElementBoundAttributeExtension(name, x)
}

case class ElementEventName[EventType <: SyntheticEvent](processor: SyntheticEventProcessor[EventType]) {

  def listen(listener: (EventType) => Unit) = {
    SyntheticEventExtension[EventType](
      processor,
      listener,
      useCapture = false
    )
  }

  def listen(listener: => Unit) = {
    SyntheticEventExtension[EventType](
      processor,
      _ => listener,
      useCapture = false
    )
  }

  def capture(listener: (EventType) => Unit) = {
    SyntheticEventExtension[EventType](
      processor,
      listener,
      useCapture = true
    )
  }
}

case class ElementPropertyName[A](name: String) {

  def :=(x: A) = ElementPropertyExtension(name, x)

  def :=(x: Bindable[A]) = ElementBoundPropertyExtension(name, x)

  def =:= (x: Var[A]) = VarPropertyExtension(name, x)

  def from(x: ComponentBase): Future[A] = x.ref.get(name).map(_.asInstanceOf[A])
}

class BoundExtensionFactory[A](static: (A => ElementExtension), bound: (Bindable[A]) => BoundElementExtension) 
  extends StaticExtensionFactory[A](static) {

  def :=(x: Bindable[A]) = bound(x)
}

class StaticExtensionFactory[A](static: (A => ElementExtension)) {

  def :=(x: A) = static(x)
}

case class ElementAttributeExtension(name: String, value: String) extends ElementExtension {

  override def assignElement(element: ComponentBase): Unit = {
    super.assignElement(element)
    element.ref.updateAttribute(name, value)
  }
}

case class SyntheticEventExtension[EventType <: SyntheticEvent](processor: SyntheticEventProcessor[EventType],
                                                                listener: EventType => Unit,
                                                                useCapture: Boolean)
  extends ElementExtension {

  var slot:Option[Slot[EventType]] = None
  
  override def assignElement(element: ComponentBase): Unit = {
    super.assignElement(element)
    slot = useCapture match {
      case false => Some(processor.addListener(element, listener))
      case true => Some(processor.addCapture(element, listener))
    }
  }

  override def kill(): Unit = {
    slot.foreach(_.kill())
  }
}

case class ElementPropertyExtension[A](name: String, value: A) extends ElementExtension {
  override def assignElement(element: ComponentBase): Unit = {
    super.assignElement(element)
    element.ref.set(name, value)
  }
}

case class UseClassExtension(className: String, trigger:Boolean) extends ElementExtension {
  override def assignElement(element: ComponentBase): Unit = {
    if (trigger) {
      element.ref.classAdd(className)
    }
    else {
      element.ref.classRemove(className)
    }
  }
}

sealed trait BoundElementExtension extends ElementExtension {

  protected var subscription: Event[_] = null

  override def kill(): Unit = {
    if (subscription != null)
      subscription.kill()
  }
}

case class UseClassBoundExtension(className: String, trigger:Bindable[Boolean]) 
  extends ElementExtension with BoundElementExtension {
  
  override def assignElement(element: ComponentBase): Unit = {
    subscription = trigger observe { _ =>
      trigger() match {
        case true => element.ref.classAdd(className)
        case false => element.ref.classRemove(className)
      }
    }
  }
}

case class ElementBoundPropertyExtension[A](name: String, value: Bindable[A])
  extends ElementExtension with BoundElementExtension {

  override def assignElement(element: ComponentBase): Unit = {
    super.assignElement(element)
    subscription = value observe { _ =>
      element.ref.set(name, value())
    }
  }
}

case class VarPropertyExtension[A](name: String, value: Var[A])
  extends ElementExtension {

  var subscriptions: List[Slot[_]] = Nil

  override def assignElement(element: ComponentBase): Unit = {
    super.assignElement(element)
    subscriptions ::= value observe { _ =>
      element.ref.set(name, value())
    }
    subscriptions ::= ChangeEventProcessor.addListener(element,
      _ => element.ref.get(name).onSuccess {
        case x => value() = x.asInstanceOf[A]
      }
    )
  }

  override def kill(): Unit = {
    super.kill()
    subscriptions.foreach(_.kill())
  }
}

case class ElementBoundAttributeExtension(name: String, value: Bindable[String])
  extends ElementExtension
  with BoundElementExtension {

  override def assignElement(component: ComponentBase): Unit = {
    super.assignElement(component)
    subscription = value observe { _ =>
      component.ref.updateAttribute(name, value())
    }
  }
}

