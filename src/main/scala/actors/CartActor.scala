package actors

import java.util.concurrent.TimeUnit

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

  override def receive = empty

  def empty: Receive = {
    case _ => {
      println("Jestem empty")
      timers.startSingleTimer(timerAction(), "15 seconds later...", timeToDumpTheBucket)
      context.become(full)
    }
  }

  def nonempty: Receive = {
    case _ => {
      println("Jestem nonempty")
    }
  }

  def incheckout: Receive = {
    case _ => {
      print("Jestem inCheckout")
    }
  }

  def full: Receive = {
    case _ => {
      println("Jestem full")
      context.become(empty)
    }
  }

  def timerAction(): Unit = {
    println("Timer zakonczony")
  }
}


