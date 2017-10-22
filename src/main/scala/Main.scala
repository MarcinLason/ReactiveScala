import actors.CartActor.{CheckoutCancelled, ItemAdded, ItemRemoved}
import actors.{CartActor, CheckoutActor}
import akka.actor.{ActorSystem, Props}

import scala.io.StdIn

object Main extends App {
  println("This is App created for testing Cart and Checkout actors:")

  val actorSystem = ActorSystem("e-Sklep")

  try {
    val cartActor = actorSystem.actorOf(Props[CartActor], "cartActor")
    val checkoutActor = actorSystem.actorOf(Props[CheckoutActor], "checkoutActor")

    cartActor ! ItemAdded
    cartActor ! ItemRemoved
    cartActor ! ItemAdded
    cartActor ! ItemAdded
    cartActor ! CheckoutCancelled

    println(">>> Press ENTER to exit <<<")
    StdIn.readLine()
  }
  finally {
    actorSystem.terminate()
  }
}
