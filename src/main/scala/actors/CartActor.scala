package actors

import java.util.concurrent.TimeUnit

import actors.CartActor._
import akka.actor.{Actor, Timers}

import scala.concurrent.duration.FiniteDuration

object CartActor {

  case class ItemAdded()

  case class ItemRemoved()

  case class CartTimerExpired()

  case class CheckoutStarted()

  case class CheckoutCancelled()

  case class CheckoutClosed()

}

class CartActor extends Actor with Timers {

  val timeToDumpTheBucket = new FiniteDuration(15, TimeUnit.SECONDS)
  val timerKey = "timerKey"
  var itemCounter = 0

  override def receive = empty

  def empty: Receive = {

    case ItemAdded => {
      itemCounter = itemCounter + 1
      println("Item added. " + itemCounter + " items in the Bucket.")
      timers.startSingleTimer(timerKey, CartTimerExpired, timeToDumpTheBucket)
      context.become(nonempty)
    }

    case _ => {
      println("The bucket is empty.")
    }
  }

  def nonempty: Receive = {

    case ItemAdded => {
      itemCounter = itemCounter + 1
      timers.startSingleTimer(timerKey, CartTimerExpired, timeToDumpTheBucket)
      println("Item added. " + itemCounter + " items in the Bucket.")
    }

    case ItemRemoved => {
      itemCounter = itemCounter - 1
      timers.startSingleTimer("timerKey", CartTimerExpired, timeToDumpTheBucket)
      println("Item removed. " + itemCounter + " items in the Bucket.")

      if (itemCounter <= 0) {
        println("The bucket is empty.")
        timers.cancel(timerKey)
        context.become(empty)
      }
    }

    case CartTimerExpired => {
      println("CartTimerExpired: the bucket will be dumped.")
      itemCounter = 0
      context.become(empty)
    }

    case CheckoutStarted => {
      println("Checkout Started.")
      timers.cancel(timerKey)
      context.become(incheckout)
    }

    case _ => {
      println("Bucket contains " + itemCounter + " items.")
    }
  }

  def incheckout: Receive = {

    case CheckoutClosed => {
      println("Checkout closed. The bucket is empty.")
      itemCounter = 0;
      context.become(empty)
    }

    case CheckoutCancelled => {
      println("Checkout cancelled. " + itemCounter + " items in the Bucket.")
      timers.startSingleTimer("timerKey", CartTimerExpired, timeToDumpTheBucket)
      context.become(nonempty)
    }

    case _ => {
      println("Bucket in checkout mode.")
    }
  }
}


