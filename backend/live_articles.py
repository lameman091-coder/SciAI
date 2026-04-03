import httpx
import asyncio
import xml.etree.ElementTree as ET
import random
import urllib.parse
import hashlib
from datetime import datetime
from diskcache import Cache
from logger import log
from config import settings
from typing import List, Dict, Any, Tuple, Optional

# Simple persistent disk cache
cache = Cache(settings.CACHE_DIR)

# Global shared client for efficiency
_client: Optional[httpx.AsyncClient] = None

def get_client() -> httpx.AsyncClient:
    global _client
    if _client is None or _client.is_closed:
        _client = httpx.AsyncClient(timeout=20, follow_redirects=True)
    return _client

def generate_article_id(title: str, link: str) -> str:
    """Generate a stable unique ID for an article."""
    raw = f"{title}_{link}".encode('utf-8')
    return hashlib.md5(raw).hexdigest()

# ── CATEGORY MAPPING ──
ARXIV_CATEGORY_MAP = {
    "biology": "q-bio.*",
    "bio": "q-bio.*",
    "chemistry": "chem-ph",
    "physics": "physics.*",
    "all": ""
}

PUBMED_DOMAIN_MAP = {
    "biology": "(\"Biology\"[MeSH Major Topic] OR \"Biology\"[Title/Abstract])",
    "bio": "(\"Biology\"[MeSH Major Topic] OR \"Biology\"[Title/Abstract])",
    "chemistry": "(\"Chemistry\"[MeSH Major Topic] OR \"Chemistry\"[Title/Abstract])",
    "chem": "(\"Chemistry\"[MeSH Major Topic] OR \"Chemistry\"[Title/Abstract])",
    "physics": "(\"Physics\"[MeSH Major Topic] OR \"Physics\"[Title/Abstract])",
    "all": ""
}

TRENDING_TOPICS = [
    "CRISPR gene editing", "mRNA vaccine", "quantum computing",
    "artificial intelligence drug discovery", "nanomaterials",
    "climate change mitigation", "protein folding", "stem cell therapy",
    "graphene applications", "neuroscience consciousness",
    "cancer immunotherapy", "dark matter detection"
]

async def fetch_wikipedia(client: httpx.AsyncClient, query: str) -> List[Dict[str, Any]]:
    cache_key = f"wiki_{query}"
    if cache_key in cache:
        return cache[cache_key]
        
    encoded_query = urllib.parse.quote(query)
    url = f"https://en.wikipedia.org/api/rest_v1/page/summary/{encoded_query}"
    try:
        response = await client.get(url, timeout=10)
        if response.status_code == 200:
            data = response.json()
            link = data.get("content_urls", {}).get("desktop", {}).get("page", "")
            title = data.get("title", "No Title")
            result = [{
                "id": generate_article_id(title, link),
                "title": title,
                "summary": data.get("extract", "No extract available."),
                "source": "Wikipedia",
                "link": link,
                "score": 0.4,
                "authors": "Wikipedia Contributors",
                "journal": "Wikipedia Encyclopedia",
                "date": data.get("timestamp", "")[:10] if data.get("timestamp") else "Last Updated",
                "tier": "background"
            }]
            cache.set(cache_key, result, expire=3600*24) # 24h for Wiki
            return result
    except Exception as e:
        log.error(f"Wikipedia API error: {e}")
    return []

async def fetch_pubmed(client: httpx.AsyncClient, query: str, sort="pub+date", retstart=0, retmax=10, domain="") -> Tuple[List[Dict[str, Any]], int]:
    cache_key = f"pubmed_{query}_{domain}_{sort}_{retstart}_{retmax}"
    if cache_key in cache:
        return cache[cache_key]

    search_query = query
    if domain and domain.lower() not in ("all", ""):
        domain_term = PUBMED_DOMAIN_MAP.get(domain.lower(), "")
        if domain_term:
            search_query = f"{query} AND {domain_term}"

    search_url = (
        f"https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi"
        f"?db=pubmed&term={search_query}&retmode=json&retmax={retmax}"
        f"&retstart={retstart}&sort={sort}"
    )
    
    try:
        log.info(f"PubMed: Searching for '{search_query}' (retmax={retmax})")
        # Add slight jitter to stagger parallel requests naturally
        await asyncio.sleep(random.uniform(0.1, 0.5))
        
        search_res = await client.get(search_url, timeout=15)
        if search_res.status_code == 429:
            log.warning("PubMed search hit 429 Rate Limit. Retrying after 1.5s...")
            await asyncio.sleep(1.5)
            search_res = await client.get(search_url, timeout=15)
            
        if search_res.status_code != 200:
            log.error(f"PubMed search failed with status {search_res.status_code}")
            return [], 0
            
        search_data = search_res.json()
        id_list = search_data.get('esearchresult', {}).get('idlist', [])
        total_count = int(search_data.get('esearchresult', {}).get('count', '0'))
        
        log.info(f"PubMed search returned {len(id_list)} IDs (Total: {total_count})")
        
        if not id_list:
            return [], 0
        
        ids = ",".join(id_list)
        fetch_url = f"https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id={ids}&retmode=xml"
        
        # Add stagger before fetch
        await asyncio.sleep(random.uniform(0.2, 0.6))
        fetch_res = await client.get(fetch_url, timeout=25)
        if fetch_res.status_code == 429:
            log.warning("PubMed efetch hit 429 Rate Limit. Retrying after 2s...")
            await asyncio.sleep(2.0)
            fetch_res = await client.get(fetch_url, timeout=25)
        
        results = []
        if fetch_res.status_code == 200:
            root = ET.fromstring(fetch_res.content)
            for article in root.findall('.//PubmedArticle'):
                try:
                    title_elem = article.find('.//ArticleTitle')
                    title = "".join(title_elem.itertext()) if title_elem is not None else "Untitled"
                    
                    abstract_parts = article.findall('.//AbstractText')
                    abstract = " ".join(["".join(p.itertext()) for p in abstract_parts if p is not None])
                    
                    journal_elem = article.find('.//Journal/Title')
                    journal = journal_elem.text if journal_elem is not None else "Unknown"
                    
                    authors = []
                    for author in article.findall('.//Author'):
                        ln = author.find('LastName')
                        fn = author.find('ForeName')
                        if ln is not None:
                            authors.append(f"{ln.text} {fn.text if fn is not None else ''}")
                    
                    pmid_elem = article.find('.//PMID')
                    pmid = pmid_elem.text if pmid_elem is not None else ""
                    link = f"https://pubmed.ncbi.nlm.nih.gov/{pmid}/" if pmid else ""
                    
                    results.append({
                        "id": generate_article_id(title, link),
                        "title": title.strip(),
                        "summary": abstract.strip() or "No abstract available.",
                        "source": "PubMed",
                        "link": link,
                        "score": 1.0,
                        "authors": ", ".join(authors[:3]) or "Various Authors",
                        "journal": journal.strip(),
                        "date": "Recent",
                        "tier": "peer_reviewed"
                    })
                except Exception as item_err:
                    log.error(f"PubMed item parse error: {item_err}")
            
            log.info(f"PubMed: Successfully parsed {len(results)} articles.")
            if results:
                cache.set(cache_key, (results, total_count), expire=3600)
            return results, total_count
        else:
            log.error(f"PubMed efetch failed with status {fetch_res.status_code}")
    except Exception as e:
        log.error(f"PubMed final error: {type(e).__name__}: {e}")
    return [], 0

async def fetch_arxiv(client: httpx.AsyncClient, query: str, sort_by="submittedDate", start=0, max_results=10, domain="") -> Tuple[List[Dict[str, Any]], int]:
    cache_key = f"arxiv_{query}_{domain}_{sort_by}_{start}_{max_results}"
    if cache_key in cache:
        return cache[cache_key]

    category_prefix = ARXIV_CATEGORY_MAP.get(domain.lower(), "") if domain else ""
    arxiv_query = f"all:{query}+AND+cat:{category_prefix}*" if category_prefix else f"all:{query}"
    
    url = (
        f"http://export.arxiv.org/api/query"
        f"?search_query={arxiv_query}&start={start}&max_results={max_results}"
        f"&sortBy={sort_by}&sortOrder=descending"
    )
    
    try:
        response = await client.get(url, timeout=15)
        if response.status_code == 200:
            ns = {'atom': 'http://www.w3.org/2005/Atom', 'opensearch': 'http://a9.com/-/spec/opensearch/1.1/'}
            root = ET.fromstring(response.content)
            
            total_elem = root.find('opensearch:totalResults', ns)
            total_count = int(total_elem.text) if total_elem is not None else 0
            
            results = []
            for entry in root.findall('atom:entry', ns):
                title = entry.find('atom:title', ns).text.strip().replace('\n', ' ')
                summary = entry.find('atom:summary', ns).text.strip().replace('\n', ' ')
                summary = (summary[:497] + "...") if len(summary) > 500 else summary
                
                author_names = [a.text for a in entry.findall('atom:author/atom:name', ns) if a.text]
                authors_str = ", ".join(author_names[:3]) + ("..." if len(author_names) > 3 else "")
                
                published = entry.find('atom:published', ns).text[:10]
                link = ""
                for link_elem in entry.findall('atom:link', ns):
                    if link_elem.get('rel') == 'alternate':
                        link = link_elem.get('href', '')
                        break
                
                results.append({
                    "id": generate_article_id(title, link),
                    "title": title, "summary": summary, "source": "arXiv", "link": link,
                    "score": 0.7, "authors": authors_str or "Various Authors",
                    "journal": "arXiv Preprint", "date": published, "tier": "preprint"
                })
            cache.set(cache_key, (results, total_count), expire=3600)
            return results, total_count
    except Exception as e:
        log.error(f"arXiv API error: {e}")
    return [], 0

async def search_articles(query: str, sort="pub+date", page=1, limit=10,
                          source_filter="all", domain_filter="all",
                          date_filter="all", type_filter="all") -> Dict[str, Any]:
    log.info(f"Article Search: '{query}' | Page: {page}")
    
    retstart = (page - 1) * limit
    fetch_pubmed_flag = source_filter.lower() in ("all", "pubmed") and type_filter.lower() != "preprint"
    fetch_arxiv_flag = source_filter.lower() in ("all", "arxiv") and type_filter.lower() != "peer_reviewed"

    client = get_client()
    tasks = []
    if fetch_pubmed_flag:
        log.info(f"Adding PubMed task for '{query}'")
        tasks.append(fetch_pubmed(client, query, sort=sort, retstart=retstart, retmax=limit, domain=domain_filter))
    if fetch_arxiv_flag:
        log.info(f"Adding arXiv task for '{query}'")
        arxiv_sort = "submittedDate" if sort == "pub+date" else "relevance"
        tasks.append(fetch_arxiv(client, query, sort_by=arxiv_sort, start=retstart, max_results=limit, domain=domain_filter))
    
    # ── Wikipedia Search (Latest first) ──
    try:
        if source_filter in ["all", "wikipedia"] and page == 1:
            log.info(f"Adding Wikipedia task for '{query}'")
            # Wikipedia doesn't have a formal sort parameter in REST API, uses relevance by default
            tasks.append(fetch_wikipedia(client, query))
    except Exception as e:
        log.error(f"Wikipedia setup error: {e}")

    if not tasks:
        log.warning(f"No tasks generated for Article Search with filters.")
        return {"articles": [], "total_count": 0, "page": page}

    try:
        responses = await asyncio.gather(*tasks, return_exceptions=True)
    except Exception as e:
        log.error(f"Gather failed: {e}")
        return {"articles": [], "total_count": 0, "page": page}

    all_articles = []
    total_count = 0
    
    for i, resp in enumerate(responses):
        if isinstance(resp, Exception):
            log.error(f"Task {i} failed with exception: {resp}")
            continue
            
        if isinstance(resp, tuple): # PubMed/arXiv
            articles, count = resp
            log.info(f"Source {i} returned {len(articles)} articles (Total in source: {count})")
            all_articles.extend(articles)
            total_count += count
        elif isinstance(resp, list): # Wiki
            log.info(f"Wikipedia returned {len(resp)} articles")
            all_articles.extend(resp)
            total_count += len(resp)
        else:
            log.warning(f"Unexpected response type from task {i}: {type(resp)}")

    # Final ID and Null-Safety cleanup before returning
    for art in all_articles:
        if not art.get("id"):
            # Synthetic fallback ID if backend/source failed
            raw = f"{art.get('title','')}_{art.get('link','')}_{art.get('source','')}"
            art["id"] = hashlib.md5(raw.encode('utf-8')).hexdigest()

    # Simple date filtering (last5)
    if date_filter.lower() == "last5":
        cutoff = datetime.now().year - 5
        all_articles = [
            a for a in all_articles
            if not a.get('date')
            or a.get('date') in ("No Date", "Recent", "Last Updated", "Unknown Date")
            or (len(a['date']) >= 4 and a['date'][:4].isdigit() and int(a['date'][:4]) >= cutoff)
        ]

    return {"articles": all_articles, "total_count": total_count, "page": page}

async def fetch_trending(limit=25) -> Dict[str, Any]:
    topics = random.sample(TRENDING_TOPICS, min(3, len(TRENDING_TOPICS)))
    log.info(f"Fetching trending for topics: {topics}")
    
    client = get_client()
    tasks = []
    log.info("Trending: Starting parallel fetch for topics...")
    for topic in topics:
        tasks.append(fetch_pubmed(client, topic, sort="pub+date", retmax=10))
        tasks.append(fetch_arxiv(client, topic, sort_by="submittedDate", max_results=5))
    
    responses = await asyncio.gather(*tasks, return_exceptions=True)

    all_articles = []
    for i, resp in enumerate(responses):
        if isinstance(resp, Exception):
            log.error(f"Trending: Task {i} failed: {resp}")
            continue
        if isinstance(resp, tuple):
            articles, count = resp
            all_articles.extend(articles)
    
    log.info(f"Trending: Total fetched {len(all_articles)} articles.")
    
    if not all_articles:
        # Fallback to extremely broad research keywords if specific topics fail
        log.warning("Trending: No articles found. Trying fallback broad topics...")
        broad_tasks = [
            fetch_pubmed(client, "science research", retmax=10),
            fetch_arxiv(client, "physics", max_results=10)
        ]
        broad_resp = await asyncio.gather(*broad_tasks, return_exceptions=True)
        for r in broad_resp:
            if isinstance(r, tuple):
                all_articles.extend(r[0])

    random.shuffle(all_articles)
    final = all_articles[:limit]
    return {"articles": final, "total_count": len(final), "page": 1}
