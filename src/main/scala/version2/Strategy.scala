package version2

/**
 * FUNCTIONAL PANDEMIC CONTROL STRATEGY MODEL
 * Pure functional approach with pattern matching and sealed trait hierarchy
 */
sealed trait Strategy {
  def cooperate: Boolean
  def name: String
  
  // PATTERN MATCHING for strategy operations
  def opposite: Strategy = this match {
    case _: Compliant => NonCompliant()
    case _: NonCompliant => Compliant()
  }
  
  // FUNCTIONAL STRATEGY COMPARISON
  def sameAs(other: Strategy): Boolean = (this, other) match {
    case (_: Compliant, _: Compliant) => true
    case (_: NonCompliant, _: NonCompliant) => true
    case _ => false
  }
}

/**
 * COMPLIANT STRATEGY - Following pandemic control measures
 */
case class Compliant() extends Strategy {
  override val cooperate: Boolean = true
  override val name: String = "Compliant"
}

/**
 * NON-COMPLIANT STRATEGY - Not following pandemic control measures
 */
case class NonCompliant() extends Strategy {
  override val cooperate: Boolean = false
  override val name: String = "NonCompliant"
}

/**
 * FUNCTIONAL STRATEGY OPERATIONS - Higher-order functions and currying
 */
object StrategyOps {
  
  // CURRIED STRATEGY FILTERS
  val filterByType: Strategy => List[Strategy] => List[Strategy] = 
    targetStrategy => strategies => strategies.filter(_.sameAs(targetStrategy))
  
  val filterCompliant: List[Strategy] => List[Strategy] = filterByType(Compliant())
  val filterNonCompliant: List[Strategy] => List[Strategy] = filterByType(NonCompliant())
  
  // FUNCTIONAL STRATEGY ANALYSIS
  val calculateComplianceRate: List[Strategy] => Double = strategies =>
    strategies match {
      case Nil => 0.0
      case nonEmpty => nonEmpty.count(_.cooperate).toDouble / nonEmpty.size
    }
  
  // STRATEGY PATTERN MATCHING
  val strategyClassifier: Strategy => String = {
    case _: Compliant => "Cooperative"
    case _: NonCompliant => "Defective"
  }
  
  // CURRIED STRATEGY TRANSFORMATIONS
  val flipStrategies: List[Strategy] => List[Strategy] = 
    strategies => strategies.map(_.opposite)
  
  val countByType: List[Strategy] => Map[String, Int] = strategies => {
    val grouped = strategies.groupBy(strategyClassifier)
    grouped.map { case (strategyType, strategyList) => strategyType -> strategyList.size }
  }
  
  // HIGHER-ORDER FUNCTION for strategy analysis
  def analyzeStrategies[T](strategies: List[Strategy])(analyzer: List[Strategy] => T): T = 
    analyzer(strategies)
}
