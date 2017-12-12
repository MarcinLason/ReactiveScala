package catalog.simulations

import akka.event.slf4j.SLF4JLogging
import com.typesafe.config.{ConfigException, ConfigFactory}

import scala.util.Try

trait TestConfig extends SLF4JLogging {

  private[this] val config = ConfigFactory.load()
  val hostAddress = getRequiredString("service.hostAddress")
  val apiPath = getRequiredString("service.apiPath")
  val repeatAmount = getRequiredInt("scenario.repeatAmount")
  val threadsAmount = getRequiredInt("scenario.threadsAmount")
  val successRate = Try(config.getInt("scenario.successRate")).getOrElse(100)

  def getRequiredString(path: String) = {
    Try(config.getString(path)).getOrElse {
      handleError(path)
    }
  }

  def getRequiredInt(path: String) = {
    Try(config.getInt(path)).getOrElse {
      handleError(path)
    }
  }

  def getRequiredStringList(path: String) = {
    Try(config.getStringList(path)).getOrElse {
      handleError(path)
    }
  }

  private[this] def handleError(path: String) = {
    val errorMessage = s"Missing required value: $path"
    log.error(errorMessage)
    throw new ConfigException.Missing(errorMessage)
  }
}