package actors

object Message {
  case object ItemAdded
  case object ItemRemoved
  case object CartTimerExpired
  case object CheckoutStarted
  case object CheckoutCancelled
  case object CheckoutClosed

  case object CheckoutTimerExpired
  case object DeliveryMethodSelected
  case object PaymentSelected
  case object PaymentReceived
  case object Start
  case object Cancel

}
class Message {

}
