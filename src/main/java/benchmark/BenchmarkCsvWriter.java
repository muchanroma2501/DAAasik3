package benchmark;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Writes fresh raw and summary CSV files in UTF-8.
 */
public final class BenchmarkCsvWriter {
    private static final String[] RAW_HEADER = {
            "algorithm", "dataset", "graph_family", "vertices", "requested_edges",
            "actual_edges", "seed", "repeat", "elapsed_ns", "correctness_status",
            "edge_examinations", "priority_queue_polls", "priority_queue_insertions",
            "successful_relaxations", "union_attempts", "successful_unions",
            "vertices_processed", "vertices_visited"
    };
    private static final String[] SUMMARY_HEADER = {
            "algorithm", "dataset", "graph_family", "vertices", "requested_edges",
            "actual_edges", "seed", "sample_count", "mean_ns", "median_ns", "min_ns",
            "max_ns", "mean_edge_examinations", "mean_priority_queue_polls",
            "mean_priority_queue_insertions", "mean_successful_relaxations",
            "mean_union_attempts", "mean_successful_unions", "mean_vertices_processed",
            "mean_vertices_visited"
    };

    private BenchmarkCsvWriter() {
    }

    public static void write(
            Path outputDirectory, BenchmarkConfig config, List<BenchmarkMetrics> runs)
            throws IOException {
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(runs, "runs");
        Map<String, BenchmarkConfig.Dataset> datasets = new LinkedHashMap<>();
        for (BenchmarkConfig.Dataset dataset : config.datasets()) {
            datasets.put(dataset.name(), dataset);
        }

        Map<String, List<BenchmarkMetrics>> groups = new LinkedHashMap<>();
        for (BenchmarkMetrics run : runs) {
            if (!"PASS".equals(run.correctnessStatus())) {
                throw new IllegalArgumentException("Refusing to write a failed benchmark run");
            }
            BenchmarkConfig.Dataset dataset = datasets.get(run.dataset());
            if (dataset == null) {
                throw new IllegalArgumentException("Unknown dataset in measurement: " + run.dataset());
            }
            String key = run.algorithm() + '\u0000' + run.dataset();
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(run);
        }

        List<BenchmarkSummary> summaries = new ArrayList<>(groups.size());
        for (List<BenchmarkMetrics> group : groups.values()) {
            BenchmarkConfig.Dataset dataset = datasets.get(group.get(0).dataset());
            summaries.add(BenchmarkSummary.fromRuns(group, dataset.edges()));
        }

        Files.createDirectories(outputDirectory);
        Path rawPath = outputDirectory.resolve("benchmark_runs.csv");
        Path summaryPath = outputDirectory.resolve("benchmark_summary.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(rawPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            writeRow(writer, List.of(RAW_HEADER));
            for (BenchmarkMetrics run : runs) {
                BenchmarkConfig.Dataset dataset = datasets.get(run.dataset());
                OperationCounter.OperationCounts counts = run.operations();
                writeRow(writer, List.of(
                        run.algorithm(), run.dataset(), run.graphType(),
                        Integer.toString(run.vertices()), Integer.toString(dataset.edges()),
                        Integer.toString(run.edges()), Long.toString(run.seed()),
                        Integer.toString(run.repeat()), Long.toString(run.elapsedNanos()),
                        run.correctnessStatus(), Long.toString(counts.edgeExaminations()),
                        Long.toString(counts.priorityQueuePolls()),
                        Long.toString(counts.priorityQueueInsertions()),
                        Long.toString(counts.successfulRelaxations()),
                        Long.toString(counts.unionAttempts()),
                        Long.toString(counts.successfulUnions()),
                        Long.toString(counts.verticesProcessed()),
                        Long.toString(counts.verticesVisited())));
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(summaryPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            writeRow(writer, List.of(SUMMARY_HEADER));
            for (BenchmarkSummary summary : summaries) {
                writeRow(writer, List.of(
                        summary.algorithm(), summary.dataset(), summary.graphFamily(),
                        Integer.toString(summary.vertices()),
                        Integer.toString(summary.requestedEdges()),
                        Integer.toString(summary.actualEdges()), Long.toString(summary.seed()),
                        Integer.toString(summary.sampleCount()), decimal(summary.meanNanos()),
                        decimal(summary.medianNanos()), Long.toString(summary.minimumNanos()),
                        Long.toString(summary.maximumNanos()),
                        decimal(summary.meanEdgeExaminations()),
                        decimal(summary.meanPriorityQueuePolls()),
                        decimal(summary.meanPriorityQueueInsertions()),
                        decimal(summary.meanSuccessfulRelaxations()),
                        decimal(summary.meanUnionAttempts()),
                        decimal(summary.meanSuccessfulUnions()),
                        decimal(summary.meanVerticesProcessed()),
                        decimal(summary.meanVerticesVisited())));
            }
        }
    }

    static String escape(String value) {
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    private static void writeRow(BufferedWriter writer, List<String> values) throws IOException {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                writer.write(',');
            }
            writer.write(escape(values.get(index)));
        }
        writer.newLine();
    }

    private static String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
