#!/usr/bin/env bash
set -euo pipefail
secret="${1:-JBSWY3DPEHPK3PXP}"
python3 - "$secret" <<'PY'
import base64
import hashlib
import hmac
import struct
import sys
import time

secret = sys.argv[1].strip().upper()
key = base64.b32decode(secret + "=" * ((8 - len(secret) % 8) % 8), casefold=True)
counter = int(time.time()) // 30
digest = hmac.new(key, struct.pack(">Q", counter), hashlib.sha1).digest()
offset = digest[-1] & 0x0F
code = (struct.unpack(">I", digest[offset:offset + 4])[0] & 0x7FFFFFFF) % 1_000_000
print(f"{code:06d}")
PY
