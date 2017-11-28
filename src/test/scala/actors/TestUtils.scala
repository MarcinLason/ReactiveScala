package actors

import java.io.File

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.iq80.leveldb.util.FileUtils
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

class TestUtils extends TestKit(ActorSystem()) with WordSpecLike with BeforeAndAfterAll with ImplicitSender
  with Matchers  with BeforeAndAfterEach {

  val configurations = List(
    new File(system.settings.config.getString("akka.persistence.journal.leveldb.dir")),
    new File(system.settings.config.getString("akka.persistence.snapshot-store.local.dir"))
  )

  override def beforeAll() {
    super.beforeAll()
    configurations foreach FileUtils.deleteRecursively
  }

  override def afterAll() {
    super.afterAll()
    system.terminate()
    configurations foreach FileUtils.deleteRecursively
  }
}
