"""Chunk preset contract for the local Yuxi/RAGFlow parser port."""

from copy import deepcopy

from . import _upstream  # noqa: F401
from yuxi.knowledge.chunking.ragflow_like.presets import (
    CHUNK_ENGINE_VERSION as YUXI_CHUNK_ENGINE_VERSION,
    CHUNK_PRESETS as YUXI_CHUNK_PRESETS,
    get_chunk_preset_options as get_yuxi_chunk_preset_options,
)

DEFAULT_CHUNK_PRESET_ID = "general"
CHUNK_ENGINE_VERSION = f"{YUXI_CHUNK_ENGINE_VERSION}+source-map-v1"
CHUNK_PRESETS = {
    preset_id: {
        "label": preset["label"],
        "description": preset["description"],
    }
    for preset_id, preset in YUXI_CHUNK_PRESETS.items()
}


def deep_merge(base: dict, override: dict | None) -> dict:
    result = deepcopy(base)
    for key, value in (override or {}).items():
        if isinstance(value, dict) and isinstance(result.get(key), dict):
            result[key] = deep_merge(result[key], value)
        else:
            result[key] = value
    return result


def normalize_preset(value: str | None) -> str:
    preset = str(value or DEFAULT_CHUNK_PRESET_ID).strip().lower()
    if preset not in CHUNK_PRESETS:
        raise ValueError(f"Unknown chunk preset: {preset}")
    return preset


def normalize_chunk_config(config: dict | None) -> dict:
    raw = config or {}
    if not isinstance(raw, dict):
        raise ValueError("chunk_parser_config must be an object")

    token_value = raw.get("chunk_token_num", 512)
    overlap_value = raw.get("overlapped_percent", 10)
    if isinstance(token_value, bool) or not isinstance(token_value, int):
        raise ValueError("chunk_token_num must be an integer from 64 to 4096")
    if not 64 <= token_value <= 4096:
        raise ValueError("chunk_token_num must be from 64 to 4096")
    if isinstance(overlap_value, bool) or not isinstance(overlap_value, int):
        raise ValueError("overlapped_percent must be an integer from 0 to 80")
    if not 0 <= overlap_value <= 80:
        raise ValueError("overlapped_percent must be from 0 to 80")

    delimiter = raw.get("delimiter", "\\n")
    if not isinstance(delimiter, str) or not delimiter:
        raise ValueError("delimiter must be a non-empty string")
    if len(delimiter) > 64:
        raise ValueError("delimiter must be at most 64 characters")
    delimiter = (
        delimiter.replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\\\", "\\")
    )
    return {
        "chunk_token_num": token_value,
        "overlapped_percent": overlap_value,
        "delimiter": delimiter,
    }


def get_options() -> list[dict]:
    return [
        {**option, "experimental": option["value"] == "semantic"}
        for option in get_yuxi_chunk_preset_options()
    ]
