import httpx
import asyncio
import sys

async def test_backend():
    print("--- SciAI Backend Functionality Test ---")
    url = "http://localhost:8000"
    
    async with httpx.AsyncClient(timeout=30) as client:
        # 1. Test Articles Search
        print("\n1. Testing Article Search (Query: 'Quantum')")
        try:
            resp = await client.get(f"{url}/articles?query=Quantum")
            if resp.status_code == 200:
                data = resp.json()
                articles = data.get("articles", [])
                print(f"   [OK] Received {len(articles)} articles.")
                if not articles:
                    print("   [WARN] No articles found. Check API connectivity or query.")
            else:
                print(f"   [ERROR] Articles API returned Status {resp.status_code}")
        except Exception as e:
            print(f"   [ERROR] Articles API call failed: {e}")

        # 2. Test Trending Articles
        print("\n2. Testing Trending Articles")
        try:
            resp = await client.get(f"{url}/articles/trending")
            if resp.status_code == 200:
                data = resp.json()
                articles = data.get("articles", [])
                print(f"   [OK] Received {len(articles)} trending articles.")
            else:
                print(f"   [ERROR] Trending API returned Status {resp.status_code}")
        except Exception as e:
            print(f"   [ERROR] Trending API call failed: {e}")

        # 3. Test Library (Books) for NPE protection
        print("\n3. Testing Library (User: test_user)")
        try:
            resp = await client.get(f"{url}/books?user_id=test_user")
            if resp.status_code == 200:
                books = resp.json()
                print(f"   [OK] Received {len(books)} books.")
                for b in books:
                    if b.get("preview") is None:
                        print(f"   [CRITICAL] Book {b['id']} has NULL preview! App will crash.")
                    else:
                        print(f"   [OK] Book {b['id']} has preview: '{b['preview'][:30]}...'")
            else:
                print(f"   [ERROR] Books API returned Status {resp.status_code}")
        except Exception as e:
            print(f"   [ERROR] Books API call failed: {e}")

if __name__ == "__main__":
    asyncio.run(test_backend())
