#!/usr/bin/env python3
"""
Mock embedding service for development.
Returns deterministic random vectors based on text hash.
"""

from fastapi import FastAPI
from pydantic import BaseModel
import hashlib
import numpy as np
from typing import List
import uvicorn

app = FastAPI()

DIMENSION = 384


class EmbedRequest(BaseModel):
    text: str


class BatchEmbedRequest(BaseModel):
    texts: List[str]


def generate_embedding(text: str) -> List[float]:
    """Generate a deterministic embedding based on text hash."""
    # Create deterministic seed from text
    seed = int(hashlib.md5(text.encode()).hexdigest(), 16) % (2**32)
    rng = np.random.RandomState(seed)
    
    # Generate random vector
    vector = rng.randn(DIMENSION).astype(np.float32)
    
    # Normalize to unit length
    norm = np.linalg.norm(vector)
    if norm > 0:
        vector = vector / norm
    
    return vector.tolist()


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/embed")
def embed(request: EmbedRequest):
    embedding = generate_embedding(request.text)
    return {"embedding": embedding}


@app.post("/embed/batch")
def embed_batch(request: BatchEmbedRequest):
    embeddings = [generate_embedding(text) for text in request.texts]
    return {"embeddings": embeddings}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
