package actors

import _root_.messages.CartManagerMessages.{CheckoutCanceled, CheckoutClosed}
import _root_.messages.CheckoutMessages.{DeliveryMethodSelected, PaymentReceived, PaymentSelected}
import _root_.messages.CustomerMessages.PaymentServiceStarted
import akka.actor.{Actor, Props}
import akka.testkit.TestProbe

import scala.concurrent.duration._

class CheckoutTest extends TestUtils {

  "Checkout actor" must {
    "handle closing checkout" in {
      val testProbe = TestProbe()
      val parent = system.actorOf(Props(new Actor {
        private val checkoutActor = context.actorOf(Props(new Checkout(System.currentTimeMillis().toString)))

        override def receive = {
          case x if sender == checkoutActor => testProbe.ref forward x
          case x => checkoutActor forward x
        }
      }))
      testProbe.send(parent, DeliveryMethodSelected)
      testProbe.send(parent, PaymentSelected)
      testProbe.expectMsgType[PaymentServiceStarted]
      testProbe.send(parent, PaymentReceived)
      testProbe.expectMsgType[CheckoutClosed](15.seconds)
    }

    "terminate checkout after payment timeout" in {
      val testProbe = TestProbe()
      val parent = system.actorOf(Props(new Actor {
        private val checkoutActor = context.actorOf(Props(new Checkout(System.currentTimeMillis().toString)))

        override def receive = {
          case x if sender == checkoutActor => testProbe.ref forward x
          case x => checkoutActor forward x
        }
      }))
      testProbe.send(parent, DeliveryMethodSelected)
      testProbe.expectMsgType[CheckoutCanceled](15.seconds)
    }

    "terminate checkout after delivery method timeout" in {
      val testProbe = TestProbe()
      system.actorOf(Props(new Actor {
        private val checkoutActor = context.actorOf(Props(new Checkout(System.currentTimeMillis().toString)))

        override def receive = {
          case x if sender == checkoutActor => testProbe.ref forward x
          case x => checkoutActor forward x
        }
      }))
      testProbe.expectMsgType[CheckoutCanceled](15.seconds)
    }
  }
}
