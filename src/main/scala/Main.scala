import actors.PaymentServer.PayPal
import actors.remote.ProductCatalog
import actors.{Customer, PaymentService}
import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import database.ProductDatabase
import messages.CustomerMessages.{Continue, Start}
import messages.PaymentServiceMessages.DoPayment
import messages.ProductCatalogMessages.SearchQuery

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val clientSystem = ActorSystem("SSystem", config.getConfig("clientapp").withFallback(config))
    val remoteSystem = ActorSystem("SSystem", config.getConfig("serverapp").withFallback(config))

    val catalog = remoteSystem.actorOf(Props(new ProductCatalog(new ProductDatabase)), "catalog")
    val customer = clientSystem.actorOf(Props[Customer])
    val paymentService = clientSystem.actorOf(Props[PaymentService])


    customer ! Start
//    paymentService ! DoPayment(PayPal)
//    customer ! SearchQuery(List("Bigfoot", "Ale"))
//    customer ! Start
//    customer ! Continue

    Await.result(clientSystem.whenTerminated, Duration.Inf)
  }
}
