package actors

import actors.PaymentServer._
import akka.actor.SupervisorStrategy.{Escalate, Restart, Stop}
import akka.actor.{Actor, OneForOneStrategy, PoisonPill, Props, Timers}
import akka.event.{Logging, LoggingReceive}
import messages.CheckoutMessages.PaymentReceived
import messages.PaymentServiceMessages.{DoPayment, InvalidPayment, PaymentConfirmed}

import scala.concurrent.duration._

class PaymentService extends Actor with Timers {
  private val log = Logging(context.system, this)

  override def receive: Receive = LoggingReceive {

    case DoPayment(address) => {
      log.info("PaymentService: start processing payment.")
      context.actorOf(Props[PaymentServer]) ! DoPayment(address)
    }

    case PaymentReceived => {
      log.info("PaymentService: received payment.")
      sender ! PaymentConfirmed
      context.parent ! PaymentReceived
      self ! PoisonPill
    }
  }

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case BadRequest => {
        log.info("PaymentService: BadRequest - stop.")
        context.parent ! InvalidPayment
      }; Stop
      case Unauthorized => {
        log.info("PaymentService: Unauthorized - stop.")
        context.parent ! InvalidPayment
      }; Stop
      case Forbidden => {
        log.info("PaymentService: Forbidden - restart.")
        context.parent ! InvalidPayment
      }; Restart
      case NotFound => {
        log.info("PaymentService: NotFound - stop.")
        context.parent ! InvalidPayment
      }; Stop
      case MethodNotAllowed =>{
        log.info("PaymentService: MethodNotAllowed - restart.")
      }; Restart
      case NotAcceptable => {
        log.info("PaymentService: NotAcceptable - restart.")
      }; Restart
      case RequestTimeout => {
        log.info("PaymentService: RequestTimeout - restart.")
      }; Restart
      case ExceptionFailed => {
        log.info("PaymentService: ExceptionFailed - restart.")
      }; Restart
      case InternalServerError => {
        log.info("PaymentService: InternalServerError - restart.")
      }; Restart
      case ServiceUnavailable => {
        log.info("PaymentService: ServiceUnavailable - restart.")
      }; Restart
      case MyException => {
        log.info("PaymentService: MyException - escalate.")
      };  Escalate
    }
}

object PaymentService {
  def apply: PaymentService = new PaymentService()
}
