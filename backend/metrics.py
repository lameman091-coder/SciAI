from prometheus_client import Counter, Histogram, Gauge, generate_latest, CONTENT_TYPE_LATEST
from fastapi import FastAPI, Response, Request
import time

request_count = Counter(
    'sciai_requests_total',
    'Total number of API requests',
    ['method', 'endpoint', 'status']
)

request_duration = Histogram(
    'sciai_request_duration_seconds',
    'Request duration in seconds',
    ['method', 'endpoint']
)

active_requests = Gauge(
    'sciai_active_requests',
    'Number of active requests'
)

llm_calls = Counter(
    'sciai_llm_calls_total',
    'Total LLM API calls',
    ['provider', 'status']
)

llm_latency = Histogram(
    'sciai_llm_latency_seconds',
    'LLM request latency',
    ['provider']
)

rag_searches = Counter(
    'sciai_rag_searches_total',
    'Total RAG searches',
    ['type']
)

cache_hits = Counter(
    'sciai_cache_hits_total',
    'Total cache hits',
    ['type']
)

cache_misses = Counter(
    'sciai_cache_misses_total',
    'Total cache misses',
    ['type']
)

db_queries = Counter(
    'sciai_db_queries_total',
    'Total database queries',
    ['operation']
)

class MetricsMiddleware:
    def __init__(self, app):
        self.app = app
    
    async def __call__(self, scope, receive, send):
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return
        
        status_code = [200] # Use list for closure mutation
        
        async def send_wrapper(message):
            if message["type"] == "http.response.start":
                status_code[0] = message["status"]
            await send(message)
        
        method = scope["method"]
        path = scope["path"]
        
        active_requests.inc()
        start_time = time.time()
        
        try:
            await self.app(scope, receive, send_wrapper)
        except Exception as e:
            status_code[0] = 500
            raise e
        finally:
            duration = time.time() - start_time
            request_count.labels(method=method, endpoint=path, status=status_code[0]).inc()
            request_duration.labels(method=method, endpoint=path).observe(duration)
            active_requests.dec()

def setup_metrics(app: FastAPI):
    # Register metrics endpoint on the root app
    @app.get("/metrics")
    async def metrics():
        return Response(content=generate_latest(), media_type=CONTENT_TYPE_LATEST)
    
    # Add middleware standard way
    app.add_middleware(MetricsMiddleware)
    
    return app