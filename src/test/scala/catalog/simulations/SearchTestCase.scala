package catalog.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import net.liftweb.json.Serialization
import catalog.simulations.RequestCreator._

import scala.util.Random

class SearchTestCase extends io.gatling.core.scenario.Simulation with TestConfig {

  val httpProtocol = http.baseURL(hostAddress)
  implicit val formats = net.liftweb.json.DefaultFormats

  def randomSerializedRequests = {
    StringBody(Serialization.write(searchWordsLists(Random.nextInt(searchWordsLists.size))))
  }

  val testCase = scenario("Simulation for the product catalog").repeat(repeatAmount) {
    exec(
      http(session => "PUT: search")
        .put(apiPath + "search")
        .header("Content-Type", "application/json")
        .body(randomSerializedRequests)
        .check(status is 200)
    ).exec(
      http(session => "GET: stats")
        .get(apiPath + "stats")
        .check(status is 200)
    )
  }

  setUp(testCase.inject(atOnceUsers(threadsAmount)))
    .protocols(httpProtocol)
    .assertions(global.successfulRequests.percent.is(successRate))
}