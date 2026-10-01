"""Expose the vendored Yuxi source package to the local RAG adapter."""

import sys
from pathlib import Path

VENDORED_YUXI_PACKAGE = Path(__file__).resolve().parents[2] / "vendor" / "yuxi" / "package"
if str(VENDORED_YUXI_PACKAGE) not in sys.path:
    sys.path.insert(0, str(VENDORED_YUXI_PACKAGE))
