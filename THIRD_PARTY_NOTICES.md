# Third-party notices

## Yuxi

The native Vue application in `frontend/yuxi-web` is copied from Yuxi v0.7.3 commit `caff3208c9db9128aa0b277bc5c669141383ddbd` under the MIT License. A SHA-256 comparison found 374 of 384 upstream-tracked files identical; 10 files adapt host authentication, API routing, or the embedded view. Its license text is `frontend/yuxi-web/LICENSE.YUXI`.

The MinerU official batch API adapter in the local RAG service follows the public integration flow used by Yuxi's open-source parser. It is implemented against the local SQLite/file pipeline and does not import Yuxi's backend runtime or Docker deployment.

## RuoYi-Vue-Plus

The backend framework sources in `backend/geo-guard-backend/framework` include RuoYi-Vue-Plus 5.x code under the MIT License. The required license text is included at `backend/geo-guard-backend/framework/LICENSE.RUOYI-VUE-PLUS`.

## Yuxi RAGFlow-style chunk parsers

The RAG Studio vendors Yuxi v0.7.3 source under `ai/geo-ai-foundation/ai-services/geo-rag-studio/backend/vendor/yuxi/package/yuxi`; see `vendor/yuxi/UPSTREAM.md` for the exact file comparison and three local patches. The original Yuxi MIT license is included at `ai/geo-ai-foundation/ai-services/geo-rag-studio/backend/vendor/yuxi/LICENSE`. GeoGuard stores knowledge-base metadata and original files in SQLite/local storage and can index/search through its own Milvus 2.5.6 service. The Milvus schema uses the Yuxi Chinese analyzer, BM25 Function and COSINE vector index; the hybrid branch uses Milvus `WeightedRanker`. A real PDF BM25-only run is verified. Vector and weighted hybrid scores still need a configured embedding provider before their real-model behavior can be accepted. SQLite remains an optional fallback, not a claim of Milvus score equivalence.

The score activation formulas in `ai/geo-ai-foundation/ai-services/geo-rag-studio/backend/app/yuxi_port/milvus_ranker.py` follow the Milvus 2.5.6 implementation used by Yuxi's local Compose stack. See [Milvus reScorer.go](https://github.com/milvus-io/milvus/blob/v2.5.6/internal/proxy/reScorer.go). The local implementation is a small formula-level port; no Milvus server source is vendored.

The local Milvus Compose stack uses the upstream `chrislusf/seaweedfs:4.41` image as its S3-compatible object store. SeaweedFS is not vendored; its upstream license is Apache 2.0: https://github.com/seaweedfs/seaweedfs/blob/master/LICENSE.

## Offline Maven build asset

The `geo-guard-offline-build-kit.zip` GitHub Release asset includes Eclipse Temurin JDK 21.0.12.1+1 for Windows x64, the unmodified Apache Maven 3.9.16 distribution, and public dependency artifacts from the isolated Maven repository used by the offline build. Temurin is distributed under GPLv2 with the Classpath Exception; the JDK's original per-component license files are preserved under `jdk/legal/` and the distribution notice is `jdk/NOTICE`. See the [Adoptium licensing FAQ](https://adoptium.net/docs/faq). Maven's original `LICENSE` and `NOTICE` files and dependency JAR/POM files remain in the archive, including upstream license/notice metadata where published. The asset does not include private Beiwuyuan artifacts or credentials.
