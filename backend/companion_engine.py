import os
import json
import logging
import asyncio
from typing import List, Dict, Optional
import llm_engine
from model_manager import manager as model_manager

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("CompanionEngine")

# ── SKILLS LOADER ──
# Based on SandecoClaw Specs: Loader -> Router -> Executor
class CompanionEngine:
    def __init__(self, skills_dir: str = None):
        if skills_dir is None:
            base_dir = os.path.dirname(os.path.abspath(__file__))
            self.skills_dir = os.path.join(base_dir, ".agents", "skills")
        else:
            self.skills_dir = skills_dir
        self.manager_skill_content = ""
        self.load_manager_skill()
        self.chat_histories = {} # Simple in-memory history (can be persisted to SQLite)

    def load_manager_skill(self):
        """Load the core Butler/Manager skill from the markdown file."""
        skill_path = os.path.join(self.skills_dir, "manager", "SKILL.md")
        if os.path.exists(skill_path):
            with open(skill_path, "r", encoding="utf-8") as f:
                self.manager_skill_content = f.read()
            logger.info("CompanionEngine: Manager skill loaded successfully.")
        else:
            logger.warning(f"CompanionEngine: Manager skill not found at {skill_path}")
            self.manager_skill_content = "You are a helpful scientific assistant. Answer simply."

    async def chat(self, user_id: str, query: str) -> Dict:
        """Process a chat message from the user and return a structured response."""
        
        # 1. Manage History
        if user_id not in self.chat_histories:
            self.chat_histories[user_id] = []
        
        history = self.chat_histories[user_id][-4:] # Reduced from 10 to 4 to save input Tokens
        
        # 1.5 Reload Skill (Dynamic)
        self.load_manager_skill()
        
        # 2. Build Prompt (SandecoClaw Agent Loop)
        system_prompt = (
            f"{self.manager_skill_content}\n\n"
            "User Context: Scientific Researcher.\n"
            "Session Context: Android Mobile App.\n\n"
            "CRITICAL: YOUR OUTPUT MUST BE A SINGLE VALID JSON OBJECT AND NOTHING ELSE. "
            "Do NOT wrap it in markdown block quotes. Do NOT add any conversational text before or after the JSON."
        )
        
        messages = [{"role": "system", "content": system_prompt}]
        for msg in history:
            messages.append(msg)
        messages.append({"role": "user", "content": query})

        try:
            # 3. Request LLM (Using model_manager to select best model)
            # We use 'speed' routing for the companion to keep it snappy.
            response_text, provider = await model_manager.generate(
                messages=messages,
                query_text=query,
                query_type="speed",
                max_tokens=150 # Strict output limit to prevent excess token burn
            )

            # 4. Parse Structured JSON from LLM
            # Ensure JSON is absolutely clean
            clean_json = self.extract_json(response_text)
            
            if clean_json:
                # Store in history
                self.chat_histories[user_id].append({"role": "user", "content": query})
                self.chat_histories[user_id].append({"role": "assistant", "content": response_text})
                return clean_json
            else:
                # Fallback if JSON parsing fails
                return {
                    "text": response_text if response_text else "I'm sorry, Scientist. I didn't quite catch that.",
                    "mode": "NORMAL",
                    "action": "NONE",
                    "query": query,
                    "target": "",
                    "emotion": "CONFUSED"
                }

        except Exception as e:
            logger.error(f"CompanionEngine: Error in chat: {e}")
            return {
                "text": "My apologies, Sir. My cognitive circuits are a bit foggy at the moment.",
                "mode": "NORMAL",
                "action": "NONE",
                "query": "",
                "target": "",
                "emotion": "SAD"
            }

    def extract_json(self, text: str) -> Optional[Dict]:
        """Extract and parse JSON from the LLM response text."""
        try:
            # Clean possible markdown
            clean_text = text.strip()
            if clean_text.startswith("```json"):
                clean_text = clean_text[7:]
            if clean_text.startswith("```"):
                clean_text = clean_text[3:]
            if clean_text.endswith("```"):
                clean_text = clean_text[:-3]
            clean_text = clean_text.strip()
                
            start = clean_text.find("{")
            end = clean_text.rfind("}") + 1
            if start >= 0 and end > start:
                json_str = clean_text[start:end]
                return json.loads(json_str)
            return None
        except Exception:
            return None

# Singleton instance
engine = CompanionEngine()
