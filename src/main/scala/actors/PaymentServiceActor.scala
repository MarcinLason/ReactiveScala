package actors

import actors.PaymentServiceActor.State
import akka.actor.FSM

object PaymentServiceActor {

  sealed trait State

}

class PaymentServiceActor extends FSM[State, Any] {

}
