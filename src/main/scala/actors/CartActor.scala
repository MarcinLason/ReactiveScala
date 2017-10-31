package actors

import java.util.concurrent.TimeUnit

import actors.CartActor._
import akka.actor.{FSM, Props}
import utils.Message._

import scala.concurrent.duration.FiniteDuration

object CartActor {

  sealed trait State

  case object Empty extends State

  case object NonEmpty extends State

  case object InCheckout extends State

  val timeToDumpTheBucket = new FiniteDuration(15, TimeUnit.SECONDS)
}

class CartActor extends FSM[State, Int] {

  startWith(Empty, 0)

  when(Empty) {
    case Event(AddItem, 0) => {
      log.debug("CartActor: Item added. 1 item in the bucket.")
      goto(NonEmpty) using 1
    }
  }

  when(NonEmpty, stateTimeout = timeToDumpTheBucket) {
    case Event(AddItem, itemCounter: Int) => {
      log.debug("CartActor: Item added. " + (itemCounter + 1) + " items in the bucket.")
      stay() using (itemCounter + 1)
    }

    case Event(RemoveItem, 1) => {
      log.debug("CartActor: Item removed. The bucket is empty.")
      goto(Empty) using 0
    }

    case Event(RemoveItem, itemCounter: Int) => {
      log.debug("CartActor: Item removed. " + (itemCounter - 1) + " items in the bucket.")
      stay() using (itemCounter - 1)
    }

    case Event(StartCheckOut, itemCounter: Int) => {
      log.debug("CartActor: Checkout started.")
      val checkout = context.system.actorOf(Props[CheckoutActor], "checkoutActor")
      checkout ! Start
      context.sender() ! CheckOutStarted(checkout)
      goto(InCheckout) using itemCounter
    }

    case Event(StateTimeout, _) => {
      log.debug("CartActor: CartTimerExpired - the bucket will be dumped.")
      goto(Empty) using 0
    }
  }

  when(InCheckout) {
    case Event(CheckoutCancelled, itemCounter: Int) => {
      log.debug("CartActor: Checkout cancelled. " + itemCounter + " items in the Bucket.")
      goto(NonEmpty) using itemCounter
    }

    case Event(CheckOutClosed, _) => {
      log.debug("CartActor: Checkout closed. The bucket is empty.")
      goto(Empty) using 0
    }
  }
}


