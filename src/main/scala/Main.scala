import actors.CartActor.{CheckoutCancelled, CheckoutStarted, ItemAdded, ItemRemoved}
import actors.CheckoutActor._
import actors.{CartActor, CheckoutActor}
import akka.actor.{ActorSystem, Props}

import scala.io.StdIn

object Main extends App {
  println("This is App created for testing Cart and Checkout actors:")

  val actorSystem = ActorSystem("e-Sklep")

  try {
    val cartActor = actorSystem.actorOf(Props[CartActor], "cartActor")
    val checkoutActor = actorSystem.actorOf(Props[CheckoutActor], "checkoutActor")

//    cartActor ! ItemAdded
//    cartActor ! ItemRemoved
//    cartActor ! ItemAdded
//    cartActor ! ItemAdded
//    cartActor ! CheckoutStarted
//    cartActor ! CheckoutCancelled

    checkoutActor ! Start
    checkoutActor ! DeliveryMethodSelected
    checkoutActor ! PaymentSelected
    checkoutActor ! PaymentReceived
    checkoutActor ! "status"

    StdIn.readLine()
  }
  finally {
    actorSystem.terminate()
  }
}
