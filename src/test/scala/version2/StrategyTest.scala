import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import version2.{Compliant, NonCompliant, Strategy}

class StrategyTest extends AnyFlatSpec with Matchers {

  "A Strategy" should "return the opposite strategy correctly" in {
    val compliant = Compliant()
    val nonCompliant = NonCompliant()
    compliant.opposite should be (nonCompliant)
    nonCompliant.opposite should be (compliant)
  }

  it should "compare strategies correctly" in {
    val compliant1 = Compliant()
    val compliant2 = Compliant()
    val nonCompliant = NonCompliant()
    compliant1.sameAs(compliant2) should be (true)
    compliant1.sameAs(nonCompliant) should be (false)
  }
}