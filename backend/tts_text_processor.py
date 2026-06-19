"""
SciAI TTS Text Processor
─────────────────────────
Optimizes raw AI answers into natural, speech-ready text using
the existing LLM model_manager. Supports voice personality styles,
mode-aware processing, and optional SSML output.

Pipeline:  raw_text → speech_optimize → style_adapt → (optional) ssml_wrap
"""

from model_manager import manager
from logger import log
from typing import Optional
import re


# ═══════════════════════════════════════════════════════════════════════════════
# MASTER SPEECH OPTIMIZATION PROMPT
# ═══════════════════════════════════════════════════════════════════════════════

MASTER_SPEECH_PROMPT = """You are a speech optimization engine for an AI learning app (SciAI).

Your task is to convert a written answer into a natural spoken format suitable for high-quality text-to-speech.

STRICT RULES:

1. Conciseness:
- Keep the optimized text length similar to the original input (within 10-20% expansion)
- Do NOT add long introductory or filler phrases unless necessary for context

1. Sentence Structure:
- Break long sentences into shorter ones (8–16 words max)
- Use simple, clear phrasing

2. Natural Flow:
- Add commas and pauses where a human would naturally breathe
- Use brief transitions (e.g., "Now," "So,")

3. Clarity:
- Simplify wording without losing meaning
- Expand abbreviations (e.g., "DNA", "RNA")

4. Formatting for TTS:
- Remove markdown (**, ##, -, etc.)
- Remove all math symbols, LaTeX, and special characters

5. Engagement:
- Add briefly guiding phrases: "Think of it like this...", "Here is the key idea."

OUTPUT:
Return ONLY the speech-optimized version of the text.
Do not explain anything. Do not add notes or meta-commentary."""


# ═══════════════════════════════════════════════════════════════════════════════
# VOICE PERSONALITY STYLE PROMPTS
# ═══════════════════════════════════════════════════════════════════════════════

VOICE_STYLE_PROMPTS = {
    "professor": """Voice Style: Academic Professor (Male)
- Calm, confident, and authoritative tone
- Use structured explanation with clear logical flow""",

    "energetic": """Voice Style: Energetic Guide (Female)
- Faster, more dynamic pace
- Engaging, lively, and motivating phrasing""",

    "storyteller": """Voice Style: Storyteller (Female)
- Warm, expressive, and narrative flow
- Make explanations feel like a story unfolding""",

    "formal_male": """Voice Style: News Anchor (Male)
- Highly professional, crisp, and objective delivery
- Clear enunciation, formal phrasing""",

    "casual_female": """Voice Style: Friendly Assistant (Female)
- Casual, upbeat, extremely conversational
- Use approachable and simple everyday phrasing""",

    "deep_male": """Voice Style: Deep Narrator (Male)
- Slow, resonant, and impactful delivery
- Add gravitas and dramatic pauses""",

    "warm_female": """Voice Style: Warm Mentor (Female)
- Nurturing, patient, and encouraging
- Soft phrasing, highly empathetic tone"""
}


# ═══════════════════════════════════════════════════════════════════════════════
# MODE-AWARE ADAPTATION PROMPTS
# ═══════════════════════════════════════════════════════════════════════════════

MODE_ADAPTATION = {
    "Concept": "Adapt for CONCEPT mode: Simple, clear explanations. Focus on building understanding step by step. Use analogies when helpful.",
    "Exam": "Adapt for EXAM mode: Precise and structured delivery. Emphasize key terms, definitions, and important facts. Be concise but thorough.",
    "Expert": "Adapt for EXPERT mode: Detailed but smooth delivery. Maintain technical accuracy. Use sophisticated vocabulary naturally.",
    "Library": "Adapt for LIBRARY mode: Speak according to the source material layout. Reference the document naturally as if reading and explaining it.",
    "Articles": "Adapt for ARTICLES mode: Read in a researcher's scientific manner. Use academic phrasing. Reference findings and methodology naturally.",
    "Quiz": "Adapt for QUIZ mode: Clear question reading. Emphasize options distinctly. Pause between each option.",
    "Test": "Adapt for TEST mode: Read questions with gravitas. Give the student time to think through pauses."
}


# ═══════════════════════════════════════════════════════════════════════════════
# SSML GENERATION PROMPT
# ═══════════════════════════════════════════════════════════════════════════════

SSML_PROMPT = """You are an SSML generation engine for natural voice synthesis.

Convert the given speech text into SSML format with expressive and human-like delivery.

RULES:

1. Prosody:
- Use <prosody rate="0.95"> for clarity
- Slight pitch variation: +1st for emphasis
- Avoid extreme changes

2. Pauses:
- Add <break time="300ms"/> between major ideas
- Add <break time="150ms"/> for short pauses
- Add <break time="500ms"/> between sections

3. Emphasis:
- Use <emphasis level="moderate"> for key terms only
- Do NOT overuse emphasis (max 3 per paragraph)

4. Flow:
- Ensure smooth transitions between sentences
- Avoid abrupt breaks

5. Clean Output:
- Ensure valid SSML structure
- Wrap everything inside <speak> tags
- Escape special XML characters

OUTPUT:
Return only valid SSML. No explanations."""


# ═══════════════════════════════════════════════════════════════════════════════
# PREMIUM VOICE REFINEMENT (for high-realism engines)
# ═══════════════════════════════════════════════════════════════════════════════

PREMIUM_REFINEMENT_PROMPT = """You are preparing text for premium AI voice synthesis (high realism).

RULES:

1. Natural Speech:
- Use highly conversational tone
- Avoid textbook-style phrasing

2. Emotional Flow:
- Add subtle emotional cues in phrasing
- Use expressive but controlled language

3. Rhythm:
- Balance sentence length variation (mix short and medium)
- Avoid monotony — vary structure

4. Smoothness:
- Ensure sentences connect naturally
- Avoid abrupt topic jumps

5. Listener Experience:
- Make it feel like a human is explaining directly
- Keep engagement high throughout

OUTPUT:
Return only premium speech-ready text."""


# ═══════════════════════════════════════════════════════════════════════════════
# TEXT PROCESSOR CLASS
# ═══════════════════════════════════════════════════════════════════════════════

class TTSTextProcessor:
    """Processes raw text into speech-optimized format using LLM."""

    @staticmethod
    def _clean_for_tts(text: str) -> str:
        """Fast regex-based cleanup before LLM processing."""
        if not text:
            return ""

        cleaned = text
        # Remove markdown formatting
        cleaned = re.sub(r'\*\*(.+?)\*\*', r'\1', cleaned)  # Bold
        cleaned = re.sub(r'\*(.+?)\*', r'\1', cleaned)      # Italic
        cleaned = re.sub(r'#{1,6}\s*', '', cleaned)          # Headers
        cleaned = re.sub(r'`(.+?)`', r'\1', cleaned)         # Inline code
        cleaned = re.sub(r'```[\s\S]*?```', '', cleaned)     # Code blocks

        # Remove LaTeX
        cleaned = re.sub(r'\$\$.*?\$\$', '', cleaned, flags=re.DOTALL)
        cleaned = re.sub(r'\$.*?\$', '', cleaned)
        cleaned = re.sub(r'\\[a-zA-Z]+\{.*?\}', '', cleaned)

        # Remove special symbols that break TTS
        cleaned = re.sub(r'[│┌┐└┘├┤┬┴┼═║╔╗╚╝╠╣╦╩╬]', '', cleaned)
        cleaned = re.sub(r'[→←↑↓⇒⇐]', '', cleaned)
        cleaned = cleaned.replace('•', ',')
        cleaned = cleaned.replace('–', '-')
        cleaned = cleaned.replace('—', ', ')

        # Expand common abbreviations
        cleaned = cleaned.replace(' e.g. ', ' for example ')
        cleaned = cleaned.replace(' i.e. ', ' that is ')
        cleaned = cleaned.replace(' etc.', ' and so on.')
        cleaned = cleaned.replace(' vs. ', ' versus ')
        cleaned = cleaned.replace(' approx. ', ' approximately ')
        cleaned = cleaned.replace(' MSc ', ' Master of Science ')
        cleaned = cleaned.replace(' PhD ', ' Doctor of Philosophy ')
        cleaned = cleaned.replace(' BSc ', ' Bachelor of Science ')
        cleaned = cleaned.replace(' R&D ', ' Research and Development ')
        cleaned = cleaned.replace(' ATP ', ' adenosine triphosphate ')
        cleaned = cleaned.replace(' DNA ', ' D N A ')
        cleaned = cleaned.replace(' RNA ', ' R N A ')

        # Clean excessive whitespace
        cleaned = re.sub(r'\n{3,}', '\n\n', cleaned)
        cleaned = re.sub(r' {2,}', ' ', cleaned)

        return cleaned.strip()

    @staticmethod
    async def optimize_for_speech(
        text: str,
        voice_style: str = "professor",
        mode: str = "Concept",
        use_ssml: bool = False
    ) -> str:
        """
        Full TTS text processing pipeline.
        
        1. Regex cleanup
        2. LLM speech optimization
        3. Voice style adaptation
        4. Optional SSML wrapping
        """
        if not text or len(text.strip()) < 10:
            return text

        # Step 1: Fast regex cleanup
        cleaned = TTSTextProcessor._clean_for_tts(text)

        # For very short texts, skip LLM processing
        if len(cleaned) < 50:
            return cleaned

        # Step 2: Build the master prompt
        style_prompt = VOICE_STYLE_PROMPTS.get(voice_style, VOICE_STYLE_PROMPTS["professor"])
        mode_prompt = MODE_ADAPTATION.get(mode, MODE_ADAPTATION["Concept"])

        system_prompt = f"""{MASTER_SPEECH_PROMPT}

VOICE PERSONALITY:
{style_prompt}

MODE CONTEXT:
{mode_prompt}"""

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": f"Convert this text to speech-ready format:\n\n{cleaned}"},
        ]

        try:
            result, provider = await manager.generate(
                messages=messages,
                query_type="speed",
                query_text="tts optimization",
                max_tokens=min(len(cleaned) * 2, 2000),
                temperature=0.3,
            )
            log.info(f"TTS: Text optimized via {provider} ({len(cleaned)} → {len(result)} chars)")

            optimized = result.strip()
            
            # Step 2.5: Secondary cleanup to catch any markdown the LLM forgot to remove
            optimized = TTSTextProcessor._clean_for_tts(optimized)

            # Step 3: Optional SSML wrapping
            if use_ssml:
                optimized = await TTSTextProcessor._generate_ssml(optimized)

            return optimized

        except Exception as e:
            log.warning(f"TTS: LLM optimization failed ({e}), using regex-cleaned text")
            return cleaned

    @staticmethod
    async def _generate_ssml(text: str) -> str:
        """Wrap speech-optimized text in SSML markup."""
        messages = [
            {"role": "system", "content": SSML_PROMPT},
            {"role": "user", "content": f"Convert to SSML:\n\n{text}"},
        ]

        try:
            result, provider = await manager.generate(
                messages=messages,
                query_type="speed",
                query_text="ssml generation",
                max_tokens=2000,
                temperature=0.2,
            )

            ssml = result.strip()
            # Validate basic SSML structure
            if "<speak>" in ssml and "</speak>" in ssml:
                log.info(f"TTS: SSML generated via {provider}")
                return ssml
            else:
                # Wrap if the model forgot
                return f"<speak>{ssml}</speak>"

        except Exception as e:
            log.warning(f"TTS: SSML generation failed ({e}), returning plain text")
            return text

    @staticmethod
    def chunk_text(text: str, max_chunk_size: int = 500) -> list[str]:
        """
        Split long text into sentence-aligned chunks for TTS processing.
        Ensures no chunk exceeds max_chunk_size characters.
        """
        if len(text) <= max_chunk_size:
            return [text]

        # Split on sentence boundaries
        sentences = re.split(r'(?<=[.!?])\s+', text)
        chunks = []
        current_chunk = ""

        for sentence in sentences:
            if len(current_chunk) + len(sentence) + 1 > max_chunk_size:
                if current_chunk:
                    chunks.append(current_chunk.strip())
                current_chunk = sentence
            else:
                current_chunk = f"{current_chunk} {sentence}" if current_chunk else sentence

        if current_chunk.strip():
            chunks.append(current_chunk.strip())

        return chunks


# Singleton
tts_processor = TTSTextProcessor()
