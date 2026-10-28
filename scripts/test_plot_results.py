"""Unit and integration tests for Day 10 plot data validation."""

from __future__ import annotations

import csv
import hashlib
import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path

from PIL import Image


SCRIPT_PATH = Path(__file__).with_name("plot_results.py")
SPEC = importlib.util.spec_from_file_location("plot_results", SCRIPT_PATH)
assert SPEC is not None and SPEC.loader is not None
plot_results = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = plot_results
SPEC.loader.exec_module(plot_results)

PROJECT_ROOT = plot_results.PROJECT_ROOT
REAL_RAW = PROJECT_ROOT / plot_results.RAW_CSV
REAL_SUMMARY = PROJECT_ROOT / plot_results.SUMMARY_CSV


def rewrite_csv(source: Path, destination: Path, mutate) -> None:
    with source.open("r", encoding="utf-8-sig", newline="") as stream:
        reader = csv.DictReader(stream)
        fieldnames = reader.fieldnames
        rows = list(reader)
    if fieldnames is None:
        raise AssertionError(f"Fixture has no CSV header: {source}")
    new_fieldnames, rows = mutate(list(fieldnames), rows)
    with destination.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=new_fieldnames, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


class PlotResultsTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary_directory = tempfile.TemporaryDirectory()
        self.temp = Path(self.temporary_directory.name)
        self.raw = self.temp / "benchmark_runs.csv"
        self.summary = self.temp / "benchmark_summary.csv"

    def tearDown(self) -> None:
        self.temporary_directory.cleanup()

    def copy_real_inputs(self) -> None:
        self.raw.write_bytes(REAL_RAW.read_bytes())
        self.summary.write_bytes(REAL_SUMMARY.read_bytes())

    def test_real_csv_files_validate_and_have_expected_shapes(self) -> None:
        data = plot_results.load_benchmark_data(REAL_RAW, REAL_SUMMARY)
        self.assertEqual(75, len(data.raw_runs))
        self.assertEqual(15, len(data.summaries))
        self.assertTrue(all(row["correctness_status"] == "PASS" for row in data.raw_runs))
        medium_dag = [
            row for row in data.raw_runs
            if row["algorithm"] == "TopologicalSort" and row["dataset"] == "Medium"
        ]
        self.assertEqual({495}, {row["actual_edges"] for row in medium_dag})

    def test_missing_input_file_gives_actionable_error(self) -> None:
        with self.assertRaisesRegex(plot_results.DataValidationError, "Run the Day 9"):
            plot_results.load_raw_runs(self.temp / "missing.csv")

    def test_missing_required_column_is_rejected(self) -> None:
        self.copy_real_inputs()

        def remove_column(fields, rows):
            fields.remove("elapsed_ns")
            for row in rows:
                row.pop("elapsed_ns")
            return fields, rows

        rewrite_csv(self.raw, self.raw, remove_column)
        with self.assertRaisesRegex(plot_results.DataValidationError, "elapsed_ns"):
            plot_results.load_raw_runs(self.raw)

    def test_malformed_numeric_value_is_rejected(self) -> None:
        self.copy_real_inputs()

        def malformed(fields, rows):
            rows[0]["elapsed_ns"] = "not-a-number"
            return fields, rows

        rewrite_csv(self.raw, self.raw, malformed)
        with self.assertRaisesRegex(plot_results.DataValidationError, "must be an integer"):
            plot_results.load_raw_runs(self.raw)

    def test_unsuccessful_correctness_status_is_rejected(self) -> None:
        self.copy_real_inputs()

        def failed(fields, rows):
            rows[0]["correctness_status"] = "FAIL"
            return fields, rows

        rewrite_csv(self.raw, self.raw, failed)
        with self.assertRaisesRegex(plot_results.DataValidationError, "not 'PASS'"):
            plot_results.load_raw_runs(self.raw)

    def test_unexpected_algorithm_and_dataset_are_rejected(self) -> None:
        self.copy_real_inputs()

        def unexpected_algorithm(fields, rows):
            rows[0]["algorithm"] = "Unknown"
            return fields, rows

        rewrite_csv(self.raw, self.raw, unexpected_algorithm)
        with self.assertRaisesRegex(plot_results.DataValidationError, "unexpected algorithm"):
            plot_results.load_raw_runs(self.raw)

        self.copy_real_inputs()

        def unexpected_dataset(fields, rows):
            rows[0]["dataset"] = "Huge"
            return fields, rows

        rewrite_csv(self.raw, self.raw, unexpected_dataset)
        with self.assertRaisesRegex(plot_results.DataValidationError, "unexpected dataset"):
            plot_results.load_raw_runs(self.raw)

    def test_incorrect_measured_repetition_count_is_rejected(self) -> None:
        self.copy_real_inputs()

        def remove_run(fields, rows):
            return fields, rows[1:]

        rewrite_csv(self.raw, self.raw, remove_run)
        with self.assertRaisesRegex(plot_results.DataValidationError, "exactly 5"):
            plot_results.load_raw_runs(self.raw)

    def test_raw_summary_mismatch_is_rejected(self) -> None:
        self.copy_real_inputs()

        def mismatch(fields, rows):
            rows[0]["mean_ns"] = str(int(rows[0]["mean_ns"]) + 100)
            return fields, rows

        rewrite_csv(self.summary, self.summary, mismatch)
        with self.assertRaisesRegex(plot_results.DataValidationError, "Summary mismatch"):
            plot_results.load_benchmark_data(self.raw, self.summary)

    def test_actual_medium_dag_edges_cannot_be_replaced_with_request(self) -> None:
        self.copy_real_inputs()

        def incorrect_edges(fields, rows):
            for row in rows:
                if row["algorithm"] == "TopologicalSort" and row["dataset"] == "Medium":
                    row["actual_edges"] = "500"
            return fields, rows

        rewrite_csv(self.raw, self.raw, incorrect_edges)
        with self.assertRaisesRegex(plot_results.DataValidationError, "must be 495"):
            plot_results.load_raw_runs(self.raw)

    def test_all_plots_are_valid_pngs_and_input_csvs_remain_unchanged(self) -> None:
        data = plot_results.load_benchmark_data(REAL_RAW, REAL_SUMMARY)
        original_hashes = {
            REAL_RAW: hashlib.sha256(REAL_RAW.read_bytes()).digest(),
            REAL_SUMMARY: hashlib.sha256(REAL_SUMMARY.read_bytes()).digest(),
        }
        output = self.temp / "nested" / "plots"
        generated = plot_results.generate_plots(data, output)

        self.assertEqual(
            {
                "runtime_mean_by_dataset.png",
                "runtime_distribution.png",
                "operation_counts.png",
                "runtime_scaling.png",
            },
            {path.name for path in generated},
        )
        self.assertTrue(output.is_dir())
        for path in generated:
            self.assertTrue(path.is_file())
            self.assertGreater(path.stat().st_size, 0)
            with Image.open(path) as image:
                image.verify()
            with Image.open(path) as image:
                self.assertEqual("PNG", image.format)
                self.assertGreaterEqual(image.width, 1200)
                self.assertGreaterEqual(image.height, 700)
        self.assertEqual(
            original_hashes[REAL_RAW], hashlib.sha256(REAL_RAW.read_bytes()).digest()
        )
        self.assertEqual(
            original_hashes[REAL_SUMMARY],
            hashlib.sha256(REAL_SUMMARY.read_bytes()).digest(),
        )

    def test_zero_elapsed_measurements_are_plotable_on_symmetric_log_axes(self) -> None:
        original = plot_results.load_benchmark_data(REAL_RAW, REAL_SUMMARY)
        raw_runs = tuple({**row, "elapsed_ns": 0} for row in original.raw_runs)
        summaries = tuple(
            {
                **row,
                "mean_ns": 0,
                "median_ns": 0,
                "min_ns": 0,
                "max_ns": 0,
            }
            for row in original.summaries
        )
        data = plot_results.BenchmarkData(raw_runs, summaries)
        plot_results.validate_summary_consistency(data.raw_runs, data.summaries)
        generated = plot_results.generate_plots(data, self.temp / "zero-plots")
        self.assertEqual(4, len(generated))
        self.assertTrue(all(path.stat().st_size > 0 for path in generated))


if __name__ == "__main__":
    unittest.main()
