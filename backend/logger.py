import sys
from loguru import logger
import os

def setup_logging():
    # Remove default handler
    logger.remove()
    
    # Standard format
    log_format = (
        "<green>{time:YYYY-MM-DD HH:mm:ss}</green> | "
        "<level>{level: <8}</level> | "
        "<cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - "
        "<level>{message}</level>"
    )
    
    # Console handler
    logger.add(sys.stdout, format=log_format, level="INFO")
    
    # File handler for production auditing
    os.makedirs("logs", exist_ok=True)
    logger.add("logs/sciai.log", rotation="10 MB", retention="10 days", format=log_format, level="INFO")
    
    return logger

# Initialize logger instance
log = setup_logging()
