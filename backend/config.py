from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import Optional
import os

class Settings(BaseSettings):
    # API Keys
    GROQ_API_KEY: str = ""
    
    # Database
    DATABASE_URL: str = "sqlite+aiosqlite:///./sciai.db"
    
    # Vector Store
    FAISS_INDEX_PATH: str = "faiss_index.bin"
    CHUNK_STORE_PATH: str = "faiss_chunks.json"
    
    # Cache
    CACHE_DIR: str = "./cache"
    
    # App Settings
    MAX_UPLOAD_SIZE_MB: int = 100
    DEBUG: bool = False
    
    # Hugging Face Settings
    USE_HF: bool = False
    HF_API_KEY: str = ""
    
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

settings = Settings()

# Ensure directories exist
os.makedirs("temp", exist_ok=True)
os.makedirs(settings.CACHE_DIR, exist_ok=True)
