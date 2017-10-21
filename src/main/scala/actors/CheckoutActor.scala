package actors

import akka.actor.Actor


object CheckoutActor {
  case class CheckoutTimerExpired()
  case class PaymentTimerExpired()
  case class DeliveryMethodSelected()
  case class PaymentSelected()
  case class PaymentReceived()
}


class CheckoutActor extends Actor {
  override def receive = {
    case _ => println("Got something")
  }
}

