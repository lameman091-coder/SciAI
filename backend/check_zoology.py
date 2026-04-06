import sqlite3
import os

db_path = 'c:/Users/HP/AndroidStudioProjects/SciAI/backend/sciai.db'
if not os.path.exists(db_path):
    print("DB missing")
else:
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    cursor.execute("SELECT id, title, preview FROM books WHERE title LIKE '%zoology%'")
    rows = cursor.fetchall()
    if not rows:
        print("No zoology book found")
    else:
        for r in rows:
            print(f"ID: {r[0]}, Title: {r[1]}, PrevLen: {len(r[2])}")
    conn.close()
