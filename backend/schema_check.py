import sqlite3
c = sqlite3.connect('sciai.db')
cursor = c.cursor()
cursor.execute("SELECT sql FROM sqlite_master WHERE type='table' AND name='saved_articles';")
print(cursor.fetchone()[0])
