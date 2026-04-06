import tiktoken
from groq import AsyncGroq
from config import settings
from logger import log
from typing import Optional, List

# Initialize encoding for token estimation (using cl100k_base which is standard for most models)
encoding = tiktoken.get_encoding("cl100k_base")

# Model selection
MODEL_FAST = "llama-3.1-8b-instant"      # Book RAG + hybrid
MODEL_SMART = "llama-3.3-70b-versatile"   # Pure LLM

def get_groq_client():
    if not settings.GROQ_API_KEY or settings.GROQ_API_KEY == "your_key_here":
        return None
    return AsyncGroq(api_key=settings.GROQ_API_KEY)

def count_tokens(text: str) -> int:
    return len(encoding.encode(text))

def trim_context(context: str, max_tokens: int = 4000) -> str:
    tokens = encoding.encode(context)
    if len(tokens) <= max_tokens:
        return context
    
    # Trim to head (most relevant chunks are usually at the top)
    trimmed = tokens[:max_tokens]
    result = encoding.decode(trimmed)
    log.info(f"LLM: Context trimmed from {len(tokens)} to {max_tokens} tokens.")
    return result

async def generate_llm_only(question: str, mode: str, domain: str) -> str:
    client = get_groq_client()
    if not client: return "Error: GROQ_API_KEY missing."
    
    system = f"You are SciAI, an expert research assistant for {domain}. Answer concisely."
    try:
        resp = await client.chat.completions.create(
            model=MODEL_SMART,
            messages=[{"role": "system", "content": system}, {"role": "user", "content": question}],
            temperature=0.7, max_tokens=1024
        )
        return resp.choices[0].message.content.strip()
    except Exception as e:
        log.error(f"LLM: Pure generation failed: {e}")
        return f"Communication error: {str(e)}"

async def generate_hybrid_answer(question: str, context: str, mode: str, domain: str) -> str:
    client = get_groq_client()
    if not client: return "Error: GROQ_API_KEY missing."
    
    system = (
        f"You are SciAI, an expert research assistant specialized in {domain}.\n"
        f"Synthesize the provided context into a refined summary. Do not just quote.\n"
        f"Structure with headings, facts, and a concluding takeaway. Mode: {mode}."
    )
    trimmed = trim_context(context, 4000)
    prompt = f"CONTEXT:\n{trimmed}\n\nQUESTION: {question}\n\nSynthesized Answer:"
    
    try:
        resp = await client.chat.completions.create(
            model=MODEL_FAST,
            messages=[{"role": "system", "content": system}, {"role": "user", "content": prompt}],
            temperature=0.15, max_tokens=1500
        )
        return resp.choices[0].message.content.strip()
    except Exception as e:
        log.error(f"LLM: Hybrid generation failed: {e}")
        return f"Error: {str(e)}"

async def generate_book_answer(question: str, context: str, mode: str, domain: str) -> str:
    client = get_groq_client()
    if not client: return "Error: GROQ_API_KEY missing."

    system = (
        f"You are SciAI, a precision precision assistant analyzing a user's document.\n"
        f"Rules: Only use the provided excerpts. No outside knowledge. If missing, say so.\n"
        f"Format: Key Findings, Detailed Analysis (Mode: {mode}), Evidence (Quotes), Summary.\n"
        f"Domain: {domain}."
    )
    trimmed = trim_context(context, 5000)
    prompt = f"EXCERPTS:\n{trimmed}\n\nUSER QUESTION: {question}\n\nPrecision Analysis:"
    
    try:
        resp = await client.chat.completions.create(
            model=MODEL_FAST,
            messages=[{"role": "system", "content": system}, {"role": "user", "content": prompt}],
            temperature=0.1, max_tokens=2000
        )
        return resp.choices[0].message.content.strip()
    except Exception as e:
        log.error(f"LLM: Book generation failed: {e}")
        return f"Selection error: {str(e)}"
