import PyPDF2
import re
import asyncio
from logger import log
from typing import List

async def process_pdf(file_path: str, chunk_size=600, overlap=100) -> List[str]:
    """
    Asynchronous PDF text extraction and chunking.
    Uses blocking PyPDF2 in a thread pool to avoid stalling the event loop.
    """
    def extract_sync():
        try:
            with open(file_path, 'rb') as f:
                reader = PyPDF2.PdfReader(f)
                text = ""
                for page in reader.pages:
                    extracted = page.extract_text()
                    if extracted:
                        text += extracted + "\n"
            return text
        except Exception as e:
            log.error(f"PDF Extraction failed: {e}")
            return ""

    # Run blocking I/O in a thread pool
    text = await asyncio.to_thread(extract_sync)
    
    if not text.strip():
        log.warning(f"PDF Extraction: No text found in {file_path}")
        return []
        
    log.info(f"PDF Extraction: Extracted {len(text)} characters from {file_path}")

    # Clean up PDF artifacts
    text = re.sub(r'\s+', ' ', text)  # Normalize whitespace
    text = re.sub(r'-\s+', '', text)  # Rejoin hyphenated words across lines
    text = text.strip()
    
    # Chunking by characters with overlap
    # (Word-based chunking is harder to relate to LLM context windows precisely)
    chunks = []
    start = 0
    while start < len(text):
        end = start + chunk_size
        chunk = text[start:end]
        
        # Don't break in the middle of a word if possible
        if end < len(text):
            last_space = chunk.rfind(' ')
            if last_space != -1:
                chunk = chunk[:last_space]
                end = start + last_space
        
        chunks.append(chunk.strip())
        start = end - overlap
        
    log.info(f"PDF: Processed {len(chunks)} chunks from {file_path}")
    return [c for c in chunks if len(c) > 50]
