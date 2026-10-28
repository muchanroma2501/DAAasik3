package benchmark;

import java.util.Objects;

/**
 * Immutable metadata and outcome for one measured benchmark execution.
 */
public record BenchmarkMetrics(
        String algorithm,
        String dataset,
        String graphType,
        int vertices,
        int edges,
        long seed,
        int repeat,
        long elapsedNanos,
        OperationCounter.OperationCounts operations,
        String correctnessStatus) {
    public BenchmarkMetrics {
        Objects.requireNonNull(algorithm, "algorithm");
        Objects.requireNonNull(dataset, "dataset");
        Objects.requireNonNull(graphType, "graphType");
        Objects.requireNonNull(operations, "operations");
        Objects.requireNonNull(correctnessStatus, "correctnessStatus");
        if (vertices < 0 || edges < 0) {
            throw new IllegalArgumentException("Graph sizes cannot be negative");
        }
        if (repeat < 1) {
            throw new IllegalArgumentException("Measured repeat index must be positive");
        }
        if (elapsedNanos < 0) {
            throw new IllegalArgumentException("Elapsed time cannot be negative");
        }
    }
}
