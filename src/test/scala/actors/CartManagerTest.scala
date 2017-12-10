package actors

import java.net.URI

import actors.CartManager.{Cart, Item}
import akka.actor.{Actor, PoisonPill, Props}
import akka.testkit.{TestActorRef, TestProbe}
import messages.CartManagerMessages.{AddItem, RemoveItem, StartCheckout}
import messages.CustomerMessages.{CartEmpty, CheckOutStarted}

import scala.concurrent.duration._

class CartManagerTest extends TestUtils {

  "Cart manager" must {
    "add Item" in {
      var cart = Cart.empty
      cart = cart.addItem(Item(new URI("www.cars.com"), "opel", "cars", 1, BigDecimal(30000)))
      assert(cart.getItems.size == 1)
    }

    "remove item" in {
      var cart = Cart.empty
      cart = cart.addItem(Item(new URI("www.cars.com"), "opel", "cars", 1, BigDecimal(30000)))
      assert(cart.getItems.size == 1)
      cart = cart.removeItem(Item(new URI("www.cars.com"), "opel", "cars", 1, BigDecimal(30000)), 1)
      assert(cart.getItems.isEmpty)
    }

    "not remove item when there is no such item in a cart" in {
      var cart = Cart.empty
      cart = cart.addItem(Item(new URI("www.cars.com"), "opel", "cars", 1, BigDecimal(30000)))
      assert(cart.getItems.size == 1)
      cart = cart.removeItem(Item(new URI("www.watches.com"), "rolex", "watches", 1, BigDecimal(30000)), 1)
      assert(cart.getItems.size == 1)
    }

    "have empty cart after initialization" in {
      val cartManager = TestActorRef[CartManager]
      assert(cartManager.underlyingActor.shoppingCart.getItems.isEmpty)
      cartManager ! PoisonPill
    }

    "handle StartCheckout" in {
      val cartManager = system.actorOf(Props[CartManager])
      cartManager ! AddItem(Item(new URI("www.cars.com"), "opel", "cars", 1, BigDecimal(30000)))
      expectNoMessage(1.second)
      cartManager ! StartCheckout
      expectMsgType[CheckOutStarted](15.seconds)
      cartManager ! PoisonPill
    }

    "handle time expiry" in {
      val proxy = TestProbe()
      val parent = system.actorOf(Props(new Actor {
        private val cartManager = context.actorOf(Props(new CartManager(System.currentTimeMillis().toString)))

        override def receive = {
          case x if sender == cartManager => proxy.ref forward x
          case x => cartManager forward x
        }
      }))
      proxy.send(parent, AddItem(Item(new URI("www.cars.com"), "opel", "cars", 1, BigDecimal(30000))))
      proxy.expectMsgType[CartEmpty](15.seconds)
    }

    "cart empty" in {
      val proxy = TestProbe()
      val parent = system.actorOf(Props(new Actor {
        private val cartManager = context.actorOf(Props(new CartManager(System.currentTimeMillis().toString)))

        override def receive = {
          case x if sender == cartManager => proxy.ref forward x
          case x => cartManager forward x
        }
      }))
      proxy.send(parent, AddItem(Item(new URI("www.cars.com"), "opel", "cars", 1, BigDecimal(30000))))
      proxy.send(parent, RemoveItem(Item(new URI("www.cars.com"), "opel", "cars", 1, BigDecimal(30000))))
      proxy.expectMsgType[CartEmpty](15.seconds)
    }
  }
}
