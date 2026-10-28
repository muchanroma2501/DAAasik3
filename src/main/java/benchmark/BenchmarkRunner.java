package benchmark;

import graph.WeightedGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Lightweight educational benchmark runner. It times algorithm execution only;
 * graph construction and result validation occur outside the timed interval.
 */
public final class BenchmarkRunner {
    private BenchmarkRunner() {
    }

    public static <T> List<BenchmarkMetrics> run(
            BenchmarkConfig config,
            BenchmarkConfig.Dataset dataset,
            WeightedGraph graph,
            String algorithm,
            String graphType,
            Function<OperationCounter, T> execution,
            Predicate<T> correctnessCheck) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(dataset, "dataset");
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(algorithm, "algorithm");
        Objects.requireNonNull(graphType, "graphType");
        Objects.requireNonNull(execution, "execution");
        Objects.requireNonNull(correctnessCheck, "correctnessCheck");
        if (graph.getVertexCount() != dataset.vertices()) {
            throw new IllegalArgumentException("Graph vertex count does not match dataset metadata");
        }

        for (int run = 0; run < config.warmupRuns(); run++) {
            executeAndValidate(execution, correctnessCheck);
        }

        List<BenchmarkMetrics> measurements =
                new ArrayList<>(config.measuredRepetitions());
        for (int repeat = 1; repeat <= config.measuredRepetitions(); repeat++) {
            OperationCounter counter = new OperationCounter();
            long start = System.nanoTime();
            T result = execution.apply(counter);
            long elapsed = System.nanoTime() - start;
            if (!correctnessCheck.test(result)) {
                throw new IllegalStateException(
                        algorithm + " correctness check failed for dataset " + dataset.name());
            }
            measurements.add(new BenchmarkMetrics(algorithm, dataset.name(), graphType,
                    graph.getVertexCount(), graph.getEdges().size(), config.seed(), repeat,
                    elapsed, counter.snapshot(), "PASS"));
        }
        return List.copyOf(measurements);
    }

    private static <T> void executeAndValidate(Function<OperationCounter, T> execution,
                                               Predicate<T> correctnessCheck) {
        T result = execution.apply(new OperationCounter());
        if (!correctnessCheck.test(result)) {
            throw new IllegalStateException("Benchmark warm-up correctness check failed");
        }
    }
}
