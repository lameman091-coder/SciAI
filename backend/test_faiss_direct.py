import sys
import os
import json
import faiss
import numpy as np
from sentence_transformers import SentenceTransformer

# Load model
model = SentenceTransformer("all-MiniLM-L6-v2")

def test_search(book_id, query):
    index_path = 'c:/Users/HP/AndroidStudioProjects/SciAI/backend/faiss_index.bin'
    chunks_path = 'c:/Users/HP/AndroidStudioProjects/SciAI/backend/faiss_chunks.json'
    
    if not os.path.exists(index_path) or not os.path.exists(chunks_path):
        print("Files missing")
        return

    index = faiss.read_index(index_path)
    with open(chunks_path, "r", encoding="utf-8") as f:
        data = json.load(f)
        chunks = data.get("chunks", [])
        metadata = data.get("metadata", [])

    print(f"Index total: {index.ntotal}")
    print(f"Chunks total: {len(chunks)}")
    print(f"Metadata total: {len(metadata)}")

    # Embed query and normalize for IndexFlatIP (cosine similarity)
    q_emb = model.encode([query])
    q_emb = np.array(q_emb).astype("float32")
    q_emb = q_emb / np.linalg.norm(q_emb, axis=1, keepdims=True)
    
    scores, indices = index.search(q_emb, index.ntotal)
    
    count = 0
    for i, idx in enumerate(indices[0]):
        if idx == -1: continue
        meta = metadata[idx]
        if meta.get("book_id") == book_id:
            count += 1
            if count <= 2:
                print(f"Match found! Score: {scores[0][i]}, Text snippet: {chunks[idx][:50]}")
    
    print(f"Total matches for book_id {book_id}: {count}")

if __name__ == "__main__":
    test_search("03c42a23-4537-4faf-aa67-0dfeb668b261", "TELL ME ABOUT THIS PDF")
