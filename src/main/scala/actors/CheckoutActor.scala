package actors

import java.util.concurrent.TimeUnit

import actors.CheckoutActor._
import akka.actor.{Actor, Timers}

import scala.concurrent.duration.FiniteDuration


object CheckoutActor {

  case class CheckoutTimerExpired()

  case class PaymentTimerExpired()

  case class DeliveryMethodSelected()

  case class PaymentSelected()

  case class PaymentReceived()

  case class Start()

  case class Cancelled()

}


class CheckoutActor extends Actor with Timers {

  val timeToDumpTheCheckout = new FiniteDuration(30, TimeUnit.SECONDS)
  val timeToDumpThePayment = new FiniteDuration(30, TimeUnit.SECONDS)
  val checkoutTimerKey = "checkoutTimerKey"
  val paymentTimerKey = "paymentTimerKey"
  var itemCounter = 3

  override def receive = selectingDelivery

  def selectingDelivery: Receive = {

    case Start => {
      timers.startSingleTimer(checkoutTimerKey, CheckoutTimerExpired, timeToDumpTheCheckout)
      println("Checkout process started.")
    }

    case Cancelled => {
      println("Checkout cancelled while selecting delivery.")
      timers.cancel(checkoutTimerKey)
      context.become(cancelled)
    }

    case CheckoutTimerExpired => {
      println("Checkout time expired, it will be cancelled.")
      context.become(cancelled)
    }

    case DeliveryMethodSelected => {
      println("Delivery method selected.")
      context.become(selectingPaymentMethod)
    }

    case _ => {
      println("Selecting delivery mode.")
    }
  }

  def selectingPaymentMethod: Receive = {

    case Cancelled => {
      println("Checkout cancelled while selecting payment method.")
      timers.cancel(checkoutTimerKey)
      context.become(cancelled)
    }

    case CheckoutTimerExpired => {
      println("Checkout time expired, it will be cancelled.")
      context.become(cancelled)
    }

    case PaymentSelected => {
      println("Payment method selected.")
      timers.cancel(checkoutTimerKey)
      timers.startSingleTimer(paymentTimerKey, PaymentTimerExpired, timeToDumpThePayment)
      context.become(processingPayment)
    }

    case _ => {
      println("Selecting payment method mode.")
    }
  }

  def processingPayment: Receive = {

    case Cancelled => {
      println("Payment cancelled while processing.")
      timers.cancel(paymentTimerKey)
      context.become(cancelled)
    }

    case PaymentTimerExpired => {
      println("Payment time expired, it will be cancelled.")
      context.become(cancelled)
    }

    case PaymentReceived => {
      timers.cancel(paymentTimerKey)
      println("Payment received.")
      context.become(closed)
    }

    case _ => {
      println("Processing payment mode.")
    }
  }

  def cancelled: Receive = {
    case _ => {
      println("Transaction cancelled.")
      context.stop(self)
    }
  }

  def closed: Receive = {
    case _ => {
      println("Transaction closed.")
      context.stop(self)
    }
  }
}

