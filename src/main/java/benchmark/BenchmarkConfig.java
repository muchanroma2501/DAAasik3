package benchmark;

import java.util.List;
import java.util.Objects;

/**
 * Immutable benchmark settings and the standard assignment datasets.
 */
public record BenchmarkConfig(List<Dataset> datasets, int warmupRuns,
                              int measuredRepetitions, long seed) {
    public static final long DEFAULT_SEED = 20261029L;
    public static final int DEFAULT_WARMUP_RUNS = 2;
    public static final int MINIMUM_MEASURED_REPETITIONS = 5;
    public static final int MEDIUM_DAG_EDGE_LIMIT = 495;

    public BenchmarkConfig {
        datasets = List.copyOf(Objects.requireNonNull(datasets, "datasets"));
        if (datasets.isEmpty()) {
            throw new IllegalArgumentException("At least one dataset is required");
        }
        if (datasets.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Datasets cannot contain null");
        }
        if (warmupRuns < 0) {
            throw new IllegalArgumentException("Warm-up count cannot be negative");
        }
        if (measuredRepetitions < MINIMUM_MEASURED_REPETITIONS) {
            throw new IllegalArgumentException(
                    "At least " + MINIMUM_MEASURED_REPETITIONS
                            + " measured repetitions are required");
        }
    }

    public static BenchmarkConfig standard() {
        return new BenchmarkConfig(List.of(
                new Dataset("Small", 10, 30),
                new Dataset("Medium", 100, 500),
                new Dataset("Large", 1000, 5000)),
                DEFAULT_WARMUP_RUNS, MINIMUM_MEASURED_REPETITIONS, DEFAULT_SEED);
    }

    /**
     * Returns the requested DAG edge count, limited to the maximum possible
     * number of distinct forward edges. The assignment's standard Medium DAG
     * workload is explicitly capped at 495 edges.
     */
    public static int dagEdgeCount(Dataset dataset) {
        Objects.requireNonNull(dataset, "dataset");
        long maximum = (long) dataset.vertices() * (dataset.vertices() - 1) / 2;
        if (dataset.name().equals("Medium")
                && dataset.vertices() == 100 && dataset.edges() == 500) {
            return MEDIUM_DAG_EDGE_LIMIT;
        }
        return (int) Math.min(dataset.edges(), maximum);
    }

    public record Dataset(String name, int vertices, int edges) {
        public Dataset {
            Objects.requireNonNull(name, "name");
            if (name.isBlank()) {
                throw new IllegalArgumentException("Dataset name cannot be blank");
            }
            if (vertices < 0) {
                throw new IllegalArgumentException("Vertex count cannot be negative");
            }
            if (edges < 0) {
                throw new IllegalArgumentException("Edge count cannot be negative");
            }
        }
    }
}
