package catalog.simulations

import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

object GatlingTestMain {

  def main(args: Array[String]) {
    val simulationClass = classOf[SearchTestCase].getName
    val propertiesBuilder = new GatlingPropertiesBuilder
    propertiesBuilder.sourcesDirectory("./src/main/scala")
    propertiesBuilder.binariesDirectory("./target/scala-2.11/classes")
    propertiesBuilder.simulationClass(simulationClass)
    Gatling.fromMap(propertiesBuilder.build)
  }
}