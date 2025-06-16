package version2

/**
 * FUNCTIONAL FINITE-SIZE ACTOR MODEL
 * Pure functional approach with pattern matching, currying, and higher-order functions
 */
sealed trait Agent {
  def id: Long
  def strategy: Strategy
  def position: Position
  def payoff: Double
  def radius: Double
  def isMobile: Boolean
  
  // Functional updates - immutable transformations
  def withStrategy(newStrategy: Strategy): Agent
  def withPosition(newPosition: Position): Agent  
  def withPayoff(newPayoff: Double): Agent
}

/**
 * FUNCTIONAL AGENT IMPLEMENTATION
 * Using pattern matching, for comprehensions, and curried functions
 */
case class SimpleAgent(
  id: Long,
  strategy: Strategy,
  position: Position,
  payoff: Double = 0.0,
  radius: Double = 1.5,
  isMobile: Boolean = true
) extends Agent {
  
  // Pure functional updates
  override def withStrategy(newStrategy: Strategy): SimpleAgent = copy(strategy = newStrategy)
  override def withPosition(newPosition: Position): SimpleAgent = copy(position = newPosition)
  override def withPayoff(newPayoff: Double): SimpleAgent = copy(payoff = newPayoff)
  
  // FUNCTIONAL NEIGHBOR DETECTION - using filter and for comprehension
  def neighborsInRadius(allAgents: Map[Position, Agent]): List[Agent] = 
    for {
      (_, agent) <- allAgents.toList
      if agent.id != this.id
      if position.isWithinRadius(agent.position, radius + agent.radius)
    } yield agent
  

  
  // FUNCTIONAL PAYOFF NORMALIZATION - curried function
  val normalizePayoff: (Double, Int, Int, Double) => Double = 
    (payoffDiff, myCount, neighborCount, temptation) => (myCount, neighborCount, temptation) match {
      case (0, _, _) | (_, _, 0) => 0.0
      case (mc, nc, t) => payoffDiff / math.max(mc, nc) / t
    }
  
  // STRATEGY CONVERSION - payoff-based only
  def evaluateStrategyConversion(
    neighbors: List[Agent], 
    bestNeighborPayoff: Double,
    bestNeighborCount: Int
  ): Double => Strategy = random => {
    
    val payoffDiff = bestNeighborPayoff - payoff
    val myNeighborCount = neighbors.size
    
    val normalizedPayoff = normalizePayoff(payoffDiff, myNeighborCount, bestNeighborCount, PayoffMatrix.T)
    val conversionProb = math.max(0.0, math.min(1.0, normalizedPayoff))
    
    (random < conversionProb, strategy.cooperate) match {
      case (true, true) => NonCompliant()
      case (true, false) => Compliant()
      case (false, _) => strategy
    }
  }
  
  // LAZY EVALUATION for expensive neighbor calculations
  lazy val neighborAnalyzer: Map[Position, Agent] => List[Agent] = neighborsInRadius
  
  // FUNCTIONAL toString using string interpolation and pattern matching
  override def toString: String = {
    val strategyName = strategy match {
      case _: Compliant => "Compliant"
      case _: NonCompliant => "NonCompliant"
    }
    val mobilityStatus = isMobile match {
      case true => "Mobile"
      case false => "Static"  
    }
    s"Agent($id, $strategyName, pos(${position.x}%.2f,${position.y}%.2f), payoff=$payoff%.2f, radius=$radius, $mobilityStatus)"
  }
}

/**
 * FUNCTIONAL AGENT OPERATIONS - Higher-order functions and currying
 */
object AgentOps {
  
  // CURRIED AGENT FILTER FUNCTIONS
  val filterByStrategy: Strategy => List[Agent] => List[Agent] = 
    targetStrategy => agents => agents.filter(_.strategy == targetStrategy)
  
  val filterCompliant: List[Agent] => List[Agent] = filterByStrategy(Compliant())
  val filterNonCompliant: List[Agent] => List[Agent] = filterByStrategy(NonCompliant())
  
  // CURRIED MOBILITY FILTERS
  val filterByMobility: Boolean => List[Agent] => List[Agent] = 
    mobility => agents => agents.filter(_.isMobile == mobility)
  
  val filterMobile: List[Agent] => List[Agent] = filterByMobility(true)
  val filterStatic: List[Agent] => List[Agent] = filterByMobility(false)
  
  // FUNCTIONAL AGENT TRANSFORMATIONS
  val updatePayoffs: Map[Long, Double] => List[Agent] => List[Agent] = 
    payoffMap => agents => agents.map(agent => 
      agent.withPayoff(payoffMap.getOrElse(agent.id, 0.0))
    )
  
  // TAIL RECURSIVE AGENT GROUPING
  @annotation.tailrec
  def groupAgentsByRadius(
    agents: List[Agent], 
    center: Position, 
    radius: Double,
    acc: (List[Agent], List[Agent]) = (List.empty, List.empty)
  ): (List[Agent], List[Agent]) = agents match {
    case Nil => acc
    case head :: tail =>
      val (inside, outside) = acc
      head.position.isWithinRadius(center, radius) match {
        case true => groupAgentsByRadius(tail, center, radius, (head :: inside, outside))
        case false => groupAgentsByRadius(tail, center, radius, (inside, head :: outside))
      }
  }
  
  // FUNCTIONAL AGENT STATISTICS
  val calculateComplianceRate: List[Agent] => Double = agents =>
    agents match {
      case Nil => 0.0
      case nonEmpty => nonEmpty.count(_.strategy.cooperate).toDouble / nonEmpty.size
    }
  
  // HIGHER-ORDER FUNCTION for agent analysis
  def analyzeAgents[T](agents: List[Agent])(analyzer: List[Agent] => T): T = analyzer(agents)
}
