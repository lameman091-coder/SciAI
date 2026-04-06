import httpx
import asyncio
import os

async def test_upload():
    print("--- Testing PDF Upload & Preview Logic ---")
    url = "http://localhost:8000"
    
    # Create a dummy PDF
    pdf_path = "test_dummy.pdf"
    with open(pdf_path, "wb") as f:
        # A minimal PDF header
        f.write(b"%PDF-1.1\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n3 0 obj\n<< /Type /Page /Parent 2 0 R /Contents 4 0 R >>\nendobj\n4 0 obj\n<< /Length 50 >>\nstream\nBT /F1 12 Tf 100 700 Td (This is a test PDF for SciAI preview logic.) Tj ET\nendstream\nendobj\nxref\n0 5\n0000000000 65535 f\n0000000009 00000 n\n0000000058 00000 n\n0000000115 00000 n\n0000000178 00000 n\ntrailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n278\n%%EOF")

    async with httpx.AsyncClient(timeout=60) as client:
        print("\n1. Uploading PDF...")
        files = {'file': open(pdf_path, 'rb')}
        data = {'user_id': 'test_user', 'domain': 'Science'}
        resp = await client.post(f"{url}/upload-book", files=files, data=data)
        
        if resp.status_code == 200:
            print(f"   [OK] Upload Success: {resp.json()}")
            
            # Wait for background task
            print("\n2. Waiting for background processing (5s)...")
            await asyncio.sleep(5)
            
            print("\n3. Checking Library for Preview...")
            resp = await client.get(f"{url}/books?user_id=test_user")
            books = resp.json()
            for b in books:
                if b['title'] == 'test_dummy.pdf':
                    if b.get('preview'):
                        print(f"   [SUCCESS] Book has preview: '{b['preview']}'")
                    else:
                        print("   [CRITICAL] Book Preview is STILL NULL or Empty!")
        else:
            print(f"   [ERROR] Upload failed: {resp.status_code} - {resp.text}")

    if os.path.exists(pdf_path):
        os.remove(pdf_path)

if __name__ == "__main__":
    asyncio.run(test_upload())
