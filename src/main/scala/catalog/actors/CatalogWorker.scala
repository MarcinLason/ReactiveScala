package catalog.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import catalog.{ProductCatalog, SearchResults}

class CatalogWorker(val id: Int, val creator: ActorRef, var productCatalog: ProductCatalog) extends Actor
  with ActorLogging {

  import CatalogWorker._

  def receive: Receive = LoggingReceive {
    case SearchForItems(words) =>
      sender() ! SearchResults(productCatalog.searchForItems(words))
  }
}

object CatalogWorker {
  case class SearchForItems(words: List[String])
}
