from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship
from sqlalchemy import String, Float, ForeignKey, Text, DateTime
from datetime import datetime
from typing import List, Optional
import uuid
from config import settings
from logger import log

# Create async engine with connection pooling
engine = create_async_engine(
    settings.DATABASE_URL,
    echo=False,
    pool_size=10,
    max_overflow=20,
    pool_pre_ping=True,
    pool_recycle=3600
)

# Async session factory
AsyncSessionLocal = async_sessionmaker(
    bind=engine,
    class_=AsyncSession,
    expire_on_commit=False
)

class Base(DeclarativeBase):
    pass

class User(Base):
    __tablename__ = "users"
    
    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    
    books = relationship("Book", back_populates="owner", cascade="all, delete-orphan")
    saved_articles = relationship("SavedArticle", back_populates="owner", cascade="all, delete-orphan")

class Book(Base):
    __tablename__ = "books"
    
    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"))
    title: Mapped[str] = mapped_column(String(255))
    domain: Mapped[str] = mapped_column(String(100), default="General")
    preview: Mapped[str] = mapped_column(Text, default="")
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    
    owner = relationship("User", back_populates="books")

class SavedArticle(Base):
    __tablename__ = "saved_articles"
    
    id: Mapped[str] = mapped_column(String(255), primary_key=True)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"), primary_key=True)
    title: Mapped[str] = mapped_column(String(512))
    summary: Mapped[str] = mapped_column(Text)
    source: Mapped[str] = mapped_column(String(100))
    link: Mapped[str] = mapped_column(String(1024), default="")
    score: Mapped[float] = mapped_column(Float, default=0.0)
    authors: Mapped[str] = mapped_column(String(255), default="Various Authors")
    journal: Mapped[str] = mapped_column(String(255), default="Nature / PubMed")
    date: Mapped[str] = mapped_column(String(50), default="Unknown Date")
    tier: Mapped[str] = mapped_column(String(50), default="peer_reviewed")
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    
    owner = relationship("User", back_populates="saved_articles")

async def init_db():
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

# Dependency to get session
async def get_db():
    async with AsyncSessionLocal() as session:
        yield session
