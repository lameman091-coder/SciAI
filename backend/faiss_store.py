import faiss
import numpy as np
import json
import os
import threading
from sentence_transformers import SentenceTransformer
from logger import log
from config import settings

_model = None

def get_model():
    global _model
    if _model is None:
        log.info("Loading embedding model...")
        from sentence_transformers import SentenceTransformer
        _model = SentenceTransformer("all-MiniLM-L6-v2")
    return _model

def get_embeddings(texts: list) -> np.ndarray:
    if settings.USE_HF and settings.HF_API_KEY:
        import httpx
        try:
            HF_API_URL = "https://api-inference.huggingface.co/pipeline/feature-extraction/sentence-transformers/all-MiniLM-L6-v2"
            HF_HEADERS = {"Authorization": f"Bearer {settings.HF_API_KEY}"}
            with httpx.Client() as client:
                res = client.post(HF_API_URL, headers=HF_HEADERS, json={"inputs": texts, "options":{"wait_for_model":True}}, timeout=30.0)
                res.raise_for_status()
                return np.array(res.json())
        except Exception as e:
            log.error(f"HF API Failed, falling back to local model... {e}")
            
    return get_model().encode(texts)

class VectorStore:
    def __init__(self):
        self.index = None
        self.chunk_store = []
        self.doc_metadata = []
        self.lock = threading.Lock()
        self._loaded = False

    def ensure_loaded(self):
        if not self._loaded:
            self.load()
            self._loaded = True

    def embed_text(self, text: str) -> np.ndarray:
        embedding = get_embeddings([text])[0]
        # Normalize for cosine similarity (Inner Product)
        return embedding / np.linalg.norm(embedding)

    def save(self):
        with self.lock:
            try:
                if self.index is not None and self.index.ntotal > 0:
                    faiss.write_index(self.index, settings.FAISS_INDEX_PATH)
                    with open(settings.CHUNK_STORE_PATH, "w", encoding="utf-8") as f:
                        json.dump({"chunks": self.chunk_store, "metadata": self.doc_metadata}, f)
                    log.info(f"FAISS: Saved {self.index.ntotal} vectors.")
            except Exception as e:
                log.error(f"FAISS: Save error: {e}")

    def load(self):
        with self.lock:
            try:
                if os.path.exists(settings.FAISS_INDEX_PATH) and os.path.exists(settings.CHUNK_STORE_PATH):
                    self.index = faiss.read_index(settings.FAISS_INDEX_PATH)
                    with open(settings.CHUNK_STORE_PATH, "r", encoding="utf-8") as f:
                        data = json.load(f)
                        self.chunk_store = data.get("chunks", [])
                        self.doc_metadata = data.get("metadata", [])
                    log.info(f"FAISS: Loaded {self.index.ntotal} vectors of type {type(self.index).__name__}.")

                    # Migrate IndexFlatIP/L2 to IndexIDMap2 to support add_with_ids
                    if type(self.index).__name__ in ["IndexFlatIP", "IndexFlatL2", "IndexFlat"]:
                        log.info(f"FAISS: Migrating {type(self.index).__name__} to IndexIDMap2...")
                        ntotal = self.index.ntotal
                        dim = self.index.d
                        # In SciAI, we use cosine similarity (IndexFlatIP), so we convert all older indexes to FlatIP.
                        new_index = faiss.IndexIDMap2(faiss.IndexFlatIP(dim))
                        if ntotal > 0:
                            all_vectors = self.index.reconstruct_n(0, ntotal)
                            new_ids = np.arange(ntotal).astype('int64')
                            new_index.add_with_ids(all_vectors.astype('float32'), new_ids)
                        self.index = new_index
                        log.info("FAISS: Migration successful.")
                else:
                    log.warning("FAISS: No existing index found.")
            except Exception as e:
                log.error(f"FAISS: Load error: {e}")

    def add_document(self, chunks: list, domain="General", source_type="PDF", book_id=None, user_id="guest"):
        self.ensure_loaded()
        if not chunks: return
        
        embeddings = get_embeddings(chunks)
        # Normalize embeddings
        embeddings = embeddings / np.linalg.norm(embeddings, axis=1, keepdims=True)
        
        with self.lock:
            dim = embeddings.shape[1]
            if self.index is None:
                # Use IDMap2 wrapping IndexFlatIP
                self.index = faiss.IndexIDMap2(faiss.IndexFlatIP(dim))
            
            # Generate IDs for these chunks (using current end index as start)
            start_idx = len(self.chunk_store)
            batch_ids = np.arange(start_idx, start_idx + len(embeddings)).astype('int64')
            
            self.index.add_with_ids(np.array(embeddings).astype("float32"), batch_ids)
            
            for c in chunks:
                self.chunk_store.append(c)
                self.doc_metadata.append({"domain": domain, "source_type": source_type, "book_id": book_id, "user_id": user_id})
        
        self.save()

    def remove_document(self, book_id: str) -> int:
        self.ensure_loaded()
        with self.lock:
            if not self.doc_metadata: return 0
            
            # Find indices to remove
            indices_to_remove = [i for i, m in enumerate(self.doc_metadata) if m.get("book_id") == book_id]
            removed_count = len(indices_to_remove)
            
            if removed_count == 0: return 0
            
            # Remove from FAISS index using IDs
            # IDs in IDMap2 correspond to our list indices
            if self.index:
                ids_to_remove = np.array(indices_to_remove).astype('int64')
                self.index.remove_ids(ids_to_remove)
                
            # Update memory stores (must maintain index parity)
            # Since we removed from index, we have a problem: higher indices shifted?
            # NO, IndexIDMap2 doesn't shift internal IDs, but our list DOES shift.
            # CRITICAL: If we remove from list, all subsequent indices in FAISS will be wrong.
            
            # BETTER APPROACH for Flat Index:
            # Rebuild the IndexIDMap2 from the remaining vectors in the index!
            # But FAISS doesn't allow easy "move" of vectors.
            
            # Actually, the most robust way for Flat index without re-encoding is:
            # 1. Get all remaining vectors from index: self.index.reconstruct_n(0, ntotal)
            # 2. Rebuild list and metadata
            # 3. Create fresh index and add vectors
            
            all_indices = list(range(len(self.doc_metadata)))
            indices_to_keep = [i for i in all_indices if i not in indices_to_remove]
            
            if not indices_to_keep:
                self.chunk_store = []
                self.doc_metadata = []
                self.index = None
            else:
                # Optimized rebuild WITHOUT re-encoding
                ntotal = self.index.ntotal
                # Extract vectors before we mess with the list
                remaining_vectors = []
                for i in indices_to_keep:
                    # IndexIDMap2 allows reconstruction by ID
                    vec = self.index.reconstruct(i)
                    remaining_vectors.append(vec)
                
                self.chunk_store = [self.chunk_store[i] for i in indices_to_keep]
                self.doc_metadata = [self.doc_metadata[i] for i in indices_to_keep]
                
                dim = len(remaining_vectors[0])
                self.index = faiss.IndexIDMap2(faiss.IndexFlatIP(dim))
                # Add with NEW IDs (0 to len-1) to restore parity
                new_ids = np.arange(len(indices_to_keep)).astype('int64')
                self.index.add_with_ids(np.array(remaining_vectors).astype('float32'), new_ids)
        
        self.save()
        log.info(f"FAISS: Removed {removed_count} chunks for book_id={book_id} (Fast Rebuild)")
        return removed_count

    def search(self, query: str, top_k=5, threshold=0.3, book_id=None, user_id="guest") -> list:
        self.ensure_loaded()
        if self.index is None or self.index.ntotal == 0: return []
        
        # Embed query and normalize for IndexFlatIP (cosine similarity)
        # We do this OUTSIDE the lock because it's CPU intensive and doesn't modify state
        embeddings = get_embeddings([query])
        q_emb = np.array(embeddings).astype("float32")
        q_emb = q_emb / np.linalg.norm(q_emb, axis=1, keepdims=True)
        
        with self.lock:
            if self.index is None: return []
            search_k = self.index.ntotal if book_id else min(top_k * 5, self.index.ntotal)
            if search_k == 0: return []
            
            # FAISS search is fast and thread-safe for reading from similar pointers
            scores, indices = self.index.search(q_emb, search_k)
            
            # Snapshots of metadata to prevent async deletion mismatches during loop
            current_chunks = self.chunk_store
            current_meta = self.doc_metadata
            
            log.info(f"FAISS Search: query='{query[:30]}...', search_k={search_k}, ntotal={self.index.ntotal}")
            
        results = []
        for i, idx in enumerate(indices[0]):
            if idx == -1 or idx >= len(current_chunks): continue
            
            score = float(scores[0][i])
            meta = current_meta[idx]
            
            if book_id and meta.get("book_id") != book_id: continue
            if not book_id and user_id != "guest" and meta.get("user_id", "guest") != user_id: continue
            if not book_id and score < threshold: continue
            
            # Simple keyword boost
            query_words = set(query.lower().split())
            chunk_text = current_chunks[idx]
            chunk_lower = chunk_text.lower()
            hit_count = sum(1 for w in query_words if len(w) > 3 and w in chunk_lower)
            boosted_score = score + (hit_count * 0.05)
            
            results.append({
                "text": chunk_text,
                "metadata": meta,
                "score": score,
                "boosted_score": boosted_score,
                "keyword_hits": hit_count
            })
        
        log.info(f"FAISS Search: Matches for book_id={book_id}: {len(results)}")
        results.sort(key=lambda x: x["boosted_score"], reverse=True)
        return results[:top_k]

# Global instance
store = VectorStore()

# Wrapper functions for compatibility with main.py
def add_document(*args, **kwargs): return store.add_document(*args, **kwargs)
def search(*args, **kwargs): return store.search(*args, **kwargs)
def remove_document(*args, **kwargs): return store.remove_document(*args, **kwargs)
def save_store(): return store.save()
def load_store(): return store.load()
