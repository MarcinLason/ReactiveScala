package utils

import akka.actor.ActorRef

object Message {
  sealed trait Message
  case object CartTimerExpired extends Message
  case object CheckoutCancelled extends Message
  case object CheckoutTimerExpired extends Message

  case object Start extends Message
  case class StartCheckoutActor(customerActor: ActorRef) extends Message
  case object Cancel extends Message
  case object Cancelled extends Message
  case object AddItem extends Message
  case object RemoveItem extends Message
  case object StartCheckOut extends Message
  case class CheckOutStarted(checkoutActor: ActorRef) extends Message
  case object DeliveryMethodSelected extends Message
  case object PaymentSelected extends Message
  case class PaymentServiceStarted(paymentServiceActor: ActorRef) extends Message
  case object DoPayment extends Message
  case object PaymentConfirmed extends Message
  case object PaymentReceived extends Message
  case object CheckOutClosed extends Message
  case class CartEmpty(cartActor: ActorRef) extends Message
  case class CheckoutTerminated(cartActor: ActorRef) extends Message

  case object UnknownMessage extends Message

  def getObjectMessage(messageName: String): Message.Message = {
    if (messageName.equals("Start")) return Start
    if (messageName.equals("Cancel")) return Cancel
    if (messageName.equals("AddItem")) return AddItem
    if (messageName.equals("RemoveItem")) return RemoveItem
    if (messageName.equals("StartCheckOut")) return StartCheckOut
    if (messageName.equals("DeliveryMethodSelected")) return DeliveryMethodSelected
    if (messageName.equals("PaymentSelected")) return PaymentSelected
    if (messageName.equals("DoPayment")) return DoPayment
    return Message.UnknownMessage
  }
}
class Message {

}
