import os
from groq import Groq
from dotenv import load_dotenv

load_dotenv()

# Model selection for token budget management
# llama-3.1-8b-instant: Higher TPM limits, faster, great for RAG with context
# llama-3.3-70b-versatile: Smarter, lower TPM limits, for pure LLM
MODEL_FAST = "llama-3.1-8b-instant"      # Book RAG + hybrid (high context)
MODEL_SMART = "llama-3.3-70b-versatile"   # Pure LLM (no context overhead)

def get_groq_client():
    api_key = os.getenv("GROQ_API_KEY")
    if not api_key or api_key == "your_key_here" or api_key.strip() == "":
        return None
    return Groq(api_key=api_key)

def estimate_tokens(text: str) -> int:
    """Rough token estimation: ~4 chars per token for English text."""
    return len(text) // 4

def trim_context(context: str, max_context_tokens: int = 4000) -> str:
    """Trim context to fit within token budget. Keeps most relevant (first) chunks."""
    estimated = estimate_tokens(context)
    if estimated <= max_context_tokens:
        return context
    
    # Split by chunk separators and keep as many as fit
    chunks = context.split("\n\n")
    trimmed_parts = []
    running_tokens = 0
    for chunk in chunks:
        chunk_tokens = estimate_tokens(chunk)
        if running_tokens + chunk_tokens > max_context_tokens:
            break
        trimmed_parts.append(chunk)
        running_tokens += chunk_tokens
    
    result = "\n\n".join(trimmed_parts)
    print(f"[SciAI] Context trimmed: {estimated} → {estimate_tokens(result)} estimated tokens ({len(trimmed_parts)}/{len(chunks)} chunks kept)")
    return result

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
            model=MODEL_SMART,
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
        f"DO NOT dump raw text snippets. Provide a refined, coherent research summary.\n\n"
        f"RESPONSE FORMAT:\n"
        f"- Start with a clear overview paragraph\n"
        f"- Use descriptive section headings to organize your answer\n"
        f"- Include specific facts, data, and findings from the context\n"
        f"- End with a brief conclusion or key takeaway\n\n"
        f"Mode: {mode}. Adapt your explanation depth to this mode."
    )
    
    # Trim context to fit token budget
    trimmed_context = trim_context(context, max_context_tokens=4000)
    prompt = f"CONTEXT:\n{trimmed_context}\n\nQUESTION:\n{question}\n\nFinal Synthesized Answer:"
    
    try:
        completion = client.chat.completions.create(
            model=MODEL_FAST,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": prompt}
            ],
            temperature=0.15,
            max_tokens=1500,
        )
        answer = completion.choices[0].message.content.strip()
        if not answer:
            print("[SciAI] WARNING: Groq returned empty answer for Hybrid.")
        return answer
    except Exception as e:
        return f"Error communicating with LLM: {str(e)}"

def generate_book_answer(question: str, context: str, mode: str, domain: str) -> str:
    """
    Generates a DETAILED, well-structured answer STRICTLY from the uploaded book/PDF context.
    Does NOT use general knowledge — only the provided document chunks.
    """
    client = get_groq_client()
    if not client:
        return "Error: GROQ_API_KEY not configured."
    
    # Adapt depth instruction based on mode
    depth_instruction = {
        "Exam": "Provide exam-ready detail: cover definitions, mechanisms, and key facts from the document.",
        "Concept": "Explain the concepts found in the document clearly with examples drawn from the text.",
        "Expert": "Provide research-level depth with precise technical details, data points, and methodology from the document.",
        "Library": "Provide a comprehensive, well-organized analysis of what the document says about this topic."
    }.get(mode, "Provide a thorough, well-organized answer.")
        
    system_prompt = (
        f"You are SciAI, a precision research assistant analyzing a user's uploaded document.\n\n"
        f"CRITICAL RULES:\n"
        f"1. Answer ONLY using the provided document excerpts. DO NOT use general knowledge.\n"
        f"2. DO NOT fabricate or hallucinate information not present in the excerpts.\n"
        f"3. If the excerpts don't cover the topic, explicitly state: 'The uploaded document does not contain specific information about this topic.'\n\n"
        f"RESPONSE FORMAT — You MUST structure your answer with these sections:\n"
        f"Key Findings\n"
        f"Write 2-4 bullet points summarizing the most important points found in the document about this topic.\n\n"
        f"Detailed Analysis\n"
        f"Provide a thorough explanation based on the document content. Include specific details, data points, "
        f"terminology, and relationships described in the text. Be comprehensive.\n\n"
        f"Document Evidence\n"
        f"Quote or closely paraphrase 2-3 relevant passages from the document that support your answer. "
        f"Use quotation marks for direct quotes.\n\n"
        f"Summary\n"
        f"A concise 2-3 sentence conclusion synthesizing the key takeaway.\n\n"
        f"DEPTH: {depth_instruction}\n"
        f"Domain: {domain}."
    )
    
    # Trim context to fit token budget (book queries can be large)
    trimmed_context = trim_context(context, max_context_tokens=5000)
    
    prompt = (
        f"DOCUMENT EXCERPTS (from user's uploaded PDF):\n"
        f"---BEGIN EXCERPTS---\n{trimmed_context}\n---END EXCERPTS---\n\n"
        f"USER QUESTION: {question}\n\n"
        f"Analyze the document excerpts above and provide a detailed, well-structured answer:"
    )
    
    try:
        completion = client.chat.completions.create(
            model=MODEL_FAST,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": prompt}
            ],
            temperature=0.15,
            max_tokens=1500,
        )
        answer = completion.choices[0].message.content.strip()
        if not answer:
            print("[SciAI] WARNING: Groq returned empty answer for Book RAG.")
            return "The document analysis returned an empty result. Please try rephrasing your question."
        return answer
    except Exception as e:
        return f"Error communicating with LLM: {str(e)}"
