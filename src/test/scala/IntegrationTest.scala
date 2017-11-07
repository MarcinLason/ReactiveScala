import actors.{CartActor, CheckoutActor}
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import utils.Message._

class IntegrationTest extends TestKit(ActorSystem("TestSystem")) with WordSpecLike with ImplicitSender with Matchers
  with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }


  "CheckoutActor" should {
    "inform Customer and Cart actors about closing the checkout" in {
      val cartActor = TestProbe()
      val customerActor = TestProbe()
      val paymentServiceActor = TestProbe()
      val checkoutActor = cartActor.childActorOf(Props[CheckoutActor])
      cartActor.send(checkoutActor, StartCheckoutActor(customerActor.testActor))
      customerActor.send(checkoutActor, DeliveryMethodSelected)
      customerActor.send(checkoutActor, PaymentSelected)
      customerActor.expectMsgClass(classOf[PaymentServiceStarted])
      paymentServiceActor.send(checkoutActor, PaymentReceived)

      customerActor.expectMsg(CheckOutClosed)
      cartActor.expectMsg(CheckOutClosed)

    }

    "handle Cancel event during SelectingDelivery" in {
      val cartActor = TestProbe()
      val customerActor = TestProbe()
      val checkoutActor = cartActor.childActorOf(Props[CheckoutActor])

      cartActor.send(checkoutActor, StartCheckoutActor(customerActor.testActor))
      customerActor.send(checkoutActor, Cancelled)

      cartActor.expectMsg(CheckoutCancelled)
      customerActor.expectMsgClass(classOf[CheckoutTerminated])
    }

    "handle Cancel event during SelectingPaymentMethod" in {
      val cartActor = TestProbe()
      val customerActor = TestProbe()
      val checkoutActor = cartActor.childActorOf(Props[CheckoutActor])

      cartActor.send(checkoutActor, StartCheckoutActor(customerActor.testActor))
      customerActor.send(checkoutActor, DeliveryMethodSelected)
      customerActor.send(checkoutActor, Cancelled)

      cartActor.expectMsg(CheckoutCancelled)
      customerActor.expectMsgClass(classOf[CheckoutTerminated])
    }
  }

  "Cart actor" should {
    "become empty after closing checkout." in {
      val testManager = TestProbe()
      val cartActor = testManager.childActorOf(Props[CartActor])

      testManager.send(cartActor, AddItem)
      testManager.send(cartActor, AddItem)
      testManager.send(cartActor, StartCheckOut)

      testManager.expectMsgClass(classOf[CheckOutStarted])

      testManager.send(cartActor, CheckOutClosed)
      testManager.expectMsg(CartEmpty(cartActor))
    }
  }
}
