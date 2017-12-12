package catalog.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import catalog.{ProductCatalog, SearchResults}

class ProductCatalogWorkerActor(val id: Int, val creator: ActorRef, var productCatalog: ProductCatalog) extends Actor with ActorLogging {

  import ProductCatalogWorkerActor._

  def receive: Receive = LoggingReceive {
    case SearchForItems(words) =>
      sender() ! SearchResults(productCatalog.searchForItems(words))
  }
}

object ProductCatalogWorkerActor {

  case class SearchForItems(words: List[String])

}
