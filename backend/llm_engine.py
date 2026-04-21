import tiktoken
from model_manager import manager
from logger import log
from typing import Optional, List
import json

# Initialize encoding for token estimation (using cl100k_base which is standard for most models)
encoding = tiktoken.get_encoding("cl100k_base")

# Model references (actual model is selected by ModelManager based on routing)
MODEL_FAST = "llama-3.1-8b-instant"      # Book RAG + hybrid
MODEL_SMART = "llama-3.3-70b-versatile"   # Pure LLM

# ── EXPERT TIER PROMPTS ──────────────────────────────────────────────────
EXPERT_PROMPTS = {
    "Beginner": """You are an expert science tutor designed to explain complex topics in the simplest possible way.
Your goal is to make the user deeply understand the concept from scratch, even if they have no prior knowledge.

STRICT INSTRUCTIONS:
- Start with a very simple definition (1–2 lines)
- Use real-life analogies wherever possible
- Break down the concept into small logical steps
- Avoid heavy jargon; if used, explain immediately
- Build intuition before giving technical depth
- Use examples frequently
- End with a quick recap

STRUCTURE:
1. Simple Definition
2. Real-life Analogy
3. Step-by-step Explanation
4. Key Terms (explained simply)
5. Practical Example
6. Quick Recap

TONE: Friendly, clear, and engaging. Avoid robotic or overly academic tone.""",

    "Academic": """You are a university-level biotechnology professor generating high-quality, exam-oriented answers.
Your goal is to provide detailed, structured, and comprehensive answers suitable for MSc-level exams.

STRICT INSTRUCTIONS:
- Cover the topic completely (no missing subtopics)
- Use detailed bullet points (each point must contain explanation, not just keywords)
- Maintain logical flow and clarity
- Include definitions, mechanisms, and significance
- Include diagrams in text form (flowcharts, arrows, steps)
- Add scientific terminology where necessary
- Provide examples and applications
- Include limitations and future perspectives if relevant

STRUCTURE:
1. Definition / Introduction
2. Core Concept Explanation
3. Mechanism / Process (step-wise)
4. Key Components / Factors
5. Applications / Importance
6. Limitations
7. Conclusion / Summary
8. Flowchart / Diagram Representation (Textual)

FORMAT RULES:
- Use headings and subheadings
- Use bullet points with 2–3 lines explanation each
- Avoid one-word bullets
- Maintain exam-ready format

TONE: Formal but clear, precise and informative.""",

    "Research": """You are a senior scientific researcher and domain expert generating publication-level insights.
Your goal is to provide deep, critical, and research-oriented explanations that go beyond textbooks.

STRICT INSTRUCTIONS:
- Provide advanced conceptual depth
- Include molecular mechanisms and underlying biology
- Integrate current research trends and discoveries
- Critically analyze the topic (not just describe)
- Discuss challenges, limitations, and controversies
- Include comparisons with alternative approaches
- Suggest future research directions
- If applicable, connect to real-world research or technologies

STRUCTURE:
1. Advanced Introduction (context + significance)
2. Deep Mechanistic Insight
3. Current Research & Developments
4. Critical Analysis (strengths vs limitations)
5. Comparative Perspective
6. Challenges & Open Questions
7. Future Directions / Innovations
8. Integration with related fields

TONE: Analytical, precise, and authoritative. Avoid oversimplification. Assume user has prior knowledge."""
}

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

async def generate_llm_only(question: str, mode: str, domain: str, level: str = "Academic") -> str:
    structure_rules = """
Structure the answer into clear sections.

Rules:
- Each section must start with a short heading (concise, 3–6 words)
- Followed by explanation
- Do NOT use markdown symbols like ### or ** for emphasis (keep it clean)
- Do NOT include <think> or hidden reasoning
- Write equations in readable plain format (NO LaTeX, no \\frac, $$, \\, etc.)
- Example Correct: K = ([C]^c × [D]^d) / ([A]^a × [B]^b)
"""

    domain_prompts = {
        "Biology": "Focus on biological processes, flow, and real-life examples. Use clear terminology.",
        "Physics": "Include formulas, variables, units, and real-world applications. Explain each variable clearly.",
        "Chemistry": "Include chemical equations, reaction mechanisms, and symbolic representation.",
        "Science": "Focus on interdisciplinary connections and fundamental principles."
    }

    mode_prompts = {
        "Exam": "You are an exam-focused assistant. Give concise, high-yield answers suitable for scoring. Use bullet-style clarity.",
        "Concept": "You are a conceptual teacher. Explain in a simple, intuitive way. Focus on understanding.",
        "Expert": EXPERT_PROMPTS.get(level, EXPERT_PROMPTS["Academic"])
    }

    system_prompt = (
        f"You are SciAI, the world's most advanced scientific research assistant.\n"
        f"{mode_prompts.get(mode, 'Provide an expert response.')}\n"
        f"Domain: {domain}. {domain_prompts.get(domain, '')}\n"
        f"{structure_rules}\n"
        "Ensure the response is PREMIUM, professional, and scientifically accurate."
    )

    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": question},
    ]
    
    result, provider = await manager.generate(
        messages=messages,
        query_type="auto",
        query_text=question,
        max_tokens=1500,
        temperature=0.7,
        use_smart_model=True,
    )
    log.info(f"LLM: Pure generation via {provider}")
    return result

async def analyze_image(image_b64: str, question: Optional[str] = None) -> str:
    """Analyze a scientific image using Gemini Flash."""
    prompt = (
        "Analyze this scientific image carefully.\n"
        "If it contains a scientific question or diagram:\n"
        "- Extract and solve the problem\n"
        "- Format with clear headings (No symbols like ### or **)\n"
        "- No LaTeX formatting (use plain readable text)\n"
        "Structure: Observation, Analysis, Conclusion."
    )
    if question:
        prompt += f"\n\nUser Question: {question}"

    # Use the model manager to find a gemini provider
    gemini_provider = manager.providers.get("gemini_flash")
    if not gemini_provider:
        return "Error: Gemini Flash provider not configured on backend."
    
    key = gemini_provider.get_next_key()
    if not key:
        return "Error: All Gemini keys are in cooldown."

    await manager._ensure_client()
    url = f"{gemini_provider.base_url}/models/gemini-1.5-flash:generateContent?key={key.key}"
    
    body = {
        "contents": [{
            "parts": [
                {"text": prompt},
                {"inlineData": {"mimeType": "image/jpeg", "data": image_b64}}
            ]
        }],
        "generationConfig": {"temperature": 0.4, "maxOutputTokens": 1024}
    }

    try:
        resp = await manager.client.post(url, json=body)
        if resp.status_code != 200:
            return f"Error: Gemini API failure ({resp.status_code})"
        
        data = resp.json()
        content = data["candidates"][0]["content"]["parts"][0]["text"]
        return content.strip()
    except Exception as e:
        return f"Error analyzing image: {str(e)}"

async def generate_hybrid_answer(question: str, context: str, mode: str, domain: str, level: str = "Academic") -> str:
    expert_spec = f"\n{EXPERT_PROMPTS.get(level, EXPERT_PROMPTS['Academic'])}" if mode == "Expert" else f"Mode: {mode}."
    system = (
        f"You are SciAI, an expert research assistant specialized in {domain}.\n"
        f"Synthesize the provided context into a refined summary. Do not just quote.\n"
        f"Structure with headings, facts, and a concluding takeaway. {expert_spec}"
    )
    trimmed = trim_context(context, 4000)
    prompt = f"CONTEXT:\n{trimmed}\n\nQUESTION: {question}\n\nSynthesized Answer:"
    messages = [
        {"role": "system", "content": system},
        {"role": "user", "content": prompt},
    ]
    
    result, provider = await manager.generate(
        messages=messages,
        query_type="scale",
        query_text=question,
        max_tokens=1500,
        temperature=0.15,
        has_context=True,
    )
    log.info(f"LLM: Hybrid generation via {provider}")
    return result

async def generate_book_answer(question: str, context: str, mode: str, domain: str, level: str = "Academic") -> str:
    expert_spec = f"\n{EXPERT_PROMPTS.get(level, EXPERT_PROMPTS['Academic'])}" if mode == "Expert" else f"Mode: {mode}."
    system = (
        f"You are SciAI, a precision precision assistant analyzing a user's document.\n"
        f"Rules: Only use the provided excerpts. No outside knowledge. If missing, say so.\n"
        f"Format: Key Findings, Detailed Analysis. {expert_spec}\n"
        f"Domain: {domain}."
    )
    trimmed = trim_context(context, 5000)
    prompt = f"EXCERPTS:\n{trimmed}\n\nUSER QUESTION: {question}\n\nPrecision Analysis:"
    messages = [
        {"role": "system", "content": system},
        {"role": "user", "content": prompt},
    ]
    
    result, provider = await manager.generate(
        messages=messages,
        query_type="long_context",
        query_text=question,
        max_tokens=2000,
        temperature=0.1,
        is_book=True,
    )
    log.info(f"LLM: Book generation via {provider}")
    return result

async def generate_questions(
    mode: str, topic: str, domain: str, level: int, count: int = 4,
    context_chunks: List[str] = None, previous_questions: List[dict] = None
) -> str:
    system = """You are SciAI's intelligent question generation engine.
You generate HIGH-QUALITY, NON-REPETITIVE, DOMAIN-AWARE scientific questions.

STRICT RULES:
- Never repeat previously generated questions (ensure variation in framing, depth, and angle)
- Questions must be scientifically accurate and concept-driven
- Avoid generic textbook phrasing
- Adapt difficulty dynamically based on level
- Use retrieved context if available, else expand using domain knowledge
- Questions must feel like they are from a top-tier competitive exam or research discussion

--------------------------------------

INPUT PARAMETERS:
- mode: {quiz | test}
- topic: user input topic (can be broad or specific)
- domain: {physics, chemistry, biology, general_science, interdisciplinary, other}
- level: {10, 30, 50, 80, 100, 500, 1000}
- context_chunks: retrieved FAISS data (optional)
- previous_questions: list of last generated questions (to avoid repetition)

--------------------------------------

1. MODE DIFFERENTIATION:

QUIZ MODE (Gaming / MCQ):
- Format: STRICTLY Multiple Choice Questions (MCQ).
- Options: ALWAYS exactly 4 options (A, B, C, D).
- Scope: Conceptual, quick-thinking, fact-based.

TEST MODE (Theoretical / Insightful):
- Format: STRICTLY Open-ended / Theoretical questions.
- Options: NULL (Do not provide options).
- Scope: In-depth analysis, mechanism exploration, "how" and "why" questions.
- Requires user to write a detailed answer.

--------------------------------------

3. DIFFICULTY SCALING:

Level 10:
- Basic definitions, simple recall
Level 30:
- Understanding + basic application
Level 50:
- Concept integration
Level 80:
- Multi-step reasoning
Level 100:
- Advanced application + tricky concepts
Level 500 (Legendary):
- Cross-domain + edge cases
- Real-world + research-level thinking
Level 1000 (GOAT):
- Research-grade
- Hypothetical scenarios
- Requires deep conceptual synthesis

--------------------------------------

4. QUESTION VARIATION LOGIC:
Ensure variation in:
- Cognitive skill (recall, application, analysis)
- Structure (direct, case-based, reverse logic)
- Context (real-world, experimental, theoretical)
Avoid:
- Reworded duplicates
- Same pattern repetition

--------------------------------------

5. OUTPUT FORMAT (STRICT JSON):
{
  "mode": "quiz/test",
  "topic": "expanded/interpreted topic",
  "domain": "final inferred domain",
  "level": number,
  "questions": [
    {
      "id": "unique_id",
      "type": "open|mcq|assertion_reason|numerical|case",
      "question": "text",
      "options": ["A", "B", "C", "D"] # (only for MCQ)
      "answer": "correct answer or explanation",
      "explanation": "deep conceptual explanation",
      "difficulty_tag": "easy|medium|hard|legendary|goat",
      "concepts": ["concept1", "concept2"],
      "variation_tag": "application|analysis|conceptual|edge_case"
    }
  ]
}

--------------------------------------

6. CONTEXT USAGE (FAISS):
If context_chunks available:
- Prioritize them
- Generate questions grounded in retrieved knowledge
Else:
- Use general scientific knowledge
- Expand intelligently

--------------------------------------

7. NON-REPETITION:
Compare with previous_questions:
- Avoid similar structure or concept overlap
- Generate fresh angles

--------------------------------------

8. BONUS INTELLIGENCE:
- Occasionally include:
  - Trick questions
  - Misconception-based questions
  - Real-world scenarios

OUTPUT ONLY JSON. NO EXTRA TEXT. MAKE THE UI/UX PREMIUM"""

    prompt_parts = [
        f"MODE: {mode}",
        f"TOPIC: {topic}",
        f"DOMAIN: {domain}",
        f"LEVEL: {level}",
        f"COUNT: Generate exactly {count} distinct questions."
    ]
    
    if context_chunks:
        trimmed_context = trim_context("\n".join(context_chunks), 3000)
        prompt_parts.append(f"CONTEXT_CHUNKS:\n{trimmed_context}")
        
    if previous_questions:
        # Pass a compressed view of previous questions
        try:
            prev_str = json.dumps([{"q": pq.get("question"), "t": pq.get("type")} for pq in previous_questions])
            prompt_parts.append(f"PREVIOUS_QUESTIONS:\n{prev_str}")
        except Exception:
            pass
            
    prompt = "\n\n".join(prompt_parts) + "\n\nGenerate strictly valid JSON."
    messages = [
        {"role": "system", "content": system},
        {"role": "user", "content": prompt},
    ]

    result, provider = await manager.generate(
        messages=messages,
        query_type="deep",
        query_text=topic,
        max_tokens=3000,
        temperature=0.7,
        json_mode=True,
    )
    log.info(f"LLM: Question generation via {provider}")
    return result

async def evaluate_theory_answer(question: str, user_answer: str, correct_answer: str) -> str:
    """
    Evaluates a user's theoretical answer against the expert answer.
    Provides conceptual corrections and improvement suggestions.
    """
    system = (
        "You are SciAI's Grading Engine. Analyze the user's theoretical answer.\n"
        "1. Compare it with the expert answer.\n"
        "2. Identify conceptual gaps or errors.\n"
        "3. Provide exactly 3 short, actionable suggestions for improvement.\n"
        "4. Tone: Encouraging, academic, and precise.\n"
        "Format: [Corrections]\n- ...\n\n[Improvement Suggestions]\n- ..."
    )
    
    prompt = (
        f"QUESTION: {question}\n"
        f"EXPERT ANSWER: {correct_answer}\n"
        f"USER ANSWER: {user_answer}\n\n"
        "Analyze and provide feedback:"
    )
    messages = [
        {"role": "system", "content": system},
        {"role": "user", "content": prompt},
    ]

    result, provider = await manager.generate(
        messages=messages,
        query_type="speed",
        query_text=question,
        max_tokens=1000,
        temperature=0.3,
    )
    log.info(f"LLM: Evaluation via {provider}")
    return result

async def evaluate_theory_answer_detailed(question: str, user_answer: str, correct_answer: str) -> str:
    """
    Enhanced evaluation returning structured JSON with:
    - accuracy, depth, structure scores (0-100)
    - missing_points, good_points lists
    - model_answer
    - overall_feedback
    """
    system = """You are SciAI's Advanced AI Grading Engine — the most precise science answer evaluator.

TASK: Evaluate the user's theoretical answer against the expert answer.

SCORING CRITERIA:
- accuracy (0-100): How factually correct is the answer? Check key terms, processes, and mechanisms.
- depth (0-100): How deeply does the answer explore the topic? Check for detail, examples, and nuanced reasoning.
- structure (0-100): How well-organized is the answer? Check for logical flow, headings, and coherence.

ANALYSIS TASKS:
1. Identify SPECIFIC missing points (max 3)
2. Identify SPECIFIC good points the user nailed (max 3)
3. Generate a concise MODEL ANSWER (max 150 words)
4. Write a brief encouraging overall_feedback (max 2 sentences)

OUTPUT FORMAT (strict JSON):
{
  "accuracy": number,
  "depth": number,
  "structure": number,
  "missing_points": ["point 1", "point 2"],
  "good_points": ["point 1", "point 2"],
  "model_answer": "ideal concise answer",
  "overall_feedback": "encouraging summary"
}

OUTPUT ONLY JSON. NO EXTRA TEXT."""
    
    prompt = (
        f"QUESTION: {question}\n\n"
        f"EXPERT ANSWER: {correct_answer}\n\n"
        f"USER ANSWER: {user_answer}\n\n"
        "Evaluate and return JSON:"
    )
    messages = [
        {"role": "system", "content": system},
        {"role": "user", "content": prompt},
    ]

    try:
        result, provider = await manager.generate(
            messages=messages,
            query_type="speed",
            query_text=question,
            max_tokens=1500,
            temperature=0.2,
            json_mode=True,
        )
        log.info(f"LLM: Detailed evaluation via {provider}")
        return result
    except Exception as e:
        log.error(f"LLM: Detailed evaluation failed: {e}")
        return json.dumps({
            "accuracy": 0, "depth": 0, "structure": 0,
            "missing_points": ["Evaluation failed"],
            "good_points": [],
            "model_answer": correct_answer[:300] if correct_answer else "Not available",
            "overall_feedback": f"Evaluation error: {str(e)}"
        })
