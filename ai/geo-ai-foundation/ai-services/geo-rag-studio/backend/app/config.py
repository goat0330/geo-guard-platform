from pathlib import Path

from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    rag_db_path: str = "../data/rag-v2.sqlite3"
    rag_upload_dir: str = "../data/documents-v2"
    embedding_base_url: str = ""
    embedding_api_key: str = ""
    embedding_model: str = ""
    embedding_dimensions: int | None = None
    embedding_batch_size: int = 32
    rerank_base_url: str = ""
    rerank_api_key: str = ""
    rerank_model: str = ""
    rerank_protocol: str = "openai"
    mineru_enabled: bool = False
    mineru_api_uri: str = ""
    mineru_api_key: str = ""
    mineru_timeout_seconds: int = 1800
    default_final_top_k: int = 8
    default_recall_top_k: int = 50
    vector_weight: float = 0.7
    bm25_weight: float = 0.3
    cors_origins: str = "http://127.0.0.1:5175"

    @field_validator("embedding_dimensions", mode="before")
    @classmethod
    def blank_dimensions_are_unset(cls, value):
        return None if value == "" else value

    @property
    def cors_origin_list(self) -> list[str]:
        return [origin.strip() for origin in self.cors_origins.split(",") if origin.strip()]

    def ensure_dirs(self) -> None:
        Path(self.rag_db_path).parent.mkdir(parents=True, exist_ok=True)
        Path(self.rag_upload_dir).mkdir(parents=True, exist_ok=True)


settings = Settings()
settings.ensure_dirs()
