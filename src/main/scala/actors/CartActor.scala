package actors

import akka.actor.Actor

object CartActor {
  case class ItemAdded()
  case class ItemRemoved()
  case class CartTimerExpired()
  case class CheckoutStarted()
  case class CheckoutCancelled()
  case class CheckoutClosed()
}

class CartActor extends Actor {

  override def receive = {
    case _ => println("Got something")
  }
}


