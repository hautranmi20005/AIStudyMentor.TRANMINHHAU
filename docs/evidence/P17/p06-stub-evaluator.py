#!/usr/bin/env python3
"""Replay the P06 benchmark runner against a deterministic local-only evaluator."""

import json
import subprocess
import sys
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
BENCHMARK = ROOT / "docs" / "ai-benchmark"
EVIDENCE = ROOT / "docs" / "evidence" / "P17"
DATASET = json.loads((BENCHMARK / "questions.json").read_text(encoding="utf-8"))
SAMPLES = {sample["question"]: sample for sample in DATASET}


class Evaluator(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers.get("Content-Length", "0"))
        request = json.loads(self.rfile.read(length).decode("utf-8"))
        sample = SAMPLES.get(request.get("questionText"))
        if sample is None:
            self.send_error(404)
            return
        if sample["kind"] == "refusal":
            self._respond(422, {"code": "AI_OUT_OF_SCOPE"})
            return

        answer = sample["expectedAnswer"]
        self._respond(201, {
            "answer": {
                "structuredExplanation": {
                    "subject": sample["subject"],
                    "difficulty": "secondary",
                    "summary": answer,
                    "steps": [{
                        "stepNumber": 1,
                        "title": "Stub evaluation",
                        "explanation": answer,
                    }],
                    "finalAnswer": answer,
                    "keyConcepts": ["stub"],
                    "commonMistakes": ["not applicable"],
                    "followUpQuestions": ["none"],
                }
            }
        })

    def log_message(self, _format, *_args):
        return

    def _respond(self, status, payload):
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def main():
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    server = ThreadingHTTPServer(("127.0.0.1", 8765), Evaluator)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        output = EVIDENCE / "p06-stub-benchmark-report.md"
        result = subprocess.run(
            [
                sys.executable,
                str(BENCHMARK / "run.py"),
                "--base-url",
                "http://127.0.0.1:8765",
                "--token",
                "p17-local-stub-only",
                "--output",
                str(output),
            ],
            capture_output=True,
            text=True,
            check=False,
        )
        console = result.stdout + result.stderr
        (EVIDENCE / "p06-stub-benchmark-console.log").write_text(console, encoding="utf-8")
        sys.stdout.write(console)
        return result.returncode
    finally:
        server.shutdown()
        server.server_close()
        thread.join(timeout=5)


if __name__ == "__main__":
    raise SystemExit(main())
