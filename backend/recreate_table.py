import asyncio
import os
from sqlalchemy.ext.asyncio import create_async_engine
from sqlalchemy import text
import sys

# Add backend to path so we can import database
sys.path.append(os.getcwd())

from database import Base

async def recreate(path):
    if not os.path.exists(path):
        print(f"Skipping {path} - file not found.")
        return
        
    print(f"Recreating table in {path}...")
    # SQLite URL construction
    db_url = f"sqlite+aiosqlite:///{path}"
    temp_engine = create_async_engine(db_url)
    
    try:
        async with temp_engine.begin() as conn:
            print(f" - Dropping old saved_articles table")
            await conn.execute(text("DROP TABLE IF EXISTS saved_articles;"))
            print(f" - Recreating all tables from models")
            await conn.run_sync(Base.metadata.create_all)
        print(f"Done for {path}.")
    except Exception as e:
        print(f"Error recreating {path}: {e}")
    finally:
        await temp_engine.dispose()

async def main():
    # Fix both potential database locations
    # Paths depend on where this prompt runs from
    cwd = os.getcwd()
    print(f"Current Directory: {cwd}")
    
    # We'll try common variations
    possible_paths = [
        "sciai.db",
        "backend/sciai.db",
        "../sciai.db"
    ]
    
    for p in possible_paths:
        await recreate(p)

if __name__ == "__main__":
    asyncio.run(main())
