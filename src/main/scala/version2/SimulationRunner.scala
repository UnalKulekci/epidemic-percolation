package version2

import java.io.{File, PrintWriter}
import java.util.Locale
import scala.util.Random
import scala.collection.parallel.CollectionConverters._

/**
 * PHASE-DIAGRAM SIMULATION RUNNER
 * --------------------------------
 * Federal Office for Health için parametre taraması:
 *   - Başlangıç defector oranı (0.1 - 0.9)
 *   - Temptation değeri (1.1 - 1.5)
 *   - Ajan yoğunluğu (10 - 100)
 * Her kombinasyon için ölçülen metrikler:
 *   – avgCompliance : Son adım uyum oranı
 *   – defectorPercolationProb : Uyumsuz kümelerin perkolasyon olasılığı
 *   – geometricPercolationProb : Tüm ajanların oluşturduğu ağın perkolasyonu
 *   – avgLargestClusterFrac : En büyük uyumsuz kümenin büyüklüğü / N
 */
object SimulationRunner {

  // ----------------- Genel Ayarlar -----------------
  val worldSize: Double = 10.0
  val steps: Int = 500
  val runsPerSetting: Int = 20

  // ----------------- Tarama Parametreleri -----------------
  val defectorRatios: List[Double] = (1 to 9).map(_ / 10.0).toList
  val temptationValues: List[Double] = List(1.1, 1.3, 1.5, 1.7, 2.0)
  val agentCounts: List[Int] = (10 to 100 by 10).toList

  // ----------------- Fiziksel Parametreler -----------------
  val exclusionRadius: Double = 0.5
  val interactionRadius: Double = 2.0

  // ----------------- Parametre ve Metrik Sınıfları -----------------
  case class SimulationParams(
    agentCount: Int,
    defectorRatio: Double,
    temptation: Double
  )

  case class RunMetrics(
    finalCompliance: Double,
    defectorPercolation: Boolean,
    geometricPercolation: Boolean,
    largestClusterFrac: Double
  )

  case class SettingMetrics(
    params: SimulationParams,
    density: Double,
    avgCompliance: Double,
    defectorPercolationProb: Double,
    geometricPercolationProb: Double,
    avgLargestClusterFrac: Double
  )

  // ----------------- Ana Program -----------------
  def main(args: Array[String]): Unit = {
    // Tüm parametre kombinasyonlarını oluştur
    val allParams = for {
      n <- agentCounts
      ratio <- defectorRatios
      t <- temptationValues
    } yield SimulationParams(n, ratio, t)

    println(s"Toplam ${allParams.size} parametre kombinasyonu test edilecek...")

    // Paralel parametre taraması
    val results = allParams.par.map(runParameterSetting).toList
      .sortBy(m => (m.params.agentCount, m.params.defectorRatio, m.params.temptation))

    writeCsv("phase_diagram_results.csv", results)
    writeJson("phase_diagram_results.json", results)
    println("Sonuçlar CSV ve JSON dosyalarına kaydedildi.")
  }

  // ----------------- Tek Parametre Seti Simülasyonu -----------------
  private def runParameterSetting(params: SimulationParams): SettingMetrics = {
    val metricsPerRun = (1 to runsPerSetting).toList.map { runIdx =>
      val random = new Random(System.nanoTime() + runIdx)
      val world0 = createWorld(params, random)
      val history = WorldOps.runSimulationSteps(world0, steps)
      val finalWorld = history.last

      RunMetrics(
        finalCompliance = finalWorld.complianceRate,
        defectorPercolation = PercolationAnalyzer.analyzePercolation(finalWorld).hasAnyPercolation,
        geometricPercolation = PercolationAnalyzer.geometricPercolates(finalWorld),
        largestClusterFrac = finalWorld.analyzeNonCompliantClusters.largestClusterSize.toDouble / params.agentCount
      )
    }

    aggregateMetrics(params, metricsPerRun)
  }

  // ----------------- Dünya Oluşturucu -----------------
  private def createWorld(params: SimulationParams, rnd: Random): ContinuousWorld[SimpleAgent] = {
    val agents = (1 to params.agentCount).map { id =>
      val pos = Position(rnd.nextDouble() * worldSize, rnd.nextDouble() * worldSize)
      val strategy = if (rnd.nextDouble() >= params.defectorRatio) Compliant() else NonCompliant()
      pos -> SimpleAgent(
        id = id.toLong,
        strategy = strategy,
        position = pos,
        radius = exclusionRadius,
        isMobile = true
      )
    }.toMap

    ContinuousWorld(
      width = worldSize,
      height = worldSize,
      agents = agents,
      neighborType = RadiusNeighborhood(interactionRadius),
      random = rnd
    )
  }

  // ----------------- Metrik Hesaplayıcı -----------------
  private def aggregateMetrics(params: SimulationParams, runs: List[RunMetrics]): SettingMetrics = {
    val n = runs.size.toDouble
    SettingMetrics(
      params = params,
      density = params.agentCount.toDouble / (worldSize * worldSize),
      avgCompliance = runs.map(_.finalCompliance).sum / n,
      defectorPercolationProb = runs.count(_.defectorPercolation).toDouble / n,
      geometricPercolationProb = runs.count(_.geometricPercolation).toDouble / n,
      avgLargestClusterFrac = runs.map(_.largestClusterFrac).sum / n
    )
  }

  // ----------------- CSV Yazıcı -----------------
  private def writeCsv(path: String, results: List[SettingMetrics]): Unit = {
    val header = "agentCount,defectorRatio,temptation,density,avgCompliance,defectorPercolationProb,geometricPercolationProb,avgLargestClusterFrac"
    val lines = results.map { r =>
      String.format(
        Locale.US,
        "%d,%.2f,%.2f,%.4f,%.4f,%.4f,%.4f,%.4f",
        Int.box(r.params.agentCount),
        Double.box(r.params.defectorRatio),
        Double.box(r.params.temptation),
        Double.box(r.density),
        Double.box(r.avgCompliance),
        Double.box(r.defectorPercolationProb),
        Double.box(r.geometricPercolationProb),
        Double.box(r.avgLargestClusterFrac)
      )
    }
    val writer = new PrintWriter(new File(path))
    try {
      writer.println(header)
      lines.foreach(writer.println)
    } finally writer.close()
  }

  // ----------------- JSON Yazıcı -----------------
  private def writeJson(path: String, results: List[SettingMetrics]): Unit = {
    val header = String.format(Locale.US,
      """{"world_size": %.1f, "steps": %d, "runs_per_setting": %d, "results": [""",
      Double.box(worldSize), Int.box(steps), Int.box(runsPerSetting))

    val entries = results.map { r =>
      String.format(Locale.US,
        """{
          |  "agent_count": %d,
          |  "defector_ratio": %.2f,
          |  "temptation": %.2f,
          |  "density": %.4f,
          |  "avg_compliance": %.4f,
          |  "defector_percolation_prob": %.4f,
          |  "geometric_percolation_prob": %.4f,
          |  "avg_largest_cluster_frac": %.4f
          |}""".stripMargin,
        Int.box(r.params.agentCount),
        Double.box(r.params.defectorRatio),
        Double.box(r.params.temptation),
        Double.box(r.density),
        Double.box(r.avgCompliance),
        Double.box(r.defectorPercolationProb),
        Double.box(r.geometricPercolationProb),
        Double.box(r.avgLargestClusterFrac)
      )
    }

    val content = header + entries.mkString(",\n") + "]}"
    val writer = new PrintWriter(new File(path))
    try {
      writer.write(content)
    } finally writer.close()
  }
}