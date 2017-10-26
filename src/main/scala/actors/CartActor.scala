package actors

import java.util.concurrent.TimeUnit

import actors.CartActor._
import akka.actor.FSM

import scala.concurrent.duration.FiniteDuration

object CartActor {

  sealed trait State
  case object Empty extends State
  case object NonEmpty extends State
  case object InCheckout extends State

  case class ItemAdded()
  case class ItemRemoved()
  case class CartTimerExpired()
  case class CheckoutStarted()
  case class CheckoutCancelled()
  case class CheckoutClosed()

  val timeToDumpTheBucket = new FiniteDuration(15, TimeUnit.SECONDS)
}

class CartActor extends FSM[State, Int] {

  startWith(Empty, 0)

  when(Empty) {
    case Event(ItemAdded, 0) => {
      println("Item added. 1 item in the bucket.")
      goto (NonEmpty) using 1
    }
  }

  when(NonEmpty, stateTimeout = timeToDumpTheBucket) {
    case Event(ItemAdded, itemCounter:Int) => {
      println("Item added. " + (itemCounter + 1) + " items in the bucket.")
      stay() using (itemCounter + 1)
    }

    case Event(ItemRemoved, 1) => {
      println("Item removed. The bucket is empty.")
      goto (Empty) using 0
    }

    case Event(ItemRemoved, itemCounter:Int) => {
      println("Item removed. " + (itemCounter - 1) + " items in the bucket.")
      stay() using (itemCounter - 1)
    }

    case Event(StateTimeout, _) => {
      println("CartTimerExpired: the bucket will be dumped.")
      goto (Empty) using 0
    }

    case Event(CheckoutStarted, itemCounter:Int) => {
      println("Checkout started.")
      goto (InCheckout) using itemCounter
    }
  }

  when(InCheckout) {
    case Event(CheckoutCancelled, itemCounter:Int) => {
      println("Checkout cancelled. " + itemCounter + " items in the Bucket.")
      goto (NonEmpty) using itemCounter
    }

    case Event(CheckoutClosed, _) => {
      println("Checkout closed. The bucket is empty.")
      goto (Empty) using 0
    }
  }

  initialize()
}


