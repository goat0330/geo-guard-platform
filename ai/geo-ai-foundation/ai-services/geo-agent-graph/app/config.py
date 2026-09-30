from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")
    rag_base_url: str = "http://127.0.0.1:8010"
    llm_base_url: str = ""
    llm_api_key: str = ""
    llm_model: str = ""
    rag_top_k: int = 5

settings = Settings()
