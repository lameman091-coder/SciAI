import os
import shutil

print("[SciAI] Initiating System Reset...")

# 1. Clear database
DB_FILE = "library_data.json"
if os.path.exists(DB_FILE):
    os.remove(DB_FILE)
    print(f"[SciAI] Deleted stale database: {DB_FILE}")

# 2. Clear temp files
TEMP_DIR = "temp"
if os.path.exists(TEMP_DIR):
    shutil.rmtree(TEMP_DIR)
    os.makedirs(TEMP_DIR)
    print(f"[SciAI] Cleared and rebuilt temp directory: {TEMP_DIR}")

# 3. Clear pycache
for root, dirs, files in os.walk("."):
    for d in dirs:
        if d == "__pycache__":
            shutil.rmtree(os.path.join(root, d))

print("[SciAI] Reset Complete. PLEASE RESTART main.py NOW.")
