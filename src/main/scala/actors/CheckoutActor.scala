package actors

import java.util.concurrent.TimeUnit

import actors.CheckoutActor._
import utils.Message._
import akka.actor.{ActorRef, FSM, Props}

import scala.concurrent.duration.FiniteDuration

object CheckoutActor {

  sealed trait State
  case object Uninitialized extends State
  case object SelectingDelivery extends State
  case object SelectingPaymentMethod extends State
  case object ProcessingPayment extends State
  case object Closed extends State
  case object Cancelled extends State

  val checkoutTimerKey = "checkoutTimerKey"
  val timeToDumpTheCheckout = new FiniteDuration(60, TimeUnit.SECONDS)
  val timeToDumpThePayment = new FiniteDuration(60, TimeUnit.SECONDS)
  val timeToTerminate = new FiniteDuration(2, TimeUnit.SECONDS)

  var customer:ActorRef = null
}

class CheckoutActor extends FSM[State, Any] {

  startWith(Uninitialized, null)

  when (Uninitialized) {
    case Event(Start, _) => {
      setTimer(checkoutTimerKey, CheckoutTimerExpired, timeToDumpTheCheckout)
      log.debug("CheckoutActor: Checkout created.")
      goto(SelectingDelivery) using context.sender()
    }
  }

  when(SelectingDelivery) {
    case Event(DeliveryMethodSelected, cart) => {
      log.debug("CheckoutActor: Delivery method selected.")
      goto(SelectingPaymentMethod) using cart
    }
    case Event(Cancel, cart) => {
      log.debug("CheckoutActor: Checkout cancelled while selecting delivery.")
      cancelTimer(checkoutTimerKey)
      goto(Cancelled) using cart
    }
    case Event(CheckoutTimerExpired, cart) => {
      log.debug("CheckoutActor: Checkout time expired, it will be cancelled.")
      goto(Cancelled) using cart
    }
  }

  when(SelectingPaymentMethod) {
    case Event(PaymentSelected, cart) => {
      log.debug("CheckoutActor: Payment method selected.")
      cancelTimer(checkoutTimerKey)
      val paymentService = context.system.actorOf(Props[PaymentServiceActor], "paymentServiceActor")
      paymentService ! Start
      customer = context.sender()
      context.sender() ! PaymentServiceStarted(paymentService)
      goto(ProcessingPayment) using cart
    }
    case Event(Cancel, cart) => {
      log.debug("CheckoutActor: Checkout cancelled while selecting payment method.")
      cancelTimer(checkoutTimerKey)
      goto(Cancelled) using cart
    }
    case Event(CheckoutTimerExpired, cart) => {
      log.debug("CheckoutActor: Checkout time expired, it will be cancelled.")
      goto(Cancelled) using cart
    }
  }

  when(ProcessingPayment, stateTimeout = timeToDumpThePayment) {
    case Event(PaymentReceived, cart) => {
      log.debug("CheckoutActor: Payment received.")
      customer ! CheckOutClosed
      cart.asInstanceOf[ActorRef] ! CheckOutClosed
      goto(Closed) using cart
    }
    case Event(Cancel, cert) => {
      log.debug("CheckoutActor: Payment cancelled while processing.")
      goto(Cancelled) using cert
    }
    case Event(StateTimeout, cert) => {
      log.debug("CheckoutActor: Payment time expired, it will be cancelled.")
      goto(Cancelled) using cert
    }
  }

  when(Closed, stateTimeout = timeToTerminate) {
    case Event(StateTimeout, _) => {
      log.debug("CheckoutActor: Transaction closed.")
      context.stop(self)
      stay()
    }
  }

  when(Cancelled, stateTimeout = timeToTerminate) {
    case Event(StateTimeout, _) => {
      log.debug("CheckoutActor: Transaction cancelled.")
      context.stop(self)
      stay()
    }
  }
}

