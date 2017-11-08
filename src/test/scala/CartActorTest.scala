import actors.CartActor
import actors.CartActor.{Empty, InCheckout, NonEmpty}
import akka.actor.ActorSystem
import akka.actor.FSM.StateTimeout
import akka.testkit.{ImplicitSender, TestFSMRef, TestKitBase}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import utils.Message._

class CartActorTest extends TestKitBase with WordSpecLike with BeforeAndAfterAll with ImplicitSender {

  implicit lazy val system = ActorSystem()
  var fsm = TestFSMRef(new CartActor)
  val typedProperly: TestFSMRef[CartActor.State, Int, CartActor] = fsm

  override def afterAll(): Unit = {
    system.terminate
  }

  "Cart Actor" should {
    "become NonEmpty after adding element." in {
      assert(fsm.stateName == Empty)
      fsm ! AddItem
      assert(fsm.stateData == 1)
      assert(fsm.stateName == NonEmpty)
    }
  }

  "Cart Actor" should {
    "become Empty again after removing all items." in {
      fsm.setState(Empty, 0)
      assert(fsm.stateName == Empty)
      fsm ! AddItem
      assert(fsm.stateName == NonEmpty)
      assert(fsm.stateData == 1)
      fsm ! RemoveItem
      assert(fsm.stateName == Empty)
      assert(fsm.stateData == 0)
    }
  }


  "Cart Actor" should {
    "still be NonEmpty after removing some of items." in {
      fsm.setState(Empty, 0)
      assert(fsm.stateName == Empty)
      fsm ! AddItem
      fsm ! AddItem
      assert(fsm.stateName == NonEmpty)
      fsm ! RemoveItem
      assert(fsm.stateName == NonEmpty)
      assert(fsm.stateData == 1)
    }
  }

  "Cart Actor" should {
    "dump bucket after specified period of time." in {
      fsm.setState(Empty, 0)
      assert(fsm.stateName == Empty)
      fsm ! AddItem
      assert(fsm.stateName == NonEmpty)
      assert(fsm.isStateTimerActive == true)
      fsm ! StateTimeout
      assert(fsm.stateName == Empty)
      assert(fsm.stateData == 0)
    }
  }

  "Cart Actor" should {
    "go back with it's counter after checkout cancellation." in {
      fsm.setState(Empty, 0)
      assert(fsm.stateName == Empty)
      fsm ! AddItem
      fsm ! AddItem
      fsm ! StartCheckOut
      assert(fsm.stateName == InCheckout)
      fsm ! CheckoutCancelled
      assert(fsm.stateName == NonEmpty)
      assert(fsm.stateData == 2)
    }
  }
}
