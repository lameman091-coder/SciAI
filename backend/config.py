from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import Optional
from pathlib import Path
import os

# Resolve absolute base directory (the 'backend' folder)
BASE_DIR = Path(__file__).resolve().parent
_ENV_FILE = str(BASE_DIR / ".env")

class Settings(BaseSettings):
    # ── Redis Cache ──
    REDIS_HOST: str = "127.0.0.1"
    REDIS_PORT: int = 6379
    REDIS_DB: int = 0
    REDIS_PASSWORD: Optional[str] = None
    
    # ── Rate Limiting ──
    RATE_LIMIT_PER_MINUTE: int = 30
    
    # ── Sentry Monitoring ──
    SENTRY_DSN: Optional[str] = None
    
    # ── TIER 1: Groq (Speed) — 5 keys ──
    GROQ_API_KEY: str = ""
    GROQ_API_KEY_2: str = ""
    GROQ_API_KEY_3: str = ""
    GROQ_API_KEY_4: str = ""
    GROQ_API_KEY_5: str = ""
    GROQ_API_KEY_6: str = ""
    GROQ_API_KEY_7: str = ""
    GROQ_API_KEY_8: str = ""
    GROQ_API_KEY_9: str = ""
    GROQ_API_KEY_10: str = ""
    
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
    OPENROUTER_API_KEY_11: str = ""
    OPENROUTER_API_KEY_12: str = ""
    OPENROUTER_API_KEY_13: str = ""
    OPENROUTER_API_KEY_14: str = ""
    OPENROUTER_API_KEY_15: str = ""
    
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
    
    # ── TTS Configuration ──
    HF_TTS_API_KEY: str = ""           # Separate HF key for TTS fallback
    TTS_CACHE_DIR: str = str(BASE_DIR / "cache" / "tts")
    TTS_DEFAULT_VOICE: str = "professor"
    TTS_MAX_TEXT_LENGTH: int = 15000
    TTS_CHUNK_SIZE: int = 200           # Max chars per TTS chunk
    
    # Database
    DATABASE_URL: str = f"sqlite+aiosqlite:///{BASE_DIR}/sciai.db"
    
    # Vector Store
    FAISS_INDEX_PATH: str = str(BASE_DIR / "faiss_index.bin")
    CHUNK_STORE_PATH: str = str(BASE_DIR / "faiss_chunks.json")
    
    # Cache & Temp
    CACHE_DIR: str = str(BASE_DIR / "cache")
    TEMP_DIR: str = str(BASE_DIR / "temp")
    
    # App Settings
    MAX_UPLOAD_SIZE_MB: int = 100
    DEBUG: bool = False
    
    model_config = SettingsConfigDict(env_file=_ENV_FILE, extra="ignore")

settings = Settings()

# Ensure directories exist
os.makedirs(settings.TEMP_DIR, exist_ok=True)
os.makedirs(settings.CACHE_DIR, exist_ok=True)
os.makedirs(settings.TTS_CACHE_DIR, exist_ok=True)
