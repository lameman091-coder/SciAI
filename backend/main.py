from fastapi import FastAPI, HTTPException, UploadFile, File, Form, Depends, BackgroundTasks, Request
from fastapi.responses import StreamingResponse as FastAPIStreamingResponse
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, delete
from pydantic import BaseModel, Field
from typing import Optional
import os
import uuid
import json
import sentry_sdk
from sentry_sdk.integrations.fastapi import FastApiIntegration
from slowapi import Limiter
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded
from fastapi.responses import JSONResponse
import io
from slowapi.middleware import SlowAPIMiddleware
from metrics import setup_metrics
from fastapi.exceptions import RequestValidationError

from config import settings
from logger import log
from database import init_db, get_db, User, Book, SavedArticle, AsyncSessionLocal
from pdf_processor import process_pdf
from faiss_store import add_document, search, remove_document, load_store, save_store
import llm_engine
import live_articles
import controller_prompt
import companion_engine
from tts_engine import tts_engine
from tts_text_processor import tts_processor
from model_manager import manager as model_manager
from cache_service import cache_service
import time
import asyncio

# Rate Limiter
limiter = Limiter(key_func=get_remote_address)

MAX_CACHE_SIZE = 200

# API Version
API_V1_PREFIX = "/api/v1"

# Sentry Setup
if settings.SENTRY_DSN:
    sentry_sdk.init(
        dsn=settings.SENTRY_DSN,
        integrations=[FastApiIntegration()],
        traces_sample_rate=0.1,
        environment="production"
    )
    log.info("Sentry monitoring enabled")

app = FastAPI(
    title="SciAI Core Backend",
    version="4.2.0",
    docs_url=f"{API_V1_PREFIX}/docs",
    redoc_url=f"{API_V1_PREFIX}/redoc"
)

# Include versioned API routes
from fastapi import APIRouter

api_router = APIRouter(prefix=API_V1_PREFIX)

# CORS Setup
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.exception_handler(RateLimitExceeded)
async def rate_limit_handler(request: Request, exc: RateLimitExceeded):
    return JSONResponse(
        status_code=429,
        content={"detail": "Rate limit exceeded. Please try again later."}
    )

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """Detailed logging for 422 Unprocessable Content errors."""
    log.error(f"422 Validation Error at {request.url.path}: {exc.errors()}")
    log.error(f"Body: {await request.body()}")
    return JSONResponse(
        status_code=422,
        content={"detail": exc.errors(), "body": str(await request.body())}
    )

# Standard Middlewares initialization
app.add_middleware(SlowAPIMiddleware)
app.state.limiter = limiter

# Initialize Metrics
setup_metrics(app)

@app.on_event("startup")
async def startup_event():
    print("Starting SciAI backend...")
    log.info("SciAI PRODUCTION ENGINE STARTING...")
    await init_db()
    await cache_service.connect()
    log.info(f"ModelManager: {len([p for p in model_manager.providers.values() if p.is_configured])} providers configured")
    # Initialize TTS engine (non-blocking, lazy-loads models)
    try:
        await tts_engine.initialize()
        log.info("TTS Engine: Initialized")
    except Exception as e:
        log.warning(f"TTS Engine: Init deferred — {e}")

@app.on_event("shutdown")
async def shutdown_event():
    log.info("SciAI SHUTTING DOWN...")
    save_store()
    await cache_service.close()
    await model_manager.close()

# ── REQUEST MODELS ──
class AskRequest(BaseModel):
    question: str = Field(..., min_length=1, max_length=2000)
    mode: str = Field(..., pattern="^(Concept|Exam|Expert|Quiz|Library|Test)$")
    domain: str = Field(..., min_length=1, max_length=100)
    level: str = Field(default="Academic", max_length=50)
    book_id: Optional[str] = None
    hybrid: bool = False
    user_id: str = Field(default="guest", max_length=100)

class ImageAnalysisRequest(BaseModel):
    image_b64: str
    question: Optional[str] = None

class SavedArticleRequest(BaseModel):
    user_id: str = Field(..., max_length=100)
    id: str = Field(..., max_length=100)
    title: str = Field(..., min_length=1, max_length=500)
    summary: str = Field(..., max_length=2000)
    source: str = Field(..., max_length=100)
    link: str = Field(..., max_length=1000)
    score: float = Field(default=0.0, ge=0.0, le=1.0)
    authors: str = Field(default="Various Authors", max_length=200)
    journal: str = Field(default="Research Journal", max_length=200)
    date: str = Field(default="Unknown Date", max_length=50)
    tier: str = Field(default="peer_reviewed", pattern="^(peer_reviewed|preprint|blog|news)$")

class GenerateQuestionsRequest(BaseModel):
    mode: str = Field(..., pattern="^(Concept|Exam|Expert|Quiz|Library|Test)$")
    topic: str = Field(..., min_length=1, max_length=500)
    domain: str = Field(..., min_length=1, max_length=100)
    level: int = Field(..., ge=1, le=1000)
    count: int = Field(default=4, ge=1, le=20)
    context_chunks: list[str] = Field(default_factory=list)
    previous_questions: list[dict] = Field(default_factory=list)

class EvaluateAnswerRequest(BaseModel):
    question: str = Field(..., min_length=1, max_length=1000)
    user_answer: str = Field(..., min_length=1, max_length=5000)
    correct_answer: str = Field(..., min_length=1, max_length=5000)

class RouteQueryRequest(BaseModel):
    query: str = Field(..., min_length=1, max_length=1000)

class TTSRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=5000)
    voice_style: str = Field(default="professor", pattern="^(professor|energetic|storyteller)$")
    mode: str = Field(default="Concept", pattern="^(Concept|Exam|Expert|Quiz|Library|Articles|Test)$")
    speed: float = Field(default=1.0, ge=0.5, le=2.0)
    use_ssml: bool = False

@api_router.post("/route")
@limiter.limit("30/minute")
async def route_query(request: Request, request_body: RouteQueryRequest):
    """Controller Decision Engine — classify query into mode + domain."""
    try:
        result = await controller_prompt.classify(request_body.query)
        return result
    except Exception as e:
        log.error(f"Controller: Route endpoint failed: {e}")
        return {
            "mode": "Concept",
            "domain": "General",
            "companion_message": "Processing your request.",
            "confidence": 0.0
        }

class CompanionChatRequest(BaseModel):
    user_id: str = Field(..., max_length=100)
    query: str = Field(..., min_length=1, max_length=2000)

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

@api_router.post("/companion/chat")
@limiter.limit("20/minute")
async def companion_chat(request: Request, request_body: CompanionChatRequest):
    """AI Companion Chat — uses the Butler skill for app management."""
    result = await companion_engine.engine.chat(request_body.user_id, request_body.query)
    return result

@api_router.post("/ask")
@limiter.limit("30/minute")
async def ask_question(request: Request, request_body: AskRequest):
    log.info(f"Question: {request_body.question} | Hybrid: {request_body.hybrid} | Book: {request_body.book_id}")
    
    # Check Cache
    cache_key = f"ask:{request_body.question}_{request_body.book_id}_{request_body.domain}_{request_body.hybrid}_{request_body.user_id}"
    cached = await cache_service.get(cache_key)
    if cached:
        return cached
            
    async def _process_ask():
        # 1. PURE LLM
        if not request_body.hybrid:
            answer = await llm_engine.generate_llm_only(
                request_body.question, 
                request_body.mode, 
                request_body.domain,
                request_body.level
            )
            return {"answer": answer, "type": "LLM_ONLY", "sources": []}
        
        # 2. SMART RAG
        # Local Context (FAISS) - offloaded to non-blocking thread
        faiss_results = await asyncio.to_thread(search, query=request_body.question, top_k=10 if request_body.book_id else 5, book_id=request_body.book_id, user_id=request_body.user_id)
        
        # External Context (Live)
        external_articles = []
        if not request_body.book_id:
            res = await live_articles.search_articles(request_body.question)
            external_articles = res.get("articles", [])
        
        if not faiss_results and not external_articles:
            if request_body.book_id:
                return {"answer": "No indexed content found for this document.", "type": "LLM_FALLBACK", "sources": []}
            answer = await llm_engine.generate_llm_only(request_body.question, request_body.mode, request_body.domain)
            return {"answer": answer, "type": "LLM_FALLBACK", "sources": []}
        
        # Synthesis
        context_parts = [f"({r['metadata']['source_type']}) - {r['text']}" for r in faiss_results]
        context_parts.extend([f"({a['source']}) - {a['title']}: {a['summary']}" for a in external_articles[:3]])
        context_text = "\n\n".join(context_parts)
        
        is_book_query = request_body.book_id is not None
        if is_book_query:
            answer = await llm_engine.generate_book_answer(request_body.question, context_text, request_body.mode, request_body.domain, request_body.level)
        else:
            answer = await llm_engine.generate_hybrid_answer(request_body.question, context_text, request_body.mode, request_body.domain, request_body.level)
        
        sources = list(set([r['metadata']['source_type'] for r in faiss_results] + [a['source'] for a in external_articles[:3]]))
        return {"answer": answer, "type": "SCIAI_RAG", "sources": sources}

    try:
        final_result = await asyncio.wait_for(_process_ask(), timeout=45)
    except asyncio.TimeoutError:
        return {"answer": "Request timeout: The query took too long to process.", "type": "ERROR", "sources": []}
        
    # Set Cache (300 seconds TTL)
    await cache_service.set(cache_key, final_result, ttl=300)
    return final_result

@api_router.post("/generate-questions")
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

@api_router.post("/upload-book")
async def upload_book(
    file: UploadFile = File(...), 
    domain: str = Form("General"), 
    source_type: str = Form("PDF"),
    user_id: str = Form(...),
    db: AsyncSession = Depends(get_db)
):
    from celery_config import process_pdf_async
    await get_or_create_user(db, user_id)
    
    book_id = str(uuid.uuid4())
    temp_path = os.path.join(settings.TEMP_DIR, f"{book_id}.pdf")
    
    # ensure temp dir exists
    os.makedirs(settings.TEMP_DIR, exist_ok=True)
    
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
    new_book = Book(id=book_id, user_id=user_id, title=file.filename, domain=domain, preview="Processing...")
    db.add(new_book)
    await db.commit()
    
    # Offload processing to Celery
    process_pdf_async.delay(book_id, domain, source_type, user_id)
    
    return {"status": "success", "book_id": book_id, "message": "Processing started via Celery."}

@api_router.get("/articles")
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

@api_router.get("/articles/trending")
async def get_trending(limit: int = 20):
    return await live_articles.fetch_trending(limit=limit)

@api_router.post("/save-article")
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

@api_router.post("/evaluate-answer")
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

@api_router.post("/evaluate-answer-detailed")
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

@api_router.get("/saved-articles")
async def get_saved_articles(user_id: str, db: AsyncSession = Depends(get_db)):
    res = await db.execute(select(SavedArticle).where(SavedArticle.user_id == user_id).order_by(SavedArticle.created_at.desc()))
    return res.scalars().all()

@api_router.delete("/saved-articles/{user_id}/{article_id}")
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

@api_router.get("/books")
async def get_books(user_id: str, db: AsyncSession = Depends(get_db)):
    res = await db.execute(select(Book).where(Book.user_id == user_id))
    return res.scalars().all()

@api_router.delete("/books/{book_id}")
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

# ── TTS ENDPOINTS ──────────────────────────────────────────────────────────────

@api_router.post("/tts")
@limiter.limit("15/minute")
async def text_to_speech(request: Request, request_body: TTSRequest):
    """Generate speech audio from text using tiered TTS engines.
    
    Pipeline: raw text → LLM speech optimization → Kyutai/HF audio generation → WAV stream
    Falls back gracefully: Kyutai → HuggingFace → error (client uses Android TTS)
    """
    try:
        # Step 1: Preprocess text with LLM for natural speech
        log.info(f"TTS: Processing {len(request_body.text)} chars, style={request_body.voice_style}, mode={request_body.mode}")
        
        optimized_text = await tts_processor.optimize_for_speech(
            text=request_body.text,
            voice_style=request_body.voice_style,
            mode=request_body.mode,
            use_ssml=request_body.use_ssml
        )
        
        if not optimized_text:
            raise HTTPException(status_code=400, detail="Text optimization produced empty result")
        
        # Step 2: Return as StreamingResponse for real-time playback
        return FastAPIStreamingResponse(
            content=tts_engine.generate_stream(
                text=optimized_text,
                voice_style=request_body.voice_style,
                speed=request_body.speed
            ),
            media_type="audio/wav",
            headers={
                "Content-Disposition": "inline; filename=tts_output.wav",
                "X-TTS-Engine": "kyutai" if tts_engine.get_status()["kyutai"]["loaded"] else "huggingface"
                # Content-Length is omitted for streaming
            }
        )

        
    except HTTPException:
        raise
    except Exception as e:
        log.error(f"TTS: Endpoint failed: {e}")
        raise HTTPException(status_code=500, detail=f"TTS generation failed: {str(e)}")

@api_router.get("/tts/status")
async def tts_status():
    """Real-time status of TTS engines."""
    return tts_engine.get_status()

@api_router.post("/analyze-image")
async def analyze_image_endpoint(request: ImageAnalysisRequest):
    """Analyze a scientific image / question."""
    try:
        answer = await llm_engine.analyze_image(request.image_b64, request.question)
        return {"answer": answer}
    except Exception as e:
        log.error(f"API: Image analysis error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@api_router.get("/health")
async def health_check():
    """Lightweight zero-downtime healthcheck."""
    return {"status": "ok", "version": "4.3.0"}

@api_router.get("/provider-status")
async def provider_status():
    """Real-time status of all AI providers — keys, usage, cooldowns."""
    return model_manager.get_status()

# Mount Versioned Router
app.include_router(api_router)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
