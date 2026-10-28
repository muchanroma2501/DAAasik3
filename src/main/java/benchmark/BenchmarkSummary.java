package benchmark;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Aggregated statistics for one algorithm and dataset.
 */
public record BenchmarkSummary(
        String algorithm,
        String dataset,
        String graphFamily,
        int vertices,
        int requestedEdges,
        int actualEdges,
        long seed,
        int sampleCount,
        BigDecimal meanNanos,
        BigDecimal medianNanos,
        long minimumNanos,
        long maximumNanos,
        BigDecimal meanEdgeExaminations,
        BigDecimal meanPriorityQueuePolls,
        BigDecimal meanPriorityQueueInsertions,
        BigDecimal meanSuccessfulRelaxations,
        BigDecimal meanUnionAttempts,
        BigDecimal meanSuccessfulUnions,
        BigDecimal meanVerticesProcessed,
        BigDecimal meanVerticesVisited) {

    public BenchmarkSummary {
        Objects.requireNonNull(algorithm, "algorithm");
        Objects.requireNonNull(dataset, "dataset");
        Objects.requireNonNull(graphFamily, "graphFamily");
        if (sampleCount < 1) {
            throw new IllegalArgumentException("At least one sample is required");
        }
    }

    public static BenchmarkSummary fromRuns(
            List<BenchmarkMetrics> runs, int requestedEdges) {
        if (runs.isEmpty()) {
            throw new IllegalArgumentException("Cannot summarize an empty measurement set");
        }
        List<BenchmarkMetrics> sorted = new ArrayList<>(runs);
        sorted.sort(Comparator.comparingLong(BenchmarkMetrics::elapsedNanos));
        BenchmarkMetrics first = sorted.get(0);
        for (BenchmarkMetrics run : sorted) {
            if (!run.algorithm().equals(first.algorithm())
                    || !run.dataset().equals(first.dataset())
                    || !run.graphType().equals(first.graphType())
                    || run.vertices() != first.vertices()
                    || run.edges() != first.edges()
                    || run.seed() != first.seed()) {
                throw new IllegalArgumentException(
                        "Summary measurements must share algorithm and graph metadata");
            }
            if (!"PASS".equals(run.correctnessStatus())) {
                throw new IllegalArgumentException("Cannot summarize a failed measurement");
            }
        }

        List<Long> elapsed = sorted.stream().map(BenchmarkMetrics::elapsedNanos).toList();
        return new BenchmarkSummary(
                first.algorithm(), first.dataset(), first.graphType(), first.vertices(),
                requestedEdges, first.edges(), first.seed(), sorted.size(),
                meanLongs(elapsed), median(elapsed),
                elapsed.get(0), elapsed.get(elapsed.size() - 1),
                meanOperations(sorted, OperationCounter.OperationCounts::edgeExaminations),
                meanOperations(sorted, OperationCounter.OperationCounts::priorityQueuePolls),
                meanOperations(sorted, OperationCounter.OperationCounts::priorityQueueInsertions),
                meanOperations(sorted, OperationCounter.OperationCounts::successfulRelaxations),
                meanOperations(sorted, OperationCounter.OperationCounts::unionAttempts),
                meanOperations(sorted, OperationCounter.OperationCounts::successfulUnions),
                meanOperations(sorted, OperationCounter.OperationCounts::verticesProcessed),
                meanOperations(sorted, OperationCounter.OperationCounts::verticesVisited));
    }

    public static BigDecimal median(List<Long> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Cannot calculate a median from no values");
        }
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return BigDecimal.valueOf(sorted.get(middle));
        }
        return BigDecimal.valueOf(sorted.get(middle - 1))
                .add(BigDecimal.valueOf(sorted.get(middle)))
                .divide(BigDecimal.valueOf(2), 1, RoundingMode.UNNECESSARY)
                .stripTrailingZeros();
    }

    static BigDecimal meanLongs(List<Long> values) {
        BigDecimal total = values.stream().map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }

    private static BigDecimal meanOperations(
            List<BenchmarkMetrics> runs,
            java.util.function.ToLongFunction<OperationCounter.OperationCounts> field) {
        BigDecimal total = runs.stream()
                .map(run -> BigDecimal.valueOf(field.applyAsLong(run.operations())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(runs.size()), 6, RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }
}
