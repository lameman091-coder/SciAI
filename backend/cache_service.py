import json
import hashlib
from typing import Any, Optional
import redis.asyncio as redis
from config import settings
from logger import log

class CacheService:
    def __init__(self):
        self.redis_client: Optional[redis.Redis] = None
        self.fallback_cache = {}
        self.is_redis_available = False
    
    async def connect(self):
        try:
            self.redis_client = redis.Redis(
                host=settings.REDIS_HOST if hasattr(settings, 'REDIS_HOST') else '127.0.0.1',
                port=settings.REDIS_PORT if hasattr(settings, 'REDIS_PORT') else 6379,
                db=settings.REDIS_DB if hasattr(settings, 'REDIS_DB') else 0,
                decode_responses=True,
                socket_connect_timeout=5,
                socket_timeout=5
            )
            await self.redis_client.ping()
            self.is_redis_available = True
            log.info("Redis cache connected successfully")
        except Exception as e:
            log.warning(f"Redis unavailable, using in-memory fallback: {e}")
            self.is_redis_available = False
    
    async def close(self):
        if self.redis_client:
            await self.redis_client.close()
    
    def _hash_key(self, key: str) -> str:
        return hashlib.sha256(key.encode()).hexdigest()[:32]
    
    async def get(self, key: str) -> Optional[Any]:
        if self.is_redis_client:
            try:
                value = await self.redis_client.get(self._hash_key(key))
                return json.loads(value) if value else None
            except Exception as e:
                log.warning(f"Redis get error: {e}")
        
        return self.fallback_cache.get(key)
    
    async def set(self, key: str, value: Any, ttl: int = 300):
        cached_value = json.dumps(value)
        
        if self.is_redis_available and self.redis_client:
            try:
                await self.redis_client.setex(self._hash_key(key), ttl, cached_value)
                return
            except Exception as e:
                log.warning(f"Redis set error: {e}")
        
        self.fallback_cache[key] = value
    
    async def delete(self, key: str):
        if self.is_redis_available and self.redis_client:
            try:
                await self.redis_client.delete(self._hash_key(key))
            except Exception as e:
                log.warning(f"Redis delete error: {e}")
        
        self.fallback_cache.pop(key, None)
    
    async def clear_pattern(self, pattern: str):
        if self.is_redis_available and self.redis_client:
            try:
                keys = []
                async for key in self.redis_client.scan_iter(match=f"*{pattern}*"):
                    keys.append(key)
                if keys:
                    await self.redis_client.delete(*keys)
            except Exception as e:
                log.warning(f"Redis clear pattern error: {e}")
        
        keys_to_delete = [k for k in self.fallback_cache if pattern in k]
        for k in keys_to_delete:
            del self.fallback_cache[k]
    
    @property
    def is_redis_client(self):
        return self.is_redis_available and self.redis_client is not None

cache_service = CacheService()