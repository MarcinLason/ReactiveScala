package catalog.actors

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import catalog._
import com.typesafe.config.ConfigFactory

import scala.language.postfixOps

class CatalogClusterManager extends Actor {

  val SERVICE_UNAVAILABLE = "Service unavailable."
  var clusterNodes = IndexedSeq.empty[ActorRef]
  var idsMap = Map[ActorRef, String]()
  var statisticsActor: ActorRef = _
  var loggingActor: ActorRef = _
  var jobCounter = 0

  def receive = {
    case job: SearchForItems if clusterNodes.isEmpty =>
      sender() ! ProductCatalogJobFailed(SERVICE_UNAVAILABLE, job)

    case job: SearchForItems =>
      jobCounter += 1
      clusterNodes(jobCounter % clusterNodes.size) forward job

    case job: GetStats =>
      statisticsActor forward job

    case ClusterNodeRegistration(id: String) if !clusterNodes.contains(sender()) =>
      context watch sender()
      idsMap += (sender() -> id)
      clusterNodes = clusterNodes :+ sender()

    case StatsActorRegistration(id: String) =>
      context watch sender()
      idsMap += (sender() -> id)
      statisticsActor = sender()

    case LoggingActorRegistration(id: String) =>
      context watch sender()
      idsMap += (sender() -> id)
      loggingActor = sender()

    case Terminated(actor) if clusterNodes.contains(actor) =>
      clusterNodes = clusterNodes.filterNot(_ == actor)
      CatalogManager.main(Seq("0", idsMap.getOrElse(actor, default = "0")).toArray)

    case Terminated(actor) if statisticsActor == actor =>
      CatalogStatistics.main(Seq("0", idsMap.getOrElse(actor, default = "stats")).toArray)

    case Terminated(actor) if loggingActor == actor =>
      CatalogLogger.main(Seq("0", idsMap.getOrElse(actor, default = "logs")).toArray)
  }
}

object CatalogClusterManager {
  def run(args: Array[String]): ActorRef = {

    val port = if (args.isEmpty) "0" else args(0)
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [clusterManager]")).
      withFallback(ConfigFactory.load("cluster"))

    val system = ActorSystem("ClusterSystem", config)
    val clusterManager = system.actorOf(Props[CatalogClusterManager], name = "clusterManager")

    val counter = new AtomicInteger
    clusterManager
  }
}