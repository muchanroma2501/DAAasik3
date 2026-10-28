package benchmark;

import connectivity.KosarajuSCC;
import connectivity.TopologicalSort;
import graph.WeightedGraph;
import mst.Kruskal;
import mst.MSTResult;
import mst.Prim;
import shortestpath.Dijkstra;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Runs the required five algorithms over each configured dataset.
 */
public final class BenchmarkExperiment {
    private BenchmarkExperiment() {
    }

    public static List<BenchmarkMetrics> run(BenchmarkConfig config) {
        Objects.requireNonNull(config, "config");
        List<BenchmarkMetrics> measurements = new ArrayList<>();
        for (BenchmarkConfig.Dataset dataset : config.datasets()) {
            runDataset(config, dataset, measurements);
        }

        int expectedMeasurements = config.datasets().size() * 5 * config.measuredRepetitions();
        if (measurements.size() != expectedMeasurements) {
            throw new IllegalStateException("Expected " + expectedMeasurements
                    + " measured runs but obtained " + measurements.size());
        }
        return List.copyOf(measurements);
    }

    public static List<BenchmarkMetrics> runAndWrite(
            BenchmarkConfig config, Path outputDirectory) throws IOException {
        List<BenchmarkMetrics> measurements = run(config);
        BenchmarkCsvWriter.write(outputDirectory, config, measurements);
        return measurements;
    }

    private static void runDataset(BenchmarkConfig config, BenchmarkConfig.Dataset dataset,
                                   List<BenchmarkMetrics> measurements) {
        WeightedGraph mstGraph = GraphGenerator.generateUndirectedConnectedWeighted(
                dataset.vertices(), dataset.edges(), config.seed());
        verifyGraphMetadata(mstGraph, dataset, dataset.edges(), "undirected MST graph");

        MSTResult kruskalBaseline = Kruskal.findMst(mstGraph);
        MSTResult primBaseline = Prim.findMst(mstGraph);
        if (!BenchmarkValidators.areEquivalentMinimumSpanningTrees(
                mstGraph, kruskalBaseline, primBaseline)) {
            throw new IllegalStateException(
                    "MST implementations disagree or returned an invalid tree for "
                            + dataset.name());
        }
        long minimumWeight = kruskalBaseline.getTotalWeight();
        measurements.addAll(BenchmarkRunner.run(config, dataset, mstGraph, "Kruskal",
                "UNDIRECTED_WEIGHTED_CONNECTED", counter -> Kruskal.findMst(mstGraph, counter),
                result -> BenchmarkValidators.isValidSpanningTree(mstGraph, result)
                        && result.getTotalWeight() == minimumWeight));
        measurements.addAll(BenchmarkRunner.run(config, dataset, mstGraph, "Prim",
                "UNDIRECTED_WEIGHTED_CONNECTED", counter -> Prim.findMst(mstGraph, counter),
                result -> BenchmarkValidators.isValidSpanningTree(mstGraph, result)
                        && result.getTotalWeight() == minimumWeight));

        int dagEdges = BenchmarkConfig.dagEdgeCount(dataset);
        WeightedGraph dag = GraphGenerator.generateDag(dataset.vertices(), dagEdges, config.seed());
        verifyGraphMetadata(dag, dataset, dagEdges, "DAG");
        if (TopologicalSort.sort(dag).hasCycle()) {
            throw new IllegalStateException("Topological-sort experiment input contains a cycle");
        }
        measurements.addAll(BenchmarkRunner.run(config, dataset, dag, "TopologicalSort",
                "DIRECTED_DAG", counter -> TopologicalSort.sort(dag, counter),
                result -> BenchmarkValidators.isValidTopologicalOrdering(dag, result)));

        WeightedGraph cyclic = GraphGenerator.generateDirectedCyclicGraph(
                dataset.vertices(), dataset.edges(), config.seed());
        verifyGraphMetadata(cyclic, dataset, dataset.edges(), "directed SCC graph");
        if (!TopologicalSort.sort(cyclic).hasCycle()) {
            throw new IllegalStateException("SCC experiment input is not cyclic");
        }
        measurements.addAll(BenchmarkRunner.run(config, dataset, cyclic, "KosarajuSCC",
                "DIRECTED_CYCLIC", counter ->
                        KosarajuSCC.findStronglyConnectedComponents(cyclic, counter),
                components -> BenchmarkValidators.isValidSccPartition(cyclic, components)));

        WeightedGraph dijkstraGraph = GraphGenerator.generateDijkstraGraph(
                dataset.vertices(), dataset.edges(), config.seed());
        verifyGraphMetadata(dijkstraGraph, dataset, dataset.edges(), "Dijkstra graph");
        long[] referenceDistances = BenchmarkValidators.calculateReferenceDistances(
                dijkstraGraph, 0);
        measurements.addAll(BenchmarkRunner.run(config, dataset, dijkstraGraph, "Dijkstra",
                "UNDIRECTED_WEIGHTED_CONNECTED", counter ->
                        Dijkstra.findShortestPaths(dijkstraGraph, 0, counter),
                result -> BenchmarkValidators.isValidShortestPaths(
                        dijkstraGraph, result, 0, referenceDistances)));
    }

    private static void verifyGraphMetadata(WeightedGraph graph,
                                            BenchmarkConfig.Dataset dataset,
                                            int expectedEdges, String family) {
        if (graph.getVertexCount() != dataset.vertices()
                || graph.getEdges().size() != expectedEdges) {
            throw new IllegalStateException(family + " metadata mismatch for " + dataset.name()
                    + ": expected V=" + dataset.vertices() + ", E=" + expectedEdges
                    + " but got V=" + graph.getVertexCount()
                    + ", E=" + graph.getEdges().size());
        }
    }
}
