package benchmark;

import java.nio.file.Path;
import java.util.List;

/**
 * Command-line entry point for the complete Day 9 experiment.
 */
public final class BenchmarkExperimentMain {
    private static final Path OUTPUT_DIRECTORY = Path.of("results", "tables");

    private BenchmarkExperimentMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 1) {
            throw new IllegalArgumentException(
                    "Usage: BenchmarkExperimentMain [output-directory]");
        }
        Path outputDirectory = args.length == 1
                ? Path.of(args[0]) : OUTPUT_DIRECTORY;
        List<BenchmarkMetrics> measurements = BenchmarkExperiment.runAndWrite(
                BenchmarkConfig.standard(), outputDirectory);
        System.out.println("Completed " + measurements.size() + " measured runs.");
        System.out.println("Raw results: " + outputDirectory.resolve("benchmark_runs.csv"));
        System.out.println("Summary results: " + outputDirectory.resolve("benchmark_summary.csv"));
    }
}
