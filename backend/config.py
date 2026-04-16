from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import Optional
from pathlib import Path
import os

# Resolve .env path relative to THIS file, not the working directory
_ENV_FILE = str(Path(__file__).resolve().parent / ".env")

class Settings(BaseSettings):
    # ── TIER 1: Groq (Speed) — 5 keys ──
    GROQ_API_KEY: str = ""
    GROQ_API_KEY_2: str = ""
    GROQ_API_KEY_3: str = ""
    GROQ_API_KEY_4: str = ""
    GROQ_API_KEY_5: str = ""
    
    # ── TIER 2/3: Gemini Flash + Pro — 5 keys ──
    GEMINI_API_KEY: str = ""
    GEMINI_API_KEY_2: str = ""
    GEMINI_API_KEY_3: str = ""
    GEMINI_API_KEY_4: str = ""
    GEMINI_API_KEY_5: str = ""
    GEMINI_API_KEY_6: str = ""
    GEMINI_API_KEY_7: str = ""
    GEMINI_API_KEY_8: str = ""
    GEMINI_API_KEY_9: str = ""
    GEMINI_API_KEY_10: str = ""
    
    # ── TIER 4/5: OpenRouter (Qwen + Hub) — 5 keys ──
    OPENROUTER_API_KEY: str = ""
    OPENROUTER_API_KEY_2: str = ""
    OPENROUTER_API_KEY_3: str = ""
    OPENROUTER_API_KEY_4: str = ""
    OPENROUTER_API_KEY_5: str = ""
    OPENROUTER_API_KEY_6: str = ""
    OPENROUTER_API_KEY_7: str = ""
    OPENROUTER_API_KEY_8: str = ""
    OPENROUTER_API_KEY_9: str = ""
    OPENROUTER_API_KEY_10: str = ""
    
    # ── TIER 6: Backup Providers ──
    MISTRAL_API_KEY: str = ""
    TOGETHER_API_KEY: str = ""
    NVIDIA_API_KEY: str = ""
    
    # ── TIER 7: Cohere (RAG) ──
    COHERE_API_KEY: str = ""
    
    # ── TIER 8: Cerebras (Ultra-fast) ──
    CEREBRAS_API_KEY: str = ""
    
    # ── TIER 9: Hugging Face (Last Resort) ──
    HF_API_KEY: str = ""
    USE_HF: bool = False
    
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
    
    model_config = SettingsConfigDict(env_file=_ENV_FILE, extra="ignore")

settings = Settings()

# Ensure directories exist
os.makedirs("temp", exist_ok=True)
os.makedirs(settings.CACHE_DIR, exist_ok=True)
