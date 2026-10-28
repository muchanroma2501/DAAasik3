#!/usr/bin/env python3
"""Validate the Day 9 benchmark CSVs and create Day 10 visualizations."""

from __future__ import annotations

import csv
import sys
from collections import defaultdict
from dataclasses import dataclass
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP
from pathlib import Path
from typing import Iterable

import matplotlib

matplotlib.use("Agg")

import matplotlib.pyplot as plt
import numpy as np


PROJECT_ROOT = Path(__file__).resolve().parents[1]
RAW_CSV = Path("results") / "tables" / "benchmark_runs.csv"
SUMMARY_CSV = Path("results") / "tables" / "benchmark_summary.csv"
DEFAULT_OUTPUT_DIRECTORY = Path("results") / "plots"
EXPECTED_SEED = 20261029
EXPECTED_REPETITIONS = 5
DATASETS = {
    "Small": {"vertices": 10, "requested_edges": 30},
    "Medium": {"vertices": 100, "requested_edges": 500},
    "Large": {"vertices": 1000, "requested_edges": 5000},
}
ALGORITHM_FAMILIES = {
    "Kruskal": "UNDIRECTED_WEIGHTED_CONNECTED",
    "Prim": "UNDIRECTED_WEIGHTED_CONNECTED",
    "TopologicalSort": "DIRECTED_DAG",
    "KosarajuSCC": "DIRECTED_CYCLIC",
    "Dijkstra": "UNDIRECTED_WEIGHTED_CONNECTED",
}
ALGORITHMS = tuple(ALGORITHM_FAMILIES)
DATASET_ORDER = ("Small", "Medium", "Large")
EXPECTED_ACTUAL_EDGES = {
    (algorithm, dataset): (
        495 if algorithm == "TopologicalSort" and dataset == "Medium"
        else definition["requested_edges"]
    )
    for algorithm in ALGORITHMS
    for dataset, definition in DATASETS.items()
}
RAW_REQUIRED_COLUMNS = (
    "algorithm", "dataset", "graph_family", "vertices", "requested_edges",
    "actual_edges", "seed", "repeat", "elapsed_ns", "correctness_status",
    "edge_examinations", "priority_queue_polls", "priority_queue_insertions",
    "successful_relaxations", "union_attempts", "successful_unions",
    "vertices_processed", "vertices_visited",
)
SUMMARY_REQUIRED_COLUMNS = (
    "algorithm", "dataset", "graph_family", "vertices", "requested_edges",
    "actual_edges", "seed", "sample_count", "mean_ns", "median_ns", "min_ns",
    "max_ns", "mean_edge_examinations", "mean_priority_queue_polls",
    "mean_priority_queue_insertions", "mean_successful_relaxations",
    "mean_union_attempts", "mean_successful_unions", "mean_vertices_processed",
    "mean_vertices_visited",
)
RAW_INTEGER_COLUMNS = (
    "vertices", "requested_edges", "actual_edges", "seed", "repeat",
    "elapsed_ns", "edge_examinations", "priority_queue_polls",
    "priority_queue_insertions", "successful_relaxations", "union_attempts",
    "successful_unions", "vertices_processed", "vertices_visited",
)
SUMMARY_INTEGER_COLUMNS = (
    "vertices", "requested_edges", "actual_edges", "seed", "sample_count",
    "median_ns", "min_ns", "max_ns",
)
SUMMARY_DECIMAL_COLUMNS = (
    "mean_ns", "mean_edge_examinations", "mean_priority_queue_polls",
    "mean_priority_queue_insertions", "mean_successful_relaxations",
    "mean_union_attempts", "mean_successful_unions", "mean_vertices_processed",
    "mean_vertices_visited",
)
COUNTER_FIELDS = (
    "edge_examinations", "priority_queue_polls", "priority_queue_insertions",
    "successful_relaxations", "union_attempts", "successful_unions",
    "vertices_processed", "vertices_visited",
)
COUNTERS_BY_ALGORITHM = {
    "Kruskal": ("edge_examinations", "union_attempts", "successful_unions"),
    "Prim": ("edge_examinations", "priority_queue_polls", "priority_queue_insertions"),
    "TopologicalSort": ("edge_examinations", "vertices_processed"),
    "KosarajuSCC": ("edge_examinations", "vertices_visited"),
    "Dijkstra": (
        "edge_examinations", "priority_queue_polls",
        "priority_queue_insertions", "successful_relaxations",
    ),
}
COUNTER_LABELS = {
    "edge_examinations": "Edge examinations (algorithm-specific scans)",
    "priority_queue_polls": "Priority-queue removals (includes stale entries)",
    "priority_queue_insertions": "Priority-queue insertions",
    "successful_relaxations": "Successful distance relaxations",
    "union_attempts": "DSU union attempts",
    "successful_unions": "Successful DSU unions",
    "vertices_processed": "Vertices removed from ready queue",
    "vertices_visited": "First-time visits across both DFS passes",
}
MEAN_TOLERANCE = Decimal("0.000001")
ALGORITHM_COLORS = {
    "Kruskal": "#1f77b4",
    "Prim": "#ff7f0e",
    "TopologicalSort": "#2ca02c",
    "KosarajuSCC": "#d62728",
    "Dijkstra": "#9467bd",
}
class DataValidationError(ValueError):
    """Raised when input CSV data cannot safely support the plots."""


@dataclass(frozen=True)
class BenchmarkData:
    raw_runs: tuple[dict[str, object], ...]
    summaries: tuple[dict[str, object], ...]


def _read_csv(path: Path, required_columns: Iterable[str], label: str) -> list[dict[str, str]]:
    if not path.is_file():
        raise DataValidationError(
            f"Missing {label} CSV: {path}. Run the Day 9 benchmark experiment first."
        )
    try:
        with path.open("r", encoding="utf-8-sig", newline="") as stream:
            reader = csv.DictReader(stream, strict=True)
            if reader.fieldnames is None:
                raise DataValidationError(f"{label} CSV is empty: {path}")
            if len(reader.fieldnames) != len(set(reader.fieldnames)):
                raise DataValidationError(f"{label} CSV has duplicate header names: {path}")
            missing = sorted(set(required_columns) - set(reader.fieldnames))
            if missing:
                raise DataValidationError(
                    f"{label} CSV {path} is missing required columns: {', '.join(missing)}"
                )
            rows = []
            for line_number, row in enumerate(reader, start=2):
                if None in row:
                    raise DataValidationError(
                        f"{label} CSV line {line_number} has more fields than its header."
                    )
                if any(value is None for value in row.values()):
                    raise DataValidationError(
                        f"{label} CSV line {line_number} has fewer fields than its header."
                    )
                rows.append(row)
    except UnicodeDecodeError as error:
        raise DataValidationError(f"{label} CSV is not valid UTF-8: {path}: {error}") from error
    except csv.Error as error:
        raise DataValidationError(f"Could not parse {label} CSV {path}: {error}") from error
    if not rows:
        raise DataValidationError(f"{label} CSV has a header but no data rows: {path}")
    return rows


def _parse_integer(value: str, column: str, line_number: int, label: str) -> int:
    try:
        parsed = int(value.strip())
    except (AttributeError, ValueError) as error:
        raise DataValidationError(
            f"{label} CSV line {line_number}: column '{column}' must be an integer; "
            f"got {value!r}."
        ) from error
    return parsed


def _parse_decimal(value: str, column: str, line_number: int, label: str) -> Decimal:
    try:
        parsed = Decimal(value.strip())
    except (AttributeError, InvalidOperation, ValueError) as error:
        raise DataValidationError(
            f"{label} CSV line {line_number}: column '{column}' must be numeric; "
            f"got {value!r}."
        ) from error
    if not parsed.is_finite():
        raise DataValidationError(
            f"{label} CSV line {line_number}: column '{column}' must be finite; "
            f"got {value!r}."
        )
    return parsed


def load_raw_runs(path: Path) -> tuple[dict[str, object], ...]:
    rows = _read_csv(path, RAW_REQUIRED_COLUMNS, "Raw runs")
    parsed_rows = []
    seen_repeats: set[tuple[str, str, int]] = set()
    group_counts: dict[tuple[str, str], int] = defaultdict(int)

    for line_number, row in enumerate(rows, start=2):
        algorithm = row["algorithm"].strip()
        dataset = row["dataset"].strip()
        if algorithm not in ALGORITHM_FAMILIES:
            raise DataValidationError(
                f"Raw CSV line {line_number}: unexpected algorithm {algorithm!r}; "
                f"expected one of {', '.join(ALGORITHMS)}."
            )
        if dataset not in DATASETS:
            raise DataValidationError(
                f"Raw CSV line {line_number}: unexpected dataset {dataset!r}; "
                f"expected one of {', '.join(DATASET_ORDER)}."
            )
        family = row["graph_family"].strip()
        if family != ALGORITHM_FAMILIES[algorithm]:
            raise DataValidationError(
                f"Raw CSV line {line_number}: {algorithm} uses unexpected graph_family "
                f"{family!r}; expected {ALGORITHM_FAMILIES[algorithm]!r}."
            )
        status = row["correctness_status"].strip()
        if status != "PASS":
            raise DataValidationError(
                f"Raw CSV line {line_number}: correctness_status is {status!r}, not 'PASS'."
            )

        parsed: dict[str, object] = {
            "algorithm": algorithm,
            "dataset": dataset,
            "graph_family": family,
            "correctness_status": status,
        }
        for column in RAW_INTEGER_COLUMNS:
            parsed[column] = _parse_integer(row[column], column, line_number, "Raw runs")
        for column in RAW_INTEGER_COLUMNS:
            if parsed[column] < 0:
                raise DataValidationError(
                    f"Raw CSV line {line_number}: column '{column}' cannot be negative."
                )
        if parsed["repeat"] < 1:
            raise DataValidationError(
                f"Raw CSV line {line_number}: repeat must be positive."
            )
        if parsed["seed"] != EXPECTED_SEED:
            raise DataValidationError(
                f"Raw CSV line {line_number}: seed {parsed['seed']} does not match "
                f"configured seed {EXPECTED_SEED}."
            )
        expected = DATASETS[dataset]
        actual_edges = EXPECTED_ACTUAL_EDGES[(algorithm, dataset)]
        if parsed["vertices"] != expected["vertices"]:
            raise DataValidationError(
                f"Raw CSV line {line_number}: {dataset} expects {expected['vertices']} "
                f"vertices, got {parsed['vertices']}."
            )
        if parsed["requested_edges"] != expected["requested_edges"]:
            raise DataValidationError(
                f"Raw CSV line {line_number}: {dataset} requested_edges should be "
                f"{expected['requested_edges']}, got {parsed['requested_edges']}."
            )
        if parsed["actual_edges"] != actual_edges:
            explanation = (
                "Medium DAG actual_edges must be 495, not its requested target of 500."
                if algorithm == "TopologicalSort" and dataset == "Medium"
                else f"Expected actual_edges={actual_edges}."
            )
            raise DataValidationError(f"Raw CSV line {line_number}: {explanation}")

        repeat_key = (algorithm, dataset, parsed["repeat"])
        if repeat_key in seen_repeats:
            raise DataValidationError(
                f"Raw CSV line {line_number}: duplicate measured repeat "
                f"{parsed['repeat']} for {algorithm}/{dataset}."
            )
        seen_repeats.add(repeat_key)
        group_counts[(algorithm, dataset)] += 1
        parsed_rows.append(parsed)

    expected_groups = {
        (algorithm, dataset) for algorithm in ALGORITHMS for dataset in DATASET_ORDER
    }
    actual_groups = set(group_counts)
    if actual_groups != expected_groups:
        missing = sorted(expected_groups - actual_groups)
        extra = sorted(actual_groups - expected_groups)
        raise DataValidationError(
            f"Raw CSV algorithm/dataset groups are incomplete; missing={missing}, extra={extra}."
        )
    invalid_counts = {
        key: count for key, count in group_counts.items()
        if count != EXPECTED_REPETITIONS
    }
    if invalid_counts:
        raise DataValidationError(
            f"Expected exactly {EXPECTED_REPETITIONS} measured runs per algorithm/dataset; "
            f"found {invalid_counts}."
        )
    for key in expected_groups:
        repeats = {run["repeat"] for run in parsed_rows
                   if (run["algorithm"], run["dataset"]) == key}
        if repeats != set(range(1, EXPECTED_REPETITIONS + 1)):
            raise DataValidationError(
                f"Raw CSV repeats for {key[0]}/{key[1]} must be 1 through "
                f"{EXPECTED_REPETITIONS}; found {sorted(repeats)}."
            )
    return tuple(parsed_rows)


def load_summaries(path: Path) -> tuple[dict[str, object], ...]:
    rows = _read_csv(path, SUMMARY_REQUIRED_COLUMNS, "Summary")
    parsed_rows = []
    seen_groups: set[tuple[str, str]] = set()
    for line_number, row in enumerate(rows, start=2):
        algorithm = row["algorithm"].strip()
        dataset = row["dataset"].strip()
        if algorithm not in ALGORITHM_FAMILIES:
            raise DataValidationError(
                f"Summary CSV line {line_number}: unexpected algorithm {algorithm!r}."
            )
        if dataset not in DATASETS:
            raise DataValidationError(
                f"Summary CSV line {line_number}: unexpected dataset {dataset!r}."
            )
        family = row["graph_family"].strip()
        if family != ALGORITHM_FAMILIES[algorithm]:
            raise DataValidationError(
                f"Summary CSV line {line_number}: unexpected graph_family {family!r} "
                f"for {algorithm}."
            )
        key = (algorithm, dataset)
        if key in seen_groups:
            raise DataValidationError(
                f"Summary CSV line {line_number}: duplicate group {algorithm}/{dataset}."
            )
        seen_groups.add(key)

        parsed: dict[str, object] = {
            "algorithm": algorithm,
            "dataset": dataset,
            "graph_family": family,
        }
        for column in SUMMARY_INTEGER_COLUMNS:
            parsed[column] = _parse_integer(row[column], column, line_number, "Summary")
            if parsed[column] < 0:
                raise DataValidationError(
                    f"Summary CSV line {line_number}: column '{column}' cannot be negative."
                )
        for column in SUMMARY_DECIMAL_COLUMNS:
            parsed[column] = _parse_decimal(row[column], column, line_number, "Summary")
            if parsed[column] < 0:
                raise DataValidationError(
                    f"Summary CSV line {line_number}: column '{column}' cannot be negative."
                )
        expected = DATASETS[dataset]
        if (parsed["vertices"] != expected["vertices"]
                or parsed["requested_edges"] != expected["requested_edges"]
                or parsed["actual_edges"] != EXPECTED_ACTUAL_EDGES[(algorithm, dataset)]
                or parsed["seed"] != EXPECTED_SEED
                or parsed["sample_count"] != EXPECTED_REPETITIONS):
            raise DataValidationError(
                f"Summary CSV line {line_number}: graph metadata or sample count is "
                f"invalid for {algorithm}/{dataset}."
            )
        parsed_rows.append(parsed)

    expected_groups = {
        (algorithm, dataset) for algorithm in ALGORITHMS for dataset in DATASET_ORDER
    }
    if seen_groups != expected_groups:
        raise DataValidationError(
            "Summary CSV must contain exactly one row for every algorithm/dataset group; "
            f"missing={sorted(expected_groups - seen_groups)}, "
            f"extra={sorted(seen_groups - expected_groups)}."
        )
    return tuple(parsed_rows)


def validate_summary_consistency(
    raw_runs: Iterable[dict[str, object]],
    summaries: Iterable[dict[str, object]],
) -> None:
    grouped: dict[tuple[str, str], list[dict[str, object]]] = defaultdict(list)
    for run in raw_runs:
        grouped[(str(run["algorithm"]), str(run["dataset"]))].append(run)
    summary_by_group = {
        (str(summary["algorithm"]), str(summary["dataset"])): summary
        for summary in summaries
    }
    if set(grouped) != set(summary_by_group):
        raise DataValidationError(
            "Raw and summary CSVs contain different algorithm/dataset groups."
        )

    for key, runs in grouped.items():
        summary = summary_by_group[key]
        if len(runs) != EXPECTED_REPETITIONS:
            raise DataValidationError(
                f"Cannot compare summary for {key[0]}/{key[1]}: expected "
                f"{EXPECTED_REPETITIONS} raw runs, found {len(runs)}."
            )
        for metadata in (
            "graph_family", "vertices", "requested_edges", "actual_edges", "seed"
        ):
            if any(run[metadata] != summary[metadata] for run in runs):
                raise DataValidationError(
                    f"Raw and summary metadata mismatch for {key[0]}/{key[1]} "
                    f"in column '{metadata}'."
                )

        elapsed = sorted(int(run["elapsed_ns"]) for run in runs)
        mean = (Decimal(sum(elapsed)) / Decimal(len(elapsed))).quantize(
            Decimal("0.000001"), rounding=ROUND_HALF_UP
        )
        expected_summary: dict[str, Decimal | int] = {
            "sample_count": len(runs),
            "mean_ns": mean,
            "median_ns": Decimal(elapsed[2]),
            "min_ns": elapsed[0],
            "max_ns": elapsed[-1],
        }
        for field in COUNTER_FIELDS:
            mean_counter = (
                Decimal(sum(int(run[field]) for run in runs)) / Decimal(len(runs))
            ).quantize(Decimal("0.000001"), rounding=ROUND_HALF_UP)
            expected_summary[f"mean_{field}"] = mean_counter

        for field, expected_value in expected_summary.items():
            actual_value = summary[field]
            if isinstance(expected_value, Decimal):
                matches = abs(Decimal(actual_value) - expected_value) <= MEAN_TOLERANCE
            else:
                matches = int(actual_value) == expected_value
            if not matches:
                raise DataValidationError(
                    f"Summary mismatch for {key[0]}/{key[1]} column '{field}': "
                    f"CSV has {actual_value}, raw runs calculate {expected_value}."
                )


def load_benchmark_data(raw_path: Path, summary_path: Path) -> BenchmarkData:
    raw_runs = load_raw_runs(raw_path)
    summaries = load_summaries(summary_path)
    validate_summary_consistency(raw_runs, summaries)
    return BenchmarkData(raw_runs, summaries)


def _groups_by_algorithm_dataset(
    rows: Iterable[dict[str, object]],
) -> dict[tuple[str, str], list[dict[str, object]]]:
    groups: dict[tuple[str, str], list[dict[str, object]]] = defaultdict(list)
    for row in rows:
        groups[(str(row["algorithm"]), str(row["dataset"]))].append(row)
    return groups


def _ordered_summaries(data: BenchmarkData) -> list[dict[str, object]]:
    return sorted(
        data.summaries,
        key=lambda summary: (
            DATASET_ORDER.index(str(summary["dataset"])),
            ALGORITHMS.index(str(summary["algorithm"])),
        ),
    )


def plot_runtime_mean(data: BenchmarkData, output_directory: Path) -> Path:
    summaries = _ordered_summaries(data)
    x = np.arange(len(DATASET_ORDER))
    fig, axis = plt.subplots(figsize=(11, 6.5), constrained_layout=True)
    for algorithm_index, algorithm in enumerate(ALGORITHMS):
        values = [
            float(next(
                summary["mean_ns"] for summary in summaries
                if summary["algorithm"] == algorithm and summary["dataset"] == dataset
            )) / 1_000_000
            for dataset in DATASET_ORDER
        ]
        offset = (algorithm_index - (len(ALGORITHMS) - 1) / 2) * 0.12
        axis.scatter(
            x + offset, values, label=algorithm,
            color=ALGORITHM_COLORS[algorithm], s=70, zorder=3,
        )
    axis.set_yscale("symlog", linthresh=0.001)
    axis.set_ylabel("Mean elapsed time (milliseconds, symmetric log scale)")
    axis.set_xlabel("Tested dataset (categorical)")
    axis.set_xticks(x, DATASET_ORDER)
    axis.set_title("Mean Runtime by Algorithm and Dataset")
    axis.grid(axis="y", which="both", alpha=0.25)
    axis.legend(title="Algorithm", ncols=2)
    path = output_directory / "runtime_mean_by_dataset.png"
    fig.savefig(path, dpi=180, bbox_inches="tight")
    plt.close(fig)
    return path


def plot_runtime_distribution(data: BenchmarkData, output_directory: Path) -> Path:
    raw_groups = _groups_by_algorithm_dataset(data.raw_runs)
    fig, axes = plt.subplots(1, 3, figsize=(16, 6), sharey=True, constrained_layout=True)
    for axis, dataset in zip(axes, DATASET_ORDER):
        values = [
            [int(run["elapsed_ns"]) / 1_000_000
             for run in raw_groups[(algorithm, dataset)]]
            for algorithm in ALGORITHMS
        ]
        box = axis.boxplot(values, patch_artist=True, showmeans=True, widths=0.65)
        for patch, algorithm in zip(box["boxes"], ALGORITHMS):
            patch.set_facecolor(ALGORITHM_COLORS[algorithm])
            patch.set_alpha(0.65)
        axis.set_yscale("symlog", linthresh=0.001)
        axis.set_title(dataset)
        axis.set_xticks(range(1, len(ALGORITHMS) + 1), ALGORITHMS, rotation=35, ha="right")
        axis.set_xlabel("Algorithm")
        axis.grid(axis="y", which="both", alpha=0.25)
    axes[0].set_ylabel(
        "Measured elapsed time (milliseconds, symmetric log scale)"
    )
    fig.suptitle("Runtime Distribution Across Five Measured Repetitions")
    fig.text(
        0.5, -0.02,
        "Raw runs only; warm-ups excluded. Box center is median; triangle is mean.",
        ha="center", fontsize=9,
    )
    path = output_directory / "runtime_distribution.png"
    fig.savefig(path, dpi=180, bbox_inches="tight")
    plt.close(fig)
    return path


def plot_operation_counts(data: BenchmarkData, output_directory: Path) -> Path:
    summaries = _ordered_summaries(data)
    fig, axes = plt.subplots(
        len(ALGORITHMS), 4, figsize=(17, 15), sharex=True, constrained_layout=True
    )
    for row_index, algorithm in enumerate(ALGORITHMS):
        fields = COUNTERS_BY_ALGORITHM[algorithm]
        for column_index, field in enumerate(fields):
            axis = axes[row_index, column_index]
            values = [
                float(next(
                    summary[f"mean_{field}"] for summary in summaries
                    if summary["algorithm"] == algorithm
                    and summary["dataset"] == dataset
                ))
                for dataset in DATASET_ORDER
            ]
            axis.plot(DATASET_ORDER, values, marker="o", linewidth=1.8,
                      color=ALGORITHM_COLORS[algorithm])
            axis.set_xticks(range(len(DATASET_ORDER)), DATASET_ORDER)
            axis.tick_params(axis="x", labelbottom=True)
            axis.set_title(COUNTER_LABELS[field], fontsize=9)
            axis.set_ylabel("Mean count")
            axis.grid(axis="y", alpha=0.25)
            if row_index == len(ALGORITHMS) - 1:
                axis.set_xlabel("Tested dataset (categorical)")
        axes[row_index, 0].text(
            -0.27, 0.5, algorithm, transform=axes[row_index, 0].transAxes,
            rotation=90, va="center", ha="center", fontweight="bold",
        )
        for column_index in range(len(fields), 4):
            axes[row_index, column_index].set_visible(False)
    fig.suptitle(
        "Algorithm-Specific Operation Counters\n"
        "Separate panels are distinct metrics; counts are not cross-comparable",
        fontsize=14,
    )
    path = output_directory / "operation_counts.png"
    fig.savefig(path, dpi=180, bbox_inches="tight")
    plt.close(fig)
    return path


def plot_runtime_scaling(data: BenchmarkData, output_directory: Path) -> Path:
    summaries = _ordered_summaries(data)
    fig, axes = plt.subplots(1, len(ALGORITHMS), figsize=(18, 5.5), sharey=True,
                             constrained_layout=True)
    for axis, algorithm in zip(axes, ALGORITHMS):
        algorithm_summaries = [
            next(
                summary for summary in summaries
                if summary["algorithm"] == algorithm and summary["dataset"] == dataset
            )
            for dataset in DATASET_ORDER
        ]
        categories = [
            f"{summary['dataset']}\nV={summary['vertices']}, E={summary['actual_edges']}"
            for summary in algorithm_summaries
        ]
        means_ms = [float(summary["mean_ns"]) / 1_000_000
                    for summary in algorithm_summaries]
        axis.plot(
            range(len(categories)), means_ms, marker="o", linewidth=2,
            color=ALGORITHM_COLORS[algorithm],
        )
        axis.set_xticks(range(len(categories)), categories)
        axis.set_title(algorithm)
        axis.set_xlabel("Categorical dataset (actual V and E)")
        axis.set_yscale("symlog", linthresh=0.001)
        axis.grid(axis="y", which="both", alpha=0.25)
    axes[0].set_ylabel(
        "Mean elapsed time (milliseconds, symmetric log scale)"
    )
    fig.suptitle(
        "Runtime Across Tested Datasets (Not a Proof of Asymptotic Complexity)"
    )
    path = output_directory / "runtime_scaling.png"
    fig.savefig(path, dpi=180, bbox_inches="tight")
    plt.close(fig)
    return path


def generate_plots(data: BenchmarkData, output_directory: Path) -> tuple[Path, ...]:
    output_directory.mkdir(parents=True, exist_ok=True)
    return (
        plot_runtime_mean(data, output_directory),
        plot_runtime_distribution(data, output_directory),
        plot_operation_counts(data, output_directory),
        plot_runtime_scaling(data, output_directory),
    )


def main(argv: list[str] | None = None) -> int:
    args = list(sys.argv[1:] if argv is None else argv)
    if len(args) > 1:
        print(
            "Usage: python scripts/plot_results.py [output-directory]",
            file=sys.stderr,
        )
        return 2
    output_directory = (
        Path(args[0]).resolve()
        if args else PROJECT_ROOT / DEFAULT_OUTPUT_DIRECTORY
    )
    raw_path = PROJECT_ROOT / RAW_CSV
    summary_path = PROJECT_ROOT / SUMMARY_CSV
    try:
        data = load_benchmark_data(raw_path, summary_path)
        generated = generate_plots(data, output_directory)
    except (DataValidationError, OSError, ValueError) as error:
        print(f"Plot generation failed: {error}", file=sys.stderr)
        return 1

    print(f"Validated input: {raw_path}")
    print(f"Validated input: {summary_path}")
    print("Generated plots:")
    for path in generated:
        print(f"  {path}")
    print(
        f"Included {len(data.raw_runs)} measured observations "
        f"({EXPECTED_REPETITIONS} per algorithm/dataset); all correctness statuses PASS."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
