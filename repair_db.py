import sqlite3
import json
import os
from datetime import datetime

# Root paths for server running from root
db_path = 'c:/Users/HP/AndroidStudioProjects/SciAI/sciai.db'
faiss_path = 'c:/Users/HP/AndroidStudioProjects/SciAI/faiss_chunks.json'

if not os.path.exists(db_path) or not os.path.exists(faiss_path):
    print(f"Files missing at root: {db_path}, {faiss_path}")
else:
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    with open(faiss_path, "r", encoding="utf-8") as f:
        data = json.load(f)
        chunks = data.get("chunks", [])
        metadata = data.get("metadata", [])
        
    print(f"Total chunks in JSON: {len(chunks)}")
    
    # Identify unique book_ids in FAISS
    book_ids = set(m.get("book_id") for m in metadata if m.get("book_id"))
    print(f"Total unique books in Vector Store: {len(book_ids)}")
    
    # Identify unique books in DB
    cursor.execute("SELECT id FROM books")
    db_ids = set(r[0] for r in cursor.fetchall())
    print(f"Total unique books in Database: {len(db_ids)}")
    
    orphaned_ids = book_ids - db_ids
    print(f"Orphaned books found (in Vector Store but NOT in DB): {len(orphaned_ids)}")
    
    now = datetime.now().isoformat()
    
    for b_id in orphaned_ids:
        # Find first chunk for metadata
        meta = next(m for m in metadata if m.get("book_id") == b_id)
        u_id = meta.get("user_id", "dc074a61-c488-4fca-afff-eaa39ed429a0") # Default to the active user
        
        # Zoology Strategy.pdf check
        title = "Restored Book"
        # We can try to guess title if it's missing, but we'll use a placeholder or check common files
        # Actually in the screenshot it showed Zoology Strategy.pdf.
        if b_id == "00b209f9-7263-486a-89f0-6d210c349153": title = "Zoology Strategy.pdf"
        elif b_id == "15406eb7-2a4d-475c-8956-6984045746fe": title = "Science Chunks"
        
        # Get first chunk for preview
        try:
            first_chunk = next(c for i, c in enumerate(chunks) if metadata[i].get("book_id") == b_id)
            preview = first_chunk[:250] + "..." if len(first_chunk) > 250 else first_chunk
        except StopIteration:
            preview = "No text content found."

        print(f"Restoring {title} (ID: {b_id}) for User: {u_id}")
        cursor.execute("INSERT OR REPLACE INTO books (id, user_id, title, domain, preview, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                      (b_id, u_id, title, "General", preview, now))
    
    conn.commit()
    print("Database Repair Complete.")
    conn.close()
