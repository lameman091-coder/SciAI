import os
from groq import Groq
from dotenv import load_dotenv

load_dotenv()

def get_groq_client():
    api_key = os.getenv("GROQ_API_KEY")
    if not api_key or api_key == "your_key_here" or api_key.strip() == "":
        return None
    return Groq(api_key=api_key)

def generate_llm_only(question: str, mode: str, domain: str) -> str:
    """
    Generates a pure LLM response using Groq (no context).
    """
    client = get_groq_client()
    if not client:
        return "Error: GROQ_API_KEY not configured."
        
    system_prompt = (
        "You are SciAI, an expert research assistant. Answer the user's question clearly and concisely."
    )
    
    try:
        completion = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": question}
            ],
            temperature=0.7,
            max_tokens=1024,
        )
        answer = completion.choices[0].message.content.strip()
        if not answer:
            print("[SciAI] WARNING: Groq returned empty answer for LLM-only.")
        return answer
    except Exception as e:
        return f"Error communicating with LLM: {str(e)}"

def generate_hybrid_answer(question: str, context: str, mode: str, domain: str) -> str:
    """
    Generates a synthesized answer based on the provided context (Smart RAG).
    """
    client = get_groq_client()
    if not client:
        return "Error: GROQ_API_KEY not configured."
        
    system_prompt = (
        f"You are SciAI, an expert research assistant specialized in {domain}.\n"
        f"Answer the user's question by SYNTHESIZING and SUMMARIZING the provided context.\n"
        f"DO NOT dump raw text snippets. Provide a refined, coherent research summary.\n"
        f"Always start your answer with the exact string: 'Retrieve Augmented Reality Check' followed by clear headings.\n"
        f"Mode: {mode}. Adapt your explanation depth to this mode."
    )
    
    prompt = f"CONTEXT:\n{context}\n\nQUESTION:\n{question}\n\nFinal Synthesized Answer:"
    
    try:
        completion = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": prompt}
            ],
            temperature=0.1,
            max_tokens=1024,
        )
        answer = completion.choices[0].message.content.strip()
        if not answer:
            print("[SciAI] WARNING: Groq returned empty answer for Hybrid.")
        return answer
    except Exception as e:
        return f"Error communicating with LLM: {str(e)}"

def generate_book_answer(question: str, context: str, mode: str, domain: str) -> str:
    """
    Generates an answer STRICTLY from the uploaded book/PDF context.
    Does NOT use general knowledge — only the provided document chunks.
    """
    client = get_groq_client()
    if not client:
        return "Error: GROQ_API_KEY not configured."
        
    system_prompt = (
        f"You are SciAI, a research assistant analyzing a specific uploaded document.\n"
        f"You MUST answer the user's question ONLY using the provided document excerpts below.\n"
        f"DO NOT use your general knowledge. DO NOT make up information not found in the context.\n"
        f"If the context does not contain enough information to answer, say: "
        f"'The uploaded document does not contain specific information about this topic.'\n"
        f"Provide a clear, well-structured answer with relevant quotes or references from the document.\n"
        f"Domain: {domain}. Mode: {mode}."
    )
    
    prompt = f"DOCUMENT EXCERPTS:\n{context}\n\nUSER QUESTION:\n{question}\n\nAnswer based ONLY on the document excerpts above:"
    
    try:
        completion = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": prompt}
            ],
            temperature=0.1,
            max_tokens=1024,
        )
        answer = completion.choices[0].message.content.strip()
        if not answer:
            print("[SciAI] WARNING: Groq returned empty answer for Book RAG.")
        return answer
    except Exception as e:
        return f"Error communicating with LLM: {str(e)}"
