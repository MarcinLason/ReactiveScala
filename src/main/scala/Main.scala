import actors.CartActor._
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
//    cartActor ! ItemAdded
//    cartActor ! ItemRemoved
//    cartActor ! ItemAdded
//    cartActor ! CheckoutStarted
//    cartActor ! CheckoutCancelled
//    cartActor ! CheckoutStarted
//    cartActor ! CheckoutClosed
//    cartActor ! ItemAdded

    checkoutActor ! Start
    checkoutActor ! DeliveryMethodSelected
    checkoutActor ! PaymentSelected
    checkoutActor ! PaymentReceived

    StdIn.readLine()
  }
  finally {
    actorSystem.terminate()
  }
}
