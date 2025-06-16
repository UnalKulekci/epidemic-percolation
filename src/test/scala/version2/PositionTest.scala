import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import version2.Position

class PositionTest extends AnyFlatSpec with Matchers {

  "A Position" should "calculate distance correctly" in {
    val pos1 = Position(0.0, 0.0)
    val pos2 = Position(3.0, 4.0)
    pos1.distanceTo(pos2) should be (5.0)
  }

  it should "check if within radius correctly" in {
    val pos1 = Position(0.0, 0.0)
    val pos2 = Position(1.0, 1.0)
    pos1.isWithinRadius(pos2, 1.5) should be (true)
    pos1.isWithinRadius(pos2, 1.0) should be (false)
  }

  it should "constrain to bounds correctly" in {
    val pos = Position(10.0, 10.0)
    val constrainedPos = pos.constrainToBounds(5.0, 5.0)
    constrainedPos should be (Position(4.9, 4.9))
  }
}