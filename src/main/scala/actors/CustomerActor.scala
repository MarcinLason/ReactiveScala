package actors

import actors.CustomerActor._
import akka.actor.{ActorRef, FSM, Props}
import utils.Message._

object CustomerActor {

  sealed trait State

  case object Uninitialized extends State

  case object DuringShopping extends State

  case object DuringCheckout extends State

  case object DuringPayment extends State

}


class CustomerActor extends FSM[State, ActorRef] {

  startWith(Uninitialized, null)

  when(Uninitialized) {
    case Event(Start, _) => {
      log.debug("CustomerActor: actor started.")
      goto(DuringShopping) using context.system.actorOf(Props[CartActor], "cartActor")
    }
    case Event(_, _) => {
      log.debug("CustomerActor: Can not perform action before initialization.")
      stay()
    }
  }

  when(DuringShopping) {
    case Event(ItemAdded, actorRef) => {
      log.debug("Customer Actor: User requested to add item.")
      actorRef ! ItemAdded
      stay() using actorRef
    }

    case Event(ItemRemoved, actorRef) => {
      log.debug("CustomerActor: User requested to remove item.")
      actorRef ! ItemRemoved
      stay() using actorRef
    }

    case Event(CheckoutStarted, actorRef) => {
      log.debug("CustomerActor: User requested to start checkout.")
      actorRef ! CheckoutStarted
      goto(DuringCheckout) using null
    }
  }

  when(DuringCheckout) {
    case Event(CheckoutActorCreated, actorRef) => {
      log.debug("CustomerActor: Checkout actor initialized.")
      stay() using actorRef
    }
  }


}
