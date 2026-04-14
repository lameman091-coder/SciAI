import sqlite3
import os

db_path = "sciai.db"

if not os.path.exists(db_path):
    print(f"Database {db_path} not found.")
    exit(1)

conn = sqlite3.connect(db_path)
cursor = conn.cursor()

print("Schema for saved_articles:")
cursor.execute("SELECT sql FROM sqlite_master WHERE type='table' AND name='saved_articles';")
print(cursor.fetchone()[0])

print("\nDetailed table_info:")
cursor.execute("PRAGMA table_info(saved_articles);")
for row in cursor.fetchall():
    print(row)

conn.close()
