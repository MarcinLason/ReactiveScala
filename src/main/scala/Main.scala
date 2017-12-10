import actors.remote.ProductCatalog
import actors.{Customer, PaymentService}
import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import database.ProductDatabase
import messages.CustomerMessages.{Restore, Start}
import messages.ProductCatalogMessages.SearchQuery

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val clientSystem = ActorSystem("LocalSystem", config.getConfig("clientapp").withFallback(config))
    val remoteSystem = ActorSystem("RemoteSystem", config.getConfig("serverapp").withFallback(config))

    val catalog = remoteSystem.actorOf(Props(new ProductCatalog(new ProductDatabase)), "catalog")
    val customer = clientSystem.actorOf(Props[Customer])
    val paymentService = clientSystem.actorOf(Props[PaymentService])

    customer ! Start
//    customer ! Restore
//    customer ! SearchQuery(List("Nike", "Roshe"))

    Await.result(clientSystem.whenTerminated, Duration.Inf)
  }
}
