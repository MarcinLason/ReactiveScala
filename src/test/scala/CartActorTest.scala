import actors.CartActor
import actors.CartActor.{Empty, NonEmpty}
import akka.actor.ActorSystem
import akka.testkit.{TestFSMRef, TestKitBase}
import org.scalatest.{FlatSpec, Matchers}
import utils.Message.{AddItem, RemoveItem}

class CartActorTest extends FlatSpec with TestKitBase with Matchers {


  implicit lazy val system = ActorSystem()


  val fsm = TestFSMRef(new CartActor)
  val mustBeTypedProperly: TestFSMRef[CartActor.State, Int, CartActor] = fsm


  "Cart Actor" should "become NonEmpty after adding element." in {
    assert(fsm.stateName == Empty)
    fsm ! AddItem
    assert(fsm.stateData == 1)
    assert(fsm.stateName == NonEmpty)
  }

  "Cart Actor" should "become Empty again after removing all items." in {
    assert(fsm.stateName == Empty)
    fsm ! AddItem
    assert(fsm.stateName == NonEmpty)
    fsm ! RemoveItem
    assert(fsm.stateName == Empty)
    assert(fsm.stateData == 0)
  }

}
