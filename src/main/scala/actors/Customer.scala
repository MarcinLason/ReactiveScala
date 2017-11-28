package actors

import java.net.URI

import actors.CartManager.Item
import actors.PaymentServer.PayPal
import akka.actor.{Actor, Props, Timers}
import akka.event.{Logging, LoggingReceive}
import messages.CartManagerMessages.{AddItem, CheckoutClosed, RemoveItem, StartCheckout}
import messages.CheckoutMessages.{DeliveryMethodSelected, PaymentSelected}
import messages.CustomerMessages._
import messages.PaymentServiceMessages.{DoPayment, PaymentConfirmed}
import messages.ProductCatalogMessages.{SearchQuery, SearchQueryResponse}

class Customer extends Actor with Timers {
  private val log = Logging(context.system, this)

  override def receive: Receive = LoggingReceive {
    case Start => {
      log.info("Customer: creating CartManager")
      val cartManager = context.actorOf(Props[CartManager])
      cartManager ! AddItem(Item(new URI("7"), "7", "", 1, BigDecimal(1.0)))
      cartManager ! AddItem(Item(new URI("11"), "11", "", 1, BigDecimal(1.0)))
      cartManager ! AddItem(Item(new URI("13"), "13", "", 1, BigDecimal(1.0)))
      cartManager ! RemoveItem(Item(new URI("11"), "11", "", 1, BigDecimal(1.0)))
      cartManager ! StartCheckout
      log.info("Customer: checkout started.")
    }

    case CheckOutStarted(checkoutActor) =>{
      log.info("Customer: checkout confirmed. Got checkout actor.")
      checkoutActor ! DeliveryMethodSelected
//      System.exit(0)
      checkoutActor ! PaymentSelected
    }

    case PaymentServiceStarted(paymentService) => {
      log.info("Customer: payment started. Got payment service actor.")
      paymentService ! DoPayment(PayPal)
    }

    case Continue =>
      val cartManager = context.actorOf(Props[CartManager])
      val checkoutActor = context.actorOf(Props[Checkout])
      checkoutActor ! PaymentSelected

    case SearchQuery(parameters) =>
      log.info("Customer: searching for product in ProductCatalog.")
      val productCatalog = context.actorSelection("akka.tcp://RemoteSystem@127.0.0.1:2552/user/catalog")
      productCatalog ! SearchQuery(parameters)

    case SearchQueryResponse(response) =>
      log.info("Customer: got response from ProductCatalog. Add " + response.head + " to the cart.")
      val cartManager = context.actorOf(Props[CartManager])
      cartManager ! AddItem(response.head)

    case CartEmpty() => {
      log.info("Customer: got CartEmpty()")
    }
    case CheckoutClosed() => {
      log.info("Customer: got CheckoutClosed()")
    }
    case PaymentConfirmed => {
      log.info("Customer: got PaymentConfirmed.")
    }
  }
}

object Customer {
  def apply: Customer = new Customer()
}
