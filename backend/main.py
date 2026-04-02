from fastapi import FastAPI, HTTPException, UploadFile, File, Form
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import os
import uuid
import json

from pdf_processor import process_pdf
from faiss_store import add_document, search, remove_document, load_store as load_faiss, save_store as save_faiss
from ranking_logic import rank_results

app = FastAPI(title="SciAI RAG Backend")

# File-based persistence
DB_FILE = "library_data.json"

@app.on_event("startup")
def startup_event():
    print("-" * 50)
    print("SciAI PRODUCTION ENGINE v2.0 - LIVE MODE ACTIVATED")
    print("-" * 50)
    load_db()
    load_faiss()  # Restore FAISS vector index from disk

@app.on_event("shutdown")
def shutdown_event():
    save_db()
    save_faiss()  # Persist FAISS vector index to disk

def save_db():
    try:
        data = {
            "books": books_db,
            "saved_articles": saved_articles
        }
        with open(DB_FILE, "w") as f:
            json.dump(data, f)
        print(f"[SciAI] Saved Database: {len(books_db)} books, {len(saved_articles)} articles.")
    except Exception as e:
        print(f"[SciAI] Save DB error: {e}")

def load_db():
    global books_db, saved_articles
    if os.path.exists(DB_FILE):
        try:
            with open(DB_FILE, "r") as f:
                data = json.load(f)
                dirty_books = data.get("books", [])
                # MIGRATION: Only keep books that have a user_id field (new v3.0 format)
                # Old books without user_id are discarded per user request
                books_db = [b for b in dirty_books if b.get('user_id')]
                discarded = len(dirty_books) - len(books_db)
                if discarded > 0:
                    print(f"[SciAI] Discarded {discarded} legacy books without user_id.")
                saved_articles = data.get("saved_articles", [])
                print(f"[SciAI] Loaded Database: {len(books_db)} user books, {len(saved_articles)} saved articles.")
        except Exception as e:
            print(f"[SciAI] Load DB error: {e}")
            books_db = []
            saved_articles = []
    else:
        print("[SciAI] No existing database found. Starting clean v3.0 state.")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# In-memory storage as requested
articles_cache = []
books_db = []
saved_articles = []

class AskRequest(BaseModel):
    question: str
    mode: str
    domain: str
    book_id: str = None
    hybrid: bool = False

@app.post("/ask")
def ask_question(request: AskRequest):
    from llm_engine import generate_llm_only, generate_hybrid_answer
    
    # 1. HYBRID OFF -> Pure LLM
    if not request.hybrid:
        print(f"[SciAI] Mode: Pure LLM. Question: {request.question}")
        answer = generate_llm_only(request.question, request.mode, request.domain)
        return {
            "answer": answer,
            "type": "LLM_ONLY",
            "sources": []
        }
    
    # 2. HYBRID ON -> Smart RAG
    # Retrieve local FAISS results (more chunks for book-specific queries)
    book_top_k = 10 if request.book_id else 5
    faiss_results = search(request.question, top_k=book_top_k, book_id=request.book_id)
    
    print(f"[SciAI] FAISS results for book_id={request.book_id}: {len(faiss_results)} chunks found")
    for i, r in enumerate(faiss_results):
        kw = r.get('keyword_hits', 0)
        print(f"  Chunk {i}: dist={r['distance']:.3f}, boost={r.get('boosted_distance', r['distance']):.3f}, kw_hits={kw}, book={r['metadata'].get('book_id', 'N/A')}, preview={r['chunk'][:80]}...")
    
    # Only fetch external articles if NOT a book-specific query
    external_articles = []
    if not request.book_id:
        from live_articles import search_articles
        external_result = search_articles(request.question)
        external_articles = external_result.get("articles", []) if isinstance(external_result, dict) else external_result
    
    # Check for relevance/context availability
    if not faiss_results and not external_articles:
        # For book-specific queries, provide a specific error instead of LLM fallback
        if request.book_id:
            print(f"[SciAI] CRITICAL: Book query for book_id={request.book_id} returned 0 FAISS chunks. Index may be empty or corrupted.")
            return {
                "answer": "No indexed content found for this document. The PDF may not have been processed correctly, or the vector index may need rebuilding. Try re-uploading the document.",
                "type": "LLM_FALLBACK",
                "sources": []
            }
        print(f"[SciAI] Mode: Smart RAG (Fallback). No relevant context found.")
        answer = generate_llm_only(request.question, request.mode, request.domain)
        return {
            "answer": answer,
            "type": "LLM_FALLBACK",
            "sources": []
        }
    
    # Combine context for refinement
    context_text_parts = []
    unique_sources = set()
    
    # Add FAISS chunks (from uploaded PDF when book_id is set)
    for r in faiss_results:
        context_text_parts.append(f"(Local: {r['metadata']['source_type']}) - {r['chunk']}")
        unique_sources.add(f"Local: {r['metadata']['source_type']}")
    
    # Add External Articles (only for non-book queries)
    for article in external_articles[:3]:
        context_text_parts.append(f"(External: {article['source']}) - {article['title']}: {article['summary']}")
        unique_sources.add(f"External: {article['source']}")
        
    context_text = "\n\n".join(context_text_parts)
    
    # Use a specialized prompt for book-specific queries
    is_book_query = request.book_id is not None
    print(f"[SciAI] Mode: {'Book-Specific RAG' if is_book_query else 'Smart RAG'}. Generating refined answer with {len(unique_sources)} sources.")
    
    if is_book_query:
        from llm_engine import generate_book_answer
        answer = generate_book_answer(request.question, context_text, request.mode, request.domain)
    else:
        answer = generate_hybrid_answer(request.question, context_text, request.mode, request.domain)
    
    return {
        "answer": answer,
        "type": "HYBRID",
        "sources": list(unique_sources)
    }

MAX_UPLOAD_SIZE_MB = 100  # Reject files larger than 100 MB

@app.post("/upload-book")
async def upload_book(
    file: UploadFile = File(...), 
    domain: str = Form("General"), 
    source_type: str = Form("PDF"),
    user_id: str = Form(...)
):
    if not user_id:
        raise HTTPException(status_code=400, detail="user_id is required")
    
    os.makedirs("temp", exist_ok=True)
    temp_path = f"temp/{file.filename}"

    # Stream file to disk in 1MB chunks (non-blocking, handles large files)
    total_bytes = 0
    try:
        with open(temp_path, "wb") as buffer:
            while True:
                chunk = await file.read(1024 * 1024)  # 1MB chunk
                if not chunk:
                    break
                total_bytes += len(chunk)
                if total_bytes > MAX_UPLOAD_SIZE_MB * 1024 * 1024:
                    buffer.close()
                    os.remove(temp_path)
                    raise HTTPException(status_code=413, detail=f"File too large. Max allowed size is {MAX_UPLOAD_SIZE_MB} MB.")
                buffer.write(chunk)
    except HTTPException:
        raise
    except Exception as e:
        if os.path.exists(temp_path):
            os.remove(temp_path)
        raise HTTPException(status_code=500, detail=f"File write failed: {str(e)}")

    print(f"[SciAI] Received file: {file.filename} ({total_bytes / (1024*1024):.1f} MB) from user={user_id}. Processing PDF...")

    try:
        chunks = process_pdf(temp_path)
    except Exception as e:
        os.remove(temp_path)
        raise HTTPException(status_code=500, detail=f"PDF processing failed: {str(e)}")

    book_id = str(uuid.uuid4())

    # Save to global memory with user_id for isolation
    books_db.append({
        "id": book_id,
        "user_id": user_id,
        "title": file.filename,
        "domain": domain,
        "preview": chunks[0][:150] + "..." if chunks else "No preview available"
    })

    add_document(chunks, domain=domain, source_type=source_type, book_id=book_id)

    save_db()  # Persist on every upload
    os.remove(temp_path)
    print(f"[SciAI] Upload complete: {file.filename} → {len(chunks)} chunks, book_id={book_id}, user_id={user_id}")
    return {"status": "success", "chunks_processed": len(chunks), "source": source_type, "book_id": book_id}

@app.get("/articles")
def get_articles(
    query: str = None, 
    sort: str = "pub+date", 
    page: int = 1, 
    limit: int = 10,
    source: str = "all",
    domain: str = "all",
    date_range: str = "all",
    type: str = "all"
):
    if query:
        from live_articles import search_articles
        results = search_articles(
            query, sort=sort, page=page, limit=limit,
            source_filter=source, domain_filter=domain,
            date_filter=date_range, type_filter=type
        )
        return results
    return {"articles": [], "total_count": 0, "page": page}

@app.get("/articles/trending")
def get_trending(limit: int = 25):
    from live_articles import fetch_trending
    return fetch_trending(limit=limit)

class SavedArticle(BaseModel):
    title: str
    summary: str
    source: str
    score: float
    authors: str = "Various Authors"
    journal: str = "Nature / PubMed"
    date: str = "Unknown Date"

@app.post("/save-article")
def save_article(article: SavedArticle):
    saved_articles.append(article.model_dump())
    save_db() # Persist on save
    return {"status": "success"}

@app.get("/saved-articles")
def get_saved_articles():
    return saved_articles

@app.get("/books")
def get_books(user_id: str = None):
    if not user_id:
        return []  # Safety: no user_id = empty library
    user_books = [b for b in books_db if b.get("user_id") == user_id]
    return user_books

@app.delete("/books/{book_id}")
def delete_book(book_id: str, user_id: str = None):
    global books_db
    
    if not user_id:
        return {"status": "error", "message": "user_id is required"}
    
    # Find the book and verify ownership
    target_book = next((b for b in books_db if b["id"] == book_id), None)
    if not target_book:
        return {"status": "error", "message": "Book not found"}
    if target_book.get("user_id") != user_id:
        return {"status": "error", "message": "Not authorized to delete this book"}
    
    books_db = [b for b in books_db if b["id"] != book_id]
    
    # Remove associated chunks from FAISS
    removed_chunks = remove_document(book_id)
    save_db()
    
    print(f"[SciAI] Deleted book {book_id} for user {user_id}. Removed {removed_chunks} FAISS chunks.")
    return {"status": "success", "chunks_removed": removed_chunks}

if __name__ == "__main__":
    import uvicorn
    # Startup and Shutdown are handled by @app.on_event
    uvicorn.run(app, host="0.0.0.0", port=8000)
