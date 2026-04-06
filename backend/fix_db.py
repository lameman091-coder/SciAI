import sqlite3
conn = sqlite3.connect('sciai.db')
c = conn.cursor()
c.execute("ALTER TABLE saved_articles ADD COLUMN link VARCHAR(1024) DEFAULT '';")
conn.commit()
conn.close()
print("Success")
