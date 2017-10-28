package actors

import actors.CustomerActor._
import akka.actor.{ActorRef, ActorSystem, FSM, Props}

object CustomerActor {

  sealed trait State

  case object DuringShopping extends State
  case object DuringCheckout extends State
  case object DuringPayment extends State

}


class CustomerActor extends FSM[State, ActorRef] {
//  val actorSystem = ActorSystem("e-Sklep")
//
//  startWith(DuringShopping, actorSystem.actorOf(Props[CartActor], "cartActor"))
//
//  when (DuringShopping) {
//    case Event("elo", a) => {
//      println("time" + actorSystem.uptime)
//      stay() using a
//    }
//  }

//  when (DuringShopping) {
//    case
//  }
}
