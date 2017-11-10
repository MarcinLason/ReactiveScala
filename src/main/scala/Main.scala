import actors.CustomerActor
import akka.actor.{ActorSystem, Props}
import utils.Message._

import scala.io.StdIn

object Main extends App {
  println("This is App created for testing Cart and Checkout actors:")

  val actorSystem = ActorSystem("e-Sklep")

  try {
    val customerActor = actorSystem.actorOf(Props[CustomerActor], "customerActor")

    var line = StdIn.readLine()
    while (line != "exit") {
      if (line != "") {
        customerActor ! getObjectMessage(line)
      }
      line = StdIn.readLine()
    }
  }
  finally {
    actorSystem.terminate()
  }
}
