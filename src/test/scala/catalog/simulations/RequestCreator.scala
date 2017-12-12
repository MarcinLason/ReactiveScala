package catalog.simulations

import catalog.utils.Words

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