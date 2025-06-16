package version2

/**
 * FUNCTIONAL POSITION MODEL FOR CONTINUOUS 2D SPACE
 * Pure functional operations with pattern matching, currying, and lazy evaluation
 */
case class Position(x: Double, y: Double) {
  
  // PURE DISTANCE CALCULATION - no side effects
  lazy val distanceCalculator: Position => Double = other =>
    math.sqrt(math.pow(x - other.x, 2) + math.pow(y - other.y, 2))
  
  // CURRIED RADIUS CHECK
  val withinRadius: Double => Position => Boolean = 
    radius => other => distanceCalculator(other) <= radius
  
  // FUNCTIONAL MOVEMENT WITH PATTERN MATCHING
  val moveTowards: (Position, Double) => Position = (target, stepSize) => {
    val distance = distanceCalculator(target)
    distance <= stepSize match {
      case true => target
      case false => 
        val ratio = stepSize / distance
        Position(
          x + (target.x - x) * ratio,
          y + (target.y - y) * ratio
        )
    }
  }
  
  // CURRIED RANDOM MOVEMENT GENERATOR
  val randomMovement: Double => scala.util.Random => Position = 
    stepSize => random => {
      val angle = random.nextDouble() * 2 * math.Pi
      Position(
        x + stepSize * math.cos(angle),
        y + stepSize * math.sin(angle)
      )
    }
  
  // PATTERN MATCHING for boundary checking
  def constrainToBounds(width: Double, height: Double): Position = 
    Position(
      math.max(0.0, math.min(width - 0.1, x)),
      math.max(0.0, math.min(height - 0.1, y))
    )
  
  // FUNCTIONAL DISTANCE TO MULTIPLE POSITIONS
  lazy val distancesToPositions: List[Position] => List[(Position, Double)] = 
    positions => positions.map(pos => pos -> distanceCalculator(pos))
  
  // CONVENIENCE METHODS using functional composition
  def distanceTo(other: Position): Double = distanceCalculator(other)
  def isWithinRadius(other: Position, radius: Double): Boolean = withinRadius(radius)(other)
  def randomNeighbor(stepSize: Double, random: scala.util.Random): Position = randomMovement(stepSize)(random)
}

/**
 * FUNCTIONAL POSITION OPERATIONS - Higher-order functions and currying
 */
object PositionOps {
  
  // CURRIED POSITION FILTERS
  val filterByDistance: Double => Position => List[Position] => List[Position] = 
    maxDistance => center => positions => 
      positions.filter(pos => center.distanceTo(pos) <= maxDistance)
  
  // POSITION CLUSTERING WITH TAIL RECURSION
  @annotation.tailrec
  def findClusters(
    positions: List[Position], 
    radius: Double,
    acc: List[List[Position]] = List.empty
  ): List[List[Position]] = positions match {
    case Nil => acc
    case head :: tail =>
      // Expand transitive neighborhood with BFS
      val (cluster, remaining) = bfsExtractComponent(head, tail, radius)
      findClusters(remaining, radius, cluster :: acc)
  }
  
  /**
   * Extracts all connected components with transitive neighborhood from a starting point.
   */
  private def bfsExtractComponent(
    start: Position,
    others: List[Position],
    radius: Double
  ): (List[Position], List[Position]) = {
    @annotation.tailrec
    def loop(frontier: List[Position], remaining: List[Position], component: Set[Position]): (Set[Position], List[Position]) = frontier match {
      case Nil => (component, remaining)
      case current :: rest =>
        val (nbrs, nonNbrs) = remaining.partition(pos => current.isWithinRadius(pos, radius))
        loop(rest ::: nbrs, nonNbrs, component ++ nbrs + current)
    }

    val (compSet, rem) = loop(List(start), others, Set.empty)
    (compSet.toList, rem)
  }
  
  // FUNCTIONAL POSITION TRANSFORMATIONS
  val translatePositions: (Double, Double) => List[Position] => List[Position] = 
    (dx, dy) => positions => positions.map(pos => Position(pos.x + dx, pos.y + dy))
  
  val scalePositions: Double => List[Position] => List[Position] = 
    factor => positions => positions.map(pos => Position(pos.x * factor, pos.y * factor))
  
  // CURRIED BOUNDARY CONSTRAINT
  val constrainAllToBounds: (Double, Double) => List[Position] => List[Position] = 
    (width, height) => positions => positions.map(_.constrainToBounds(width, height))
  
  // FUNCTIONAL STATISTICS
  val calculateCentroid: List[Position] => Option[Position] = positions =>
    positions match {
      case Nil => None
      case nonEmpty => 
        val (totalX, totalY) = nonEmpty.foldLeft((0.0, 0.0)) { case ((sumX, sumY), pos) =>
          (sumX + pos.x, sumY + pos.y)
        }
        Some(Position(totalX / nonEmpty.size, totalY / nonEmpty.size))
    }
  
  // CURRIED DISTANCE MATRIX CALCULATION
  val calculateDistanceMatrix: List[Position] => Map[(Position, Position), Double] = 
    positions => {
      for {
        pos1 <- positions
        pos2 <- positions
        if pos1 != pos2
      } yield (pos1, pos2) -> pos1.distanceTo(pos2)
    }.toMap
  
  // HIGHER-ORDER FUNCTION for position analysis
  def analyzePositions[T](positions: List[Position])(analyzer: List[Position] => T): T = 
    analyzer(positions)
  
  // LAZY STREAM of random positions
  def randomPositionStream(width: Double, height: Double)(random: scala.util.Random): LazyList[Position] = 
    LazyList.continually(Position(random.nextDouble() * width, random.nextDouble() * height))
}
