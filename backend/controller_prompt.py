"""
SciAI Controller Prompt — Decision Engine
Routes user queries to the correct mode + domain before the main LLM call.
Lightweight, fast (~200-500ms), cached.
"""

import json
from model_manager import manager
from logger import log
from typing import Optional
import re

# ── CONTROLLER SYSTEM PROMPT ──────────────────────────────────────────────────
CONTROLLER_SYSTEM = """You are the Controller of SciAI system.

ROLE:
- Decide how the system should respond to the user
- You DO NOT answer the question directly
- You ONLY route the request and provide a short butler-style acknowledgment

AVAILABLE MODES:
1. Exam → structured, point-wise answers for exam preparation
2. Concept → deep understanding, explanations, "how does X work"
3. Expert → advanced reasoning, research-level analysis
4. Quiz → MCQ question generation for practice
5. Test → theory/open-ended question generation
6. Article → research paper analysis and discovery
7. Normal → casual conversation, greetings, meta-questions

AVAILABLE DOMAINS:
- Biology (cells, DNA, genetics, evolution, ecology, anatomy, physiology)
- Physics (forces, energy, motion, atoms, quantum, thermodynamics, optics)
- Chemistry (molecules, reactions, acids, bonds, periodic table, organic)
- Science (general science, earth science, astronomy, interdisciplinary)
- General (non-science, casual, app-related questions)

DECISION RULES:
- If user asks for explanation/concept/how/why → Concept
- If user asks exam-type/structured question → Exam
- If complex/multi-step/research-level → Expert
- If user wants quiz/practice/MCQ → Quiz
- If user wants test/write/theory practice → Test
- If user mentions articles/papers/research discovery → Article
- If casual/greeting/meta → Normal
- Default to Concept if ambiguous scientific question

OUTPUT FORMAT (strict JSON):
{
  "mode": "Exam|Concept|Expert|Quiz|Test|Article|Normal",
  "domain": "Biology|Physics|Chemistry|Science|General",
  "companion_message": "Short butler-style message (max 15 words)",
  "confidence": 0.0-1.0
}

COMPANION MESSAGE RULES:
- Keep under 15 words
- Friendly but professional
- Reference the detected mode naturally
- Examples:
  - "Got it. Breaking this down in Concept Mode."
  - "Launching Quiz Mode on Genetics. Ready?"
  - "Switching to Expert analysis. Stand by."
  - "I'll find recent research articles on this."

OUTPUT ONLY JSON. NO EXTRA TEXT."""


# ── KEYWORD FALLBACK ──────────────────────────────────────────────────────────
def _keyword_fallback(query: str) -> dict:
    """Fast keyword-based fallback when LLM routing fails."""
    q = query.lower()

    # Domain detection
    domain = "General"
    bio_kw = ["cell", "dna", "biology", "plant", "animal", "protein", "gene", "evolution",
              "ecology", "mitosis", "meiosis", "enzyme", "photosynthesis", "organism"]
    phy_kw = ["force", "gravity", "physics", "energy", "atom", "light", "motion", "matter",
              "velocity", "acceleration", "quantum", "wave", "electric", "magnetic"]
    chem_kw = ["chemical", "molecule", "chemistry", "acid", "reaction", "bond", "periodic",
               "element", "compound", "organic", "inorganic", "pH", "oxidation"]
    sci_kw = ["earth", "space", "science", "nature", "climate", "geology", "astronomy"]

    if any(k in q for k in bio_kw): domain = "Biology"
    elif any(k in q for k in phy_kw): domain = "Physics"
    elif any(k in q for k in chem_kw): domain = "Chemistry"
    elif any(k in q for k in sci_kw): domain = "Science"

    # Mode detection
    mode = "Concept"
    if any(k in q for k in ["quiz", "ask me", "trivia", "mcq"]): mode = "Quiz"
    elif any(k in q for k in ["test", "theory practice", "write answer"]): mode = "Test"
    elif any(k in q for k in ["exam", "marks", "paper", "point-wise"]): mode = "Exam"
    elif any(k in q for k in ["deep", "advanced", "expert", "research-level"]): mode = "Expert"
    elif any(k in q for k in ["article", "paper", "journal", "publish"]): mode = "Article"
    elif any(k in q for k in ["hi", "hello", "hey", "what can you do", "help"]): mode = "Normal"
    elif any(k in q for k in ["explain", "what is", "how", "why", "concept"]): mode = "Concept"

    messages = {
        "Concept": f"Breaking this down in Concept Mode for {domain}.",
        "Exam": f"Structuring an exam-ready answer on {domain}.",
        "Expert": f"Switching to Expert analysis. Stand by.",
        "Quiz": f"Launching Quiz Mode on {domain}. Ready?",
        "Test": f"Setting up theory practice on {domain}.",
        "Article": f"Searching research articles for you.",
        "Normal": "How can I help you navigate SciAI?"
    }

    return {
        "mode": mode,
        "domain": domain,
        "companion_message": messages.get(mode, "Processing your request."),
        "confidence": 0.6
    }


# ── MAIN CLASSIFY FUNCTION ───────────────────────────────────────────────────
_route_cache: dict = {}
MAX_ROUTE_CACHE = 500

async def classify(query: str) -> dict:
    """
    Classify user query into mode + domain using LLM Controller.
    Falls back to keyword detection on failure.
    Returns: {mode, domain, companion_message, confidence}
    """
    # Check cache
    cache_key = query.strip().lower()[:100]
    if cache_key in _route_cache:
        log.info(f"Controller: Cache hit for '{cache_key[:30]}...'")
        return _route_cache[cache_key]

    # Short queries get keyword fallback (save LLM cost)
    if len(query.strip()) < 4:
        result = _keyword_fallback(query)
        _route_cache[cache_key] = result
        return result

    try:
        messages = [
            {"role": "system", "content": CONTROLLER_SYSTEM},
            {"role": "user", "content": query}
        ]

        raw_result, provider = await manager.generate(
            messages=messages,
            query_type="speed",
            query_text=query,
            max_tokens=150,
            temperature=0.1,
            json_mode=True,
        )

        log.info(f"Controller: Routing via {provider}")

        # Parse JSON response
        result = json.loads(raw_result)

        # Validate required fields
        valid_modes = {"Exam", "Concept", "Expert", "Quiz", "Test", "Article", "Normal"}
        valid_domains = {"Biology", "Physics", "Chemistry", "Science", "General"}

        if result.get("mode") not in valid_modes:
            result["mode"] = "Concept"
        if result.get("domain") not in valid_domains:
            result["domain"] = "General"
        if not result.get("companion_message"):
            result["companion_message"] = f"Processing in {result['mode']} Mode."
        if not isinstance(result.get("confidence"), (int, float)):
            result["confidence"] = 0.85

        # Cache result
        if len(_route_cache) > MAX_ROUTE_CACHE:
            _route_cache.clear()
        _route_cache[cache_key] = result

        return result

    except json.JSONDecodeError as e:
        log.warning(f"Controller: JSON parse failed: {e}. Using keyword fallback.")
        result = _keyword_fallback(query)
        _route_cache[cache_key] = result
        return result

    except Exception as e:
        log.error(f"Controller: LLM routing failed: {e}. Using keyword fallback.")
        result = _keyword_fallback(query)
        _route_cache[cache_key] = result
        return result
