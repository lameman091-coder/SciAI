import requests
import xml.etree.ElementTree as ET
import json
import random
from datetime import datetime

# ─────────────────────────────────────────────
# CATEGORY MAPPING: Domain → arXiv categories
# ─────────────────────────────────────────────
ARXIV_CATEGORY_MAP = {
    "biology": "q-bio",
    "bio": "q-bio",
    "chemistry": "chem-ph",
    "chem": "chem-ph",
    "physics": "physics",
    "all": ""
}

PUBMED_DOMAIN_MAP = {
    "biology": "biology",
    "bio": "biology",
    "chemistry": "chemistry",
    "chem": "chemistry",
    "physics": "physics",
    "all": ""
}

# ─────────────────────────────────────────────
# TRENDING TOPICS (rotated on each call)
# ─────────────────────────────────────────────
TRENDING_TOPICS = [
    "CRISPR gene editing",
    "mRNA vaccine",
    "quantum computing",
    "artificial intelligence drug discovery",
    "nanomaterials",
    "climate change mitigation",
    "protein folding",
    "stem cell therapy",
    "graphene applications",
    "neuroscience consciousness",
    "cancer immunotherapy",
    "dark matter detection"
]


def fetch_wikipedia(query: str, max_results=3):
    """
    Fetches real page summary from Wikipedia REST API as Background Knowledge.
    Tier 3 — score 0.4
    """
    encoded_query = requests.utils.quote(query)
    url = f"https://en.wikipedia.org/api/rest_v1/page/summary/{encoded_query}"
    
    try:
        response = requests.get(url, timeout=10)
        if response.status_code == 200:
            data = response.json()
            return [{
                "title": data.get("title", "No Title"),
                "summary": data.get("extract", "No extract available."),
                "source": "Wikipedia",
                "link": data.get("content_urls", {}).get("desktop", {}).get("page", ""),
                "score": 0.4,
                "authors": "Wikipedia Contributors",
                "journal": "Wikipedia Encyclopedia",
                "date": data.get("timestamp", "")[:10] if data.get("timestamp") else "Last Updated",
                "tier": "background"
            }]
    except Exception as e:
        print(f"[SciAI] Wikipedia API error: {e}")
    
    return []


def fetch_pubmed(query: str, sort="pub+date", retstart=0, retmax=10, domain=""):
    """
    Fetches research papers from PubMed with full metadata.
    Tier 1 — Peer Reviewed — score 1.0
    """
    # Append domain filter to query if provided
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
    results = []
    
    try:
        search_res = requests.get(search_url, timeout=15)
        if search_res.status_code == 200:
            search_data = search_res.json()
            id_list = search_data.get('esearchresult', {}).get('idlist', [])
            total_count = int(search_data.get('esearchresult', {}).get('count', '0'))
            
            if not id_list:
                return [], 0
            
            ids = ",".join(id_list)
            fetch_url = f"https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id={ids}&retmode=xml"
            fetch_res = requests.get(fetch_url, timeout=15)
            
            if fetch_res.status_code == 200:
                root = ET.fromstring(fetch_res.content)
                for article in root.findall('.//PubmedArticle'):
                    # Title
                    title_elem = article.find('.//ArticleTitle')
                    title = "".join(title_elem.itertext()) if title_elem is not None else "Untitled Paper"
                    
                    # Abstract
                    abstract_parts = article.findall('.//AbstractText')
                    abstract = " ".join(["".join(p.itertext()) for p in abstract_parts if p is not None])
                    
                    # Journal
                    journal_elem = article.find('.//Journal/Title')
                    journal = journal_elem.text if journal_elem is not None else "Unknown Journal"
                    
                    # Authors
                    authors = []
                    for author in article.findall('.//Author'):
                        last_name = author.find('LastName')
                        fore_name = author.find('ForeName')
                        if last_name is not None:
                            authors.append(f"{last_name.text} {fore_name.text if fore_name is not None else ''}")
                    authors_str = ", ".join(authors[:3]) + ("..." if len(authors) > 3 else "")
                    
                    # Date
                    pub_date = article.find('.//PubDate')
                    year = pub_date.find('Year').text if pub_date is not None and pub_date.find('Year') is not None else ""
                    month = pub_date.find('Month').text if pub_date is not None and pub_date.find('Month') is not None else ""
                    day = pub_date.find('Day').text if pub_date is not None and pub_date.find('Day') is not None else ""
                    
                    date_str = f"{year}-{month}-{day}".strip("-")
                    if not date_str:
                        medline_date = pub_date.find('MedlineDate') if pub_date is not None else None
                        date_str = medline_date.text if medline_date is not None else "No Date"
                    
                    pmid = article.find('.//PMID').text if article.find('.//PMID') is not None else ""
                    link = f"https://pubmed.ncbi.nlm.nih.gov/{pmid}/" if pmid else ""
                    
                    results.append({
                        "title": title.strip(),
                        "summary": abstract.strip() if abstract else "No abstract available for this research paper.",
                        "source": "PubMed",
                        "link": link,
                        "score": 1.0,
                        "authors": authors_str.strip() or "Various Authors",
                        "journal": journal.strip(),
                        "date": date_str,
                        "tier": "peer_reviewed"
                    })
            return results, total_count
    except Exception as e:
        print(f"[SciAI] PubMed API error: {e}")
        
    return results, 0


def fetch_arxiv(query: str, sort_by="submittedDate", start=0, max_results=10, domain=""):
    """
    Fetches preprint papers from arXiv API.
    Tier 2 — Preprint — score 0.7
    """
    # Map domain to arXiv category prefix
    category_prefix = ""
    if domain and domain.lower() not in ("all", ""):
        category_prefix = ARXIV_CATEGORY_MAP.get(domain.lower(), "")
    
    # Build search query
    if category_prefix:
        arxiv_query = f"all:{query}+AND+cat:{category_prefix}*"
    else:
        arxiv_query = f"all:{query}"
    
    # sort_by can be: submittedDate, relevance, lastUpdatedDate
    sort_order = "descending"
    
    url = (
        f"http://export.arxiv.org/api/query"
        f"?search_query={arxiv_query}"
        f"&start={start}&max_results={max_results}"
        f"&sortBy={sort_by}&sortOrder={sort_order}"
    )
    
    results = []
    
    try:
        response = requests.get(url, timeout=15)
        if response.status_code == 200:
            # arXiv uses Atom XML with namespaces
            ns = {
                'atom': 'http://www.w3.org/2005/Atom',
                'opensearch': 'http://a9.com/-/spec/opensearch/1.1/'
            }
            root = ET.fromstring(response.content)
            
            # Get total results count
            total_elem = root.find('opensearch:totalResults', ns)
            total_count = int(total_elem.text) if total_elem is not None else 0
            
            for entry in root.findall('atom:entry', ns):
                # Title
                title_elem = entry.find('atom:title', ns)
                title = title_elem.text.strip().replace('\n', ' ') if title_elem is not None else "Untitled"
                
                # Summary
                summary_elem = entry.find('atom:summary', ns)
                summary = summary_elem.text.strip().replace('\n', ' ') if summary_elem is not None else "No abstract available."
                # Truncate very long abstracts
                if len(summary) > 500:
                    summary = summary[:497] + "..."
                
                # Authors
                author_elems = entry.findall('atom:author/atom:name', ns)
                author_names = [a.text for a in author_elems if a.text]
                authors_str = ", ".join(author_names[:3]) + ("..." if len(author_names) > 3 else "")
                
                # Date (published)
                published_elem = entry.find('atom:published', ns)
                date_str = ""
                if published_elem is not None and published_elem.text:
                    # Format: 2023-01-15T12:00:00Z → 2023-01-15
                    date_str = published_elem.text[:10]
                
                # Link (prefer abs link)
                link = ""
                for link_elem in entry.findall('atom:link', ns):
                    if link_elem.get('title') == 'pdf':
                        continue
                    if link_elem.get('rel') == 'alternate':
                        link = link_elem.get('href', '')
                        break
                if not link:
                    id_elem = entry.find('atom:id', ns)
                    link = id_elem.text if id_elem is not None else ""
                
                # Categories
                categories = []
                for cat in entry.findall('atom:category', ns):
                    term = cat.get('term', '')
                    if term:
                        categories.append(term)
                
                results.append({
                    "title": title,
                    "summary": summary,
                    "source": "arXiv",
                    "link": link,
                    "score": 0.7,
                    "authors": authors_str or "Various Authors",
                    "journal": f"arXiv ({', '.join(categories[:2])})" if categories else "arXiv Preprint",
                    "date": date_str or "No Date",
                    "tier": "preprint"
                })
            
            return results, total_count
    except Exception as e:
        print(f"[SciAI] arXiv API error: {e}")
    
    return results, 0


def search_articles(query: str, sort="pub+date", page=1, limit=10,
                    source_filter="all", domain_filter="all",
                    date_filter="all", type_filter="all"):
    """
    Main aggregator for multi-source research data.
    Returns dict: { "articles": [...], "total_count": N, "page": P }
    """
    print(f"[SciAI] Fetching articles for: '{query}' | Sort: {sort} | Page: {page} | "
          f"Source: {source_filter} | Domain: {domain_filter} | Date: {date_filter} | Type: {type_filter}")
    
    retstart = (page - 1) * limit
    
    all_articles = []
    total_count = 0
    
    # ── Determine which sources to fetch ──
    fetch_pubmed_flag = source_filter.lower() in ("all", "pubmed")
    fetch_arxiv_flag = source_filter.lower() in ("all", "arxiv")
    fetch_wiki_flag = source_filter.lower() in ("all", "wikipedia")
    
    # Type filter overrides
    if type_filter.lower() == "peer_reviewed":
        fetch_arxiv_flag = False
        fetch_wiki_flag = False
    elif type_filter.lower() == "preprint":
        fetch_pubmed_flag = False
        fetch_wiki_flag = False
    
    # ── Date filter: modify query for PubMed ──
    pubmed_date_filter = ""
    if date_filter.lower() == "last5":
        # PubMed date range: last 5 years
        current_year = datetime.now().year
        pubmed_date_filter = f"&mindate={current_year - 5}&maxdate={current_year}&datetype=pdat"
    
    # ── Fetch PubMed (Tier 1) ──
    pubmed_results = []
    pubmed_total = 0
    if fetch_pubmed_flag:
        arxiv_sort = "submittedDate" if sort == "pub+date" else "relevance"
        pubmed_results, pubmed_total = fetch_pubmed(
            query, sort=sort, retstart=retstart, retmax=limit, domain=domain_filter
        )
        all_articles.extend(pubmed_results)
        total_count += pubmed_total
    
    # ── Fetch arXiv (Tier 2) ──
    arxiv_results = []
    arxiv_total = 0
    if fetch_arxiv_flag:
        arxiv_sort = "submittedDate" if sort == "pub+date" else "relevance"
        arxiv_results, arxiv_total = fetch_arxiv(
            query, sort_by=arxiv_sort, start=retstart, max_results=limit, domain=domain_filter
        )
        all_articles.extend(arxiv_results)
        total_count += arxiv_total
    
    # ── Fetch Wikipedia (Tier 3) — only page 1 ──
    if fetch_wiki_flag and page == 1:
        wiki_results = fetch_wikipedia(query)
        all_articles.extend(wiki_results)
        total_count += len(wiki_results)
    
    # ── Date filter post-processing (for arXiv results) ──
    if date_filter.lower() == "last5":
        cutoff_year = datetime.now().year - 5
        filtered = []
        for art in all_articles:
            if art.get("date", "No Date") == "No Date":
                filtered.append(art)
                continue
            try:
                year = int(art["date"][:4])
                if year >= cutoff_year:
                    filtered.append(art)
            except (ValueError, IndexError):
                filtered.append(art)
        all_articles = filtered
    
    # ── Format and ensure all fields ──
    final_output = []
    for art in all_articles:
        final_output.append({
            "title": art.get("title") or "Untitled",
            "summary": art.get("summary") or "No description available.",
            "source": art.get("source") or "Research Source",
            "link": art.get("link") or "",
            "score": float(art.get("score", 0.0)),
            "authors": art.get("authors", "Various Authors"),
            "journal": art.get("journal", "Research Journal"),
            "date": art.get("date", "Unknown Date"),
            "tier": art.get("tier", "peer_reviewed")
        })
    
    return {
        "articles": final_output,
        "total_count": total_count,
        "page": page
    }


def fetch_trending(limit=25):
    """
    Fetch trending articles from multiple sources using rotating topics.
    Called on app screen open — no search query needed.
    Uses 3 topics × (5 PubMed + 3 arXiv) = ~24 articles for a rich feed.
    """
    # Pick 3 random trending topics for variety without too many API calls
    topics = random.sample(TRENDING_TOPICS, min(3, len(TRENDING_TOPICS)))
    
    all_articles = []
    
    for topic in topics:
        print(f"[SciAI] Fetching trending: '{topic}'")
        
        # PubMed — 5 latest per topic
        pubmed_results, _ = fetch_pubmed(topic, sort="pub+date", retstart=0, retmax=5)
        all_articles.extend(pubmed_results)
        
        # arXiv — 3 latest per topic
        arxiv_results, _ = fetch_arxiv(topic, sort_by="submittedDate", start=0, max_results=3)
        all_articles.extend(arxiv_results)
    
    # Shuffle to mix sources
    random.shuffle(all_articles)
    
    # Limit total to requested amount
    all_articles = all_articles[:limit]
    
    # Format
    final_output = []
    for art in all_articles:
        final_output.append({
            "title": art.get("title") or "Untitled",
            "summary": art.get("summary") or "No description available.",
            "source": art.get("source") or "Research Source",
            "link": art.get("link") or "",
            "score": float(art.get("score", 0.0)),
            "authors": art.get("authors", "Various Authors"),
            "journal": art.get("journal", "Research Journal"),
            "date": art.get("date", "Unknown Date"),
            "tier": art.get("tier", "peer_reviewed")
        })
    
    return {
        "articles": final_output,
        "total_count": len(final_output),
        "page": 1
    }

