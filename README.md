# Epidemic Percolation Analysis

Analysis of collective behavior spread using prisoner's dilemma theory and percolation analysis.

## Implementation Design & Modeling Assumptions

### Core Model Components

1. **Continuous Space Model**
   - Agents move in a continuous 2D space (not grid-based)
   - Each agent has:
     - Physical radius (rP) for hard-disk collisions
     - Interaction radius (rint) for strategy interactions
     - Binary strategy (Compliant/NonCompliant)

2. **Time Evolution**
   - Discrete time steps with synchronous updates
   - Each step consists of three phases:
     1. Movement: All agents calculate tentative positions
     2. Collision Resolution: Hard-disk constraints enforced
     3. Strategy Update: Based on neighborhood payoffs

3. **State Management**
   - Local State: Each agent's position, strategy, and payoff
   - Global State: Percolation analysis and cluster formation
   - All state updates are immutable (functional approach)

### Modeling Assumptions

1. **Physical Constraints**
   - Agents cannot overlap (hard-disk model)
   - Periodic boundary conditions
   - Random walk mobility with constant step size

2. **Strategy Dynamics**
   - Prisoner's Dilemma payoff matrix (R=3, T=4, P=2, S=1)
   - Strategy updates based on local neighborhood success
   - No memory of past interactions

3. **Percolation Analysis**
   - Both geometric (all agents) and strategy-based percolation
   - Boundary-to-boundary connectivity check
   - Cluster analysis with transitive neighborhood

## Functional Programming Examples

### Example 1: Curried Strategy Update System
```scala
// World.scala
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
          agent.evaluateStrategyConversion(neighborList, bestPayoff, bestNeighborCount)(random.nextDouble())
      }
  }
}
```
This demonstrates:
- Currying for partial application
- Pattern matching for null safety
- Pure function composition

### Example 2: Tail-Recursive Cluster Analysis
```scala
// PositionOps.scala
@annotation.tailrec
def findClusters(
  positions: List[Position], 
  radius: Double,
  acc: List[List[Position]] = List.empty
): List[List[Position]] = positions match {
  case Nil => acc
  case head :: tail =>
    val (cluster, remaining) = bfsExtractComponent(head, tail, radius)
    findClusters(remaining, radius, cluster :: acc)
}
```
This demonstrates:
- Tail recursion optimization
- Immutable accumulator pattern
- Pattern matching for list processing

## Running the Simulation

1. Prerequisites:
```bash
# Install Scala and sbt
# Install Python dependencies
pip install pandas numpy matplotlib seaborn scipy
```

2. Run simulation:
```bash
sbt run
```

3. Analyze results:
```bash
python plot_improved_visualization.py
```

Results will be saved in:
- `phase_diagram_results.csv`: Raw simulation data
- `improved_visualizations/`: Analysis plots showing:
  - Phase transitions in agent density
  - Percolation probability
  - Cluster size distributions

## Project Structure

```
src/main/scala/version2/
├── Agent.scala         # Agent behavior and properties
├── World.scala        # Continuous world implementation
├── Strategy.scala     # Strategy definitions and operations
├── Position.scala     # 2D space operations
├── PayoffMatrix.scala # Game theory calculations
├── PercolationAnalyzer.scala # Percolation analysis
└── SimulationRunner.scala    # Main simulation runner
```

## References

Based on research paper:
"Percolation and cooperation with mobile agents: geometric and strategy clusters"
by Mendeli H. Vainstein, Carolina Brito, and Jeferson J. Arenzon (2014) 