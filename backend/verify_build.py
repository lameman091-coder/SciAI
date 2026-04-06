import sys
import os

def verify():
    print("--- SciAI Backend Verification ---")
    
    try:
        print("1. Checking Imports...")
        import fastapi
        import sqlalchemy
        import aiosqlite
        import httpx
        import loguru
        import tiktoken
        import diskcache
        import groq
        import faiss
        import numpy as np
        print("   [OK] All dependencies found.")
        
        print("2. Checking App Configuration...")
        from config import settings
        print(f"   [OK] Settings loaded (DB: {settings.DATABASE_URL})")
        
        print("3. Checking Database Connectivity...")
        import asyncio
        from database import init_db, engine
        
        async def check_db():
            await init_db()
            print("   [OK] Database initialized and tables created.")
            
        asyncio.run(check_db())
        
        print("4. Checking Vector Store...")
        from faiss_store import store
        if store.index is None:
            print("   [OK] Vector store initialized (Empty index).")
        else:
            print(f"   [OK] Vector store loaded with {store.index.ntotal} vectors.")
            
        print("5. Checking LLM Engine...")
        from llm_engine import get_groq_client
        client = get_groq_client()
        if client:
            print("   [OK] Groq client initialized.")
        else:
            print("   [WARN] Groq client NOT initialized (API Key missing).")
            
        print("\n[SUCCESS] Backend build verified. Ready for deployment!")
        return True
        
    except Exception as e:
        print(f"\n[FAILED] Verification error: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    success = verify()
    sys.exit(0 if success else 1)
