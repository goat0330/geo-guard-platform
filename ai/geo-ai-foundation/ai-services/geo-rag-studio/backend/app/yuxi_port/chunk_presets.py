"""Chunk preset contract for the local Yuxi/RAGFlow parser port."""

from copy import deepcopy

DEFAULT_CHUNK_PRESET_ID = "general"
CHUNK_ENGINE_VERSION = "yuxi-ragflow-like-v1+source-map-v1"
CHUNK_PRESETS = {
    "general": {"label": "General", "description": "通用分块：按段落和长度组织文本。"},
    "qa": {"label": "QA", "description": "问答分块：将问题与回答尽量保留在同一块。"},
    "book": {"label": "Book", "description": "手册分块：保留章节标题与后续内容。"},
    "laws": {"label": "Laws", "description": "法规分块：按章、节、条组织规范文本。"},
    "semantic": {"label": "Semantic", "description": "实验：当前为段落分块 fallback，不执行语义聚类。"},
    "separator": {"label": "Separator", "description": "按指定分隔符切分，超长片段再按 token 目标拆分。"},
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
        {"value": key, **value, "experimental": key == "semantic"}
        for key, value in CHUNK_PRESETS.items()
    ]
