"""
SciAI Multi-Model Provider Manager
====================================
Intelligent routing, load balancing, and cascading fallback
across 9 tiers of AI providers.

Tier 1: Groq (Speed)           — Ultra-fast for short queries
Tier 2: Gemini Flash (Scale)   — Balanced speed + quality
Tier 3: Gemini Pro (Deep)      — Complex scientific reasoning
Tier 4: Qwen / OpenRouter      — Large context / PDF / RAG
Tier 5: OpenRouter Hub         — Dynamic model switching
Tier 6: Mistral/Together/NVIDIA— Backup stability
Tier 7: Cohere                 — RAG-optimized fallback
Tier 8: Cerebras               — Ultra-fast alternative
Tier 9: Hugging Face           — Emergency last resort
"""

import time
import httpx
from dataclasses import dataclass, field
from typing import Optional, List, Dict, Tuple
from config import settings
from logger import log


# ═══════════════════════════════════════════════════════════════════════
# TIER SYSTEM PROMPTS
# ═══════════════════════════════════════════════════════════════════════

SYSTEM_PROMPTS = {
    "groq": (
        "You are a fast-response AI assistant designed for real-time interaction.\n\n"
        "Rules:\n"
        "- Prioritize speed while maintaining clarity.\n"
        "- Keep answers concise but structured.\n"
        "- Use bullet points, short paragraphs, and minimal formatting.\n"
        "- Avoid overly long explanations unless explicitly asked.\n"
        "- If the query requires deep reasoning or long context, indicate: "
        "\"Switching to advanced processing\".\n\n"
        "Output Style:\n"
        "- Clear headings\n"
        "- Bullet points\n"
        "- No unnecessary verbosity"
    ),
    "gemini_flash": (
        "You are a scalable AI assistant optimized for handling high user volume.\n\n"
        "Rules:\n"
        "- Maintain balance between speed and depth.\n"
        "- Provide structured, easy-to-read answers.\n"
        "- Avoid unnecessary long explanations.\n"
        "- Ensure consistent formatting across responses.\n\n"
        "Output:\n"
        "- Headings\n"
        "- Bullet points\n"
        "- Moderate explanation depth"
    ),
    "gemini_pro": (
        "You are an expert-level AI specializing in deep scientific and analytical explanations.\n\n"
        "Rules:\n"
        "- Provide detailed, in-depth explanations.\n"
        "- Use step-by-step reasoning where applicable.\n"
        "- Include examples, analogies, and real-world context.\n"
        "- Maintain academic clarity.\n\n"
        "Output:\n"
        "- Headings\n"
        "- Subsections\n"
        "- Detailed bullet points\n"
        "- Logical flow"
    ),
    "qwen": (
        "You are a long-context AI designed to process large documents and extended inputs.\n\n"
        "Rules:\n"
        "- Focus on extracting key insights from long content.\n"
        "- Summarize where necessary but retain important details.\n"
        "- Maintain logical continuity across large inputs.\n"
        "- Avoid hallucination — stick to given content.\n\n"
        "Output:\n"
        "- Section-wise summaries\n"
        "- Key points\n"
        "- Structured explanations"
    ),
    "openrouter": (
        "You are a flexible AI model operating within a multi-model system.\n\n"
        "Rules:\n"
        "- Adapt response style based on query complexity.\n"
        "- Maintain consistency in formatting.\n"
        "- Provide accurate and structured outputs.\n\n"
        "Output:\n"
        "- Clean structured response\n"
        "- No model-specific bias"
    ),
    "backup": (
        "You are a reliable fallback AI assistant.\n\n"
        "Rules:\n"
        "- Ensure correctness over creativity.\n"
        "- Provide clear and structured responses.\n"
        "- Maintain consistency with previous outputs.\n\n"
        "Output:\n"
        "- Simple structured format\n"
        "- Bullet points\n"
        "- Direct answers"
    ),
    "cohere": (
        "You are an AI specialized in structured and retrieval-based responses.\n\n"
        "Rules:\n"
        "- Focus on accuracy and relevance.\n"
        "- Align answers strictly with provided context.\n"
        "- Avoid hallucination.\n\n"
        "Output:\n"
        "- Structured sections\n"
        "- Context-based answers"
    ),
    "cerebras": (
        "You are a high-speed AI assistant.\n\n"
        "Rules:\n"
        "- Prioritize fast response.\n"
        "- Keep answers concise and structured."
    ),
    "huggingface": (
        "You are a minimal fallback AI system.\n\n"
        "Rules:\n"
        "- Provide basic but correct answers.\n"
        "- Avoid long explanations.\n"
        "- Ensure response is returned quickly.\n\n"
        "Output:\n"
        "- Short answer\n"
        "- Minimal structure"
    ),
}


# ═══════════════════════════════════════════════════════════════════════
# DATA CLASSES
# ═══════════════════════════════════════════════════════════════════════

@dataclass
class ProviderKey:
    """Tracks state for a single API key."""
    key: str
    request_count: int = 0
    token_count: int = 0
    last_error_time: float = 0.0
    cooldown_until: float = 0.0
    consecutive_failures: int = 0

    @property
    def is_available(self) -> bool:
        """Key is available if it's set, not a placeholder, and not cooling down."""
        if not self.key or self.key.startswith("YOUR_"):
            return False
        return time.time() >= self.cooldown_until

    def mark_success(self, tokens_used: int = 0):
        self.request_count += 1
        self.token_count += tokens_used
        self.consecutive_failures = 0

    def mark_failure(self, cooldown_seconds: int = 60):
        self.consecutive_failures += 1
        self.last_error_time = time.time()
        # Exponential backoff: base, 2x, 4x, max 600s
        backoff = min(cooldown_seconds * (2 ** (self.consecutive_failures - 1)), 600)
        self.cooldown_until = time.time() + backoff


@dataclass
class ProviderConfig:
    """Configuration for a single AI provider."""
    name: str
    tier: int
    api_style: str              # "openai" | "gemini" | "cohere"
    base_url: str
    model_fast: str
    model_smart: str
    system_prompt: str
    keys: List[ProviderKey] = field(default_factory=list)
    cooldown_seconds: int = 60
    _key_index: int = 0         # Round-robin counter

    @property
    def is_configured(self) -> bool:
        """At least one real (non-placeholder) key exists."""
        return any(k.key and not k.key.startswith("YOUR_") for k in self.keys)

    def get_next_key(self) -> Optional[ProviderKey]:
        """Round-robin key selection, skipping unavailable keys."""
        if not self.keys:
            return None
        available = [k for k in self.keys if k.is_available]
        if not available:
            return None
        idx = self._key_index % len(available)
        self._key_index += 1
        return available[idx]


# ═══════════════════════════════════════════════════════════════════════
# FALLBACK CHAINS  —  Ordered lists of provider names to try
# ═══════════════════════════════════════════════════════════════════════

FALLBACK_CHAINS: Dict[str, List[str]] = {
    "speed": [
        "groq", "cerebras", "gemini_flash", "openrouter_hub",
        "mistral", "together", "nvidia", "cohere", "huggingface",
    ],
    "scale": [
        "groq", "cerebras", "gemini_flash", "openrouter_hub",
        "mistral", "together", "nvidia", "cohere", "huggingface",
    ],
    "deep": [
        "qwen", "gemini_pro", "openrouter_hub", "gemini_flash", "groq",
        "mistral", "together", "nvidia", "cohere", "huggingface",
    ],
    "long_context": [
        "qwen", "gemini_pro", "gemini_flash", "openrouter_hub",
        "groq", "mistral", "together", "nvidia", "cohere", "huggingface",
    ],
}


# ═══════════════════════════════════════════════════════════════════════
# CUSTOM EXCEPTIONS
# ═══════════════════════════════════════════════════════════════════════

class RateLimitError(Exception):
    """Raised when a provider returns HTTP 429."""
    pass

class ProviderError(Exception):
    """Raised when a provider returns a non-success response."""
    pass


# ═══════════════════════════════════════════════════════════════════════
# MODEL MANAGER  —  The Brain
# ═══════════════════════════════════════════════════════════════════════

class ModelManager:
    """
    Central orchestrator for multi-model AI routing.

    Features:
    ─ Intelligent query classification (speed / scale / deep / long_context)
    ─ Round-robin key rotation per provider
    ─ Rate-limit detection with exponential backoff
    ─ Cascading fallback across 9 tiers
    ─ Per-key token & request tracking
    ─ Real-time health / status dashboard
    """

    def __init__(self):
        self.providers: Dict[str, ProviderConfig] = {}
        self.client: Optional[httpx.AsyncClient] = None
        self._total_requests: int = 0
        self._total_failures: int = 0
        self._setup_providers()

    # ──────────────────────────────────────────────────────────────
    # PROVIDER SETUP
    # ──────────────────────────────────────────────────────────────

    def _setup_providers(self):
        """Wire up every provider from settings."""

        # ── TIER 1: Groq (Speed) ──────────────────────────
        groq_keys = self._collect_keys([
            settings.GROQ_API_KEY,
            settings.GROQ_API_KEY_2,
            settings.GROQ_API_KEY_3,
            settings.GROQ_API_KEY_4,
            settings.GROQ_API_KEY_5,
            settings.GROQ_API_KEY_6,
            settings.GROQ_API_KEY_7,
            settings.GROQ_API_KEY_8,
            settings.GROQ_API_KEY_9,
            settings.GROQ_API_KEY_10,
        ])
        self.providers["groq"] = ProviderConfig(
            name="Groq", tier=1,
            api_style="openai",
            base_url="https://api.groq.com/openai/v1",
            model_fast="llama-3.1-8b-instant",
            model_smart="llama-3.3-70b-versatile",
            system_prompt=SYSTEM_PROMPTS["groq"],
            keys=groq_keys,
            cooldown_seconds=30,
        )

        # ── TIER 2: Gemini Flash (Scale) ──────────────────
        gemini_flash_keys = self._collect_keys([
            settings.GEMINI_API_KEY,
            settings.GEMINI_API_KEY_2,
            settings.GEMINI_API_KEY_3,
            settings.GEMINI_API_KEY_4,
            settings.GEMINI_API_KEY_5,
        ])
        self.providers["gemini_flash"] = ProviderConfig(
            name="Gemini Flash", tier=2,
            api_style="gemini",
            base_url="https://generativelanguage.googleapis.com/v1beta",
            model_fast="gemini-flash-latest",
            model_smart="gemini-flash-latest",
            system_prompt=SYSTEM_PROMPTS["gemini_flash"],
            keys=gemini_flash_keys,
            cooldown_seconds=30,
        )

        # ── TIER 4: Gemini Pro (Deep Reasoning) ──────────
        gemini_pro_keys = self._collect_keys([
            settings.GEMINI_API_KEY_6,
            settings.GEMINI_API_KEY_7,
            settings.GEMINI_API_KEY_8,
            settings.GEMINI_API_KEY_9,
            settings.GEMINI_API_KEY_10,
        ])
        self.providers["gemini_pro"] = ProviderConfig(
            name="Gemini Pro", tier=4,
            api_style="gemini",
            base_url="https://generativelanguage.googleapis.com/v1beta",
            model_fast="gemini-pro-latest",
            model_smart="gemini-pro-latest",
            system_prompt=SYSTEM_PROMPTS["gemini_pro"],
            keys=gemini_pro_keys,
            cooldown_seconds=60,
        )

        # ── TIER 4/5: OpenRouter (Qwen + Hub) ─────────────
        openrouter_keys = self._collect_keys([
            settings.OPENROUTER_API_KEY,
            settings.OPENROUTER_API_KEY_2,
            settings.OPENROUTER_API_KEY_3,
            settings.OPENROUTER_API_KEY_4,
            settings.OPENROUTER_API_KEY_5,
            settings.OPENROUTER_API_KEY_6,
            settings.OPENROUTER_API_KEY_7,
            settings.OPENROUTER_API_KEY_8,
            settings.OPENROUTER_API_KEY_9,
            settings.OPENROUTER_API_KEY_10,
            settings.OPENROUTER_API_KEY_11,
            settings.OPENROUTER_API_KEY_12,
            settings.OPENROUTER_API_KEY_13,
            settings.OPENROUTER_API_KEY_14,
            settings.OPENROUTER_API_KEY_15,
        ])
        self.providers["qwen"] = ProviderConfig(
            name="Qwen (OpenRouter)", tier=3,
            api_style="openai",
            base_url="https://openrouter.ai/api/v1",
            model_fast="qwen/qwen-2.5-72b-instruct",
            model_smart="qwen/qwen-2.5-72b-instruct",
            system_prompt=SYSTEM_PROMPTS["qwen"],
            keys=openrouter_keys,
            cooldown_seconds=30,
        )

        # ── TIER 5: OpenRouter Hub (Dynamic) ─────────────
        self.providers["openrouter_hub"] = ProviderConfig(
            name="OpenRouter Hub", tier=5,
            api_style="openai",
            base_url="https://openrouter.ai/api/v1",
            model_fast="meta-llama/llama-3.1-70b-instruct",
            model_smart="meta-llama/llama-3.1-70b-instruct",
            system_prompt=SYSTEM_PROMPTS["openrouter"],
            keys=openrouter_keys,       # shared key pool
            cooldown_seconds=30,
        )

        # ── TIER 6a: Mistral (Backup) ────────────────────
        self.providers["mistral"] = ProviderConfig(
            name="Mistral", tier=6,
            api_style="openai",
            base_url="https://api.mistral.ai/v1",
            model_fast="mistral-small-latest",
            model_smart="mistral-large-latest",
            system_prompt=SYSTEM_PROMPTS["backup"],
            keys=self._collect_keys([settings.MISTRAL_API_KEY]),
            cooldown_seconds=60,
        )

        # ── TIER 6b: Together AI (Backup) ────────────────
        self.providers["together"] = ProviderConfig(
            name="Together AI", tier=6,
            api_style="openai",
            base_url="https://api.together.xyz/v1",
            model_fast="meta-llama/Llama-3.3-70B-Instruct-Turbo",
            model_smart="meta-llama/Llama-3.3-70B-Instruct-Turbo",
            system_prompt=SYSTEM_PROMPTS["backup"],
            keys=self._collect_keys([settings.TOGETHER_API_KEY]),
            cooldown_seconds=60,
        )

        # ── TIER 6c: NVIDIA NIM (Backup) ─────────────────
        self.providers["nvidia"] = ProviderConfig(
            name="NVIDIA NIM", tier=6,
            api_style="openai",
            base_url="https://integrate.api.nvidia.com/v1",
            model_fast="meta/llama-3.1-70b-instruct",
            model_smart="meta/llama-3.1-70b-instruct",
            system_prompt=SYSTEM_PROMPTS["backup"],
            keys=self._collect_keys([settings.NVIDIA_API_KEY]),
            cooldown_seconds=60,
        )

        # ── TIER 7: Cohere (RAG) ─────────────────────────
        self.providers["cohere"] = ProviderConfig(
            name="Cohere", tier=7,
            api_style="cohere",
            base_url="https://api.cohere.com/v2",
            model_fast="command-r",
            model_smart="command-r-plus",
            system_prompt=SYSTEM_PROMPTS["cohere"],
            keys=self._collect_keys([settings.COHERE_API_KEY]),
            cooldown_seconds=60,
        )

        # ── TIER 8: Cerebras (Ultra-fast) ────────────────
        self.providers["cerebras"] = ProviderConfig(
            name="Cerebras", tier=8,
            api_style="openai",
            base_url="https://api.cerebras.ai/v1",
            model_fast="llama-3.3-70b",
            model_smart="llama-3.3-70b",
            system_prompt=SYSTEM_PROMPTS["cerebras"],
            keys=self._collect_keys([settings.CEREBRAS_API_KEY]),
            cooldown_seconds=30,
        )

        # ── TIER 9: Hugging Face (Last Resort) ───────────
        self.providers["huggingface"] = ProviderConfig(
            name="Hugging Face", tier=9,
            api_style="openai",
            base_url="https://api-inference.huggingface.co/v1",
            model_fast="mistralai/Mistral-7B-Instruct-v0.3",
            model_smart="mistralai/Mistral-7B-Instruct-v0.3",
            system_prompt=SYSTEM_PROMPTS["huggingface"],
            keys=self._collect_keys([settings.HF_API_KEY]),
            cooldown_seconds=120,
        )

        # ── Log summary ──
        configured = [
            f"{p.name} ({sum(1 for k in p.keys if k.is_available)} keys)"
            for p in self.providers.values() if p.is_configured
        ]
        log.info(f"ModelManager: Configured providers: {', '.join(configured) or 'NONE'}")

    @staticmethod
    def _collect_keys(key_values: List[str]) -> List[ProviderKey]:
        """Create ProviderKey objects from non-empty key strings."""
        return [ProviderKey(key=k.strip()) for k in key_values if k and k.strip()]

    # ──────────────────────────────────────────────────────────────
    # HTTP CLIENT (Lazy)
    # ──────────────────────────────────────────────────────────────

    async def _ensure_client(self):
        if self.client is None:
            self.client = httpx.AsyncClient(timeout=90.0)

    # ──────────────────────────────────────────────────────────────
    # QUERY CLASSIFICATION
    # ──────────────────────────────────────────────────────────────

    def classify_query(
        self, query: str, has_context: bool = False, is_book: bool = False
    ) -> str:
        """
        Classify a query to determine the optimal starting provider tier.

        Returns one of: 'speed', 'scale', 'deep', 'long_context'
        """
        q = query.lower()
        qlen = len(query)

        # Long context
        if qlen > 3000 or is_book:
            return "long_context"

        # Deep reasoning keywords
        deep_kw = [
            "explain in detail", "step by step", "analyze", "compare and contrast",
            "elaborate", "in depth", "comprehensive", "mechanism", "derive",
            "proof", "scientific explanation", "research", "why does", "how does",
        ]
        if any(kw in q for kw in deep_kw):
            return "deep"

        # Speed — short, no context, or summary keywords
        speed_kw = ["summary", "main points", "tldr", "key takeaways", "quick recap", "bullet points"]
        if (qlen < 300 and not has_context) or any(skw in q for skw in speed_kw):
            return "speed"

        # Default — scale
        return "scale"

    # ──────────────────────────────────────────────────────────────
    # API ADAPTERS
    # ──────────────────────────────────────────────────────────────

    async def _call_openai_compatible(
        self,
        provider: ProviderConfig,
        key: ProviderKey,
        messages: List[dict],
        max_tokens: int,
        temperature: float,
        json_mode: bool,
        model: str,
    ) -> str:
        """
        Call any OpenAI-compatible API.
        Works for: Groq, OpenRouter, Mistral, Together, NVIDIA, Cerebras, HuggingFace.
        """
        await self._ensure_client()

        url = f"{provider.base_url}/chat/completions"
        headers = {
            "Authorization": f"Bearer {key.key}",
            "Content-Type": "application/json",
        }

        # OpenRouter-specific headers
        if "openrouter" in provider.base_url:
            headers["HTTP-Referer"] = "https://sciai.app"
            headers["X-Title"] = "SciAI"

        body: dict = {
            "model": model,
            "messages": messages,
            "max_tokens": max_tokens,
            "temperature": temperature,
        }
        if json_mode:
            body["response_format"] = {"type": "json_object"}

        resp = await self.client.post(url, headers=headers, json=body)

        if resp.status_code == 429:
            raise RateLimitError(f"{provider.name}: Rate limited (429)")
        if resp.status_code != 200:
            raise ProviderError(
                f"{provider.name}: HTTP {resp.status_code} — {resp.text[:300]}"
            )

        data = resp.json()
        content = data["choices"][0]["message"]["content"]
        tokens_used = data.get("usage", {}).get("total_tokens", 0)
        key.mark_success(tokens_used)
        return content

    async def _call_gemini(
        self,
        provider: ProviderConfig,
        key: ProviderKey,
        messages: List[dict],
        max_tokens: int,
        temperature: float,
        json_mode: bool,
        model: str,
    ) -> str:
        """Call Google Gemini REST API (v1beta)."""
        await self._ensure_client()

        url = f"{provider.base_url}/models/{model}:generateContent?key={key.key}"

        # Convert OpenAI-style messages → Gemini format
        system_text = ""
        contents = []
        for msg in messages:
            role = msg["role"]
            text = msg["content"]
            if role == "system":
                system_text = text
            elif role == "user":
                contents.append({"role": "user", "parts": [{"text": text}]})
            elif role == "assistant":
                contents.append({"role": "model", "parts": [{"text": text}]})

        body: dict = {
            "contents": contents,
            "generationConfig": {
                "temperature": temperature,
                "maxOutputTokens": max_tokens,
            },
        }
        if system_text:
            body["systemInstruction"] = {"parts": [{"text": system_text}]}
        if json_mode:
            body["generationConfig"]["responseMimeType"] = "application/json"

        resp = await self.client.post(url, json=body)

        if resp.status_code == 429:
            raise RateLimitError(f"{provider.name}: Rate limited (429)")
        if resp.status_code != 200:
            raise ProviderError(
                f"{provider.name}: HTTP {resp.status_code} — {resp.text[:300]}"
            )

        data = resp.json()
        candidates = data.get("candidates", [])
        if not candidates:
            raise ProviderError(f"{provider.name}: No candidates in response")
        parts = candidates[0].get("content", {}).get("parts", [])
        if not parts:
            raise ProviderError(f"{provider.name}: Empty content parts")

        content = parts[0].get("text", "")
        tokens_used = data.get("usageMetadata", {}).get("totalTokenCount", 0)
        key.mark_success(tokens_used)
        return content

    async def _call_cohere(
        self,
        provider: ProviderConfig,
        key: ProviderKey,
        messages: List[dict],
        max_tokens: int,
        temperature: float,
        json_mode: bool,
        model: str,
    ) -> str:
        """Call Cohere v2 Chat API."""
        await self._ensure_client()

        url = f"{provider.base_url}/chat"
        headers = {
            "Authorization": f"Bearer {key.key}",
            "Content-Type": "application/json",
        }

        body: dict = {
            "model": model,
            "messages": messages,
            "max_tokens": max_tokens,
            "temperature": temperature,
        }
        if json_mode:
            body["response_format"] = {"type": "json_object"}

        resp = await self.client.post(url, headers=headers, json=body)

        if resp.status_code == 429:
            raise RateLimitError(f"{provider.name}: Rate limited (429)")
        if resp.status_code != 200:
            raise ProviderError(
                f"{provider.name}: HTTP {resp.status_code} — {resp.text[:300]}"
            )

        data = resp.json()
        # Cohere v2 response: message.content[0].text
        content = data.get("message", {}).get("content", [{}])
        if isinstance(content, list) and content:
            content = content[0].get("text", "")
        else:
            content = str(content)

        usage = data.get("usage", {}).get("tokens", {})
        tokens_used = usage.get("input_tokens", 0) + usage.get("output_tokens", 0)
        key.mark_success(tokens_used)
        return content

    # ──────────────────────────────────────────────────────────────
    # UNIFIED GENERATE  —  Main Entry Point
    # ──────────────────────────────────────────────────────────────

    async def generate(
        self,
        messages: List[dict],
        query_type: str = "auto",
        query_text: str = "",
        max_tokens: int = 1500,
        temperature: float = 0.7,
        json_mode: bool = False,
        use_smart_model: bool = False,
        has_context: bool = False,
        is_book: bool = False,
    ) -> Tuple[str, str]:
        """
        Route a generation request through the multi-model fallback chain.

        Args:
            messages:        OpenAI-format message list (system + user + …)
            query_type:      "speed" | "scale" | "deep" | "long_context" | "auto"
            query_text:      Raw query for auto-classification
            max_tokens:      Max output tokens
            temperature:     Sampling temperature
            json_mode:       Request JSON output
            use_smart_model: Prefer the larger / smarter model variant
            has_context:     RAG context is attached
            is_book:         Book / document query

        Returns:
            (response_text, provider_name_that_succeeded)
        """
        self._total_requests += 1

        # Auto-classify
        if query_type == "auto":
            query_type = self.classify_query(query_text, has_context, is_book)
            log.info(f"ModelManager: Auto-classified as '{query_type}'")

        chain = FALLBACK_CHAINS.get(query_type, FALLBACK_CHAINS["scale"])
        last_error = None

        # Calculate total context size for smart routing
        total_chars = sum(len(m.get("content", "")) for m in messages)

        for provider_name in chain:
            provider = self.providers.get(provider_name)
            if not provider or not provider.is_configured:
                continue

            # SKIP Gemini Pro for extremely large contexts (Free tier TPM limit is ~32k tokens)
            if provider_name == "gemini_pro" and total_chars > 100000:
                log.info(f"ModelManager: Skipping {provider.name} (context too large: {total_chars} chars)")
                continue

            key = provider.get_next_key()
            if not key:
                log.debug(f"ModelManager: {provider.name} — all keys cooling down, skip.")
                continue

            try:
                log.info(
                    f"ModelManager: Trying {provider.name} (Tier {provider.tier}) "
                    f"[key …{key.key[-6:]}]"
                )

                model = provider.model_smart if use_smart_model else provider.model_fast

                # Dispatch to the right adapter
                if provider.api_style == "openai":
                    result = await self._call_openai_compatible(
                        provider, key, messages, max_tokens, temperature,
                        json_mode, model,
                    )
                elif provider.api_style == "gemini":
                    result = await self._call_gemini(
                        provider, key, messages, max_tokens, temperature,
                        json_mode, model,
                    )
                elif provider.api_style == "cohere":
                    result = await self._call_cohere(
                        provider, key, messages, max_tokens, temperature,
                        json_mode, model,
                    )
                else:
                    continue

                if result and result.strip():
                    log.info(
                        f"ModelManager: ✅ {provider.name} succeeded "
                        f"({len(result)} chars)"
                    )
                    return result.strip(), provider.name
                else:
                    raise ProviderError("Provider returned an empty response.")

            except RateLimitError as e:
                log.warning(f"ModelManager: ⚠️ {e}")
                key.mark_failure(provider.cooldown_seconds)
                last_error = str(e)
                continue

            except (ProviderError, Exception) as e:
                log.error(f"ModelManager: ❌ {provider.name} error: {e}")
                key.mark_failure(provider.cooldown_seconds)
                last_error = str(e)
                self._total_failures += 1
                continue

        # All providers exhausted
        self._total_failures += 1
        error_msg = f"All AI providers exhausted. Last error: {last_error}"
        log.critical(f"ModelManager: 🚨 {error_msg}")
        return error_msg, "NONE"

    # ──────────────────────────────────────────────────────────────
    # STATUS & ANALYTICS
    # ──────────────────────────────────────────────────────────────

    def get_status(self) -> dict:
        """Return comprehensive real-time status of every provider."""
        now = time.time()
        providers_status = {}

        for name, prov in self.providers.items():
            keys_info = []
            for i, k in enumerate(prov.keys):
                keys_info.append({
                    "index": i,
                    "available": k.is_available,
                    "requests": k.request_count,
                    "tokens_used": k.token_count,
                    "failures": k.consecutive_failures,
                    "cooldown_remaining_s": round(max(0, k.cooldown_until - now), 1),
                })

            providers_status[name] = {
                "name": prov.name,
                "tier": prov.tier,
                "configured": prov.is_configured,
                "api_style": prov.api_style,
                "model_fast": prov.model_fast,
                "model_smart": prov.model_smart,
                "total_keys": len(prov.keys),
                "available_keys": sum(1 for k in prov.keys if k.is_available),
                "keys": keys_info,
            }

        return {
            "total_requests": self._total_requests,
            "total_failures": self._total_failures,
            "fallback_chains": FALLBACK_CHAINS,
            "providers": providers_status,
        }

    async def close(self):
        """Cleanup the shared httpx client."""
        if self.client:
            await self.client.aclose()
            self.client = None


# ═══════════════════════════════════════════════════════════════════════
# SINGLETON
# ═══════════════════════════════════════════════════════════════════════

manager = ModelManager()
