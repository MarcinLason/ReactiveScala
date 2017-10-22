package actors

import actors.CartActor._
import akka.actor.FSM

object CartActor {

  sealed trait State

  case object Empty extends State

  case object NonEmpty extends State

  case object InCheckout extends State

  case class ItemAdded()

  case class ItemRemoved()

  case class CartTimerExpired()

  case class CheckoutStarted()

  case class CheckoutCancelled()

  case class CheckoutClosed()

}

class CartActor extends FSM[State, Int] {

  startWith(Empty, 0)

  onTransition {
    case Empty -> NonEmpty => {
      println("Zmieniam stan z Empty na NonEmpty")
    }
    case NonEmpty -> Empty => {
      println("Zmieniam stan z NonEmpty na Empty")
    }
    case NonEmpty -> InCheckout => {
      println("Zmieniam stan z NonEmpty na InCheckout")
    }
    case InCheckout -> Empty => {
      println("Zmieniam stan z InCheckout na Empty")
    }
  }

  when(Empty) {
    case Event(ItemAdded, 0) => {
      goto(NonEmpty) using (1)
    }
  }

  when(NonEmpty) {
    case Event(_,_) => {
      println("Non empty")
      stay() using (2)
    }
  }

  initialize()
}


