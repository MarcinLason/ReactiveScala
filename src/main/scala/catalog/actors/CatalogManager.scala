package catalog.actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, RootActorPath, Terminated}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.routing._
import com.typesafe.config.ConfigFactory
import catalog._
import shop.actors._

import scala.collection.mutable.ListBuffer

class CatalogManager(val id: String, val system: ActorSystem, var productCatalog: ProductCatalog) extends Actor
  with ActorLogging {

  import CatalogManager._

  val mediator = DistributedPubSub(system).mediator
  val numberOfRoutees = 50
  var jobDetails: JobDetails = _

  var router: Router = {
    val routees = ListBuffer[ActorRefRoutee]()
    for (p <- productCatalog.splitCatalog(numberOfRoutees); i <- 0 to numberOfRoutees) {
      val worker = system.actorOf(Props(new CatalogWorker(i, self, new ProductCatalog(p))))
      context watch worker
      routees += ActorRefRoutee(worker)
    }
    Router(RoundRobinRoutingLogic(), routees.toIndexedSeq)
  }

  val cluster = Cluster(system)

  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberUp])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: PartialFunction[Any, Unit] = {
    case SearchForItems(words) =>
      mediator ! Publish("stats", id)
      mediator ! Publish("logs", Log(id, words))
      jobDetails = JobDetails(0, CatalogSearchResults(List()), sender())
      router.route(CatalogWorker.SearchForItems(words), sender())

    case SearchResults(catalogSearchResults) =>
      val bestResults: CatalogSearchResults = mergeResults(jobDetails.bestResults, catalogSearchResults)
      if (jobDetails.workersFinished == numberOfRoutees) {
        jobDetails.sender ! bestResults.scoredItems.unzip._1
      } else {
        jobDetails = JobDetails(jobDetails.workersFinished + 1, bestResults, jobDetails.sender)
      }

    case Get => sender() ! ProductCatalogStatus(productCatalog)

    case Terminated(a) ⇒
      router = router.removeRoutee(a)
      val r = context.actorOf(Props[CatalogWorker])
      context watch r
      router = router.addRoutee(r)

    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach register

    case MemberUp(m) => register(m)
  }

  def register(member: Member): Unit =
    if (member.hasRole("clusterManager")) {
      system.actorSelection(RootActorPath(member.address) / "user" / "clusterManager") ! ClusterNodeRegistration(id)
    }

  def mergeResults(results: CatalogSearchResults, results1: CatalogSearchResults): CatalogSearchResults = {
    val items = results.scoredItems ++ results1.scoredItems
    CatalogSearchResults(items.sortWith((item1: (Item, Int), item2: (Item, Int)) => item1._2 > item2._2).take(10))
  }
}

object CatalogManager {
  case object Get
  case class ProductCatalogStatus(productCatalog: ProductCatalog)
  case class JobDetails(workersFinished: Int, bestResults: CatalogSearchResults, sender: ActorRef)

  def main(args: Array[String]): Unit = {

    val port = if (args.isEmpty) "0" else args(0)
    val id = if (args.length < 2) "0" else args(1)
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [clusterNode]")).
      withFallback(ConfigFactory.load("cluster"))

    val system: ActorSystem = ActorSystem("ClusterSystem", config)
    system.actorOf(Props(new CatalogManager(id, system, ProductCatalog.ready)), name = "clusterNode")
  }
}