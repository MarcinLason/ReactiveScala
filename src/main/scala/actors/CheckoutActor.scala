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
  case object CancelledCheckout extends State

  val checkoutTimerKey = "checkoutTimerKey"
  val timeToDumpTheCheckout = new FiniteDuration(60, TimeUnit.SECONDS)
  val timeToDumpThePayment = new FiniteDuration(60, TimeUnit.SECONDS)
  val timeToTerminate = new FiniteDuration(2, TimeUnit.SECONDS)

  var customer:ActorRef = null
}

class CheckoutActor extends FSM[State, Any] {

  startWith(Uninitialized, null)

  when (Uninitialized) {
    case Event(StartCheckoutActor(customerActor), _) => {
      setTimer(checkoutTimerKey, CheckoutTimerExpired, timeToDumpTheCheckout)
      log.debug("CheckoutActor: Checkout created.")
      customer = customerActor
      goto(SelectingDelivery) using context.sender()
    }
  }

  when(SelectingDelivery) {
    case Event(DeliveryMethodSelected, cart) => {
      log.debug("CheckoutActor: Delivery method selected.")
      goto(SelectingPaymentMethod) using cart
    }
    case Event(Cancelled, cart) => {
      log.debug("CheckoutActor: Checkout cancelled while selecting delivery.")
      cancelTimer(checkoutTimerKey)
      goto(CancelledCheckout) using cart
    }
    case Event(CheckoutTimerExpired, cart) => {
      log.debug("CheckoutActor: Checkout time expired, it will be cancelled.")
      goto(CancelledCheckout) using cart
    }
  }

  when(SelectingPaymentMethod) {
    case Event(PaymentSelected, cart) => {
      log.debug("CheckoutActor: Payment method selected.")
      cancelTimer(checkoutTimerKey)
      val paymentService = context.system.actorOf(Props[PaymentServiceActor], "paymentServiceActor")
      paymentService ! Start
      context.sender() ! PaymentServiceStarted(paymentService)
      goto(ProcessingPayment) using cart
    }
    case Event(Cancelled, cart) => {
      log.debug("CheckoutActor: Checkout cancelled while selecting payment method.")
      cancelTimer(checkoutTimerKey)
      goto(CancelledCheckout) using cart
    }
    case Event(CheckoutTimerExpired, cart) => {
      log.debug("CheckoutActor: Checkout time expired, it will be cancelled.")
      goto(CancelledCheckout) using cart
    }
  }

  when(ProcessingPayment, stateTimeout = timeToDumpThePayment) {
    case Event(PaymentReceived, cart) => {
      log.debug("CheckoutActor: Payment received.")
      customer ! CheckOutClosed
      cart.asInstanceOf[ActorRef] ! CheckOutClosed
      goto(Closed) using cart
    }
    case Event(Cancelled, cert) => {
      log.debug("CheckoutActor: Payment cancelled while processing.")
      goto(CancelledCheckout) using cert
    }
    case Event(StateTimeout, cert) => {
      log.debug("CheckoutActor: Payment time expired, it will be cancelled.")
      goto(CancelledCheckout) using cert
    }
  }

  when(Closed, stateTimeout = timeToTerminate) {
    case Event(StateTimeout, _) => {
      log.debug("CheckoutActor: Transaction closed.")
      context.stop(self)
      stay()
    }
  }

  when(CancelledCheckout, stateTimeout = timeToTerminate) {
    case Event(StateTimeout, cart) => {
      log.debug("CheckoutActor: Transaction cancelled.")
      cart.asInstanceOf[ActorRef] ! CheckoutCancelled
      customer ! CheckoutTerminated(cart.asInstanceOf[ActorRef])
      context.stop(self)
      stay()
    }
  }
}

