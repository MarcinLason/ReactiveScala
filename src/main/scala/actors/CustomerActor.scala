package actors

import actors.CustomerActor._
import akka.actor.{ActorRef, FSM, Props}
import utils.Message.{CartEmpty, _}

object CustomerActor {

  sealed trait State

  case object Uninitialized extends State
  case object DuringShopping extends State
  case object DuringCheckout extends State
  case object DuringPayment extends State
  case object WaitingForFinalize extends State
}

class CustomerActor extends FSM[State, ActorRef] {

  startWith(Uninitialized, null)

  when(Uninitialized) {
    case Event(Start, _) => {
      log.debug("CustomerActor: actor started.")
      goto(DuringShopping) using context.system.actorOf(Props[CartActor], "cartActor")
    }
    case Event(CartEmpty(cartActor:ActorRef), _) => {
      log.debug("Customer:Actor: actor restarted with empty cart.")
      goto(DuringShopping) using cartActor
    }
    case Event(_, _) => {
      log.debug("CustomerActor: Can not perform action before initialization.")
      stay()
    }
  }

  when(DuringShopping) {
    case Event(AddItem, cartActor) => {
      log.debug("Customer Actor: User requested to add item.")
      cartActor ! AddItem
      stay() using cartActor
    }

    case Event(RemoveItem, cartActor) => {
      log.debug("CustomerActor: User requested to remove item.")
      cartActor ! RemoveItem
      stay() using cartActor
    }

    case Event(StartCheckOut, cartActor) => {
      log.debug("CustomerActor: User requested to start checkout.")
      cartActor ! StartCheckOut
      goto (DuringCheckout)
    }
  }

  when(DuringCheckout) {
    case Event(CheckOutStarted(checkoutActor), _) => {
      log.debug("CustomerActor: Checkout actor initialized.")
      stay() using checkoutActor
    }

    case Event(DeliveryMethodSelected, checkoutActor) => {
      log.debug("CustomerActor: Delivery method selected.")
      checkoutActor ! DeliveryMethodSelected
      stay() using checkoutActor
    }

    case Event(PaymentSelected, checkoutActor) => {
      log.debug("CustomerActor: User requested to start payment.")
      checkoutActor ! PaymentSelected
      goto(DuringPayment)
    }

    case Event(Cancel, checkoutActor) => {
      log.debug("CustomerActor: User requested to cancel checkout.")
      checkoutActor ! Cancelled
      stay()
    }

    case Event(CheckoutTerminated(cartActor), _) => {
      log.debug("CustomerActor: Checkout terminated. Please continue your shopping.")
      goto(DuringShopping) using cartActor
    }
  }

  when (DuringPayment) {
    case Event(PaymentServiceStarted(paymentServiceActor), _ ) => {
      log.debug("CustomerActor: Payment Service actor initialized.")
      stay() using paymentServiceActor
    }

    case Event(DoPayment, paymentServiceActor) => {
      log.debug("CustomerActor: Sending payment to PaymentService.")
      paymentServiceActor ! DoPayment
      stay() using paymentServiceActor
    }

    case Event(PaymentConfirmed, paymentServiceActor) => {
      log.debug("CustomerActor: Got payment confirmation.")
      goto (WaitingForFinalize)
    }
  }

  when (WaitingForFinalize) {
    case Event(CheckOutClosed, _) => {
      log.debug("CustomerActor: Transaction finalized.")
      goto(Uninitialized)
    }
  }
}
