package actors

import akka.actor.{Actor, Timers}


object CheckoutActor {

  case class CheckoutTimerExpired()

  case class PaymentTimerExpired()

  case class DeliveryMethodSelected()

  case class PaymentSelected()

  case class PaymentReceived()

}


class CheckoutActor extends Actor with Timers {
  override def receive = {
    case _ => println("Got something")
  }
}

