"""Minimal import shim for Yuxi parsers that optionally publish OCR images to MinIO.

Geo RAG stores uploaded source files locally and does not include MinIO. Local-file
PaddleOCR parsing does not call this function; remote image publishing fails clearly.
"""

from pathlib import Path


# Keep this Geo-local MinIO-unavailable adapter as the import target while
# exposing the migrated upstream client modules as optional submodules.
__path__ = [str(Path(__file__).with_suffix(""))]


def get_minio_client():
    raise RuntimeError("MinIO image publishing is not configured in Geo RAG Studio")
