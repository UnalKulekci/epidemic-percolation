package version2

import scala.util.Random
import java.io.{File, PrintWriter}

object SimpleTest {
  
  def main(args: Array[String]): Unit = {
    val agentCounts = List(10, 20, 30, 40, 50, 60)
    val worldSize = 10.0
    val steps = 50
    
    val allResults = agentCounts.map { agentCount =>
      val world = createSimpleWorld(worldSize, agentCount)
      val results = runSimpleSimulation(world, steps)
      analyzeResults(results, agentCount)
    }
    
    saveResultsToJson(allResults)
    println("Results saved to density_test_results.json")
  }
  
  def createSimpleWorld(worldSize: Double, numAgents: Int): ContinuousWorld[SimpleAgent] = {
    val random = new Random(42)
    
    val agents = (1 to numAgents).map { id =>
      val pos = Position(
        random.nextDouble() * worldSize, 
        random.nextDouble() * worldSize
      )
      val strategy = if (random.nextDouble() < 0.8) Compliant() else NonCompliant()
      
      pos -> SimpleAgent(
        id = id.toLong,
        strategy = strategy,
        position = pos,
        radius = 1.5,
        isMobile = true
      )
    }.toMap
    
    ContinuousWorld(
      width = worldSize,
      height = worldSize,
      agents = agents,
      neighborType = RadiusNeighborhood(2.0),
      random = random
    )
  }
  
  def runSimpleSimulation(
    initialWorld: ContinuousWorld[SimpleAgent], 
    steps: Int
  ): List[ContinuousWorld[SimpleAgent]] = {
    val history = WorldOps.runSimulationSteps(initialWorld, steps)
    history.reverse
  }
  
  case class DensityTestResult(
    agentCount: Int,
    density: Double,
    initialCompliance: Double,
    finalCompliance: Double,
    avgCompliance: Double,
    minCompliance: Double,
    maxCompliance: Double,
    clusterCount: Int,
    largestClusterSize: Int,
    avgClusterSize: Double,
    percolationProbability: Double,
    horizontalPercolation: Double,
    verticalPercolation: Double,
    timeSeries: List[(Int, Double)],
    trend: String,
    outcome: String
  )
  
  def analyzeResults(worldHistory: List[ContinuousWorld[SimpleAgent]], agentCount: Int): DensityTestResult = {
    val finalWorld = worldHistory.last
    val stats = WorldOps.calculateWorldStats(worldHistory)
    
    val clusterAnalysis = finalWorld.analyzeNonCompliantClusters
    val percolationResults = worldHistory.map(PercolationAnalyzer.analyzePercolation)
    val percolationStats = PercolationAnalyzer.calculatePercolationStats(percolationResults)
    
    val complianceOverTime = worldHistory.zipWithIndex.map { case (world, step) =>
      step -> world.complianceRate
    }
    
    val startCompliance = complianceOverTime.head._2
    val endCompliance = complianceOverTime.last._2
    val trend = if (endCompliance > startCompliance) "increase" 
               else if (endCompliance < startCompliance) "decrease" 
               else "stable"
    
    val outcome = if (endCompliance < 0.3) "non_compliant_dominant"
                 else if (endCompliance > 0.7) "compliant_dominant"
                 else "coexistence"
    
    val density = agentCount.toDouble / (10.0 * 10.0)
    
    DensityTestResult(
      agentCount = agentCount,
      density = density,
      initialCompliance = startCompliance,
      finalCompliance = endCompliance,
      avgCompliance = stats("avgCompliance"),
      minCompliance = stats("minCompliance"),
      maxCompliance = stats("maxCompliance"),
      clusterCount = clusterAnalysis.clusterCount,
      largestClusterSize = clusterAnalysis.largestClusterSize,
      avgClusterSize = clusterAnalysis.averageClusterSize,
      percolationProbability = percolationStats("percolation_probability"),
      horizontalPercolation = percolationStats("horizontal_percolation_prob"),
      verticalPercolation = percolationStats("vertical_percolation_prob"),
      timeSeries = complianceOverTime,
      trend = trend,
      outcome = outcome
    )
  }
  
  def saveResultsToJson(results: List[DensityTestResult]): Unit = {
    val json = s"""{
  "experiment_type": "density_test",
  "world_size": 10.0,
  "simulation_steps": 50,
  "initial_non_compliant_ratio": 0.2,
  "results": [
${results.map(resultToJson).mkString(",\n")}
  ]
}"""
    
    val writer = new PrintWriter(new File("density_test_results.json"))
    try {
      writer.write(json)
    } finally {
      writer.close()
    }
  }
  
  def resultToJson(result: DensityTestResult): String = {
    val timeSeriesJson = result.timeSeries.map { case (step, compliance) =>
      s"""{"step": $step, "compliance": $compliance}"""
    }.mkString("[", ", ", "]")
    
    s"""    {
      "agent_count": ${result.agentCount},
      "density": ${result.density},
      "initial_compliance": ${result.initialCompliance},
      "final_compliance": ${result.finalCompliance},
      "avg_compliance": ${result.avgCompliance},
      "min_compliance": ${result.minCompliance},
      "max_compliance": ${result.maxCompliance},
      "cluster_count": ${result.clusterCount},
      "largest_cluster_size": ${result.largestClusterSize},
      "avg_cluster_size": ${result.avgClusterSize},
      "percolation_probability": ${result.percolationProbability},
      "horizontal_percolation": ${result.horizontalPercolation},
      "vertical_percolation": ${result.verticalPercolation},
      "time_series": $timeSeriesJson,
      "trend": "${result.trend}",
      "outcome": "${result.outcome}"
    }"""
  }
} 