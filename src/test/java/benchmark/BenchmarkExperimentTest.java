package benchmark;

import connectivity.KosarajuSCC;
import connectivity.TopologicalSort;
import connectivity.TopologicalSortResult;
import graph.WeightedGraph;
import mst.Kruskal;
import mst.MSTResult;
import mst.Prim;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import shortestpath.Dijkstra;
import shortestpath.ShortestPathResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchmarkExperimentTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void validatorsCheckMstTopologicalSccAndShortestPathResults() {
        WeightedGraph mstGraph = GraphGenerator.generateUndirectedConnectedWeighted(6, 9, 17);
        MSTResult kruskal = Kruskal.findMst(mstGraph);
        MSTResult prim = Prim.findMst(mstGraph);
        assertTrue(BenchmarkValidators.areEquivalentMinimumSpanningTrees(
                mstGraph, kruskal, prim));

        WeightedGraph dag = GraphGenerator.generateDag(6, 8, 18);
        TopologicalSortResult ordering = TopologicalSort.sort(dag);
        assertTrue(BenchmarkValidators.isValidTopologicalOrdering(dag, ordering));

        WeightedGraph cyclic = new WeightedGraph(5, true);
        cyclic.addEdge(0, 1, 1);
        cyclic.addEdge(1, 0, 1);
        cyclic.addEdge(1, 2, 1);
        cyclic.addEdge(2, 3, 1);
        cyclic.addEdge(3, 2, 1);
        List<List<Integer>> components = KosarajuSCC.findStronglyConnectedComponents(cyclic);
        assertTrue(BenchmarkValidators.isValidSccPartition(cyclic, components));
        assertTrue(BenchmarkValidators.sccPartitionMatchesReachability(cyclic, components));

        WeightedGraph routes = new WeightedGraph(5, false);
        routes.addEdge(0, 1, 4);
        routes.addEdge(0, 2, 1);
        routes.addEdge(2, 1, 1);
        routes.addEdge(1, 3, 2);
        ShortestPathResult shortest = Dijkstra.findShortestPaths(routes, 0);
        long[] reference = BenchmarkValidators.calculateReferenceDistances(routes, 0);
        assertTrue(BenchmarkValidators.isValidShortestPaths(routes, shortest, 0, reference));
        assertFalse(shortest.isReachable(4));
    }

    @Test
    void experimentProducesFiveValidatedRunsForEveryAlgorithmAndDataset() throws IOException {
        BenchmarkConfig config = BenchmarkConfig.standard();
        List<BenchmarkMetrics> runs = BenchmarkExperiment.runAndWrite(config, temporaryDirectory);

        assertEquals(75, runs.size());
        assertEquals(76, Files.readAllLines(
                temporaryDirectory.resolve("benchmark_runs.csv")).size());
        assertEquals(16, Files.readAllLines(
                temporaryDirectory.resolve("benchmark_summary.csv")).size());
        Set<String> combinations = new HashSet<>();
        Map<String, Integer> runCounts = new java.util.HashMap<>();
        for (BenchmarkMetrics run : runs) {
            assertEquals("PASS", run.correctnessStatus());
            assertTrue(run.repeat() >= 1 && run.repeat() <= 5);
            assertTrue(run.elapsedNanos() >= 0);
            String combination = run.algorithm() + "/" + run.dataset();
            combinations.add(combination);
            runCounts.merge(combination, 1, Integer::sum);
            if (run.algorithm().equals("TopologicalSort")
                    && run.dataset().equals("Medium")) {
                assertEquals(495, run.edges());
            } else {
                assertEquals(config.datasets().stream()
                        .filter(dataset -> dataset.name().equals(run.dataset()))
                        .findFirst().orElseThrow().edges(), run.edges());
            }
        }
        assertEquals(15, combinations.size());
        assertTrue(runCounts.values().stream().allMatch(count -> count == 5));
    }

    @Test
    void csvWriterCreatesEscapedRawRowsAndMatchingSummary(@TempDir Path temp) throws IOException {
        BenchmarkConfig config = new BenchmarkConfig(
                List.of(new BenchmarkConfig.Dataset("Tiny", 3, 3)), 0, 5, 23);
        List<BenchmarkMetrics> runs = new ArrayList<>();
        for (int repeat = 1; repeat <= 5; repeat++) {
            runs.add(new BenchmarkMetrics("Kruskal, \"test\"", "Tiny",
                    "UNDIRECTED_WEIGHTED", 3, 3, 23, repeat,
                    repeat * 10L, new OperationCounter.OperationCounts(
                    repeat, 0, 0, 0, repeat, 2, 0, 0), "PASS"));
        }
        Path output = temp.resolve("nested").resolve("tables");
        BenchmarkCsvWriter.write(output, config, runs);

        List<String> raw = Files.readAllLines(output.resolve("benchmark_runs.csv"));
        List<String> summary = Files.readAllLines(output.resolve("benchmark_summary.csv"));
        assertEquals(6, raw.size());
        assertEquals(2, summary.size());
        assertEquals("algorithm,dataset,graph_family,vertices,requested_edges,actual_edges,"
                        + "seed,repeat,elapsed_ns,correctness_status,edge_examinations,"
                        + "priority_queue_polls,priority_queue_insertions,successful_relaxations,"
                        + "union_attempts,successful_unions,vertices_processed,vertices_visited",
                raw.get(0));
        assertEquals("algorithm,dataset,graph_family,vertices,requested_edges,actual_edges,"
                        + "seed,sample_count,mean_ns,median_ns,min_ns,max_ns,"
                        + "mean_edge_examinations,mean_priority_queue_polls,"
                        + "mean_priority_queue_insertions,mean_successful_relaxations,"
                        + "mean_union_attempts,mean_successful_unions,mean_vertices_processed,"
                        + "mean_vertices_visited",
                summary.get(0));
        assertEquals(18, parseCsvRow(raw.get(0)).size());
        assertEquals(18, parseCsvRow(raw.get(1)).size());
        assertEquals(20, summary.get(0).split(",", -1).length);
        assertTrue(raw.get(1).startsWith("\"Kruskal, \"\"test\"\"\""));
        assertEquals("Kruskal, \"test\"", parseCsvRow(raw.get(1)).get(0));
        List<String> summaryFields = parseCsvRow(summary.get(1));
        assertEquals(20, summaryFields.size());
        assertEquals("5", summaryFields.get(7));
        assertEquals("30", summaryFields.get(8));
        assertEquals("30", summaryFields.get(9));
        assertEquals("10", summaryFields.get(10));
        assertEquals("50", summaryFields.get(11));
        assertEquals("3", summaryFields.get(12));
        assertEquals("3", summaryFields.get(16));

        BenchmarkCsvWriter.write(output, config, runs);
        assertEquals(6, Files.readAllLines(output.resolve("benchmark_runs.csv")).size());
        assertEquals(2, Files.readAllLines(output.resolve("benchmark_summary.csv")).size());
    }

    private static List<String> parseCsvRow(String row) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < row.length(); index++) {
            char character = row.charAt(index);
            if (quoted && character == '"' && index + 1 < row.length()
                    && row.charAt(index + 1) == '"') {
                field.append('"');
                index++;
            } else if (character == '"') {
                quoted = !quoted;
            } else if (character == ',' && !quoted) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(character);
            }
        }
        fields.add(field.toString());
        return fields;
    }

    @Test
    void summaryUsesCorrectOddAndEvenMediansAndRejectsFailedRows() {
        assertEquals(new BigDecimal("3"), BenchmarkSummary.median(List.of(1L, 3L, 5L)));
        assertEquals(new BigDecimal("4.5"),
                BenchmarkSummary.median(List.of(1L, 2L, 7L, 10L)));

        List<BenchmarkMetrics> oddRuns = new ArrayList<>();
        for (int repeat = 1; repeat <= 3; repeat++) {
            oddRuns.add(new BenchmarkMetrics("Dijkstra", "Tiny", "DIRECTED",
                    2, 1, 42, repeat, repeat * 10L,
                    new OperationCounter.OperationCounts(repeat, 0, 0, repeat, 0, 0, 0, 0),
                    "PASS"));
        }
        BenchmarkSummary summary = BenchmarkSummary.fromRuns(oddRuns, 1);
        assertEquals(0, new BigDecimal("20").compareTo(summary.meanNanos()));
        assertEquals(0, new BigDecimal("20").compareTo(summary.medianNanos()));
        assertEquals(10, summary.minimumNanos());
        assertEquals(30, summary.maximumNanos());
        assertEquals(new BigDecimal("2"), summary.meanEdgeExaminations());

        List<BenchmarkMetrics> evenRuns = new ArrayList<>();
        long[] evenTimes = {10, 20, 70, 100};
        for (int repeat = 1; repeat <= evenTimes.length; repeat++) {
            evenRuns.add(new BenchmarkMetrics("Dijkstra", "Tiny", "DIRECTED",
                    2, 1, 42, repeat, evenTimes[repeat - 1],
                    new OperationCounter.OperationCounts(0, 0, 0, 0, 0, 0, 0, 0),
                    "PASS"));
        }
        BenchmarkSummary evenSummary = BenchmarkSummary.fromRuns(evenRuns, 1);
        assertEquals(0, new BigDecimal("50").compareTo(evenSummary.meanNanos()));
        assertEquals(0, new BigDecimal("45").compareTo(evenSummary.medianNanos()));
        assertEquals(10, evenSummary.minimumNanos());
        assertEquals(100, evenSummary.maximumNanos());

        BenchmarkMetrics failed = new BenchmarkMetrics("Dijkstra", "Tiny", "DIRECTED",
                2, 1, 42, 4, 40,
                new OperationCounter.OperationCounts(0, 0, 0, 0, 0, 0, 0, 0), "FAIL");
        assertThrows(IllegalArgumentException.class, () ->
                BenchmarkSummary.fromRuns(List.of(oddRuns.get(0), failed), 1));
    }

    @Test
    void csvWriterRefusesFailedCorrectnessRows() {
        BenchmarkConfig config = new BenchmarkConfig(
                List.of(new BenchmarkConfig.Dataset("Tiny", 1, 0)), 0, 5, 1);
        BenchmarkMetrics failed = new BenchmarkMetrics("Dijkstra", "Tiny", "UNDIRECTED",
                1, 0, 1, 1, 0,
                new OperationCounter.OperationCounts(0, 0, 0, 0, 0, 0, 0, 0), "FAIL");
        assertThrows(IllegalArgumentException.class, () ->
                BenchmarkCsvWriter.write(temporaryDirectory, config, List.of(failed)));
    }
}
