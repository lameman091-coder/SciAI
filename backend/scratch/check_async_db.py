import asyncio
from sqlalchemy.ext.asyncio import create_async_engine
from sqlalchemy import text
import os

# Use the same URL as the app
DATABASE_URL = "sqlite+aiosqlite:///./sciai.db"
engine = create_async_engine(DATABASE_URL)

async def check():
    async with engine.connect() as conn:
        print("Checking tables...")
        res = await conn.execute(text("SELECT name FROM sqlite_master WHERE type='table';"))
        tables = res.fetchall()
        print("Tables:", [t[0] for t in tables])
        
        if "saved_articles" in [t[0] for t in tables]:
            print("\nColumns in saved_articles:")
            res = await conn.execute(text("PRAGMA table_info(saved_articles);"))
            columns = res.fetchall()
            for col in columns:
                print(f" - {col[1]} ({col[2]})")
        else:
            print("\nTable saved_articles NOT FOUND!")

    await engine.dispose()

if __name__ == "__main__":
    asyncio.run(check())
