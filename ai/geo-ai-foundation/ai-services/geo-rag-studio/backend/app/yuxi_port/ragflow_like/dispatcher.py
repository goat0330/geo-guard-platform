"""Dispatch supported Yuxi chunk presets without importing optional OCR/ML deps."""

from .parsers import book, general, laws, qa, semantic, separator


def chunk_markdown(markdown: str, filename: str, preset: str, config: dict, embed_fn=None) -> tuple[list[str], bool]:
    if preset == "qa":
        return qa.chunk_markdown(filename, markdown, config), False
    if preset == "book":
        return book.chunk_markdown(markdown, config), False
    if preset == "laws":
        return laws.chunk_markdown(filename, markdown, config), False
    if preset == "separator":
        return separator.chunk_markdown(markdown, config), False
    if preset == "semantic":
        return semantic.chunk_markdown(markdown, config, embed_fn=embed_fn)
    return general.chunk_markdown(markdown, config), False
