package version2

import scala.annotation.tailrec

/**
 * FUNCTIONAL PERCOLATION ANALYZER
 * Gerçek percolation tespiti: Defector kümelerin boundary-to-boundary connection
 * Pure functional approach with pattern matching and tail recursion
 */
object PercolationAnalyzer {
  
  /**
   * PERCOLATION RESULT - Immutable data structure
   */
  case class PercolationResult(
    hasHorizontalPercolation: Boolean,
    hasVerticalPercolation: Boolean,
    hasAnyPercolation: Boolean,
    largestConnectedComponent: Int,
    percolatingClusterSizes: List[Int],
    boundaryTouchingClusters: Int
  ) {
    lazy val percolationType: String = (hasHorizontalPercolation, hasVerticalPercolation) match {
      case (true, true) => "Both_Directions"
      case (true, false) => "Horizontal_Only"
      case (false, true) => "Vertical_Only"
      case (false, false) => "No_Percolation"
    }
    
    override def toString: String = 
      s"PercolationResult($percolationType, largest=${largestConnectedComponent}, touching=${boundaryTouchingClusters})"
  }
  
  /**
   * BOUNDARY CLASSIFICATION - Pattern matching for boundary detection
   */
  sealed trait BoundaryType
  case object LeftBoundary extends BoundaryType
  case object RightBoundary extends BoundaryType
  case object TopBoundary extends BoundaryType
  case object BottomBoundary extends BoundaryType
  case object NoBoundary extends BoundaryType
  
  /**
   * CURRIED BOUNDARY DETECTOR
   */
  val boundaryClassifier: (Double, Double) => Position => BoundaryType = 
    (worldWidth, worldHeight) => position => {
      val tolerance = 0.1  // Boundary proximity tolerance
      
      (position.x, position.y) match {
        case (x, _) if x <= tolerance => LeftBoundary
        case (x, _) if x >= worldWidth - tolerance => RightBoundary
        case (_, y) if y <= tolerance => BottomBoundary
        case (_, y) if y >= worldHeight - tolerance => TopBoundary
        case _ => NoBoundary
      }
    }
  
  /**
   * MAIN PERCOLATION ANALYSIS - Pattern matching approach
   */
  def analyzePercolation[A <: Agent](world: ContinuousWorld[A]): PercolationResult = {
    // Get all defector positions
    val defectorPositions = world.agents.collect {
      case (pos, agent) if !agent.strategy.cooperate => pos
    }.toList
    
    defectorPositions match {
      case Nil => PercolationResult(false, false, false, 0, List.empty, 0)
      case positions => performPercolationAnalysis(positions, world.width, world.height, world.neighborType)
    }
  }
  
  /**
   * CORE PERCOLATION COMPUTATION - Functional approach
   */
  private def performPercolationAnalysis(
    positions: List[Position], 
    worldWidth: Double, 
    worldHeight: Double,
    neighborType: NeighborType
  ): PercolationResult = {
    
    val boundaryDetector = boundaryClassifier(worldWidth, worldHeight)
    val connectedComponents = findConnectedComponents(positions, neighborType)
    
    // Analyze each component for boundary connections
    val componentAnalysis = connectedComponents.map { component =>
      analyzeComponentBoundaries(component, boundaryDetector)
    }
    
    val percolatingComponents = componentAnalysis.filter(_.isPercolating)
    val boundaryTouchingCount = componentAnalysis.count(_.touchesBoundary)
    
    val hasHorizontal = percolatingComponents.exists(_.hasHorizontalPercolation)
    val hasVertical = percolatingComponents.exists(_.hasVerticalPercolation)
    val hasAny = hasHorizontal || hasVertical
    
    val largestComponent = if (connectedComponents.nonEmpty) connectedComponents.map(_.size).max else 0
    val percolatingClusterSizes = percolatingComponents.map(_.clusterSize)
    
    PercolationResult(
      hasHorizontal, hasVertical, hasAny, 
      largestComponent, percolatingClusterSizes, boundaryTouchingCount
    )
  }
  
  /**
   * CONNECTED COMPONENTS FINDER - Tail recursive approach
   */
  @tailrec
  private def findConnectedComponents(
    positions: List[Position], 
    neighborType: NeighborType,
    components: List[List[Position]] = List.empty
  ): List[List[Position]] = positions match {
    case Nil => components
    case head :: tail =>
      val (component, remaining) = extractConnectedComponent(head, tail, neighborType)
      findConnectedComponents(remaining, neighborType, (head :: component) :: components)
  }
  
  /**
   * COMPONENT EXTRACTION - BFS-style functional approach
   */
  private def extractConnectedComponent(
    start: Position, 
    candidates: List[Position],
    neighborType: NeighborType
  ): (List[Position], List[Position]) = {
    
    @tailrec
    def expandComponent(
      current: List[Position],
      remaining: List[Position],
      component: List[Position] = List.empty
    ): (List[Position], List[Position]) = current match {
      case Nil => (component, remaining)
      case head :: tail =>
        val (neighbors, nonNeighbors) = remaining.partition(pos => areNeighbors(head, pos, neighborType))
        expandComponent(tail ::: neighbors, nonNeighbors, head :: component)
    }
    
    expandComponent(List(start), candidates)
  }
  
  /**
   * NEIGHBOR CHECK - Pattern matching for different neighbor types
   */
  private def areNeighbors(pos1: Position, pos2: Position, neighborType: NeighborType): Boolean = 
    neighborType match {
      case RadiusNeighborhood(radius) => pos1.isWithinRadius(pos2, radius)
    }
  
  /**
   * COMPONENT BOUNDARY ANALYSIS
   */
  case class ComponentBoundaryAnalysis(
    clusterSize: Int,
    boundariesTouched: Set[BoundaryType],
    hasHorizontalPercolation: Boolean,
    hasVerticalPercolation: Boolean,
    touchesBoundary: Boolean,
    isPercolating: Boolean
  )
  
  private def analyzeComponentBoundaries(
    component: List[Position], 
    boundaryDetector: Position => BoundaryType
  ): ComponentBoundaryAnalysis = {
    
    val boundariesTouched = component.map(boundaryDetector).filter(_ != NoBoundary).toSet
    
    val hasHorizontal = boundariesTouched.contains(LeftBoundary) && boundariesTouched.contains(RightBoundary)
    val hasVertical = boundariesTouched.contains(TopBoundary) && boundariesTouched.contains(BottomBoundary)
    val touchesBoundary = boundariesTouched.nonEmpty
    val isPercolating = hasHorizontal || hasVertical
    
    ComponentBoundaryAnalysis(
      component.size, boundariesTouched, hasHorizontal, hasVertical, touchesBoundary, isPercolating
    )
  }
  
  /**
   * FUNCTIONAL PERCOLATION PROBABILITY CALCULATOR
   */
  val calculatePercolationProbability: List[PercolationResult] => Double = results =>
    results match {
      case Nil => 0.0
      case nonEmpty => nonEmpty.count(_.hasAnyPercolation).toDouble / nonEmpty.size
    }
  
  /**
   * CURRIED PERCOLATION FILTER
   */
  val filterPercolatingRuns: Boolean => List[PercolationResult] => List[PercolationResult] = 
    requirePercolation => results => results.filter(_.hasAnyPercolation == requirePercolation)
  
  /**
   * PERCOLATION STATISTICS CALCULATOR
   */
  def calculatePercolationStats(results: List[PercolationResult]): Map[String, Double] = {
    results match {
      case Nil => Map(
        "percolation_probability" -> 0.0,
        "avg_largest_component" -> 0.0,
        "horizontal_percolation_prob" -> 0.0,
        "vertical_percolation_prob" -> 0.0
      )
      case nonEmpty =>
        val percolationProb = nonEmpty.count(_.hasAnyPercolation).toDouble / nonEmpty.size
        val avgLargestComponent = nonEmpty.map(_.largestConnectedComponent.toDouble).sum / nonEmpty.size
        val horizontalProb = nonEmpty.count(_.hasHorizontalPercolation).toDouble / nonEmpty.size
        val verticalProb = nonEmpty.count(_.hasVerticalPercolation).toDouble / nonEmpty.size
        
        Map(
          "percolation_probability" -> percolationProb,
          "avg_largest_component" -> avgLargestComponent,
          "horizontal_percolation_prob" -> horizontalProb,
          "vertical_percolation_prob" -> verticalProb
        )
    }
  }
  
  /**
   * HIGHER-ORDER FUNCTION: Apply percolation analysis to simulation history
   */
  def analyzeSimulationPercolation[A <: Agent](
    worldHistory: List[ContinuousWorld[A]]
  ): List[PercolationResult] = 
    worldHistory.map(analyzePercolation)
  
  /**
   * PATTERN MATCHING: Classify percolation strength
   */
  def classifyPercolationStrength(result: PercolationResult): String = 
    (result.hasAnyPercolation, result.largestConnectedComponent, result.boundaryTouchingClusters) match {
      case (false, _, _) => "No_Percolation"
      case (true, large, _) if large > 100 => "Strong_Percolation"
      case (true, medium, touching) if medium > 50 && touching > 2 => "Moderate_Percolation"
      case (true, _, _) => "Weak_Percolation"
    }
  
  /**
   * GEOMETRIC (STRATEJİDEN BAĞIMSIZ) PERCOLATION KONTROLÜ
   * Dünya içindeki TÜM ajan pozisyonları kullanılır.
   */
  def geometricPercolates[A <: Agent](world: ContinuousWorld[A]): Boolean = {
    val allPositions = world.agents.keys.toList
    if (allPositions.isEmpty) false
    else performPercolationAnalysis(allPositions, world.width, world.height, world.neighborType).hasAnyPercolation
  }
} 