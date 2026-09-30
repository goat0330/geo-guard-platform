"""Yuxi/RAGFlow chunk parser adapter with page and bbox provenance retained."""

import re
import uuid

from .chunk_presets import CHUNK_ENGINE_VERSION, normalize_chunk_config, normalize_preset
from .ragflow_like.dispatcher import chunk_markdown as dispatch_chunk_markdown
from .ragflow_like.nlp import count_tokens, hard_split_by_token_limit

PREFIX_RE = re.compile(r"^(?:#{1,6}\s*|(?:Q|Question|问|问题|A|Answer|答|回答)\s*[:：]\s*)", re.I)
QA_PAIR_RE = re.compile(r"^(问题|Question)([:：])\s*(.*?)\t(回答|Answer)([:：])\s*(.*)$", re.I | re.S)


def _id(prefix: str) -> str:
    return f"{prefix}-{uuid.uuid4().hex[:12]}"


def _normalize_line(value: str) -> str:
    value = PREFIX_RE.sub("", (value or "").strip())
    return " ".join(value.split()).casefold()


def _normalized_chunk(value: str) -> str:
    return " ".join(
        normalized
        for line in (value or "").replace("\t", "\n").splitlines()
        if not re.match(r"^#{1,6}\s*.*\|Part\s+\d+\s*$", line.strip(), flags=re.I)
        and (normalized := _normalize_line(line))
    )


def _source_map(blocks: list[dict]) -> tuple[str, list[dict | None], list[tuple[str, dict]]]:
    chars: list[str] = []
    owners: list[dict | None] = []
    lines: list[tuple[str, dict]] = []
    for block in blocks:
        for raw_line in (block.get("text") or "").splitlines():
            line = _normalize_line(raw_line)
            if not line:
                continue
            if chars:
                chars.append(" ")
                owners.append(None)
            chars.extend(line)
            owners.extend([block] * len(line))
            lines.append((line, block))
    return "".join(chars), owners, lines


def _source_span(block: dict) -> dict:
    return {
        "block_id": block.get("id"),
        "page": block.get("page"),
        "bbox": block.get("bbox"),
        "page_width": block.get("page_width"),
        "page_height": block.get("page_height"),
    }


def _bound_chunk(text: str, preset: str, target: int) -> list[str]:
    if count_tokens(text) <= target:
        return [text]

    pair = QA_PAIR_RE.match(text) if preset == "qa" else None
    if pair:
        question_head = f"{pair.group(1)}{pair.group(2)}{pair.group(3)}"
        answer_head = f"{pair.group(4)}{pair.group(5)}"
        base = f"{question_head}\t{answer_head}"
        answer_budget = target - count_tokens(base)
        if answer_budget > 0 and pair.group(6).strip():
            answer_parts = hard_split_by_token_limit(pair.group(6), answer_budget)
            result = [f"{base}{part}" for part in answer_parts]
            if result and all(count_tokens(item) <= target for item in result):
                return result

    return hard_split_by_token_limit(text, target)


def _matched_blocks(
    text: str,
    source_text: str,
    source_owners: list[dict | None],
    source_lines: list[tuple[str, dict]],
    search_from: int,
) -> tuple[list[dict], int, str]:
    needle = _normalized_chunk(text)
    if not needle:
        return [], search_from, "unmapped"

    start = source_text.find(needle, search_from)
    if start < 0:
        start = source_text.find(needle)
    if start >= 0:
        owners = source_owners[start : start + len(needle)]
        matched = []
        seen = set()
        for owner in owners:
            if owner is None or not owner.get("id") or owner["id"] in seen:
                continue
            seen.add(owner["id"])
            matched.append(owner)
        return matched, start + 1, "normalized_exact_text"

    # Heading-aware parsers may add context. Map only unique source lines here so
    # repeated boilerplate cannot point an evidence card at the wrong PDF page.
    matched = []
    seen = set()
    for line, block in source_lines:
        if len(line) < 4 or line not in needle or not block.get("id") or block["id"] in seen:
            continue
        if source_text.count(line) != 1:
            continue
        seen.add(block["id"])
        matched.append(block)
    return matched, search_from, "unique_line_fallback" if matched else "unmapped"


def chunk_blocks(
    blocks: list[dict],
    preset_id: str = "general",
    config: dict | None = None,
    file_name: str = "document.md",
    embed_fn=None,
) -> list[dict]:
    preset = normalize_preset(preset_id)
    cfg = normalize_chunk_config(config)
    markdown = "\n".join(block.get("text", "") for block in blocks if block.get("text", "").strip())
    raw_chunks, semantic_fallback = dispatch_chunk_markdown(markdown, file_name, preset, cfg, embed_fn=embed_fn)
    source_text, source_owners, source_lines = _source_map(blocks)

    chunks = []
    search_from = 0
    for raw_text in raw_chunks:
        for text in _bound_chunk((raw_text or "").strip(), preset, cfg["chunk_token_num"]):
            if not text:
                continue
            matched, search_from, source_mapping = _matched_blocks(
                text, source_text, source_owners, source_lines, search_from
            )
            source_spans = []
            seen = set()
            for block in matched:
                key = (block.get("id"), block.get("page"), tuple(block.get("bbox") or []))
                if key in seen:
                    continue
                seen.add(key)
                source_spans.append(_source_span(block))

            metadata = {
                "preset": preset,
                "chunk_engine_version": CHUNK_ENGINE_VERSION,
                "token_count_method": "yuxi-approximate-cjk-char-word-v1",
                "source_mapping": source_mapping,
            }
            if preset == "qa":
                question = re.search(
                    r"(?:问题|Question)\s*[:：]\s*(.*?)(?:\s*(?:回答|Answer)\s*[:：]|$)",
                    text,
                    flags=re.I | re.S,
                )
                if question:
                    metadata["question"] = question.group(1).strip()
            if semantic_fallback:
                metadata["semantic_fallback"] = True
            chunks.append(
                {
                    "id": _id("CHK"),
                    "text": text,
                    "token_count": count_tokens(text),
                    "source_spans": source_spans,
                    "metadata": metadata,
                }
            )
    return chunks
