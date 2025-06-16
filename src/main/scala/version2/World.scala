package version2

import scala.util.Random

/**
 * FUNCTIONAL PANDEMIC CONTROL WORLD MODEL
 * Pure functional approach with pattern matching, currying, and immutable operations
 */
sealed trait World[A <: Agent] {
  def agents: Map[Position, A]
  def neighbors(pos: Position): List[A]
  def step: World[A]
  def worldType: String
  
  // FUNCTIONAL COMPLIANCE ANALYSIS
  lazy val complianceAnalyzer: Map[Position, A] => Double = agentMap =>
    agentMap.values.toList match {
      case Nil => 0.0
      case agentList => agentList.count(_.strategy.cooperate).toDouble / agentList.size
    }
  
  def complianceRate: Double = complianceAnalyzer(agents)
  
  // CURRIED AGENT FILTERS
  val filterByCompliance: Boolean => List[A] => List[A] = 
    isCompliant => agentList => agentList.filter(_.strategy.cooperate == isCompliant)
  
  def nonCompliantAgents: List[A] = filterByCompliance(false)(agents.values.toList)
  def compliantAgents: List[A] = filterByCompliance(true)(agents.values.toList)
  
  // PATTERN MATCHING for cluster detection
  def hasNonCompliantClusters: Boolean = nonCompliantAgents.size match {
    case n if n > 1 => true
    case _ => false
  }
}

/**
 * FUNCTIONAL CONTINUOUS WORLD IMPLEMENTATION
 * Using pattern matching, currying, and tail recursion
 */
case class ContinuousWorld[A <: Agent](
  width: Double,
  height: Double,
  agents: Map[Position, A],
  neighborType: NeighborType = RadiusNeighborhood(2.0),
  random: Random = new Random()
) extends World[A] {

  override val worldType: String = "Continuous"

  // CURRIED NEIGHBOR CALCULATION
  val neighborCalculator: Position => NeighborType => List[A] = 
    pos => neighborType => neighborType match {
      case RadiusNeighborhood(radius) =>
        agents.values.filter(agent => 
          agent.position != pos && pos.isWithinRadius(agent.position, radius)
        ).toList
    }

  override def neighbors(pos: Position): List[A] = neighborCalculator(pos)(neighborType)

  // FUNCTIONAL MOVEMENT SYSTEM - sadece sınır kısıtı; çarpışma çözümü step aşamasında ele alınır
  val movementCalculator: A => Position = agent =>
    agent.isMobile match {
      case true =>
        agent.position.randomNeighbor(1.0, random).constrainToBounds(width, height)
      case false => agent.position
    }

  // CURRIED STRATEGY UPDATE SYSTEM
  val strategyUpdater: Map[Long, Double] => A => Strategy = payoffs => agent => {
    val neighborList = neighbors(agent.position)
    
    neighborList match {
      case Nil => agent.strategy
      case nonEmpty => 
        val bestNeighborOpt = PayoffMatrix.findBestNeighbor(nonEmpty)(payoffs)
        
        bestNeighborOpt match {
          case None => agent.strategy
          case Some((bestNeighbor, bestPayoff)) =>
            val bestNeighborCount = neighbors(bestNeighbor.position).size
            
            agent.asInstanceOf[SimpleAgent].evaluateStrategyConversion(neighborList, bestPayoff, bestNeighborCount)(random.nextDouble())
        }
    }
  }

  // FUNCTIONAL SIMULATION STEP - Pure functional pipeline
  override def step: ContinuousWorld[A] = {
    /*
     * 1) BÜTÜN AJANLARI HAREKET ETTİR – tentatif konumlar hesaplanır
     * 2) ÇAKIŞMALARI ÇÖZ – aynı pozisyona birden fazla ajan gittiyse hepsi eski yerinde kalır
     * 3) YENİ DÜNYADA KAZANÇLAR + STRATEJİ GÜNCELLEMESİ – payoffs yeni komşuluklara göre hesaplanır
     */

    // 1) Tentatif hareketler
    val tentative: List[(A, Position)] = agents.values.toList.map { agent =>
      val newPos = movementCalculator(agent)
      (agent, newPos)
    }

    // 2) Çakışma çözümü – sert gövde (disk) çakışmalarını önle
    val resolvedPairs: List[(Position, A)] = {
      // Kümülatif olarak kabul edilen ajanları biriktir
      tentative.foldLeft(List.empty[(Position, A)]) { (accepted, curr) =>
        val (agent, proposed) = curr
        val collides = accepted.exists { case (posOther, agentOther) =>
          proposed.distanceTo(posOther) < (agent.radius + agentOther.radius)
        }
        if (!collides) {
          // yeni pozisyon kabul
          (proposed, agent.withPosition(proposed).asInstanceOf[A]) :: accepted
        } else {
          // eski pozisyonda kal, yine çarpışma olmamasını kontrol et
          val fallbackPos = agent.position
          val collidesFallback = accepted.exists { case (posOther, agentOther) =>
            fallbackPos.distanceTo(posOther) < (agent.radius + agentOther.radius)
          }
          val finalPos = if (collidesFallback) proposed else fallbackPos // nadiren iki eski agent çakışabilir
          (finalPos, agent.withPosition(finalPos).asInstanceOf[A]) :: accepted
        }
      }
    }
     
    val afterMoveAgents: Map[Position, A] = resolvedPairs.toMap

    // 3) Yeni dünyayı oluştur ve payoffs+strateji güncelle
    val worldAfterMove: ContinuousWorld[A] = this.copy(agents = afterMoveAgents)

    val payoffs = PayoffMatrix.computeAllPayoffs(worldAfterMove)

    val finalAgents: Map[Position, A] = afterMoveAgents.values.map { agent =>
      val agentWithPayoff = agent.withPayoff(payoffs.getOrElse(agent.id, 0.0)).asInstanceOf[A]
      val newStr = worldAfterMove.strategyUpdater(payoffs)(agentWithPayoff)
      val finalAgent = agentWithPayoff.withStrategy(newStr).asInstanceOf[A]
      finalAgent.position -> finalAgent
    }.toMap

    copy(agents = finalAgents)
  }

  // FUNCTIONAL CLUSTER ANALYSIS with tail recursion
  def analyzeNonCompliantClusters: ClusterAnalysis = {
    val nonCompliantPositions = agents.collect {
      case (pos, agent) if !agent.strategy.cooperate => pos
    }.toList
    
    nonCompliantPositions match {
      case Nil => ClusterAnalysis(0, 0.0, List.empty)
      case positions => 
        val clusters = PositionOps.findClusters(positions, neighborType.getRadius)
        val avgSize = clusters match {
          case Nil => 0.0
          case nonEmpty => nonEmpty.map(_.size).sum.toDouble / nonEmpty.size
        }
        ClusterAnalysis(clusters.size, avgSize, clusters)
    }
  }
}

/**
 * FUNCTIONAL CLUSTER ANALYSIS - Using pattern matching and Options
 */
case class ClusterAnalysis(
  clusterCount: Int,
  averageClusterSize: Double,
  clusters: List[List[Position]]
) {
  
  lazy val largestClusterSize: Int = clusters match {
    case Nil => 0
    case nonEmpty => nonEmpty.map(_.size).max
  }
  
  val hasLargeClusters: Int => Boolean = threshold => 
    largestClusterSize >= threshold
  
  // PATTERN MATCHING for cluster classification
  lazy val clusterType: String = (clusterCount, largestClusterSize) match {
    case (0, _) => "No clusters"
    case (1, size) if size < 5 => "Single small cluster"
    case (1, _) => "Single large cluster"
    case (count, maxSize) if count > 5 && maxSize > 10 => "Many large clusters"
    case (count, _) if count > 5 => "Many small clusters"
    case _ => "Few moderate clusters"
  }
  
  override def toString: String = 
    s"ClusterAnalysis($clusterType: count=$clusterCount, avgSize=$averageClusterSize%.1f, largest=$largestClusterSize)"
}

/**
 * FUNCTIONAL WORLD OPERATIONS - Higher-order functions and currying
 */
object WorldOps {
  
  // CURRIED WORLD TRANSFORMATIONS
  val scaleWorld: Double => ContinuousWorld[SimpleAgent] => ContinuousWorld[SimpleAgent] = 
    factor => world => world.copy(
      width = world.width * factor,
      height = world.height * factor,
      agents = world.agents.map { case (pos, agent) => 
        Position(pos.x * factor, pos.y * factor) -> agent.withPosition(Position(pos.x * factor, pos.y * factor))
      }
    )

  // FUNCTIONAL WORLD ANALYSIS
  val analyzeWorldState: ContinuousWorld[SimpleAgent] => Map[String, Any] = world => Map(
    "worldType" -> "Continuous",
    "complianceRate" -> world.complianceRate,  
    "totalAgents" -> world.agents.size,
    "nonCompliantClusters" -> world.analyzeNonCompliantClusters.clusterCount,
    "largestCluster" -> world.analyzeNonCompliantClusters.largestClusterSize,
    "worldDensity" -> (world.agents.size.toDouble / (world.width * world.height))
  )
  
  // TAIL RECURSIVE SIMULATION RUNNER - for SimpleAgent specifically  
  @annotation.tailrec
  def runSimulationSteps(
    world: ContinuousWorld[SimpleAgent], 
    steps: Int, 
    history: List[ContinuousWorld[SimpleAgent]] = List.empty
  ): List[ContinuousWorld[SimpleAgent]] = steps match {
    case 0 => world :: history
    case n => runSimulationSteps(world.step, n - 1, world :: history)
  }
  
  // GENERIC VERSION for any Agent type
  @annotation.tailrec
  def runSimulationStepsGeneric[A <: Agent](
    world: World[A], 
    steps: Int, 
    history: List[World[A]] = List.empty
  ): List[World[A]] = steps match {
    case 0 => world :: history
    case n => runSimulationStepsGeneric(world.step, n - 1, world :: history)
  }
  
  // LAZY SIMULATION STREAM
  def simulationStream[A <: Agent](initialWorld: World[A]): LazyList[World[A]] = 
    LazyList.iterate(initialWorld)(_.step)
  
  // CURRIED WORLD FILTERS
  val filterWorldsByCompliance: Double => List[ContinuousWorld[SimpleAgent]] => List[ContinuousWorld[SimpleAgent]] = 
    minCompliance => worlds => worlds.filter(_.complianceRate >= minCompliance)

  // FUNCTIONAL WORLD STATISTICS
  val calculateWorldStats: List[ContinuousWorld[SimpleAgent]] => Map[String, Double] = worlds => {
    worlds match {
      case Nil => Map("avgCompliance" -> 0.0, "maxCompliance" -> 0.0, "minCompliance" -> 0.0)
      case nonEmpty => 
        val compliances = nonEmpty.map(_.complianceRate)
        Map(
          "avgCompliance" -> (compliances.sum / compliances.size),
          "maxCompliance" -> compliances.max,
          "minCompliance" -> compliances.min
        )
    }
  }
  
  // HIGHER-ORDER FUNCTION for world analysis
  def analyzeWorlds[T](worlds: List[ContinuousWorld[SimpleAgent]])(analyzer: List[ContinuousWorld[SimpleAgent]] => T): T = 
    analyzer(worlds)
}