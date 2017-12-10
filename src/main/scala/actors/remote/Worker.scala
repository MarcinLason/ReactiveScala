package actors.remote

import akka.actor.{Actor, PoisonPill}
import akka.event.Logging
import database.ProductDatabase
import messages.ProductCatalogMessages.{SearchQuery, SearchQueryResponse}

class Worker(productDatabase: ProductDatabase) extends Actor {
  private val log = Logging(context.system, this)

  override def receive: Receive = {
    case SearchQuery(queryArguments) =>
      log.info("DatabaseWorker: got SearchQuery to process.")
      sender() ! SearchQueryResponse(productDatabase.findProduct(queryArguments))
      self ! PoisonPill
  }
}

object Worker {
  def apply(productDatabase: ProductDatabase): Worker = new Worker(productDatabase)
}
