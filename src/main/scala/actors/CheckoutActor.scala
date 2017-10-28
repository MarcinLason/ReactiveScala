package actors

import java.util.concurrent.TimeUnit

import actors.CheckoutActor._
import akka.actor.FSM

import scala.concurrent.duration.FiniteDuration

object CheckoutActor {

  sealed trait State
  case object SelectingDelivery extends State
  case object SelectingPaymentMethod extends State
  case object ProcessingPayment extends State
  case object Closed extends State
  case object Cancelled extends State

  case class CheckoutTimerExpired()
  case class DeliveryMethodSelected()
  case class PaymentSelected()
  case class PaymentReceived()
  case class Start()
  case class Cancel()

  val checkoutTimerKey = "checkoutTimerKey"
  val timeToDumpTheCheckout = new FiniteDuration(30, TimeUnit.SECONDS)
  val timeToDumpThePayment = new FiniteDuration(30, TimeUnit.SECONDS)
  val timeToTerminate = new FiniteDuration(2, TimeUnit.SECONDS)
  val nonEmptyCart = 10
}

class CheckoutActor extends FSM[State, Any] {

  startWith(SelectingDelivery, nonEmptyCart)

  when(SelectingDelivery) {
    case Event(Start, cart) => {
      setTimer(checkoutTimerKey, CheckoutTimerExpired, timeToDumpTheCheckout)
      log.debug("Start selecting delivery. " + cart + " items in the cart.")
      stay() using cart
    }
    case Event(DeliveryMethodSelected, cart) => {
      log.debug("Delivery method selected.")
      goto(SelectingPaymentMethod) using cart
    }
    case Event(Cancel, cart) => {
      log.debug("Checkout cancelled while selecting delivery.")
      cancelTimer(checkoutTimerKey)
      goto(Cancelled) using cart
    }
    case Event(CheckoutTimerExpired, cart) => {
      log.debug("Checkout time expired, it will be cancelled.")
      goto(Cancelled) using cart
    }
  }

  when(SelectingPaymentMethod) {
    case Event(PaymentSelected, cart) => {
      log.debug("Payment method selected.")
      cancelTimer(checkoutTimerKey)
      goto(ProcessingPayment) using cart
    }
    case Event(Cancel, cart) => {
      log.debug("Checkout cancelled while selecting payment method.")
      cancelTimer(checkoutTimerKey)
      goto(Cancelled) using cart
    }
    case Event(CheckoutTimerExpired, cart) => {
      log.debug("Checkout time expired, it will be cancelled.")
      goto(Cancelled) using cart
    }
  }

  when(ProcessingPayment, stateTimeout = timeToDumpThePayment) {
    case Event(PaymentReceived, cart) => {
      log.debug("Payment received.")
      goto(Closed) using cart
    }
    case Event(Cancel, cert) => {
      log.debug("Payment cancelled while processing.")
      goto(Cancelled) using cert
    }
    case Event(StateTimeout, cert) => {
      log.debug("Payment time expired, it will be cancelled.")
      goto(Cancelled) using cert
    }
  }

  when(Closed, stateTimeout = timeToTerminate) {
    case Event(StateTimeout, _) => {
      log.debug("Transaction closed.")
      context.stop(self)
      stay()
    }
  }

  when(Cancelled, stateTimeout = timeToTerminate) {
    case Event(StateTimeout, _) => {
      log.debug("Transaction cancelled.")
      context.stop(self)
      stay()
    }
  }
}

