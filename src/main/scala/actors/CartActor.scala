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
  var itemCounter = 0

  override def receive = empty

  def empty: Receive = {

    case ItemAdded => {
      println("Item added to the bucket")
      itemCounter = itemCounter + 1
      timers.startSingleTimer("timerKey", CartTimerExpired, timeToDumpTheBucket)
      context.become(nonempty)
    }

    case _ => {
      println("The bucket is empty")
    }
  }

  def nonempty: Receive = {

    case ItemAdded => {
      itemCounter = itemCounter + 1
      println("Item added. " + itemCounter + " items in the Bucket.")
    }

    case ItemRemoved => {
      itemCounter = itemCounter - 1
      println("Item removed. " + itemCounter + " items in the Bucket.")

      if (itemCounter <= 0) {
        println("The bucket is empty.")
        context.become(empty)
      }
    }

    case CartTimerExpired => {
      println("CartTimerExpired: the bucket is empty.")
      itemCounter = 0
      context.become(empty)
    }

    case CheckoutStarted => {
      println("Checkout Started.")
      context.become(incheckout)
    }

    case _ => {
      println("I'm nonempty")
    }
  }

  def incheckout: Receive = {

    case CheckoutClosed => {
      println("Checkout closed. The bucket is empty.")
      itemCounter = 0;
      context.become(empty)
    }

    case CheckoutCancelled => {
      println("Checkout cancelled.")
      context.become(nonempty)
    }

    case _ => {
      print("I'm incheckout")
    }
  }
}


