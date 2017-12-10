package product_catalog_server.simulations

import product_catalog_server.utils.Words

import scala.util.Random

object RequestCreator extends TestConfig {

  val words = getRequiredStringList("data.requests")

  def getSearchWords: Words = {
    val number = Random.nextInt(4) + 1
    val wordsList = for {
      i <- 1 until number
    } yield words.get(Random.nextInt(words.size))
    println("TestData: Random word list: " + wordsList.toList)
    Words(wordsList.toList)
  }

  val searchWordsLists = List.fill(threadsAmount)(getSearchWords)
}