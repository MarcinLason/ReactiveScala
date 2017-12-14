package catalog.actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, RootActorPath}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.cluster.{Cluster, Member, MemberStatus}
import catalog._
import catalog.actors.CatalogStatistics.{Stat, Stats}
import com.typesafe.config.ConfigFactory

class CatalogStatistics(val id: String, val system: ActorSystem) extends Actor with ActorLogging {

  val mediator = DistributedPubSub(system).mediator
  mediator ! Subscribe("stats", self)
  val cluster = Cluster(system)
  var stats = Map[String, Int]()

  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberUp])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: PartialFunction[Any, Unit] = {
    case catalogManagerId: String ⇒
      val count = stats.getOrElse(catalogManagerId, default = 0) + 1
      stats += (catalogManagerId -> count)

    case SubscribeAck(Subscribe("stats", None, `self`)) ⇒
      log.info("CatalogStatistics: subscribing to stats.")

    case GetStats() =>
      val statistics = for {
        key <- stats.keySet
      } yield Stat(key, stats(key))
      sender() ! Stats(statistics.toList)

    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach register

    case MemberUp(m) => register(m)
  }

  def register(member: Member): Unit =
    if (member.hasRole("clusterManager")) {
      system.actorSelection(RootActorPath(member.address) / "user" / "clusterManager") ! StatsActorRegistration(id)
    }
}

object CatalogStatistics {

  case class Stats(stats: List[Stat])

  case class Stat(id: String, count: Int)

  def main(args: Array[String]): Unit = {
    val port = if (args.isEmpty) "0" else args(0)
    val id = if (args.length < 2) "0" else args(1)

    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [stats]")).
      withFallback(ConfigFactory.load("cluster"))

    val system: ActorSystem = ActorSystem("ClusterSystem", config)
    system.actorOf(Props(new CatalogStatistics(id, system)), name = "stats")
  }
}