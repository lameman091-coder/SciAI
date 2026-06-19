from celery import Celery
from config import settings
import loguru
from sqlalchemy import select
from database import AsyncSessionLocal, Book

logger = loguru.logger

celery_app = Celery(
    "sciai",
    broker=f"redis://{settings.REDIS_HOST if hasattr(settings, 'REDIS_HOST') else 'localhost'}:{settings.REDIS_PORT if hasattr(settings, 'REDIS_PORT') else 6379}/1",
    backend=f"redis://{settings.REDIS_HOST if hasattr(settings, 'REDIS_HOST') else 'localhost'}:{settings.REDIS_PORT if hasattr(settings, 'REDIS_PORT') else 6379}/2",
    include=["celery_config"]
)

celery_app.conf.update(
    task_serializer="json",
    accept_content=["json"],
    result_serializer="json",
    timezone="UTC",
    enable_utc=True,
    task_track_started=True,
    task_time_limit=600,
    worker_prefetch_multiplier=1,
    worker_max_tasks_per_child=50
)

@celery_app.task(name="process_pdf_async", bind=True)
def process_pdf_async(self, book_id: str, domain: str, source_type: str, user_id: str):
    from pdf_processor import process_pdf
    from faiss_store import add_document
    import asyncio
    import tempfile
    import os
    
    logger.info(f"Celery: Starting PDF processing for {book_id}")
    
    try:
        import os
        temp_path = os.path.join(settings.TEMP_DIR, f"{book_id}.pdf")
        
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        
        chunks = loop.run_until_complete(process_pdf(temp_path))
        
        if chunks:
            import asyncio
            loop.run_until_complete(
                asyncio.to_thread(add_document, chunks, domain=domain, source_type=source_type, book_id=book_id, user_id=user_id)
            )
            logger.info(f"Celery: Indexed {len(chunks)} chunks for {book_id}")
            
            # Update Database status from "Processing..." to preview
            async def update_db():
                async with AsyncSessionLocal() as db:
                    res = await db.execute(select(Book).where(Book.id == book_id))
                    book = res.scalar_one_or_none()
                    if book:
                        preview_text = chunks[0][:250] + "..." if len(chunks[0]) > 250 else chunks[0]
                        book.preview = preview_text
                        await db.commit()
                        logger.info(f"Celery: Updated database preview for {book_id}")
            
            loop.run_until_complete(update_db())
        else:
            # Handle empty chunks
            async def update_db_error():
                async with AsyncSessionLocal() as db:
                    res = await db.execute(select(Book).where(Book.id == book_id))
                    book = res.scalar_one_or_none()
                    if book:
                        book.preview = f"Success: Indexing complete (No preview available)."
                        await db.commit()
            loop.run_until_complete(update_db_error())

        return {"status": "success", "chunks": len(chunks) if chunks else 0}
        
    except Exception as e:
        logger.error(f"Celery: PDF processing failed for {book_id}: {e}")
        self.update_state(state="FAILURE", meta={"error": str(e)})
        raise

@celery_app.task(name="cleanup_old_cache")
def cleanup_old_cache():
    import asyncio
    from cache_service import cache_service
    
    logger.info("Celery: Running cache cleanup")
    
    async def _cleanup():
        await cache_service.clear_pattern("ask:")
        await cache_service.clear_pattern("articles:")
    
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    loop.run_until_complete(_cleanup())
    
    return {"status": "cleaned"}

@celery_app.task(name="sync_external_data")
def sync_external_data():
    import asyncio
    import live_articles
    
    logger.info("Celery: Syncing external articles")
    
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    
    topics = ["AI", "Machine Learning", "Quantum Computing", "Biotechnology"]
    results = []
    
    for topic in topics:
        try:
            result = loop.run_until_complete(live_articles.search_articles(topic, limit=10))
            results.append({"topic": topic, "count": len(result.get("articles", []))})
        except Exception as e:
            logger.error(f"Celery: Failed to sync {topic}: {e}")
    
    return {"status": "synced", "results": results}