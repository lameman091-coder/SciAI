import faiss
import numpy as np
import json
import os
from sentence_transformers import SentenceTransformer

# Free minimal embedding model
model = SentenceTransformer("all-MiniLM-L6-v2")

index = None
chunk_store = []
doc_metadata = []

# Persistence paths
FAISS_INDEX_PATH = "faiss_index.bin"
CHUNK_STORE_PATH = "faiss_chunks.json"

def embed_text(text):
    return model.encode([text])[0]

def save_store():
    """Persist FAISS index and chunk/metadata stores to disk."""
    global index, chunk_store, doc_metadata
    try:
        if index is not None and index.ntotal > 0:
            faiss.write_index(index, FAISS_INDEX_PATH)
            with open(CHUNK_STORE_PATH, "w", encoding="utf-8") as f:
                json.dump({"chunks": chunk_store, "metadata": doc_metadata}, f)
            print(f"[FAISS] Saved {index.ntotal} vectors, {len(chunk_store)} chunks to disk.")
        else:
            print("[FAISS] Nothing to save (index empty).")
    except Exception as e:
        print(f"[FAISS] Save error: {e}")

def load_store():
    """Load FAISS index and chunk/metadata stores from disk."""
    global index, chunk_store, doc_metadata
    try:
        if os.path.exists(FAISS_INDEX_PATH) and os.path.exists(CHUNK_STORE_PATH):
            index = faiss.read_index(FAISS_INDEX_PATH)
            with open(CHUNK_STORE_PATH, "r", encoding="utf-8") as f:
                data = json.load(f)
                chunk_store = data.get("chunks", [])
                doc_metadata = data.get("metadata", [])
            print(f"[FAISS] Loaded {index.ntotal} vectors, {len(chunk_store)} chunks from disk.")
        else:
            print("[FAISS] No existing index found. Starting fresh.")
    except Exception as e:
        print(f"[FAISS] Load error: {e}")
        index = None
        chunk_store = []
        doc_metadata = []

def add_document(chunks, domain="General", source_type="Wikipedia", book_id=None):
    global index, chunk_store, doc_metadata
    
    if not chunks:
        return
        
    embeddings = model.encode(chunks)
    
    if index is None:
        dim = embeddings.shape[1]
        index = faiss.IndexFlatL2(dim)
        
    index.add(np.array(embeddings).astype("float32"))
    
    for c in chunks:
        chunk_store.append(c)
        doc_metadata.append({"domain": domain, "source_type": source_type, "book_id": book_id})
    
    # Auto-save after adding documents
    save_store()

def remove_document(book_id):
    """Remove all chunks belonging to a specific book_id and rebuild the FAISS index."""
    global index, chunk_store, doc_metadata

    if not chunk_store or not doc_metadata:
        return 0

    # Find indices to keep (those NOT matching the book_id)
    keep_indices = [i for i, m in enumerate(doc_metadata) if m.get("book_id") != book_id]
    removed_count = len(chunk_store) - len(keep_indices)

    if removed_count == 0:
        return 0

    # Rebuild chunk_store and doc_metadata
    new_chunks = [chunk_store[i] for i in keep_indices]
    new_metadata = [doc_metadata[i] for i in keep_indices]

    chunk_store = new_chunks
    doc_metadata = new_metadata

    # Rebuild FAISS index from remaining chunks
    if chunk_store:
        embeddings = model.encode(chunk_store)
        dim = embeddings.shape[1]
        index = faiss.IndexFlatL2(dim)
        index.add(np.array(embeddings).astype("float32"))
    else:
        index = None

    save_store()
    print(f"[FAISS] Removed {removed_count} chunks for book_id={book_id}. {len(chunk_store)} chunks remain.")
    return removed_count


def search(query, top_k=5, threshold=1.5, book_id=None):
    if index is None or index.ntotal == 0:
        return []
        
    q_emb = embed_text(query).astype("float32").reshape(1, -1)
    
    if book_id:
        # BOOK-SPECIFIC: Search ALL vectors, filter by book_id, NO threshold
        # The user uploaded this book to query it — always return best chunks
        search_k = index.ntotal
    else:
        search_k = min(top_k * 3, index.ntotal)  # Slightly over-fetch for global
    
    distances, indices = index.search(q_emb, search_k)
    
    # Threshold check for global search only (not book-specific)
    if not book_id and distances[0][0] > threshold:
        return []
    
    results = []
    for i, idx in enumerate(indices[0]):
        if idx != -1 and idx < len(chunk_store):
            dist = float(distances[0][i])
            metadata = doc_metadata[idx]
            
            # Book-specific filtering: only keep chunks from this book
            if book_id and metadata.get("book_id") != book_id:
                continue
            
            # For global search, apply threshold
            if not book_id and dist > threshold:
                continue
            
            # Keyword boost: reduce distance for chunks containing query keywords
            query_words = set(query.lower().split())
            chunk_lower = chunk_store[idx].lower()
            keyword_hits = sum(1 for w in query_words if len(w) > 3 and w in chunk_lower)
            boosted_dist = dist - (keyword_hits * 0.15)  # Boost relevance per keyword match
                
            results.append({
                "chunk": chunk_store[idx],
                "metadata": metadata,
                "distance": dist,
                "boosted_distance": boosted_dist,
                "keyword_hits": keyword_hits
            })
    
    # Sort by boosted distance (keyword-matched chunks rank higher)
    results.sort(key=lambda r: r["boosted_distance"])
    
    # Return top_k results
    return results[:top_k]
