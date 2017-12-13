package catalog

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.Directives._
import akka.pattern.{AskTimeoutException, ask}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import catalog.actors.CatalogStatistics.Stats
import catalog.actors.{CatalogClusterManager, CatalogLogger, CatalogManager, CatalogStatistics}
import catalog.utils.Words

import scala.concurrent.duration._
import scala.io.StdIn

object CatalogServer extends App {

  import catalog.utils.JsonSupport._

  implicit val system = ActorSystem("ClusterSystem", ConfigFactory.load("cluster.conf"))
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  private implicit val timeout: Timeout = 15 seconds

  val clusterManagerRef = CatalogClusterManager.run(Seq("2555").toArray)
  var id = 1
  val routes =
    pathPrefix("product") {
      path("search") {
        put {
          decodeRequest {
            entity(as[Words]) { (words) ⇒
              try {
                val result = ToResponseMarshallable((clusterManagerRef ? SearchForItems(words.items))
                  .mapTo[SearchResults])
                complete(result)
              } catch {
                case ate: AskTimeoutException =>
                  complete(201 -> ate)
                case cce: ClassCastException =>
                  complete(202 -> cce)
                case e => complete(500 -> e)
              }
            }
          }
        }
      } ~
        path("stats") {
          get {
            complete(ToResponseMarshallable((clusterManagerRef ? GetStats())
              .mapTo[Stats]))
          }
        }
    }
  CatalogManager.main(Seq("2556", "0").toArray)
  CatalogStatistics.main(Seq("0", "stats").toArray)
  CatalogLogger.main(Seq("0", "logs").toArray)
  startClusterNodes()
  val bindingFuture = Http().bindAndHandle(routes, "localhost", 8081)

  def startClusterNodes(): Unit = {
    CatalogManager.main(Seq("0", id.toString).toArray)
    id += 1
    CatalogManager.main(Seq("0", id.toString).toArray)
    id += 1
  }

  println(s"Server online at http://localhost:8081/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}

