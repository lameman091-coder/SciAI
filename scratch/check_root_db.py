import sqlite3
import os

db_path = "sciai.db"

if not os.path.exists(db_path):
    print(f"Database {db_path} not found.")
    exit(1)

conn = sqlite3.connect(db_path)
cursor = conn.cursor()

print("Columns in saved_articles (ROOT DB):")
cursor.execute("PRAGMA table_info(saved_articles);")
columns = sorted([row[1] for row in cursor.fetchall()])
for col in columns:
    print(f" - {col}")

expected_columns = ["id", "user_id", "title", "summary", "source", "link", "score", "authors", "journal", "date", "tier", "created_at"]
missing = [col for col in expected_columns if col not in columns]

if missing:
    print("\nMissing columns:", missing)
else:
    print("\nAll columns found.")

conn.close()
