import actors.CheckoutActor
import akka.actor.{ActorRef, ActorSystem, Props}
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
      cartActor.send(checkoutActor, Start)
      customerActor.send(checkoutActor, DeliveryMethodSelected)
      customerActor.send(checkoutActor, PaymentSelected)
      customerActor.expectMsg(PaymentServiceStarted(paymentServiceActor.asInstanceOf[ActorRef]))
      paymentServiceActor.send(checkoutActor, PaymentReceived)

      cartActor.expectMsg(CheckOutClosed)
      customerActor.expectMsg(CheckOutClosed)
    }
  }

}
