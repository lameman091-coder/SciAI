import sqlite3
import os

db_path = "sciai.db"

if not os.path.exists(db_path):
    print(f"Database {db_path} not found.")
    exit(1)

conn = sqlite3.connect(db_path)
cursor = conn.cursor()

cursor.execute("SELECT count(*) FROM saved_articles;")
count = cursor.fetchone()[0]
print(f"Total rows in saved_articles: {count}")

conn.close()
