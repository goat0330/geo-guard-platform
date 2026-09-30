from dataclasses import dataclass

@dataclass
class ChunkConfig:
    max_chars: int = 1200
    overlap_chars: int = 160

def chunk_text(text: str, config: ChunkConfig | None = None) -> list[dict]:
    config = config or ChunkConfig()
    clean = "\n".join(line.rstrip() for line in text.splitlines()).strip()
    if not clean:
        return []

    chunks = []
    start = 0
    n = len(clean)
    while start < n:
        end = min(start + config.max_chars, n)
        if end < n:
            boundary = clean.rfind("\n\n", start + config.max_chars // 2, end)
            if boundary > start:
                end = boundary
        piece = clean[start:end].strip()
        if piece:
            chunks.append({
                "text": piece,
                "source": {"char_start": start, "char_end": end}
            })
        if end >= n:
            break
        start = max(end - config.overlap_chars, start + 1)
    return chunks
