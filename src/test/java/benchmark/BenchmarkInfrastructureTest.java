package benchmark;

import connectivity.KosarajuSCC;
import connectivity.TopologicalSort;
import connectivity.TopologicalSortResult;
import graph.Edge;
import graph.WeightedGraph;
import mst.Kruskal;
import mst.MSTResult;
import mst.Prim;
import org.junit.jupiter.api.Test;
import shortestpath.Dijkstra;
import shortestpath.ShortestPathResult;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchmarkInfrastructureTest {
    @Test
    void sameSeedAndConfigurationProduceEquivalentGraphs() {
        assertEquals(GraphGenerator.generateUndirectedConnectedWeighted(10, 30, 42).getEdges(),
                GraphGenerator.generateUndirectedConnectedWeighted(10, 30, 42).getEdges());
        assertEquals(GraphGenerator.generateDag(10, 30, 42).getEdges(),
                GraphGenerator.generateDag(10, 30, 42).getEdges());
        assertEquals(GraphGenerator.generateDirectedCyclicGraph(10, 30, 42).getEdges(),
                GraphGenerator.generateDirectedCyclicGraph(10, 30, 42).getEdges());
        assertEquals(GraphGenerator.generateDijkstraGraph(10, 30, 42).getEdges(),
                GraphGenerator.generateDijkstraGraph(10, 30, 42).getEdges());
    }

    @Test
    void undirectedGraphsAreExactSimpleAndConnected() {
        for (BenchmarkConfig.Dataset dataset : BenchmarkConfig.standard().datasets()) {
            WeightedGraph graph = GraphGenerator.generateUndirectedConnectedWeighted(
                    dataset.vertices(), dataset.edges(), BenchmarkConfig.DEFAULT_SEED);

            assertFalse(graph.isDirected());
            assertEquals(dataset.edges(), graph.getEdges().size());
            assertNoDuplicateOrLoop(graph, false);
            assertConnected(graph);
            assertWeightsInRange(graph);
        }
    }

    @Test
    void dagsAreAcyclicAndRespectAssignmentEdgePolicy() {
        BenchmarkConfig config = BenchmarkConfig.standard();
        for (BenchmarkConfig.Dataset dataset : config.datasets()) {
            int actualEdges = BenchmarkConfig.dagEdgeCount(dataset);
            WeightedGraph graph = GraphGenerator.generateDag(
                    dataset.vertices(), actualEdges, config.seed());
            TopologicalSortResult result = TopologicalSort.sort(graph);

            assertTrue(graph.isDirected());
            assertEquals(actualEdges, graph.getEdges().size());
            assertFalse(result.hasCycle());
            assertValidTopologicalOrdering(graph, result.getOrdering());
        }
        assertEquals(495, BenchmarkConfig.dagEdgeCount(config.datasets().get(1)));
    }

    @Test
    void cyclicGeneratorAlwaysIncludesCycleAndExactEdgeCount() {
        WeightedGraph graph = GraphGenerator.generateDirectedCyclicGraph(10, 30, 7);

        assertTrue(graph.isDirected());
        assertEquals(30, graph.getEdges().size());
        assertNoDuplicateOrLoop(graph, true);
        assertTrue(TopologicalSort.sort(graph).hasCycle());
    }

    @Test
    void dijkstraGraphsAreNonnegativeConnectedAndUndirected() {
        WeightedGraph graph = GraphGenerator.generateDijkstraGraph(100, 500, 7);

        assertFalse(graph.isDirected());
        assertEquals(500, graph.getEdges().size());
        assertConnected(graph);
        assertWeightsInRange(graph);
        ShortestPathResult result = Dijkstra.findShortestPaths(graph, 0);
        assertEquals(0, result.getDistanceTo(0));
        assertTrue(graph.getVertexCount() > 0);
        for (int vertex = 0; vertex < graph.getVertexCount(); vertex++) {
            assertTrue(result.isReachable(vertex));
        }
    }

    @Test
    void rejectsImpossibleVertexAndEdgeCounts() {
        assertThrows(IllegalArgumentException.class,
                () -> GraphGenerator.generateUndirectedConnectedWeighted(-1, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> GraphGenerator.generateDag(3, -1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> GraphGenerator.generateUndirectedConnectedWeighted(4, 2, 1));
        assertThrows(IllegalArgumentException.class,
                () -> GraphGenerator.generateUndirectedConnectedWeighted(4, 7, 1));
        assertThrows(IllegalArgumentException.class,
                () -> GraphGenerator.generateDag(4, 7, 1));
        assertThrows(IllegalArgumentException.class,
                () -> GraphGenerator.generateDirectedCyclicGraph(1, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> GraphGenerator.generateDirectedCyclicGraph(4, 1, 1));
    }

    @Test
    void benchmarkConfigRequiresFiveMeasuredRuns() {
        assertThrows(IllegalArgumentException.class, () -> new BenchmarkConfig(
                List.of(new BenchmarkConfig.Dataset("Tiny", 1, 0)), 0, 4, 1));
        assertThrows(IllegalArgumentException.class, () -> new BenchmarkConfig(
                List.of(new BenchmarkConfig.Dataset("Tiny", 1, 0)), -1, 5, 1));
        assertEquals(5, BenchmarkConfig.standard().measuredRepetitions());
        assertEquals(2, BenchmarkConfig.standard().warmupRuns());
    }

    @Test
    void runnerExcludesWarmupsAndRecordsActualGraphMetadata() {
        BenchmarkConfig config = new BenchmarkConfig(
                List.of(new BenchmarkConfig.Dataset("Tiny", 3, 3)), 2, 5, 99);
        BenchmarkConfig.Dataset dataset = config.datasets().get(0);
        WeightedGraph graph = GraphGenerator.generateUndirectedConnectedWeighted(3, 3, 99);
        AtomicInteger executions = new AtomicInteger();

        List<BenchmarkMetrics> metrics = BenchmarkRunner.run(config, dataset, graph,
                "Kruskal", "UNDIRECTED_WEIGHTED", counter -> {
                    executions.incrementAndGet();
                    return Kruskal.findMst(graph, counter);
                }, MSTResult::isConnected);

        assertEquals(7, executions.get());
        assertEquals(5, metrics.size());
        for (int index = 0; index < metrics.size(); index++) {
            BenchmarkMetrics metric = metrics.get(index);
            assertEquals("Tiny", metric.dataset());
            assertEquals("UNDIRECTED_WEIGHTED", metric.graphType());
            assertEquals(3, metric.vertices());
            assertEquals(3, metric.edges());
            assertEquals(99, metric.seed());
            assertEquals(index + 1, metric.repeat());
            assertTrue(metric.elapsedNanos() >= 0);
            assertEquals("PASS", metric.correctnessStatus());
        }
    }

    @Test
    void runnerFailsExplicitlyWhenCorrectnessCheckFails() {
        BenchmarkConfig config = new BenchmarkConfig(
                List.of(new BenchmarkConfig.Dataset("Tiny", 1, 0)), 1, 5, 1);
        WeightedGraph graph = new WeightedGraph(1, false);

        assertThrows(IllegalStateException.class, () -> BenchmarkRunner.run(
                config, config.datasets().get(0), graph, "example", "UNDIRECTED",
                counter -> 1, value -> false));
    }

    @Test
    void algorithmCountersMatchDocumentedExecutedOperations() {
        WeightedGraph triangle = new WeightedGraph(3, false);
        triangle.addEdge(0, 1, 1);
        triangle.addEdge(0, 2, 5);
        triangle.addEdge(1, 2, 1);

        OperationCounter kruskalCounter = new OperationCounter();
        Kruskal.findMst(triangle, kruskalCounter);
        OperationCounter.OperationCounts kruskal = kruskalCounter.snapshot();
        assertEquals(kruskal.edgeExaminations(), kruskal.unionAttempts());
        assertEquals(2, kruskal.successfulUnions());
        assertTrue(kruskal.edgeExaminations() <= triangle.getEdges().size());

        OperationCounter primCounter = new OperationCounter();
        Prim.findMst(triangle, primCounter);
        OperationCounter.OperationCounts prim = primCounter.snapshot();
        assertEquals(6, prim.edgeExaminations());
        assertEquals(prim.edgeExaminations(), prim.priorityQueueInsertions());
        assertEquals(6, prim.priorityQueuePolls());

        OperationCounter dijkstraCounter = new OperationCounter();
        Dijkstra.findShortestPaths(triangle, 0, dijkstraCounter);
        OperationCounter.OperationCounts dijkstra = dijkstraCounter.snapshot();
        assertEquals(6, dijkstra.edgeExaminations());
        assertEquals(4, dijkstra.priorityQueueInsertions());
        assertEquals(4, dijkstra.priorityQueuePolls());
        assertEquals(3, dijkstra.successfulRelaxations());

        WeightedGraph dag = new WeightedGraph(3, true);
        dag.addEdge(0, 1, 1);
        dag.addEdge(1, 2, 1);
        OperationCounter topologicalCounter = new OperationCounter();
        TopologicalSort.sort(dag, topologicalCounter);
        assertEquals(4, topologicalCounter.snapshot().edgeExaminations());
        assertEquals(3, topologicalCounter.snapshot().verticesProcessed());

        WeightedGraph directedCycle = new WeightedGraph(3, true);
        directedCycle.addEdge(0, 1, 1);
        directedCycle.addEdge(1, 0, 1);
        OperationCounter sccCounter = new OperationCounter();
        KosarajuSCC.findStronglyConnectedComponents(directedCycle, sccCounter);
        assertEquals(4, sccCounter.snapshot().edgeExaminations());
        assertEquals(6, sccCounter.snapshot().verticesVisited());
    }

    @Test
    void repeatedExecutionOnSameGraphHasConsistentOperationCounts() {
        WeightedGraph graph = GraphGenerator.generateDijkstraGraph(10, 30, 5);
        OperationCounter first = new OperationCounter();
        OperationCounter second = new OperationCounter();

        Dijkstra.findShortestPaths(graph, 0, first);
        Dijkstra.findShortestPaths(graph, 0, second);

        assertEquals(first.snapshot(), second.snapshot());
    }

    private static void assertConnected(WeightedGraph graph) {
        boolean[] visited = new boolean[graph.getVertexCount()];
        Queue<Integer> pending = new ArrayDeque<>();
        visited[0] = true;
        pending.add(0);
        int visitedCount = 0;
        while (!pending.isEmpty()) {
            int vertex = pending.remove();
            visitedCount++;
            for (Edge edge : graph.getAdjacencyList(vertex)) {
                int next = edge.getSource() == vertex
                        ? edge.getDestination() : edge.getSource();
                if (!visited[next]) {
                    visited[next] = true;
                    pending.add(next);
                }
            }
        }
        assertEquals(graph.getVertexCount(), visitedCount);
    }

    private static void assertNoDuplicateOrLoop(WeightedGraph graph, boolean directed) {
        Set<String> seen = new HashSet<>();
        for (Edge edge : graph.getEdges()) {
            assertNotEquals(edge.getSource(), edge.getDestination());
            int first = directed ? edge.getSource()
                    : Math.min(edge.getSource(), edge.getDestination());
            int second = directed ? edge.getDestination()
                    : Math.max(edge.getSource(), edge.getDestination());
            assertTrue(seen.add(first + ":" + second));
        }
    }

    private static void assertWeightsInRange(WeightedGraph graph) {
        for (Edge edge : graph.getEdges()) {
            assertTrue(edge.getWeight() >= GraphGenerator.MIN_WEIGHT);
            assertTrue(edge.getWeight() <= GraphGenerator.MAX_WEIGHT);
        }
    }

    private static void assertValidTopologicalOrdering(WeightedGraph graph,
                                                       List<Integer> ordering) {
        assertEquals(graph.getVertexCount(), ordering.size());
        int[] positions = new int[graph.getVertexCount()];
        for (int index = 0; index < ordering.size(); index++) {
            positions[ordering.get(index)] = index;
        }
        for (Edge edge : graph.getEdges()) {
            assertTrue(positions[edge.getSource()] < positions[edge.getDestination()]);
        }
    }
}
