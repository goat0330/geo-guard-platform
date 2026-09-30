# Third-party notices

## Yuxi RAGFlow-style chunk parsers

The files under `ai-services/geo-rag-studio/backend/app/yuxi_port/ragflow_like/`
are adapted from the Yuxi project's RAGFlow-style chunking implementation.
Copyright (c) 2025 Yuxi Project Contributors. The MIT license is included beside
the adapted files in `ragflow_like/LICENSE.txt`.

The local adapter preserves SQLite storage and the PDF source-span contract.
The General, QA, Book, Laws, Semantic, and Separator presets include the
Yuxi RAGFlow-style parsers and their Markdown/table utilities. The Semantic
preset's embedding callback uses the knowledge base's configured Embedding
Provider. Without a configured provider it records a fallback instead of
claiming semantic clustering.
