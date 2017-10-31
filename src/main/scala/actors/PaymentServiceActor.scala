package actors

import actors.PaymentServiceActor.{State, Uninitialized, WaitingForPayment}
import akka.actor.{ActorRef, FSM}
import utils.Message.{DoPayment, PaymentConfirmed, PaymentReceived, Start}

object PaymentServiceActor {

  sealed trait State

  case object Uninitialized extends State

  case object WaitingForPayment extends State

}

class PaymentServiceActor extends FSM[State, Any] {

  startWith(Uninitialized, null)

  when(Uninitialized) {
    case Event(Start, _) => {
      log.debug("PaymentServiceActor: Actor initialized.")
      val checkoutActor = context.sender()
      goto(WaitingForPayment) using checkoutActor
    }
  }

  when(WaitingForPayment) {
    case Event(DoPayment, checkoutActor) => {
      log.debug("PaymentServiceActor: Got payment to handle.")
      context.sender() ! PaymentConfirmed
      checkoutActor.asInstanceOf[ActorRef] ! PaymentReceived
      stay()
    }
  }

}
