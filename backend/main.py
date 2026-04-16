from fastapi import FastAPI, HTTPException, UploadFile, File, Form, Depends, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, delete
from pydantic import BaseModel
import os
import uuid
import json

from config import settings
from logger import log
from database import init_db, get_db, User, Book, SavedArticle, AsyncSessionLocal
from pdf_processor import process_pdf
from faiss_store import add_document, search, remove_document, load_store, save_store
import llm_engine
import live_articles
from model_manager import manager as model_manager
import time
import asyncio

query_cache = {}
MAX_CACHE_SIZE = 200

app = FastAPI(title="SciAI Core Backend", version="3.1.0")

# CORS Setup
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.on_event("startup")
async def startup_event():
    print("Starting SciAI backend...")
    log.info("SciAI PRODUCTION ENGINE STARTING...")
    await init_db()
    # load_store() is now lazy-loaded on demand
    log.info(f"ModelManager: {len([p for p in model_manager.providers.values() if p.is_configured])} providers configured")

@app.on_event("shutdown")
async def shutdown_event():
    log.info("SciAI SHUTTING DOWN...")
    save_store()
    await model_manager.close()

# ── REQUEST MODELS ──
class AskRequest(BaseModel):
    question: str
    mode: str
    domain: str
    book_id: str = None
    hybrid: bool = False
    user_id: str = "guest"

class SavedArticleRequest(BaseModel):
    user_id: str
    id: str
    title: str
    summary: str
    source: str
    link: str
    score: float = 0.0
    authors: str = "Various Authors"
    journal: str = "Research Journal"
    date: str = "Unknown Date"
    tier: str = "peer_reviewed"

class GenerateQuestionsRequest(BaseModel):
    mode: str
    topic: str
    domain: str
    level: int
    count: int = 4
    context_chunks: list[str] = []
    previous_questions: list[dict] = []

class EvaluateAnswerRequest(BaseModel):
    question: str
    user_answer: str
    correct_answer: str

# ── UTILITIES ──
async def get_or_create_user(db: AsyncSession, user_id: str) -> User:
    res = await db.execute(select(User).where(User.id == user_id))
    user = res.scalar_one_or_none()
    if not user:
        user = User(id=user_id)
        db.add(user)
        await db.commit()
    return user

# ── ENDPOINTS ──

@app.post("/ask")
async def ask_question(request: AskRequest):
    log.info(f"Question: {request.question} | Hybrid: {request.hybrid} | Book: {request.book_id}")
    
    # Check Cache
    cache_key = f"{request.question}_{request.book_id}_{request.domain}_{request.hybrid}_{request.user_id}"
    if cache_key in query_cache:
        cached_res, timestamp = query_cache[cache_key]
        if time.time() - timestamp < 300:
            return cached_res
            
    async def _process_ask():
        # 1. PURE LLM
        if not request.hybrid:
            answer = await llm_engine.generate_llm_only(request.question, request.mode, request.domain)
            return {"answer": answer, "type": "LLM_ONLY", "sources": []}
        
        # 2. SMART RAG
        # Local Context (FAISS) - offloaded to non-blocking thread
        faiss_results = await asyncio.to_thread(search, query=request.question, top_k=10 if request.book_id else 5, book_id=request.book_id, user_id=request.user_id)
        
        # External Context (Live)
        external_articles = []
        if not request.book_id:
            res = await live_articles.search_articles(request.question)
            external_articles = res.get("articles", [])
        
        if not faiss_results and not external_articles:
            if request.book_id:
                return {"answer": "No indexed content found for this document.", "type": "LLM_FALLBACK", "sources": []}
            answer = await llm_engine.generate_llm_only(request.question, request.mode, request.domain)
            return {"answer": answer, "type": "LLM_FALLBACK", "sources": []}
        
        # Synthesis
        context_parts = [f"({r['metadata']['source_type']}) - {r['text']}" for r in faiss_results]
        context_parts.extend([f"({a['source']}) - {a['title']}: {a['summary']}" for a in external_articles[:3]])
        context_text = "\n\n".join(context_parts)
        
        is_book_query = request.book_id is not None
        if is_book_query:
            answer = await llm_engine.generate_book_answer(request.question, context_text, request.mode, request.domain)
        else:
            answer = await llm_engine.generate_hybrid_answer(request.question, context_text, request.mode, request.domain)
        
        sources = list(set([r['metadata']['source_type'] for r in faiss_results] + [a['source'] for a in external_articles[:3]]))
        return {"answer": answer, "type": "HYBRID", "sources": sources}

    try:
        final_result = await asyncio.wait_for(_process_ask(), timeout=45)
    except asyncio.TimeoutError:
        return {"answer": "Request timeout: The query took too long to process.", "type": "ERROR", "sources": []}
        
    # Set Cache
    if len(query_cache) > MAX_CACHE_SIZE:
        query_cache.clear()
        
    query_cache[cache_key] = (final_result, time.time())
    return final_result

@app.post("/generate-questions")
async def generate_questions_endpoint(request: GenerateQuestionsRequest):
    result = await llm_engine.generate_questions(
        mode=request.mode,
        topic=request.topic,
        domain=request.domain,
        level=request.level,
        context_chunks=request.context_chunks,
        previous_questions=request.previous_questions
    )
    try:
        data = json.loads(result)
        return data
    except Exception as e:
        log.error(f"Failed to parse LLM JSON output: {e}\nRaw output: {result}")
        return {"error": "Invalid JSON from LLM", "raw": result}

async def process_pdf_background(temp_path: str, book_id: str, domain: str, source_type: str, user_id: str):
    try:
        chunks = await process_pdf(temp_path)
        
        # 1. Update FAISS
        if chunks:
            await asyncio.to_thread(add_document, chunks, domain=domain, source_type=source_type, book_id=book_id, user_id=user_id)
        
        # 2. Update Database with Preview
        async with AsyncSessionLocal() as db:
            res = await db.execute(select(Book).where(Book.id == book_id))
            book = res.scalar_one_or_none()
            if book:
                if chunks:
                    book.preview = chunks[0][:250] + "..." if len(chunks[0]) > 250 else chunks[0]
                    log.info(f"Book Indexing: Updated preview for {book_id} with {len(chunks)} chunks.")
                else:
                    book.preview = f"No text content could be extracted from this {source_type}."
                    log.warning(f"Book Indexing: No content for {book_id}, set preview error.")
                await db.commit()
        
        log.info(f"Background: Completed {book_id} processing.")
    except Exception as e:
        log.error(f"Background process error for {book_id}: {e}")
    finally:
        if os.path.exists(temp_path):
            os.remove(temp_path)

@app.post("/upload-book")
async def upload_book(
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...), 
    domain: str = Form("General"), 
    source_type: str = Form("PDF"),
    user_id: str = Form(...),
    db: AsyncSession = Depends(get_db)
):
    await get_or_create_user(db, user_id)
    
    book_id = str(uuid.uuid4())
    temp_path = f"temp/{book_id}_{file.filename}"
    
    # Fast Stream to Disk
    total_bytes = 0
    with open(temp_path, "wb") as buffer:
        while True:
            chunk = await file.read(1024 * 1024)
            if not chunk: break
            total_bytes += len(chunk)
            if total_bytes > settings.MAX_UPLOAD_SIZE_MB * 1024 * 1024:
                os.remove(temp_path)
                raise HTTPException(status_code=413, detail="File too large.")
            buffer.write(chunk)
    
    # Save metadata to DB
    new_book = Book(id=book_id, user_id=user_id, title=file.filename, domain=domain)
    db.add(new_book)
    await db.commit()
    
    # Offload processing
    background_tasks.add_task(process_pdf_background, temp_path, book_id, domain, source_type, user_id)
    
    return {"status": "success", "book_id": book_id, "message": "Processing started in background."}

@app.get("/articles")
async def get_articles(
    query: str = None, page: int = 1, limit: int = 10,
    source: str = "all", domain: str = "all",
    date_range: str = "all", type: str = "all",
    sort: str = "pub+date"
):
    if query:
        return await live_articles.search_articles(
            query, page=page, limit=limit, 
            source_filter=source, domain_filter=domain, 
            date_filter=date_range, type_filter=type,
            sort=sort
        )

@app.get("/articles/trending")
async def get_trending(limit: int = 20):
    return await live_articles.fetch_trending(limit=limit)

@app.post("/save-article")
async def save_article(request: SavedArticleRequest, db: AsyncSession = Depends(get_db)):
    try:
        log.info(f"DB: Saving article {request.id} for user {request.user_id}")
        await get_or_create_user(db, request.user_id)
        # Check if already exists to prevent integrity error
        res = await db.execute(select(SavedArticle).where(SavedArticle.id == request.id, SavedArticle.user_id == request.user_id))
        if res.scalar_one_or_none():
            log.info(f"DB: Article {request.id} already exists for user {request.user_id}")
            return {"status": "already_exists"}
            
        new_art_data = request.model_dump()
        new_art = SavedArticle(**new_art_data)
        db.add(new_art)
        await db.commit()
        log.info(f"DB: Successfully saved article {request.id}")
        return {"status": "success"}
    except Exception as e:
        log.error(f"DB: Save article failed: {str(e)}")
        await db.rollback()
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/evaluate-answer")
async def evaluate_answer(request: EvaluateAnswerRequest):
    try:
        feedback = await llm_engine.evaluate_theory_answer(
            request.question, 
            request.user_answer, 
            request.correct_answer
        )
        return {"feedback": feedback}
    except Exception as e:
        log.error(f"LLM: Evaluation endpoint failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))

class EvaluateAnswerDetailedRequest(BaseModel):
    question: str
    user_answer: str
    correct_answer: str

@app.post("/evaluate-answer-detailed")
async def evaluate_answer_detailed(request: EvaluateAnswerDetailedRequest):
    try:
        result = await llm_engine.evaluate_theory_answer_detailed(
            request.question,
            request.user_answer,
            request.correct_answer
        )
        data = json.loads(result)
        return data
    except Exception as e:
        log.error(f"LLM: Detailed evaluation endpoint failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/saved-articles")
async def get_saved_articles(user_id: str, db: AsyncSession = Depends(get_db)):
    res = await db.execute(select(SavedArticle).where(SavedArticle.user_id == user_id).order_by(SavedArticle.created_at.desc()))
    return res.scalars().all()

@app.delete("/saved-articles/{user_id}/{article_id}")
async def unsave_article(user_id: str, article_id: str, db: AsyncSession = Depends(get_db)):
    try:
        res = await db.execute(select(SavedArticle).where(SavedArticle.id == article_id, SavedArticle.user_id == user_id))
        target = res.scalar_one_or_none()
        if not target:
            raise HTTPException(status_code=404, detail="Article not found in your library.")
        
        await db.delete(target)
        await db.commit()
        log.info(f"DB: Successfully unsaved article {article_id} for user {user_id}")
        return {"status": "success", "message": "Article removed from library."}
    except Exception as e:
        log.error(f"DB: Unsave article failed: {e}")
        await db.rollback()
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/books")
async def get_books(user_id: str, db: AsyncSession = Depends(get_db)):
    res = await db.execute(select(Book).where(Book.user_id == user_id))
    return res.scalars().all()

@app.delete("/books/{book_id}")
async def delete_book(book_id: str, user_id: str, background_tasks: BackgroundTasks, db: AsyncSession = Depends(get_db)):
    # Ownership Check
    res = await db.execute(select(Book).where(Book.id == book_id, Book.user_id == user_id))
    target = res.scalar_one_or_none()
    if not target:
        raise HTTPException(status_code=403, detail="Book not found or unauthorized.")
    
    # 1. Remove from database
    await db.delete(target)
    await db.commit()
    
    # 2. Remove from FAISS in background
    background_tasks.add_task(remove_document, book_id)
    
    return {"status": "success", "message": "Book deletion scheduled."}

@app.get("/health")
async def health_check():
    """Lightweight zero-downtime healthcheck."""
    return {"status": "ok", "version": "4.0.0"}

@app.get("/provider-status")
async def provider_status():
    """Real-time status of all AI providers — keys, usage, cooldowns."""
    return model_manager.get_status()

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
