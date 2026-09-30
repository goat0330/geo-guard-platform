import json
import sqlite3
import uuid
from contextlib import contextmanager
from .config import settings

SCHEMA = """
CREATE TABLE IF NOT EXISTS documents (
  id TEXT PRIMARY KEY,
  file_name TEXT NOT NULL,
  metadata_json TEXT NOT NULL DEFAULT '{}',
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS chunks (
  id TEXT PRIMARY KEY,
  document_id TEXT NOT NULL,
  ordinal INTEGER NOT NULL,
  text TEXT NOT NULL,
  page INTEGER,
  section TEXT,
  source_json TEXT NOT NULL DEFAULT '{}',
  embedding_json TEXT,
  FOREIGN KEY(document_id) REFERENCES documents(id)
);
CREATE INDEX IF NOT EXISTS idx_chunks_document_id ON chunks(document_id);
"""

@contextmanager
def connect():
    con = sqlite3.connect(settings.rag_db_path)
    con.row_factory = sqlite3.Row
    try:
        con.executescript(SCHEMA)
        yield con
        con.commit()
    finally:
        con.close()

def create_document(file_name: str, metadata: dict) -> str:
    doc_id = f"DOC-{uuid.uuid4().hex[:12]}"
    with connect() as con:
        con.execute(
            "INSERT INTO documents(id, file_name, metadata_json) VALUES (?, ?, ?)",
            (doc_id, file_name, json.dumps(metadata, ensure_ascii=False)),
        )
    return doc_id

def insert_chunks(document_id: str, chunks: list[dict]) -> list[str]:
    ids = []
    with connect() as con:
        for i, chunk in enumerate(chunks):
            chunk_id = f"CHK-{uuid.uuid4().hex[:12]}"
            ids.append(chunk_id)
            con.execute(
                "INSERT INTO chunks(id, document_id, ordinal, text, page, section, source_json, embedding_json) "
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                (
                    chunk_id,
                    document_id,
                    i,
                    chunk["text"],
                    chunk.get("page"),
                    chunk.get("section"),
                    json.dumps(chunk.get("source", {}), ensure_ascii=False),
                    json.dumps(chunk.get("embedding")) if chunk.get("embedding") is not None else None,
                ),
            )
    return ids

def update_embedding(chunk_id: str, embedding: list[float]):
    with connect() as con:
        con.execute(
            "UPDATE chunks SET embedding_json=? WHERE id=?",
            (json.dumps(embedding), chunk_id),
        )

def all_chunks() -> list[dict]:
    sql = (
        "SELECT c.*, d.file_name, d.metadata_json "
        "FROM chunks c JOIN documents d ON d.id=c.document_id "
        "ORDER BY d.created_at, c.ordinal"
    )
    with connect() as con:
        rows = con.execute(sql).fetchall()

    out = []
    for row in rows:
        out.append({
            "id": row["id"],
            "document_id": row["document_id"],
            "ordinal": row["ordinal"],
            "text": row["text"],
            "page": row["page"],
            "section": row["section"],
            "source": json.loads(row["source_json"] or "{}"),
            "embedding": json.loads(row["embedding_json"]) if row["embedding_json"] else None,
            "file_name": row["file_name"],
            "metadata": json.loads(row["metadata_json"] or "{}"),
        })
    return out

def list_documents() -> list[dict]:
    with connect() as con:
        rows = con.execute(
            "SELECT d.id, d.file_name, d.metadata_json, d.created_at, COUNT(c.id) AS chunk_count "
            "FROM documents d LEFT JOIN chunks c ON c.document_id=d.id "
            "GROUP BY d.id ORDER BY d.created_at DESC"
        ).fetchall()
    return [
        {
            "document_id": row["id"],
            "file_name": row["file_name"],
            "metadata": json.loads(row["metadata_json"] or "{}"),
            "created_at": row["created_at"],
            "chunk_count": row["chunk_count"],
        }
        for row in rows
    ]

def document_chunks(document_id: str, limit: int = 100) -> list[dict] | None:
    with connect() as con:
        document = con.execute("SELECT id FROM documents WHERE id=?", (document_id,)).fetchone()
        if not document:
            return None
        rows = con.execute(
            "SELECT id, ordinal, text, page, section, source_json FROM chunks "
            "WHERE document_id=? ORDER BY ordinal LIMIT ?",
            (document_id, limit),
        ).fetchall()
    return [
        {
            "chunk_id": row["id"],
            "ordinal": row["ordinal"],
            "text": row["text"],
            "page": row["page"],
            "section": row["section"],
            "source": json.loads(row["source_json"] or "{}"),
        }
        for row in rows
    ]
