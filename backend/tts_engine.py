"""
SciAI TTS Engine
─────────────────
Tiered text-to-speech audio generation:
  Tier 1: Kyutai Pocket-TTS  (local, fast, CPU-optimized)
  Tier 2: HuggingFace SpeechT5 (local fallback)
  Tier 4: ElevenLabs (premium — placeholder for future)

Audio output: 16-bit PCM WAV
"""

import os
import io
import time
import hashlib
import asyncio
import struct
import numpy as np
from pathlib import Path
from typing import Optional
from logger import log
from config import settings

# Lazy imports — only loaded when the tier is actually used
_kyutai_model = None
_kyutai_voices = {}
_hf_pipeline = None
_hf_speaker_embeddings = {}


# ═══════════════════════════════════════════════════════════════════════════════
# VOICE PERSONALITY → ENGINE MAPPING
# ═══════════════════════════════════════════════════════════════════════════════

# Kyutai voice catalog (built-in names to avoid gated voice-cloning weights)
KYUTAI_VOICE_MAP = {
    "professor":     "alba",
    "energetic":     "cosette",
    "storyteller":   "fantine",
    "formal_male":   "marius",
    "casual_female": "eponine",
    "deep_male":     "javert",
    "warm_female":   "azelma"
}

# HuggingFace SpeechT5 speaker embedding indices (from cmu-arctic-xvectors)
HF_SPEAKER_MAP = {
    "professor":     7306,   # Calm male voice
    "energetic":     4241,   # Energetic female voice
    "storyteller":   1116,   # Warm narrator female voice
    "formal_male":   5555,   # Formal male voice
    "casual_female": 2555,   # Casual female voice
    "deep_male":     7777,   # Deep male voice
    "warm_female":   3333    # Warm female voice
}


# ═══════════════════════════════════════════════════════════════════════════════
# WAV UTILITIES
# ═══════════════════════════════════════════════════════════════════════════════

def _numpy_to_wav_bytes(audio_np: np.ndarray, sample_rate: int) -> bytes:
    """Convert numpy audio array to WAV bytes in memory."""
    # Normalize to int16 range
    if audio_np.dtype == np.float32 or audio_np.dtype == np.float64:
        audio_np = np.clip(audio_np, -1.0, 1.0)
        audio_int16 = (audio_np * 32767).astype(np.int16)
    elif audio_np.dtype == np.int16:
        audio_int16 = audio_np
    else:
        audio_int16 = audio_np.astype(np.int16)

    # Build WAV in memory
    buf = io.BytesIO()
    num_samples = len(audio_int16)
    data_size = num_samples * 2  # 16-bit = 2 bytes per sample
    num_channels = 1

    # RIFF header
    buf.write(b'RIFF')
    buf.write(struct.pack('<I', 36 + data_size))
    buf.write(b'WAVE')

    # fmt chunk
    buf.write(b'fmt ')
    buf.write(struct.pack('<I', 16))               # chunk size
    buf.write(struct.pack('<H', 1))                 # PCM format
    buf.write(struct.pack('<H', num_channels))      # mono
    buf.write(struct.pack('<I', sample_rate))        # sample rate
    buf.write(struct.pack('<I', sample_rate * 2))    # byte rate
    buf.write(struct.pack('<H', 2))                  # block align
    buf.write(struct.pack('<H', 16))                 # bits per sample

    # data chunk
    buf.write(b'data')
    buf.write(struct.pack('<I', data_size))
    buf.write(audio_int16.tobytes())

    return buf.getvalue()


def _concatenate_wav_bytes(wav_list: list[bytes]) -> bytes:
    """Concatenate multiple WAV byte arrays into one using the wave module."""
    if len(wav_list) == 1:
        return wav_list[0]

    import wave
    
    # Read first chunk to get parameters
    with io.BytesIO(wav_list[0]) as first_buf:
        with wave.open(first_buf, 'rb') as first_wav:
            params = first_wav.getparams()
            sample_rate = first_wav.getframerate()
    
    output_buf = io.BytesIO()
    with wave.open(output_buf, 'wb') as out_wav:
        out_wav.setparams(params)
        for wav_data in wav_list:
            with io.BytesIO(wav_data) as buf:
                with wave.open(buf, 'rb') as in_wav:
                    out_wav.writeframes(in_wav.readframes(in_wav.getnframes()))
    
    return output_buf.getvalue()


# ═══════════════════════════════════════════════════════════════════════════════
# CACHE
# ═══════════════════════════════════════════════════════════════════════════════

def _get_cache_path(text: str, voice_style: str) -> Path:
    """Generate cache file path from text + style hash."""
    cache_dir = Path(settings.TTS_CACHE_DIR)
    cache_dir.mkdir(parents=True, exist_ok=True)
    key = f"{voice_style}:{text}"
    hash_val = hashlib.sha256(key.encode()).hexdigest()[:16]
    return cache_dir / f"{hash_val}.wav"


def _check_cache(text: str, voice_style: str) -> Optional[bytes]:
    """Return cached WAV bytes if available."""
    path = _get_cache_path(text, voice_style)
    if path.exists() and path.stat().st_size > 44:  # Minimum valid WAV
        return path.read_bytes()
    return None


def _save_cache(text: str, voice_style: str, wav_bytes: bytes, meta_data: dict = None):
    """Save generated audio to disk cache with optional meta-information."""
    try:
        path = _get_cache_path(text, voice_style)
        path.write_bytes(wav_bytes)
        
        if meta_data:
            meta_path = path.with_suffix(".json")
            import json
            meta_path.write_text(json.dumps(meta_data))
            
        log.debug(f"TTS Cache: Saved {path.name} ({len(wav_bytes)} bytes)")
    except Exception as e:
        log.warning(f"TTS Cache: Failed to save — {e}")


# ═══════════════════════════════════════════════════════════════════════════════
# TIER 1: KYUTAI POCKET-TTS
# ═══════════════════════════════════════════════════════════════════════════════

def _init_kyutai():
    """Lazy-load Kyutai Pocket-TTS model and voice states."""
    global _kyutai_model, _kyutai_voices

    if _kyutai_model is not None:
        return True

    try:
        from pocket_tts import TTSModel
        log.info("TTS: Loading Kyutai Pocket-TTS model...")
        _kyutai_model = TTSModel.load_model()
        log.info(f"TTS: Kyutai model loaded (sample_rate={_kyutai_model.sample_rate})")

        # Pre-load voice states for all personalities
        for style, voice_id in KYUTAI_VOICE_MAP.items():
            try:
                # Use the catalog voice directly (the log suggests this avoids cloning weights)
                _kyutai_voices[style] = _kyutai_model.get_state_for_audio_prompt(voice_id)
                log.info(f"TTS: Loaded Kyutai catalog voice '{style}' ({voice_id})")
            except Exception as ve:
                log.warning(f"TTS: Failed to load Kyutai voice '{style}': {ve}")

        return bool(_kyutai_voices)

    except ImportError:
        log.warning("TTS: pocket-tts not installed — Kyutai unavailable")
        return False
    except Exception as e:
        log.error(f"TTS: Kyutai initialization failed: {e}")
        _kyutai_model = None
        return False


def _kyutai_generate(text: str, voice_style: str) -> Optional[bytes]:
    """Generate audio using Kyutai Pocket-TTS."""
    global _kyutai_model, _kyutai_voices

    if not _init_kyutai():
        return None

    style = voice_style if voice_style in _kyutai_voices else "professor"
    voice_state = _kyutai_voices.get(style)
    if voice_state is None:
        # Fallback to first available voice
        if _kyutai_voices:
            voice_state = next(iter(_kyutai_voices.values()))
        else:
            return None

    try:
        audio_tensor = _kyutai_model.generate_audio(voice_state, text)
        audio_np = audio_tensor.numpy()
        wav_bytes = _numpy_to_wav_bytes(audio_np, _kyutai_model.sample_rate)
        log.info(f"TTS: Kyutai generated {len(wav_bytes)} bytes ({voice_style})")
        return wav_bytes
    except Exception as e:
        log.error(f"TTS: Kyutai generation failed: {e}")
        return None


# ═══════════════════════════════════════════════════════════════════════════════
# TIER 2: HUGGINGFACE SPEECHT5
# ═══════════════════════════════════════════════════════════════════════════════

def _init_hf():
    """Lazy-load HuggingFace SpeechT5 pipeline."""
    global _hf_pipeline, _hf_speaker_embeddings

    if _hf_pipeline is not None:
        return True

    try:
        from transformers import pipeline as hf_pipeline
        from datasets import load_dataset
        import torch

        log.info("TTS: Loading HuggingFace SpeechT5 model...")
        _hf_pipeline = hf_pipeline("text-to-speech", model="microsoft/speecht5_tts")

        try:
            # Load speaker embeddings dataset
            embeddings_dataset = load_dataset("Matthijs/cmu-arctic-xvectors", split="validation", trust_remote_code=True)

            for style, idx in HF_SPEAKER_MAP.items():
                try:
                    _hf_speaker_embeddings[style] = torch.tensor(
                        embeddings_dataset[idx]["xvector"]
                    ).unsqueeze(0)
                    log.info(f"TTS: Loaded HF speaker embedding '{style}' (idx={idx})")
                except Exception as se:
                    log.warning(f"TTS: Failed to load HF embedding '{style}': {se}")
        except Exception as de:
            log.warning(f"TTS: Failed to load HF speaker dataset: {de}. Using default voice.")
            # Create a default zero embedding as fallback
            default_emb = torch.zeros((1, 512))
            for style in HF_SPEAKER_MAP.keys():
                _hf_speaker_embeddings[style] = default_emb

        log.info("TTS: HuggingFace SpeechT5 ready")
        return True

    except ImportError as ie:
        log.warning(f"TTS: transformers/datasets not installed — HF unavailable ({ie})")
        return False
    except Exception as e:
        log.error(f"TTS: HuggingFace initialization failed: {e}")
        _hf_pipeline = None
        return False


def _hf_generate(text: str, voice_style: str) -> Optional[bytes]:
    """Generate audio using HuggingFace SpeechT5."""
    global _hf_pipeline, _hf_speaker_embeddings

    if not _init_hf():
        return None

    style = voice_style if voice_style in _hf_speaker_embeddings else "professor"
    speaker_embedding = _hf_speaker_embeddings.get(style)

    if speaker_embedding is None:
        if _hf_speaker_embeddings:
            speaker_embedding = next(iter(_hf_speaker_embeddings.values()))
        else:
            return None

    try:
        # SpeechT5 has a ~600 token limit, truncate if needed
        truncated = text[:500] if len(text) > 500 else text

        result = _hf_pipeline(
            truncated,
            forward_params={"speaker_embeddings": speaker_embedding}
        )

        audio_np = np.array(result["audio"], dtype=np.float32)
        sample_rate = result["sampling_rate"]
        wav_bytes = _numpy_to_wav_bytes(audio_np, sample_rate)
        log.info(f"TTS: HF SpeechT5 generated {len(wav_bytes)} bytes ({voice_style})")
        return wav_bytes
    except Exception as e:
        log.error(f"TTS: HuggingFace generation failed: {e}")
        return None


# ═══════════════════════════════════════════════════════════════════════════════
# TTS ENGINE (Tiered Orchestrator)
# ═══════════════════════════════════════════════════════════════════════════════

class TTSEngine:
    """
    Orchestrates tiered TTS generation:
      Tier 1: Kyutai Pocket-TTS (fast, local, CPU)
      Tier 2: HuggingFace SpeechT5 (fallback, local)
    
    Handles text chunking for long inputs and disk caching.
    """

    def __init__(self):
        self._semaphore = asyncio.Semaphore(4)  # Limit concurrent chunk generation to 4 tasks
        self._locks = {}  # Per-request de-duplication locks
        self._lock_times = {} # Track lock creation for cleanup


    async def initialize(self):
        """Pre-load Tier 1 model at startup (optional — lazy load also works)."""
        log.info("TTS Engine: Initializing...")
        # Try to pre-load Kyutai in a non-blocking thread
        kyutai_ok = await asyncio.to_thread(_init_kyutai)
        if kyutai_ok:
            log.info("TTS Engine: Kyutai Pocket-TTS ready (Tier 1)")
        else:
            log.warning("TTS Engine: Kyutai unavailable, will use HuggingFace fallback")
            hf_ok = await asyncio.to_thread(_init_hf)
            if hf_ok:
                log.info("TTS Engine: HuggingFace SpeechT5 ready (Tier 2)")
            else:
                log.warning("TTS Engine: No TTS backend available — client must use Android TTS")

    async def generate(
        self,
        text: str,
        voice_style: str = "professor",
        speed: float = 1.0
    ) -> Optional[bytes]:
        """
        Generate WAV audio from text using the tiered engine stack.
        Returns raw WAV bytes or None if all tiers fail.
        """
        if not text or len(text.strip()) < 2:
            return None

        # Check cache first
        cached = _check_cache(text, voice_style)
        if cached:
            log.info(f"TTS: Cache hit ({voice_style})")
            return cached

        # For long text, chunk and generate separately
        from tts_text_processor import TTSTextProcessor
        chunks = TTSTextProcessor.chunk_text(text, max_chunk_size=settings.TTS_CHUNK_SIZE)

        # Limit text to avoid 5+ minute waits on long expert responses
        # 4 chunks = ~2000 chars, which is plenty for one playback session
        if len(chunks) > 4:
            log.info(f"TTS: Capping long text ({len(chunks)} chunks) to 4 chunks for responsiveness")
            chunks = chunks[:4]

        # De-duplication: prevents multiple identical requests from slamming CPU
        request_hash = hashlib.sha256(f"{voice_style}:{text[:1000]}".encode()).hexdigest()
        
        if request_hash not in self._locks:
            self._locks[request_hash] = asyncio.Lock()
            
        async with self._locks[request_hash]:
            # Re-check main cache inside lock
            cached = _check_cache(text, voice_style)
            if cached:
                return cached

            log.info(f"TTS: Generating {len(chunks)} chunks...")

            # Generate all chunks in parallel (with concurrency limit)
            tasks = [self._generate_single_with_sem(chunk, voice_style, i) for i, chunk in enumerate(chunks)]
            wav_parts_raw = await asyncio.gather(*tasks)

            # Filter out failed chunks and keep order
            wav_parts = [wav for wav in wav_parts_raw if wav is not None]

            if not wav_parts:
                return None

            # Concatenate all chunks
            final_wav = _concatenate_wav_bytes(wav_parts)

            # Save to main cache with meta
            _save_cache(text, voice_style, final_wav, meta_data={
                "voice_style": voice_style,
                "chunks": len(wav_parts),
                "timestamp": time.time(),
                "text_length": len(text)
            })

            return final_wav

    async def generate_stream(
        self,
        text: str,
        voice_style: str = "professor",
        speed: float = 1.0
    ):
        """
        Async generator that yields WAV chunks as they are generated.
        Provides zero-latency responsive feedback.
        """
        if not text or len(text.strip()) < 2:
            return

        # Check cache first (for the whole block)
        cached = _check_cache(text, voice_style)
        if cached:
            log.info(f"TTS: Cache hit (stream) for '{voice_style}'")
            yield cached
            return

        from tts_text_processor import TTSTextProcessor
        chunks = TTSTextProcessor.chunk_text(text, max_chunk_size=settings.TTS_CHUNK_SIZE)
        
        # De-duplication: prevents multiple identical requests from slamming CPU
        request_hash = hashlib.sha256(f"{voice_style}:{text[:1000]}".encode()).hexdigest()
        
        if request_hash not in self._locks:
            self._locks[request_hash] = asyncio.Lock()
            self._lock_times[request_hash] = time.time()
            
        async with self._locks[request_hash]:
            log.info(f"TTS: Streaming {len(chunks)} chunks...")
            
            # Start generating all in background, but await in order
            tasks = [self._generate_single_with_sem(chunk, voice_style, i) for i, chunk in enumerate(chunks)]
            
            # Run all, but yield in order
            futures = [asyncio.ensure_future(t) for t in tasks]
            
            all_parts = []
            try:
                for i, future in enumerate(futures):
                    wav = await future
                    if wav:
                        yield wav
                        all_parts.append(wav)
            except (GeneratorExit, asyncio.CancelledError):
                log.info(f"TTS: Streaming interrupted for '{voice_style}'. Cancelling {len(futures)} tasks.")
                for f in futures:
                    if not f.done():
                        f.cancel()
                raise
            finally:
                # Once all are done, save the full combined version to main cache
                if all_parts and len(all_parts) == len(chunks):
                    final_wav = _concatenate_wav_bytes(all_parts)
                    _save_cache(text, voice_style, final_wav, meta_data={
                        "voice_style": voice_style,
                        "chunks": len(all_parts),
                        "timestamp": time.time(),
                        "text_length": len(text),
                        "streaming": True
                    })
                
                # Periodic cleanup of old locks
                self._cleanup_locks()

    def _cleanup_locks(self):
        """Remove locks older than 5 minutes to prevent memory leaks."""
        now = time.time()
        expired = [h for h, t in self._lock_times.items() if now - t > 300]
        for h in expired:
            if h in self._locks and not self._locks[h].locked():
                del self._locks[h]
                del self._lock_times[h]

    async def _generate_single_with_sem(self, text: str, voice_style: str, index: int) -> Optional[bytes]:
        """Wrapper to run _generate_single with a semaphore and per-chunk cache."""
        # Check per-chunk cache first
        chunk_cached = _check_cache(text, voice_style)
        if chunk_cached:
            return chunk_cached

        async with self._semaphore:
            wav = await self._generate_single(text, voice_style)
            if wav:
                # Save individual chunk to cache
                _save_cache(text, voice_style, wav)
            return wav

    async def _generate_single(self, text: str, voice_style: str) -> Optional[bytes]:
        """Generate audio for a single chunk using tiered fallback."""

        # Tier 1: Kyutai Pocket-TTS
        wav = await asyncio.to_thread(_kyutai_generate, text, voice_style)
        if wav:
            return wav

        # Tier 2: HuggingFace SpeechT5
        log.info("TTS: Tier 1 failed, falling back to HuggingFace SpeechT5")
        wav = await asyncio.to_thread(_hf_generate, text, voice_style)
        if wav:
            return wav

        # All tiers failed
        log.error("TTS: All backend tiers exhausted — client should use Android TTS")
        return None

    def get_status(self) -> dict:
        """Return current TTS engine status."""
        return {
            "kyutai": {
                "loaded": _kyutai_model is not None,
                "voices": list(_kyutai_voices.keys())
            },
            "huggingface": {
                "loaded": _hf_pipeline is not None,
                "voices": list(_hf_speaker_embeddings.keys())
            }
        }


# Singleton
tts_engine = TTSEngine()
