import PyPDF2
import re

def process_pdf(file_path, chunk_size=800, overlap=200):
    with open(file_path, 'rb') as f:
        reader = PyPDF2.PdfReader(f)
        text = ""
        for page in reader.pages:
            extracted = page.extract_text()
            if extracted:
                text += extracted + "\n"
    
    # Clean up PDF artifacts: excessive whitespace, broken words, etc.
    text = re.sub(r'\s+', ' ', text)  # Normalize whitespace
    text = re.sub(r'-\s+', '', text)  # Rejoin hyphenated words across lines
    text = text.strip()
    
    if not text:
        return []
    
    words = text.split()
    chunks = []
    i = 0
    while i < len(words):
        chunk = " ".join(words[i : i + chunk_size])
        chunk = chunk.strip()
        if chunk and len(chunk) > 20:  # Skip tiny/empty chunks
            chunks.append(chunk)
        i += (chunk_size - overlap)
        
    return chunks
