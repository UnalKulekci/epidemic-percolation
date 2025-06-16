package version2

/**
 * Basit komşuluk modeli - sadece radius tabanlı etkileşim
 */
sealed trait NeighborType {
  def getRadius: Double
  
  def getNeighbors(center: Position, allPositions: Iterable[Position]): List[Position] = {
    allPositions.filter(pos => 
      pos != center && center.isWithinRadius(pos, getRadius)
    ).toList
  }
}

/**
 * Radius tabanlı komşuluk
 */
case class RadiusNeighborhood(radius: Double) extends NeighborType {
  override def getRadius: Double = radius
}

/**
 * FUNCTIONAL NEIGHBOR OPERATIONS - Higher-order functions and currying
 */
object NeighborOps {
  
  // CURRIED NEIGHBORHOOD FILTERS
  val filterByDistance: Double => Position => List[Position] => List[Position] = 
    maxDistance => center => positions => 
      positions.filter(pos => center.distanceTo(pos) <= maxDistance)
  
  // FUNCTIONAL NEIGHBORHOOD ANALYSIS
  val calculateNeighborhoodDensity: Position => List[Position] => Double => Double = 
    center => positions => radius => {
      val area = math.Pi * radius * radius
      val neighborCount = positions.count(pos => center.isWithinRadius(pos, radius))
      neighborCount.toDouble / area
    }
  
  // CURRIED NEIGHBORHOOD STATISTICS
  val analyzeNeighborhoods: List[Position] => NeighborType => Map[String, Double] = 
    positions => neighborType => {
      val neighborhoods = positions.map(center => neighborType.getNeighbors(center, positions).size)
      neighborhoods match {
        case Nil => Map("avg" -> 0.0, "max" -> 0.0, "min" -> 0.0)
        case nonEmpty => Map(
          "avg" -> (nonEmpty.sum.toDouble / nonEmpty.size),
          "max" -> nonEmpty.max.toDouble,
          "min" -> nonEmpty.min.toDouble
        )
      }
    }
  
  // PATTERN MATCHING for neighbor type classification
  val classifyNeighborType: NeighborType => String = {
    case RadiusNeighborhood(radius) if radius < 1.0 => "VeryLocal"
    case RadiusNeighborhood(radius) if radius < 2.0 => "Local" 
    case RadiusNeighborhood(radius) if radius < 4.0 => "Extended"
    case RadiusNeighborhood(_) => "WideRange"
  }
  
  // FUNCTIONAL NEIGHBOR TRANSFORMATIONS
  val scaleNeighborhood: Double => NeighborType => NeighborType = 
    factor => neighborType => neighborType match {
      case RadiusNeighborhood(radius) => RadiusNeighborhood(radius * factor)
    }
  
  // HIGHER-ORDER FUNCTION for neighborhood analysis
  def analyzeNeighborhoods[T](positions: List[Position], neighborType: NeighborType)(analyzer: List[List[Position]] => T): T = {
    val neighborhoods = positions.map(pos => neighborType.getNeighbors(pos, positions))
    analyzer(neighborhoods)
  }
}
