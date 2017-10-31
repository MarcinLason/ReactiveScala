package utils

object Message {
  sealed trait Message
  case object ItemAdded extends Message
  case object ItemRemoved extends Message
  case object CartTimerExpired extends Message
  case object CheckoutStarted extends Message
  case object CheckoutCancelled extends Message
  case object CheckoutClosed extends Message
  case object CheckoutActorCreated extends Message

  case object CheckoutTimerExpired extends Message
  case object DeliveryMethodSelected extends Message
  case object PaymentSelected extends Message
  case object PaymentReceived extends Message
  case object Start extends Message
  case object Cancel extends Message

  case object UnknownMessage extends Message

  def getObjectMessage(messageName: String): Message.Message = {
    if (messageName.equals("Start")) return Start
    if (messageName.equals("ItemAdded")) return ItemAdded
    if (messageName.equals("ItemRemoved")) return ItemRemoved
    if (messageName.equals("CheckoutStarted")) return CheckoutStarted
    if (messageName.equals("CheckoutCancelled")) return CheckoutCancelled
    if (messageName.equals("DeliveryMethodSelected")) return DeliveryMethodSelected
    if (messageName.equals("PaymentSelected")) return PaymentSelected
    return Message.UnknownMessage
  }
}
class Message {

}
