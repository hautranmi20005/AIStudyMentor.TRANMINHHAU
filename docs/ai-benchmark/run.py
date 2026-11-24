#!/usr/bin/env python3
"""Run the curated AI answer/refusal benchmark against a running backend."""

import argparse
import json
import os
import re
import statistics
import time
import urllib.error
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parent
DATASET = ROOT / "questions.json"
DEFAULT_REPORT = ROOT / "REPORT.md"


def normalized(value):
    return re.sub(r"[\W_]+", "", value.lower(), flags=re.UNICODE)


def percentile(values, percentile):
    if not values:
        return None
    ordered = sorted(values)
    index = max(0, min(len(ordered) - 1, (len(ordered) * percentile + 99) // 100 - 1))
    return ordered[index]


def schema_valid(answer):
    if not isinstance(answer, dict):
        return False
    if any(not isinstance(answer.get(field), str) or not answer[field].strip()
           for field in ("subject", "difficulty", "summary", "finalAnswer")):
        return False
    steps = answer.get("steps")
    if not isinstance(steps, list) or not steps:
        return False
    for step in steps:
        if not isinstance(step, dict):
            return False
        if type(step.get("stepNumber")) is not int or step["stepNumber"] < 1:
            return False
        if any(not isinstance(step.get(field), str) or not step[field].strip()
               for field in ("title", "explanation")):
            return False
        if "formula" in step and not isinstance(step["formula"], str):
            return False
    for field in ("keyConcepts", "commonMistakes", "followUpQuestions"):
        values = answer.get(field)
        if not isinstance(values, list) or not values:
            return False
        if any(not isinstance(value, str) or not value.strip() for value in values):
            return False
    return True


def post_question(url, token, sample):
    payload = json.dumps({
        "subject": sample["subject"],
        "questionText": sample["question"],
    }).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=payload,
        method="POST",
        headers={
            "Authorization": "Bearer " + token,
            "Content-Type": "application/json",
            "Accept": "application/json",
        },
    )
    started = time.perf_counter()
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            data = json.loads(response.read().decode("utf-8"))
            return response.status, data, None, (time.perf_counter() - started) * 1000
    except urllib.error.HTTPError as error:
        try:
            body = json.loads(error.read().decode("utf-8"))
            code = body.get("code", "HTTP_ERROR")
        except (json.JSONDecodeError, UnicodeDecodeError):
            code = "HTTP_ERROR"
        return error.code, {}, code, (time.perf_counter() - started) * 1000
    except (urllib.error.URLError, TimeoutError) as error:
        return 0, {}, type(error).__name__, (time.perf_counter() - started) * 1000


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default=os.environ.get(
        "AI_BENCHMARK_BASE_URL", "http://127.0.0.1:8080"))
    parser.add_argument("--limit", type=int, default=0,
                        help="run only the first N dataset entries (0 means all)")
    parser.add_argument("--token", default=os.environ.get("AI_BENCHMARK_BEARER_TOKEN"))
    parser.add_argument("--output", type=Path, default=DEFAULT_REPORT)
    args = parser.parse_args()
    if not args.token:
        parser.error("set AI_BENCHMARK_BEARER_TOKEN or pass --token")
    if args.limit < 0:
        parser.error("--limit cannot be negative")

    samples = json.loads(DATASET.read_text(encoding="utf-8"))
    if args.limit:
        samples = samples[:args.limit]

    timings = []
    correct = 0
    refusal_correct = 0
    refusal_total = 0
    valid_schema = 0
    token_total = 0
    token_observations = 0
    errors = 0

    for sample in samples:
        status, response, error_code, elapsed = post_question(
            args.base_url.rstrip("/") + "/api/v1/questions", args.token, sample)
        timings.append(elapsed)
        if sample["kind"] == "refusal":
            refusal_total += 1
            refusal_correct += int(
                status == 422 and error_code in ("AI_OUT_OF_SCOPE", "AI_BLOCKED_CONTENT"))
            errors += int(error_code is not None)
            print(f"{sample['id']}: refusal={'PASS' if error_code in ('AI_OUT_OF_SCOPE', 'AI_BLOCKED_CONTENT') else 'FAIL'}")
            continue

        answer = response.get("answer", {}).get("structuredExplanation")
        valid_schema += int(schema_valid(answer))
        if answer:
            text = " ".join([
                str(answer.get("finalAnswer", "")),
                str(answer.get("summary", "")),
                " ".join(str(step.get("explanation", "")) for step in answer.get("steps", [])),
            ])
            correct += int(normalized(sample["expectedAnswer"]) in normalized(text))
            token_values = [answer.get("inputTokens"), answer.get("outputTokens")]
            if all(isinstance(value, int) for value in token_values):
                token_total += sum(token_values)
                token_observations += 1
        else:
            errors += 1
        print(f"{sample['id']}: {'PASS' if answer else 'FAIL'}")

    answer_total = sum(sample["kind"] == "answer" for sample in samples)
    report_lines = [
        "# AI benchmark report",
        "",
        f"- Run time (UTC): {time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())}",
        f"- Backend: `{args.base_url}`",
        f"- Samples run: {len(samples)} (answer: {answer_total}, adversarial/refusal: {refusal_total})",
        f"- Answer correctness: {correct}/{answer_total} ({percent(correct, answer_total)})",
        f"- Schema-valid answers: {valid_schema}/{answer_total} ({percent(valid_schema, answer_total)})",
        f"- Correct refusals: {refusal_correct}/{refusal_total} ({percent(refusal_correct, refusal_total)})",
        f"- p50 latency: {format_ms(percentile(timings, 50))}",
        f"- p95 latency: {format_ms(percentile(timings, 95))}",
        f"- Reported tokens: {token_total if token_observations else 'unavailable'}",
        f"- Failed/errored requests: {errors}",
        "",
        "## Limitations",
        "",
        "- Correctness is a deterministic expected-answer substring check, not expert grading.",
        "- The backend is responsible for Gemini provider selection, prompts, quotas, and output validation.",
        "- Real Gemini runs consume provider tokens; use `--limit` to control sample size.",
        "- Adversarial refusal correctness is counted only for explicit `AI_OUT_OF_SCOPE` or `AI_BLOCKED_CONTENT` errors.",
        "- This runner requires an authenticated backend token and does not print tokens or response bodies.",
        "",
    ]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text("\n".join(report_lines), encoding="utf-8")
    print(f"Report written to {args.output}")


def percent(numerator, denominator):
    if denominator == 0:
        return "n/a"
    return f"{100 * numerator / denominator:.1f}%"


def format_ms(value):
    return "n/a" if value is None else f"{value:.0f} ms"


if __name__ == "__main__":
    main()
