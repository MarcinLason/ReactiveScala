package catalog.actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, RootActorPath}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.cluster.{Cluster, Member, MemberStatus}
import catalog._
import com.typesafe.config.ConfigFactory

class CatalogLogger(val id: String, val system: ActorSystem) extends Actor with ActorLogging {

  val mediator = DistributedPubSub(system).mediator
  mediator ! Subscribe("logs", self)
  val cluster = Cluster(system)

  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberUp])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: PartialFunction[Any, Unit] = {

    case SubscribeAck(Subscribe("logs", None, `self`)) ⇒
      log.info("CatalogLogger: subscribing to logs.")

    case logObject: Log =>
      log.info("Node " + logObject.id + " is managing query for " + logObject.query.toString())

    case state: CurrentClusterState =>
      state
        .members
        .filter(_.status == MemberStatus.Up) foreach register

    case MemberUp(m) => register(m)
  }

  def register(member: Member): Unit =
    if (member.hasRole("clusterManager")) {
      system.actorSelection(RootActorPath(member.address) / "user" / "clusterManager") ! LoggingActorRegistration(id)
    }
}

object CatalogLogger {

  def main(args: Array[String]): Unit = {
    val port = if (args.isEmpty) "0" else args(0)
    val id = if (args.length < 2) "0" else args(1)

    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [log]")).
      withFallback(ConfigFactory.load("cluster"))

    val system: ActorSystem = ActorSystem("ClusterSystem", config)
    system.actorOf(Props(new CatalogLogger(id, system)), name = "log")
  }
}