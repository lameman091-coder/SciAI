import sys
import json
import os
from datetime import datetime
from loguru import logger
import traceback

def setup_logging():
    logger.remove()
    
    log_format = (
        "<green>{time:YYYY-MM-DD HH:mm:ss}</green> | "
        "<level>{level: <8}</level> | "
        "<cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - "
        "<level>{message}</level>"
    )
    
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    logger.add(sys.stdout, format=log_format, level="INFO")
    
    os.makedirs("logs", exist_ok=True)
    logger.add("logs/sciai.log", rotation="10 MB", retention="10 days", format=log_format, level="INFO")
    
    return logger

log = setup_logging()

class StructuredLogger:
    @staticmethod
    def log(level: str, message: str, **kwargs):
        log_entry = {
            "timestamp": datetime.utcnow().isoformat(),
            "level": level.upper(),
            "message": message,
            **kwargs
        }
        
        if level.upper() == "ERROR" and "error" not in kwargs:
            log_entry["error"] = traceback.format_exc()
        
        if level.upper() == "INFO":
            log.info(json.dumps(log_entry))
        elif level.upper() == "WARNING":
            log.warning(json.dumps(log_entry))
        elif level.upper() == "ERROR":
            log.error(json.dumps(log_entry))
        else:
            log.debug(json.dumps(log_entry))
    
    @staticmethod
    def info(message: str, **kwargs):
        StructuredLogger.log("INFO", message, **kwargs)
    
    @staticmethod
    def warning(message: str, **kwargs):
        StructuredLogger.log("WARNING", message, **kwargs)
    
    @staticmethod
    def error(message: str, **kwargs):
        StructuredLogger.log("ERROR", message, **kwargs)
    
    @staticmethod
    def debug(message: str, **kwargs):
        StructuredLogger.log("DEBUG", message, **kwargs)
    
    @staticmethod
    def request(method: str, endpoint: str, status_code: int, duration_ms: float, **kwargs):
        log_entry = {
            "timestamp": datetime.utcnow().isoformat(),
            "event": "api_request",
            "method": method,
            "endpoint": endpoint,
            "status_code": status_code,
            "duration_ms": duration_ms,
            **kwargs
        }
        log.info(json.dumps(log_entry))
    
    @staticmethod
    def metric(name: str, value: float, unit: str = "", **kwargs):
        log_entry = {
            "timestamp": datetime.utcnow().isoformat(),
            "event": "metric",
            "metric_name": name,
            "value": value,
            "unit": unit,
            **kwargs
        }
        log.info(json.dumps(log_entry))

struct_log = StructuredLogger()