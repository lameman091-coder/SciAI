import sqlite3
import os

def migrate(db_path):
    if not os.path.exists(db_path):
        print(f"Skipping {db_path} - file not found.")
        return
    
    print(f"Migrating {db_path}...")
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # Check current columns
        cursor.execute("PRAGMA table_info(saved_articles);")
        columns = [row[1] for row in cursor.fetchall()]
        
        # Add missing columns
        if "link" not in columns:
            print(" - Adding column 'link'")
            cursor.execute("ALTER TABLE saved_articles ADD COLUMN link VARCHAR(1024) DEFAULT '';")
        
        if "tier" not in columns:
            print(" - Adding column 'tier'")
            cursor.execute("ALTER TABLE saved_articles ADD COLUMN tier VARCHAR(50) DEFAULT 'peer_reviewed';")
        
        conn.commit()
        conn.close()
        print("Done.")
    except Exception as e:
        print(f"Error migrating {db_path}: {e}")

if __name__ == "__main__":
    # Get current working directory to help debug paths
    print(f"Current Working Directory: {os.getcwd()}")
    
    # Migrate root database (relative to project root)
    # If this script is run from backend/, then sciai.db is ../sciai.db
    # If run from root, it is sciai.db
    
    migrate("sciai.db")
    migrate("backend/sciai.db")
    migrate("../sciai.db") # Cover case where we are in backend/
