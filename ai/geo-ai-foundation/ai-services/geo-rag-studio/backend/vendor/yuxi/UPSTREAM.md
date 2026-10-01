# Vendored Yuxi source

Source: https://github.com/xerrors/Yuxi

Version: `v0.7.3`

Commit: `caff3208c9db9128aa0b277bc5c669141383ddbd`

The Python source under `package/yuxi/knowledge` (62 files), `package/yuxi/models` (10 files), and `package/yuxi/permissions` (2 files) is vendored from the local upstream checkout. A SHA-256 comparison against the recorded v0.7.3 commit found 59/62 knowledge files, 10/10 model files, and 2/2 permission files identical; three knowledge files have small local patches. Dify and Notion re-raise upstream request failures instead of returning an indistinguishable empty result, and knowledge implementation classes load lazily so using read-only connectors does not import the optional Milvus client. Geo RAG Studio actively calls the upstream Dify and Notion connectors, `OtherEmbedding`, chunk parsers, OCR parser classes, reranker wire protocols, provider templates, and retrieval evaluation metrics through local adapters. The external knowledge-base API also calls Yuxi's `KnowledgeBase.build_search_output`, `_build_open_file_window`, and `_build_find_file_windows` helpers directly; local SQLite metadata and parsed blocks supply the source data. The chunk adapter adds PDF source-location metadata and validates knowledge-base chunk settings. Local MinIO and Redis shims report those optional services unavailable; provider credentials remain in the SQLite registry.

Geo RAG's FastAPI routes in `app/yuxi_compat.py` are local adapters, not verbatim copies of Yuxi's `backend/server/routers` modules. A method-and-path comparison against Yuxi v0.7.3's knowledge, external knowledge, evaluation, graph, and knowledge-dashboard routers found all 77 operations represented. This verifies route surface only; it does not establish equivalent behavior for every operation.

To complete the vendored knowledge package's internal source dependencies, 25 supporting Python files were copied unchanged from the same Yuxi checkout: configuration, repositories, OCR/task services, MinIO/Postgres/Neo4j adapters, and utilities. SHA-256 hashes matched the upstream working tree after copying. The Geo-local MinIO import shim remains active in place of Yuxi's service-bound package initializer, while exposing the migrated MinIO client modules as optional submodules. The Yuxi Milvus knowledge-base class now imports with its declared client dependencies, but Geo RAG's active pipeline still uses SQLite/local files; its Milvus/Postgres/Redis code is not yet the active index or retrieval path.

Parser files are from the same v0.7.3 source commit. OCR integrations use the upstream RapidOCR, MinerU, PP-Structure, DeepSeek OCR, and PaddleOCR classes when their required local packages, endpoints, credentials, and (where relevant) image storage support are present. Missing providers are reported as unavailable; no parser result is synthesized.

License: MIT, copied from the upstream root `LICENSE` file in this directory.

The vendored Yuxi knowledge manager, repository/service integrations, and Milvus-backed `MilvusKB` are source-migrated but are not the active runtime path. GeoGuard's active BM25/vector retrieval runs against SQLite; Chinese analysis follows Yuxi's Jieba search-token analyzer, BM25 uses Milvus's default `k1=1.2` and `b=0.75`, and hybrid route-score activation and weighted fusion follow the Milvus 2.5.6 `WeightedRanker` used by Yuxi. SQLite computes corpus statistics locally rather than through Milvus's BM25 index, so identical candidate rankings are not claimed until comparison against the live Yuxi service. Yuxi's PostgreSQL/Redis/Milvus/MinIO runtime has not been reintroduced; full functional parity is therefore not claimed yet.
