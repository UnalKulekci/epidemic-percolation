package version2

/**
 * FUNCTIONAL PANDEMIC CONTROL PAYOFF SYSTEM
 * Pure functional approach with pattern matching, currying, and higher-order functions
 */
object PayoffMatrix {
  
  // PANDEMIC CONTROL PARAMETERS - Immutable values
  sealed trait PayoffValue { def value: Double }
  case object Reward extends PayoffValue { val value = 3.0 }      // R: Both follow rules
  case object Temptation extends PayoffValue { val value = 4.0 }  // T: Free-ride advantage  
  case object Punishment extends PayoffValue { val value = 2.0 }  // P: Nobody follows rules
  case object Sucker extends PayoffValue { val value = 1.0 }      // S: Follow while others don't
  
  // LEGACY COMPATIBILITY
  val R: Double = Reward.value
  val T: Double = Temptation.value
  val P: Double = Punishment.value
  val S: Double = Sucker.value

  /**
   * FUNCTIONAL PAIRWISE PAYOFF - Pure pattern matching
   */
  val pairwisePayoff: (Strategy, Strategy) => Double = (agentStrategy, neighborStrategy) =>
    (agentStrategy.cooperate, neighborStrategy.cooperate) match {
      case (true,  true)  => R
      case (true,  false) => S
      case (false, true)  => T
      case (false, false) => P
    }

  /**
   * CURRIED PAYOFF CALCULATIONS
   */
  val calculateAgentPayoff: List[Agent] => Strategy => Double = 
    neighbors => agentStrategy => 
      neighbors.foldLeft(0.0)((sum, neighbor) => sum + pairwisePayoff(agentStrategy, neighbor.strategy))

  /**
   * FUNCTIONAL WORLD PAYOFF COMPUTATION - Using for comprehension
   */
  def computeAllPayoffs[A <: Agent](world: World[A]): Map[Long, Double] = {
    val payoffCalculations = for {
      (_, agent) <- world.agents
      neighbors = world.neighbors(agent.position)
      totalPayoff = calculateAgentPayoff(neighbors)(agent.strategy)
    } yield agent.id -> totalPayoff
    
    payoffCalculations.toMap
  }

  /**
   * FUNCTIONAL BEST NEIGHBOR FINDER - Using Option and pattern matching
   */
  val findBestNeighbor: List[Agent] => Map[Long, Double] => Option[(Agent, Double)] = 
    neighbors => payoffs => neighbors match {
      case Nil => None
      case nonEmpty => 
        val neighborsWithPayoffs = nonEmpty.map(n => n -> payoffs.getOrElse(n.id, 0.0))
        Some(neighborsWithPayoffs.maxBy(_._2))
    }

  /**
   * CURRIED STRATEGY FILTERING
   */
  val filterByStrategy: Boolean => List[Agent] => List[Agent] = 
    isCooperative => agents => agents.filter(_.strategy.cooperate == isCooperative)

  val filterCompliantAgents: List[Agent] => List[Agent] = filterByStrategy(true)
  val filterNonCompliantAgents: List[Agent] => List[Agent] = filterByStrategy(false)

  /**
   * FUNCTIONAL PAYOFF STATISTICS - Using pattern matching and Options
   */
  val calculateAveragePayoff: List[Double] => Option[Double] = payoffs =>
    payoffs match {
      case Nil => None
      case nonEmpty => Some(nonEmpty.sum / nonEmpty.size)
    }

  /**
   * FUNCTIONAL STRATEGY PAYOFF DIFFERENCE - Pure functional approach
   */
  def strategyPayoffDifference[A <: Agent](world: World[A], payoffs: Map[Long, Double]): Double = {
    val agents = world.agents.values.toList
    
    val compliantPayoffs = filterCompliantAgents(agents).map(a => payoffs.getOrElse(a.id, 0.0))
    val nonCompliantPayoffs = filterNonCompliantAgents(agents).map(a => payoffs.getOrElse(a.id, 0.0))
    
    val avgPayoffs = (calculateAveragePayoff(compliantPayoffs), calculateAveragePayoff(nonCompliantPayoffs))
    
    avgPayoffs match {
      case (Some(compliantAvg), Some(nonCompliantAvg)) => compliantAvg - nonCompliantAvg
      case _ => 0.0
    }
  }

  /**
   * CURRIED PAYOFF ANALYSIS FUNCTIONS
   */
  val analyzePayoffDistribution: List[Double] => (Double, Double, Double) = payoffs => {
    val mean = payoffs.sum / payoffs.size
    val sortedPayoffs = payoffs.sorted
    val median = sortedPayoffs.size match {
      case size if size % 2 == 0 => (sortedPayoffs(size/2 - 1) + sortedPayoffs(size/2)) / 2.0
      case size => sortedPayoffs(size/2)
    }
    val variance = payoffs.map(p => math.pow(p - mean, 2)).sum / payoffs.size
    (mean, median, math.sqrt(variance))
  }

  /**
   * TAIL RECURSIVE PAYOFF AGGREGATION
   */
  @annotation.tailrec
  def aggregatePayoffs(
    agents: List[Agent], 
    strategy: Strategy, 
    acc: List[Double] = List.empty
  ): List[Double] = agents match {
    case Nil => acc
    case head :: tail => 
      head.strategy match {
        case s if s == strategy => aggregatePayoffs(tail, strategy, head.payoff :: acc)
        case _ => aggregatePayoffs(tail, strategy, acc)
      }
  }

  /**
   * HIGHER-ORDER PAYOFF FUNCTIONS
   */
  val payoffTransformer: (Double => Double) => Map[Long, Double] => Map[Long, Double] = 
    transform => payoffs => payoffs.map { case (id, payoff) => id -> transform(payoff) }

  val normalizePayoffs: Map[Long, Double] => Map[Long, Double] = payoffs => {
    val maxPayoff = payoffs.values.maxOption.getOrElse(1.0)
    maxPayoff match {
      case 0.0 => payoffs
      case max => payoffs.map { case (id, payoff) => id -> (payoff / max) }
    }
  }

  /**
   * LAZY PAYOFF CALCULATIONS for performance
   */
  def lazyPayoffStream[A <: Agent](worlds: LazyList[World[A]]): LazyList[Map[Long, Double]] =
    worlds.map(computeAllPayoffs)

  /**
   * FUNCTIONAL PAYOFF MATRIX OPERATIONS
   */
  val payoffMatrix: Map[(Boolean, Boolean), Double] = Map(
    (true, true) -> R,
    (true, false) -> S,
    (false, true) -> T,
    (false, false) -> P
  )

  val getPayoffFromMatrix: (Boolean, Boolean) => Double = 
    (agentCooperates, neighborCooperates) => payoffMatrix((agentCooperates, neighborCooperates))

  /**
   * FOR COMPREHENSION example for complex payoff calculations
   */
  def complexPayoffAnalysis[A <: Agent](world: World[A]): Map[String, Double] = {
    val results = for {
      (_, agent) <- world.agents
      neighbors = world.neighbors(agent.position)
      if neighbors.nonEmpty
      strategyName = if (agent.strategy.cooperate) "Compliant" else "NonCompliant"
      avgNeighborPayoff = neighbors.map(_.payoff).sum / neighbors.size
    } yield strategyName -> avgNeighborPayoff
    
    results.groupBy(_._1).map { case (strategy, values) => 
      strategy -> (values.map(_._2).sum / values.size)
    }
  }
}
