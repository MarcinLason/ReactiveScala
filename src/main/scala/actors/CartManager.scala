package actors

import java.net.URI

import actors.CartManager._
import akka.actor.{Props, Timers}
import akka.event.{Logging, LoggingReceive}
import akka.persistence.{PersistentActor, SnapshotOffer}
import messages.CartManagerMessages._
import messages.CustomerMessages.{CartEmpty, CheckOutStarted}

import scala.collection.immutable.HashMap
import scala.concurrent.duration._

class CartManager(var shoppingCart: Cart, id: String = "007") extends PersistentActor with Timers {
  private val log = Logging(context.system, this)
  def this() = this(Cart.empty)

  def this(id: String) = this(Cart.empty, id)

  override def persistenceId: String = "cart-manager-" + id
  override def receive: Receive = receiveCommand
  override def receiveCommand: Receive = empty()

  def empty(): Receive = LoggingReceive {
    case AddItem(item: Item) =>
      log.info("CartManager: adding item to empty cart.")
      restartTimer()
      this.shoppingCart = shoppingCart.addItem(item)

      persist(CartChangeEvent(AddItemAction(item), NonEmpty)) { _ =>
        context become nonEmpty()
      }
  }

  def nonEmpty(): Receive = LoggingReceive {

    case AddItem(item: Item) =>
      log.info("CartManager: adding item to nonEmpty cart.")
      restartTimer()
      this.shoppingCart = shoppingCart.addItem(item)
      persist(CartChangeEvent(AddItemAction(item), NonEmpty)) { _ =>
      }

    case RemoveItem(item: Item) if shoppingCart.getItems.size > 1 =>
      log.info("CartManager: removing item from cart.")
      restartTimer()
      persist(CartChangeEvent(RemoveSingleItemAction(item), NonEmpty)) { _ =>
        this.shoppingCart = shoppingCart.removeItem(item, 1)
      }

    case RemoveItem(item: Item) =>
      log.info("CartManager: removing last item from cart.")
      this.shoppingCart = shoppingCart.removeItem(item, 1)
      context.parent ! CartEmpty()
      context become empty()
      saveSnapshot(shoppingCart)

    case CartTimeExpired() =>
      log.info("CartManager: time expired, removing all items from cart.")
      this.shoppingCart = shoppingCart.removeAllItems()
      context.parent ! CartEmpty()
      context become empty()
      saveSnapshot(shoppingCart)

    case StartCheckout =>
      log.info("CartManager: starting checkout")
      persist(CartChangeEvent(NewState(), InCheckout)) { _ =>
        val checkoutActor = context.actorOf(Props[Checkout])
        sender ! CheckOutStarted(checkoutActor)
        context become inCheckout()
      }
  }

  def inCheckout(): Receive = LoggingReceive {
    case CheckoutClosed() =>
      log.info("CartManager: checkout closed.")
      this.shoppingCart = shoppingCart.removeAllItems()
      context.parent ! CartEmpty()
      context become empty()
      saveSnapshot(shoppingCart)
    case CheckoutCanceled() =>
      restartTimer()
      persist(CartChangeEvent(NewState(), NonEmpty)) { _ =>
        context.parent ! CartEmpty()
        context become nonEmpty()
      }
  }

  def restartTimer() {
    persist(SetTimerEvent(System.currentTimeMillis(), CartTimeExpired())) { _ =>
      timers.startSingleTimer(CartExpirationKey, CartTimeExpired(), 10.seconds)
    }
  }

  override def receiveRecover: Receive = {

    case CartChangeEvent(action, state) =>
      action match {
        case AddItemAction(item) => shoppingCart = shoppingCart.addItem(item)
        case RemoveSingleItemAction(item) => shoppingCart = shoppingCart.removeItem(item, 1)
        case _ =>
      }
      log.info("CartManagerRECOVER: " + state)
      setState(state)

    case SnapshotOffer(_, snapshot: Cart) =>
      shoppingCart = snapshot
      setState(Empty)

    case SetTimerEvent(time, message) =>
      val currentTime = System.currentTimeMillis()
      val delay = Math.max(1000, time + 10000 - currentTime)
      timers.startSingleTimer(CartExpirationKey, message, delay.millis)
  }

  def setState(state: State): Unit = state match {
    case Empty => context become empty()
    case NonEmpty => context become nonEmpty()
    case InCheckout => {
      context.actorOf(Props[Checkout])
      context become inCheckout()
    }
  }
}

object CartManager {
  def apply: CartManager = new CartManager()

  sealed trait State
  case object Empty extends State
  case object NonEmpty extends State
  case object InCheckout extends State

  sealed trait Action
  case class AddItemAction(item: Item) extends Action
  case class RemoveSingleItemAction(item: Item) extends Action
  case class NewState() extends Action
  case class CartChangeEvent(action: Action, newState: State)
  case class SetTimerEvent(time: Long, message: Message)
  case class Item(id: URI, name: String, brand: String, count: Int, price: BigDecimal)

  case class Cart(items: Map[URI, Item]) {
    def addItem(it: Item): Cart = {
      val currentCount = if (items contains it.id) items(it.id).count else 0
      copy(items = items.updated(it.id, it.copy(count = currentCount + it.count)))
    }

    def removeItem(it: Item, cnt: Int): Cart = {
      val currentCount = if (items contains it.id) items(it.id).count else 0
      copy(items = items.updated(it.id, it.copy(count = currentCount - cnt)))
    }

    def removeAllItems(): Cart = copy(items = HashMap())
    def getItems: List[Item] = items.values.filter(it => it.count > 0).toList
  }

  object Cart {
    val empty = Cart(Map.empty)
  }
}
