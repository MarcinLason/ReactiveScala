package actors

import java.util.concurrent.TimeUnit

import actors.PaymentServiceActor._
import akka.actor.{ActorRef, FSM}
import utils.Message.{DoPayment, PaymentConfirmed, PaymentReceived, Start}

import scala.concurrent.duration.FiniteDuration

object PaymentServiceActor {

  sealed trait State

  case object Uninitialized extends State

  case object WaitingForPayment extends State

  case object Closed extends State

  val timeToTerminate = new FiniteDuration(2, TimeUnit.SECONDS)
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
      goto(Closed)
    }
  }

  when(Closed, stateTimeout = timeToTerminate) {
    case Event(StateTimeout, _) => {
      log.debug("PaymentServiceActor: Transaction closed.")
      context.stop(self)
      stay()
    }
  }
}
