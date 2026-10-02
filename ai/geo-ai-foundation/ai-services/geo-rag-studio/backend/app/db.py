import json
import sqlite3
import uuid
from contextlib import contextmanager
from datetime import datetime, timezone

from .config import settings

SCHEMA = """
PRAGMA foreign_keys=ON;
CREATE TABLE IF NOT EXISTS documents (
  id TEXT PRIMARY KEY,
  file_name TEXT NOT NULL,
  file_path TEXT,
  mime_type TEXT,
  status TEXT NOT NULL,
  parser TEXT,
  chunk_preset_id TEXT,
  chunk_parser_config_json TEXT NOT NULL DEFAULT '{}',
  metadata_json TEXT NOT NULL DEFAULT '{}',
  error_message TEXT,
  knowledge_base_id TEXT,
  folder_id TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT
);
CREATE TABLE IF NOT EXISTS knowledge_bases (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL UNIQUE,
  description TEXT NOT NULL DEFAULT '',
  kb_type TEXT NOT NULL DEFAULT 'local',
  config_json TEXT NOT NULL DEFAULT '{}',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS folders (
  id TEXT PRIMARY KEY,
  knowledge_base_id TEXT NOT NULL,
  name TEXT NOT NULL,
  parent_id TEXT,
  created_at TEXT NOT NULL,
  UNIQUE(knowledge_base_id, parent_id, name)
);
CREATE TABLE IF NOT EXISTS knowledge_views (
  id TEXT PRIMARY KEY,
  knowledge_base_id TEXT NOT NULL,
  view_type TEXT NOT NULL,
  source_id TEXT NOT NULL DEFAULT '',
  payload_json TEXT NOT NULL,
  created_at TEXT NOT NULL,
  UNIQUE(knowledge_base_id, view_type, source_id)
);
CREATE TABLE IF NOT EXISTS knowledge_graph_embeddings (
  knowledge_base_id TEXT NOT NULL,
  source_fingerprint TEXT NOT NULL,
  embedding_model TEXT NOT NULL,
  record_type TEXT NOT NULL,
  record_id TEXT NOT NULL,
  content TEXT NOT NULL,
  embedding_json TEXT NOT NULL,
  PRIMARY KEY(knowledge_base_id, embedding_model, record_type, record_id)
);
CREATE TABLE IF NOT EXISTS evaluation_datasets (
  id TEXT PRIMARY KEY,
  knowledge_base_id TEXT NOT NULL,
  name TEXT NOT NULL,
  cases_json TEXT NOT NULL,
  created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS evaluation_runs (
  id TEXT PRIMARY KEY,
  knowledge_base_id TEXT NOT NULL,
  dataset_id TEXT,
  result_json TEXT NOT NULL,
  created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS model_providers (
  provider_id TEXT PRIMARY KEY,
  payload_json TEXT NOT NULL,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS model_provider_meta (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS system_options (
  key TEXT PRIMARY KEY,
  value_json TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS sample_questions (
  id TEXT PRIMARY KEY,
  knowledge_base_id TEXT NOT NULL,
  question TEXT NOT NULL,
  created_at TEXT NOT NULL,
  UNIQUE(knowledge_base_id, question)
);
CREATE TABLE IF NOT EXISTS blocks (
  id TEXT PRIMARY KEY,
  document_id TEXT NOT NULL,
  ordinal INTEGER NOT NULL,
  page INTEGER,
  text TEXT NOT NULL,
  bbox_json TEXT,
  page_width REAL,
  page_height REAL,
  FOREIGN KEY(document_id) REFERENCES documents(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS chunks (
  id TEXT PRIMARY KEY,
  document_id TEXT NOT NULL,
  chunk_index INTEGER NOT NULL,
  text TEXT NOT NULL,
  token_count INTEGER NOT NULL,
  source_spans_json TEXT NOT NULL DEFAULT '[]',
  metadata_json TEXT NOT NULL DEFAULT '{}',
  embedding_json TEXT,
  embedding_model TEXT,
  FOREIGN KEY(document_id) REFERENCES documents(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_blocks_document ON blocks(document_id, ordinal);
CREATE INDEX IF NOT EXISTS idx_chunks_document ON chunks(document_id, chunk_index);
"""


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


@contextmanager
def connect():
    connection = sqlite3.connect(settings.rag_db_path)
    connection.row_factory = sqlite3.Row
    connection.execute("PRAGMA foreign_keys=ON")
    try:
        connection.executescript(SCHEMA)
        _migrate(connection)
        yield connection
        connection.commit()
    finally:
        connection.close()


def _migrate(connection):
    columns = {row["name"] for row in connection.execute("PRAGMA table_info(documents)")}
    if "knowledge_base_id" not in columns:
        connection.execute("ALTER TABLE documents ADD COLUMN knowledge_base_id TEXT")
    if "folder_id" not in columns:
        connection.execute("ALTER TABLE documents ADD COLUMN folder_id TEXT")
    if "error_message" not in columns:
        connection.execute("ALTER TABLE documents ADD COLUMN error_message TEXT")
    if "updated_at" not in columns:
        connection.execute("ALTER TABLE documents ADD COLUMN updated_at TEXT")
    existing = connection.execute(
        "SELECT id FROM knowledge_bases WHERE id='KB-LOCAL-DEFAULT'"
    ).fetchone()
    legacy_documents = connection.execute(
        "SELECT 1 FROM documents WHERE knowledge_base_id IS NULL LIMIT 1"
    ).fetchone()
    if legacy_documents:
        if not existing:
            timestamp = now_iso()
            connection.execute(
                "INSERT INTO knowledge_bases(id, name, description, kb_type, config_json, created_at, updated_at) "
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                (
                    "KB-LOCAL-DEFAULT",
                    "默认知识库",
                    "迁移自原有单库 RAG 资料",
                    "local",
                    json.dumps(default_knowledge_base_config(), ensure_ascii=False),
                    timestamp,
                    timestamp,
                ),
            )
        connection.execute(
            "UPDATE documents SET knowledge_base_id='KB-LOCAL-DEFAULT' WHERE knowledge_base_id IS NULL"
        )


def new_id(prefix: str) -> str:
    return f"{prefix}-{uuid.uuid4().hex[:12]}"


def default_knowledge_base_config() -> dict:
    return {
        "chunking": {
            "chunk_preset_id": "general",
            "chunk_token_num": 512,
            "overlapped_percent": 10,
            "delimiter": "\\n",
        },
        "parser": {"engine": "auto", "mineru_api_uri": "", "api_key": ""},
        "retrieval": {
            "search_mode": "vector",
            "recall_top_k": 50,
            "bm25_top_k": 50,
            "final_top_k": 10,
            "similarity_threshold": 0.0,
            "vector_weight": 0.7,
            "bm25_weight": 0.3,
            "bm25_drop_ratio_search": 0.0,
            "use_reranker": False,
            "reranker_model": "",
            "use_graph_retrieval": False,
            "graph_entity_top_k": 10,
            "graph_triple_top_k": 10,
            "graph_max_nodes": 10000,
            "graph_top_k": 20,
            "graph_weight": 1.0,
            "ppr_damping": 0.85,
        },
        "embedding": {"base_url": "", "model": "", "dimensions": None, "batch_size": 32},
        "reranker": {"base_url": "", "model": "", "protocol": "openai"},
        "share_config": {
            "version": 2,
            "read_scope": {"access_level": "global", "department_ids": [], "user_uids": []},
            "manage_scope": None,
        },
    }


def create_knowledge_base(name: str, description: str = "", kb_type: str = "local") -> dict:
    knowledge_base_id = new_id("KB")
    timestamp = now_iso()
    config = default_knowledge_base_config()
    with connect() as connection:
        connection.execute(
            "INSERT INTO knowledge_bases VALUES (?, ?, ?, ?, ?, ?, ?)",
            (
                knowledge_base_id,
                name,
                description,
                kb_type,
                json.dumps(config, ensure_ascii=False),
                timestamp,
                timestamp,
            ),
        )
    return get_knowledge_base(knowledge_base_id)


def get_knowledge_base(knowledge_base_id: str) -> dict | None:
    with connect() as connection:
        row = connection.execute(
            "SELECT * FROM knowledge_bases WHERE id=?", (knowledge_base_id,)
        ).fetchone()
    return _knowledge_base(row) if row else None


def list_knowledge_bases() -> list[dict]:
    with connect() as connection:
        rows = connection.execute(
            "SELECT kb.*, COUNT(d.id) AS document_count, COALESCE(SUM(c.chunk_count), 0) AS chunk_count, "
            "COALESCE(SUM(c.token_count), 0) AS token_count "
            "FROM knowledge_bases kb "
            "LEFT JOIN documents d ON d.knowledge_base_id=kb.id "
            "LEFT JOIN (SELECT document_id, COUNT(*) AS chunk_count, SUM(token_count) AS token_count FROM chunks GROUP BY document_id) c "
            "ON c.document_id=d.id GROUP BY kb.id ORDER BY kb.created_at DESC"
        ).fetchall()
    return [_knowledge_base(row) for row in rows]


def update_knowledge_base(knowledge_base_id: str, name: str, description: str, kb_type: str, config: dict) -> dict | None:
    with connect() as connection:
        connection.execute(
            "UPDATE knowledge_bases SET name=?, description=?, kb_type=?, config_json=?, updated_at=? WHERE id=?",
            (name, description, kb_type, json.dumps(config, ensure_ascii=False), now_iso(), knowledge_base_id),
        )
    return get_knowledge_base(knowledge_base_id)


def delete_knowledge_base(knowledge_base_id: str) -> bool:
    with connect() as connection:
        row = connection.execute(
            "SELECT COUNT(*) AS total FROM documents WHERE knowledge_base_id=?", (knowledge_base_id,)
        ).fetchone()
        if row["total"]:
            return False
        connection.execute("DELETE FROM knowledge_views WHERE knowledge_base_id=?", (knowledge_base_id,))
        connection.execute("DELETE FROM evaluation_runs WHERE knowledge_base_id=?", (knowledge_base_id,))
        connection.execute("DELETE FROM evaluation_datasets WHERE knowledge_base_id=?", (knowledge_base_id,))
        connection.execute("DELETE FROM sample_questions WHERE knowledge_base_id=?", (knowledge_base_id,))
        connection.execute("DELETE FROM folders WHERE knowledge_base_id=?", (knowledge_base_id,))
        cursor = connection.execute("DELETE FROM knowledge_bases WHERE id=?", (knowledge_base_id,))
    return cursor.rowcount > 0


def _knowledge_base(row):
    keys = row.keys()
    return {
        "id": row["id"],
        "name": row["name"],
        "description": row["description"],
        "kb_type": row["kb_type"],
        "config": json.loads(row["config_json"] or "{}"),
        "created_at": row["created_at"],
        "updated_at": row["updated_at"],
        "document_count": row["document_count"] if "document_count" in keys else 0,
        "chunk_count": row["chunk_count"] if "chunk_count" in keys else 0,
        "token_count": row["token_count"] if "token_count" in keys else 0,
    }


def list_folders(knowledge_base_id: str) -> list[dict]:
    with connect() as connection:
        rows = connection.execute(
            "SELECT * FROM folders WHERE knowledge_base_id=? ORDER BY name", (knowledge_base_id,)
        ).fetchall()
    return [dict(row) for row in rows]


def create_folder(knowledge_base_id: str, name: str, parent_id: str | None = None) -> dict:
    folder_id = new_id("FOLDER")
    with connect() as connection:
        existing = connection.execute(
            "SELECT 1 FROM folders WHERE knowledge_base_id=? AND parent_id IS ? AND name=?",
            (knowledge_base_id, parent_id, name),
        ).fetchone()
        if existing:
            raise sqlite3.IntegrityError("Folder already exists at this level")
        connection.execute(
            "INSERT INTO folders VALUES (?, ?, ?, ?, ?)",
            (folder_id, knowledge_base_id, name, parent_id, now_iso()),
        )
        row = connection.execute("SELECT * FROM folders WHERE id=?", (folder_id,)).fetchone()
    return dict(row)


def folder_exists(folder_id: str, knowledge_base_id: str) -> bool:
    with connect() as connection:
        return connection.execute(
            "SELECT 1 FROM folders WHERE id=? AND knowledge_base_id=?", (folder_id, knowledge_base_id)
    ).fetchone() is not None


def rename_folder(folder_id: str, knowledge_base_id: str, name: str) -> dict | None:
    with connect() as connection:
        row = connection.execute(
            "SELECT * FROM folders WHERE id=? AND knowledge_base_id=?", (folder_id, knowledge_base_id)
        ).fetchone()
        if not row:
            return None
        connection.execute("UPDATE folders SET name=? WHERE id=?", (name, folder_id))
        return dict(connection.execute("SELECT * FROM folders WHERE id=?", (folder_id,)).fetchone())


def move_document(document_id: str, knowledge_base_id: str, folder_id: str | None) -> bool:
    with connect() as connection:
        cursor = connection.execute(
            "UPDATE documents SET folder_id=?, updated_at=? WHERE id=? AND knowledge_base_id=?",
            (folder_id, now_iso(), document_id, knowledge_base_id),
        )
    return cursor.rowcount > 0


def move_folder(folder_id: str, knowledge_base_id: str, parent_id: str | None) -> dict:
    with connect() as connection:
        folders = {row["id"]: dict(row) for row in connection.execute(
            "SELECT * FROM folders WHERE knowledge_base_id=?", (knowledge_base_id,)
        ).fetchall()}
        if folder_id not in folders:
            raise ValueError("Folder not found")
        current = parent_id
        while current:
            if current == folder_id:
                raise ValueError("Cannot move a folder into itself or its own subfolder")
            if current not in folders:
                raise ValueError("Parent is not a folder in this knowledge base")
            current = folders[current].get("parent_id")
        if any(item["id"] != folder_id and item.get("parent_id") == parent_id
               and item["name"] == folders[folder_id]["name"] for item in folders.values()):
            raise ValueError("Folder already exists at this level")
        connection.execute("UPDATE folders SET parent_id=? WHERE id=? AND knowledge_base_id=?",
                           (parent_id, folder_id, knowledge_base_id))
        return {**folders[folder_id], "parent_id": parent_id}


def delete_folder(folder_id: str, knowledge_base_id: str) -> bool:
    with connect() as connection:
        row = connection.execute(
            "SELECT (SELECT COUNT(*) FROM folders WHERE parent_id=?) + "
            "(SELECT COUNT(*) FROM documents WHERE folder_id=?) AS children "
            "FROM folders WHERE id=? AND knowledge_base_id=?",
            (folder_id, folder_id, folder_id, knowledge_base_id),
        ).fetchone()
        if not row or row["children"]:
            return False
        cursor = connection.execute(
            "DELETE FROM folders WHERE id=? AND knowledge_base_id=?", (folder_id, knowledge_base_id)
        )
    return cursor.rowcount > 0


def save_knowledge_view(knowledge_base_id: str, view_type: str, source_id: str, payload: dict) -> dict:
    view_id = new_id("VIEW")
    timestamp = now_iso()
    with connect() as connection:
        connection.execute(
            "INSERT INTO knowledge_views(id, knowledge_base_id, view_type, source_id, payload_json, created_at) "
            "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT(knowledge_base_id, view_type, source_id) DO UPDATE SET "
            "id=excluded.id, payload_json=excluded.payload_json, created_at=excluded.created_at",
            (view_id, knowledge_base_id, view_type, source_id, json.dumps(payload, ensure_ascii=False), timestamp),
        )
        row = connection.execute(
            "SELECT * FROM knowledge_views WHERE knowledge_base_id=? AND view_type=? AND source_id=?",
            (knowledge_base_id, view_type, source_id),
        ).fetchone()
    return _knowledge_view(row)


def get_knowledge_view(knowledge_base_id: str, view_type: str, source_id: str = "") -> dict | None:
    with connect() as connection:
        row = connection.execute(
            "SELECT * FROM knowledge_views WHERE knowledge_base_id=? AND view_type=? AND source_id=?",
            (knowledge_base_id, view_type, source_id),
        ).fetchone()
    return _knowledge_view(row) if row else None


def replace_knowledge_graph_embeddings(
    knowledge_base_id: str, source_fingerprint: str, embedding_model: str, records: list[dict]
) -> int:
    with connect() as connection:
        connection.execute("DELETE FROM knowledge_graph_embeddings WHERE knowledge_base_id=?", (knowledge_base_id,))
        connection.executemany(
            "INSERT INTO knowledge_graph_embeddings(knowledge_base_id, source_fingerprint, embedding_model, "
            "record_type, record_id, content, embedding_json) VALUES (?, ?, ?, ?, ?, ?, ?)",
            [
                (
                    knowledge_base_id,
                    source_fingerprint,
                    embedding_model,
                    record["record_type"],
                    record["record_id"],
                    record["content"],
                    json.dumps(record["embedding"], ensure_ascii=False),
                )
                for record in records
            ],
        )
    return len(records)


def get_knowledge_graph_embeddings(
    knowledge_base_id: str, source_fingerprint: str, embedding_model: str
) -> list[dict]:
    with connect() as connection:
        rows = connection.execute(
            "SELECT record_type, record_id, content, embedding_json FROM knowledge_graph_embeddings "
            "WHERE knowledge_base_id=? AND source_fingerprint=? AND embedding_model=?",
            (knowledge_base_id, source_fingerprint, embedding_model),
        ).fetchall()
    return [
        {
            "record_type": row["record_type"],
            "record_id": row["record_id"],
            "content": row["content"],
            "embedding": json.loads(row["embedding_json"]),
        }
        for row in rows
    ]


def list_knowledge_views(knowledge_base_id: str, view_type: str) -> list[dict]:
    with connect() as connection:
        rows = connection.execute(
            "SELECT * FROM knowledge_views WHERE knowledge_base_id=? AND view_type=? ORDER BY created_at DESC",
            (knowledge_base_id, view_type),
        ).fetchall()
    return [_knowledge_view(row) for row in rows]


def delete_knowledge_view(knowledge_base_id: str, view_type: str, source_id: str = "") -> bool:
    with connect() as connection:
        cursor = connection.execute(
            "DELETE FROM knowledge_views WHERE knowledge_base_id=? AND view_type=? AND source_id=?",
            (knowledge_base_id, view_type, source_id),
        )
        vectors_deleted = 0
        if view_type == "graph" and not source_id:
            vectors_deleted = connection.execute(
                "DELETE FROM knowledge_graph_embeddings WHERE knowledge_base_id=?", (knowledge_base_id,)
            ).rowcount
    return cursor.rowcount > 0 or vectors_deleted > 0


def _knowledge_view(row):
    return {
        "id": row["id"],
        "knowledge_base_id": row["knowledge_base_id"],
        "view_type": row["view_type"],
        "source_id": row["source_id"],
        "payload": json.loads(row["payload_json"]),
        "created_at": row["created_at"],
    }


def create_evaluation_dataset(knowledge_base_id: str, name: str, cases: list[dict]) -> dict:
    dataset_id = new_id("DATASET")
    timestamp = now_iso()
    with connect() as connection:
        connection.execute(
            "INSERT INTO evaluation_datasets VALUES (?, ?, ?, ?, ?)",
            (dataset_id, knowledge_base_id, name, json.dumps(cases, ensure_ascii=False), timestamp),
        )
    return get_evaluation_dataset(knowledge_base_id, dataset_id)


def list_evaluation_datasets(knowledge_base_id: str) -> list[dict]:
    with connect() as connection:
        rows = connection.execute(
            "SELECT id, knowledge_base_id, name, created_at, cases_json FROM evaluation_datasets "
            "WHERE knowledge_base_id=? ORDER BY created_at DESC", (knowledge_base_id,)
        ).fetchall()
    return [
        {"id": row["id"], "knowledge_base_id": row["knowledge_base_id"], "name": row["name"],
         "case_count": len(json.loads(row["cases_json"])), "created_at": row["created_at"]}
        for row in rows
    ]


def get_evaluation_dataset(knowledge_base_id: str, dataset_id: str) -> dict | None:
    with connect() as connection:
        row = connection.execute(
            "SELECT * FROM evaluation_datasets WHERE knowledge_base_id=? AND id=?",
            (knowledge_base_id, dataset_id),
        ).fetchone()
    if not row:
        return None
    return {"id": row["id"], "knowledge_base_id": row["knowledge_base_id"], "name": row["name"],
            "cases": json.loads(row["cases_json"]), "created_at": row["created_at"]}


def delete_evaluation_dataset(dataset_id: str) -> bool:
    with connect() as connection:
        connection.execute("DELETE FROM evaluation_runs WHERE dataset_id=?", (dataset_id,))
        cursor = connection.execute("DELETE FROM evaluation_datasets WHERE id=?", (dataset_id,))
    return cursor.rowcount > 0


def create_evaluation_run(knowledge_base_id: str, dataset_id: str | None, result: dict) -> dict:
    run_id = new_id("RUN")
    timestamp = now_iso()
    with connect() as connection:
        connection.execute(
            "INSERT INTO evaluation_runs VALUES (?, ?, ?, ?, ?)",
            (run_id, knowledge_base_id, dataset_id, json.dumps(result, ensure_ascii=False), timestamp),
        )
    return {"id": run_id, "knowledge_base_id": knowledge_base_id, "dataset_id": dataset_id,
            "result": result, "created_at": timestamp}


def list_evaluation_runs(knowledge_base_id: str) -> list[dict]:
    with connect() as connection:
        rows = connection.execute(
            "SELECT id, knowledge_base_id, dataset_id, created_at FROM evaluation_runs "
            "WHERE knowledge_base_id=? ORDER BY created_at DESC", (knowledge_base_id,)
        ).fetchall()
    return [dict(row) for row in rows]


def get_evaluation_run(knowledge_base_id: str, run_id: str) -> dict | None:
    with connect() as connection:
        row = connection.execute(
            "SELECT * FROM evaluation_runs WHERE knowledge_base_id=? AND id=?", (knowledge_base_id, run_id)
        ).fetchone()
    if not row:
        return None
    return {"id": row["id"], "knowledge_base_id": row["knowledge_base_id"], "dataset_id": row["dataset_id"],
            "result": json.loads(row["result_json"]), "created_at": row["created_at"]}


def delete_evaluation_run(knowledge_base_id: str, run_id: str) -> bool:
    with connect() as connection:
        cursor = connection.execute(
            "DELETE FROM evaluation_runs WHERE knowledge_base_id=? AND id=?", (knowledge_base_id, run_id)
        )
    return cursor.rowcount > 0


def save_sample_questions(knowledge_base_id: str, questions: list[str]) -> None:
    with connect() as connection:
        connection.execute("DELETE FROM sample_questions WHERE knowledge_base_id=?", (knowledge_base_id,))
        connection.executemany(
            "INSERT OR IGNORE INTO sample_questions(id, knowledge_base_id, question, created_at) VALUES (?, ?, ?, ?)",
            [(new_id("Q"), knowledge_base_id, question, now_iso()) for question in questions],
        )


def list_sample_questions(knowledge_base_id: str) -> list[str]:
    with connect() as connection:
        rows = connection.execute(
            "SELECT question FROM sample_questions WHERE knowledge_base_id=? ORDER BY created_at", (knowledge_base_id,)
        ).fetchall()
    return [row["question"] for row in rows]


def ensure_folder_path(knowledge_base_id: str, relative_path: str | None, parent_id: str | None = None) -> str | None:
    current_parent = parent_id
    for name in [part for part in (relative_path or "").split("/") if part]:
        with connect() as connection:
            row = connection.execute(
                "SELECT id FROM folders WHERE knowledge_base_id=? AND parent_id IS ? AND name=?",
                (knowledge_base_id, current_parent, name),
            ).fetchone()
        if row:
            current_parent = row["id"]
            continue
        try:
            current_parent = create_folder(knowledge_base_id, name, current_parent)["id"]
        except sqlite3.IntegrityError:
            with connect() as connection:
                row = connection.execute(
                    "SELECT id FROM folders WHERE knowledge_base_id=? AND parent_id IS ? AND name=?",
                    (knowledge_base_id, current_parent, name),
                ).fetchone()
            if not row:
                raise
            current_parent = row["id"]
    return current_parent


def create_document(file_name, file_path, mime_type, parser, preset, parser_config, metadata, knowledge_base_id=None, folder_id=None):
    document_id = new_id("DOC")
    timestamp = now_iso()
    with connect() as connection:
        connection.execute(
            "INSERT INTO documents (id, file_name, file_path, mime_type, status, parser, chunk_preset_id, chunk_parser_config_json, metadata_json, knowledge_base_id, folder_id, created_at, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (
                document_id,
                file_name,
                file_path,
                mime_type,
                "uploaded",
                parser,
                preset,
                json.dumps(parser_config, ensure_ascii=False),
                json.dumps(metadata or {}, ensure_ascii=False),
                knowledge_base_id,
                folder_id,
                timestamp,
                timestamp,
            ),
        )
    return document_id


def update_document_status(document_id: str, status: str, error: str | None = None, parser: str | None = None) -> None:
    with connect() as connection:
        row = connection.execute("SELECT status, metadata_json FROM documents WHERE id=?", (document_id,)).fetchone()
        metadata = json.loads(row["metadata_json"] or "{}") if row else {}
        if error:
            metadata["indexing_error"] = error[:500]
            if row and row["status"] in {"uploaded", "parsing"}:
                metadata["failure_stage"] = "parse"
            elif row and row["status"] in {"parsed", "chunking", "embedding", "indexed"}:
                metadata["failure_stage"] = "index"
            connection.execute(
                "UPDATE documents SET status=?, error_message=?, metadata_json=?, parser=COALESCE(?, parser), updated_at=? WHERE id=?",
                (status, error[:500], json.dumps(metadata, ensure_ascii=False), parser, now_iso(), document_id),
            )
        else:
            metadata.pop("indexing_error", None)
            metadata.pop("failure_stage", None)
            connection.execute(
                "UPDATE documents SET status=?, error_message=NULL, metadata_json=?, parser=COALESCE(?, parser), updated_at=? WHERE id=?",
                (status, json.dumps(metadata, ensure_ascii=False), parser, now_iso(), document_id),
            )


def update_document_processing_params(document_id: str, params: dict) -> None:
    with connect() as connection:
        row = connection.execute("SELECT metadata_json FROM documents WHERE id=?", (document_id,)).fetchone()
        if not row:
            raise ValueError("Document not found")
        metadata = json.loads(row["metadata_json"] or "{}")
        metadata["processing_params"] = params
        connection.execute(
            "UPDATE documents SET chunk_preset_id=?, chunk_parser_config_json=?, metadata_json=?, updated_at=? WHERE id=?",
            (params["chunk_preset_id"], json.dumps(params["chunk_parser_config"], ensure_ascii=False),
             json.dumps(metadata, ensure_ascii=False), now_iso(), document_id),
        )


def replace_blocks(document_id: str, blocks: list[dict]) -> None:
    with connect() as connection:
        connection.execute("DELETE FROM blocks WHERE document_id=?", (document_id,))
        connection.executemany(
            "INSERT INTO blocks VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            [
                (
                    block["id"],
                    document_id,
                    index,
                    block.get("page"),
                    block["text"],
                    json.dumps(block.get("bbox")),
                    block.get("page_width"),
                    block.get("page_height"),
                )
                for index, block in enumerate(blocks)
            ],
        )


def replace_chunks(document_id: str, chunks: list[dict]) -> None:
    with connect() as connection:
        connection.execute("DELETE FROM chunks WHERE document_id=?", (document_id,))
        connection.executemany(
            "INSERT INTO chunks VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            [
                (
                    chunk["id"],
                    document_id,
                    index,
                    chunk["text"],
                    chunk["token_count"],
                    json.dumps(chunk.get("source_spans", []), ensure_ascii=False),
                    json.dumps(chunk.get("metadata", {}), ensure_ascii=False),
                    None,
                    None,
                )
                for index, chunk in enumerate(chunks)
            ],
        )


def update_embedding(chunk_id: str, embedding: list[float], model: str) -> None:
    with connect() as connection:
        connection.execute(
            "UPDATE chunks SET embedding_json=?, embedding_model=? WHERE id=?",
            (json.dumps(embedding), model, chunk_id),
        )


def get_document(document_id: str):
    with connect() as connection:
        row = connection.execute("SELECT * FROM documents WHERE id=?", (document_id,)).fetchone()
    return _document(row) if row else None


def list_documents(knowledge_base_id: str | None = None):
    with connect() as connection:
        if knowledge_base_id:
            rows = connection.execute("SELECT * FROM documents WHERE knowledge_base_id=? ORDER BY created_at DESC", (knowledge_base_id,)).fetchall()
        else:
            rows = connection.execute("SELECT * FROM documents ORDER BY created_at DESC").fetchall()
    documents = []
    for row in rows:
        document = _document(row)
        with connect() as connection:
            counts = connection.execute(
                "SELECT COUNT(*) AS total, COALESCE(SUM(token_count), 0) AS token_count, "
                "SUM(CASE WHEN embedding_json IS NOT NULL THEN 1 ELSE 0 END) AS embedded FROM chunks WHERE document_id=?",
                (row["id"],),
            ).fetchone()
        document["chunk_count"] = counts["total"] or 0
        document["token_count"] = counts["token_count"] or 0
        document["embedding_chunk_count"] = counts["embedded"] or 0
        documents.append(document)
    return documents


def search_documents(knowledge_base_id: str, query: str, offset: int, limit: int):
    # Yuxi KnowledgeFileRepository.search_files: literal filename match, then pagination.
    escaped_query = query.lower().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    filters = "knowledge_base_id=? AND lower(file_name) LIKE ? ESCAPE '\\'"
    params = (knowledge_base_id, f"%{escaped_query}%")
    with connect() as connection:
        total = connection.execute(f"SELECT COUNT(*) FROM documents WHERE {filters}", params).fetchone()[0]
        rows = connection.execute(
            f"SELECT * FROM documents WHERE {filters} ORDER BY COALESCE(updated_at, created_at) DESC, id ASC LIMIT ? OFFSET ?",
            (*params, limit, offset),
        ).fetchall()
    return [_document(row) for row in rows], total


def get_blocks(document_id: str):
    with connect() as connection:
        rows = connection.execute(
            "SELECT * FROM blocks WHERE document_id=? ORDER BY ordinal", (document_id,)
        ).fetchall()
    return [_block(row) for row in rows]


def get_chunks(document_id: str | None = None, include_embedding: bool = True):
    sql = "SELECT c.*, d.file_name, d.file_path, d.mime_type, d.chunk_preset_id, d.status, d.knowledge_base_id FROM chunks c JOIN documents d ON d.id=c.document_id"
    params = []
    if document_id:
        sql += " WHERE c.document_id=?"
        params.append(document_id)
    sql += " ORDER BY d.created_at DESC, c.chunk_index"
    with connect() as connection:
        rows = connection.execute(sql, params).fetchall()
    return [_chunk(row, include_embedding) for row in rows]


def get_chunk(chunk_id: str):
    with connect() as connection:
        row = connection.execute(
            "SELECT c.*, d.file_name, d.file_path, d.mime_type, d.chunk_preset_id, d.status, d.knowledge_base_id FROM chunks c JOIN documents d ON d.id=c.document_id WHERE c.id=?",
            (chunk_id,),
        ).fetchone()
    return _chunk(row) if row else None


def _document(row):
    return {
        "id": row["id"],
        "file_name": row["file_name"],
        "file_path": row["file_path"],
        "mime_type": row["mime_type"],
        "status": row["status"],
        "parser": row["parser"],
        "chunk_preset_id": row["chunk_preset_id"],
        "chunk_parser_config": json.loads(row["chunk_parser_config_json"] or "{}"),
        "metadata": json.loads(row["metadata_json"] or "{}"),
        "error_message": row["error_message"],
        "knowledge_base_id": row["knowledge_base_id"],
        "folder_id": row["folder_id"],
        "created_at": row["created_at"],
        "updated_at": row["updated_at"],
    }


def delete_document(document_id: str) -> dict | None:
    document = get_document(document_id)
    if not document:
        return None
    with connect() as connection:
        connection.execute("DELETE FROM documents WHERE id=?", (document_id,))
    return document


def _block(row):
    return {
        "id": row["id"],
        "document_id": row["document_id"],
        "ordinal": row["ordinal"],
        "page": row["page"],
        "text": row["text"],
        "bbox": json.loads(row["bbox_json"]) if row["bbox_json"] else None,
        "page_width": row["page_width"],
        "page_height": row["page_height"],
    }


def _chunk(row, include_embedding=True):
    keys = row.keys()
    embedding = json.loads(row["embedding_json"]) if row["embedding_json"] else None
    return {
        "id": row["id"],
        "document_id": row["document_id"],
        "chunk_index": row["chunk_index"],
        "text": row["text"],
        "token_count": row["token_count"],
        "source_spans": json.loads(row["source_spans_json"] or "[]"),
        "metadata": json.loads(row["metadata_json"] or "{}"),
        "embedding": embedding if include_embedding else None,
        "embedding_dim": len(embedding) if embedding else 0,
        "embedding_model": row["embedding_model"],
        "file_name": row["file_name"] if "file_name" in keys else None,
        "file_path": row["file_path"] if "file_path" in keys else None,
        "mime_type": row["mime_type"] if "mime_type" in keys else None,
        "chunk_preset_id": row["chunk_preset_id"] if "chunk_preset_id" in keys else None,
        "document_status": row["status"] if "status" in keys else None,
        "knowledge_base_id": row["knowledge_base_id"] if "knowledge_base_id" in keys else None,
    }


def get_system_option(key: str, default=None):
    with connect() as connection:
        row = connection.execute("SELECT value_json FROM system_options WHERE key=?", (key,)).fetchone()
    return json.loads(row["value_json"]) if row else default


def set_system_option(key: str, value) -> None:
    with connect() as connection:
        connection.execute(
            "INSERT INTO system_options(key,value_json,updated_at) VALUES(?,?,?) "
            "ON CONFLICT(key) DO UPDATE SET value_json=excluded.value_json,updated_at=excluded.updated_at",
            (key, json.dumps(value, ensure_ascii=False), now_iso()),
        )
