const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1'

export interface SearchParams {
  query: string
  topK: number
  alpha: number
  source?: string
}

export interface SearchResult {
  id: string
  sourceId: string
  title: string
  url: string
  snippet: string
  score: number
  timestamp: string
  source: string
  chunkIndex: number
}

export interface SearchResponse {
  results: SearchResult[]
  total: number
  offset: number
  limit: number
  queryTimeMs: number
  query: {
    text: string
    hasVector: boolean
    alpha: number
  }
}

export async function search(params: SearchParams): Promise<SearchResponse> {
  const requestBody = {
    q: params.query,
    k: params.topK,
    alpha: params.alpha,
    filters: params.source ? { source: params.source } : undefined,
  }

  const response = await fetch(`${API_BASE_URL}/search`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(requestBody),
  })

  if (!response.ok) {
    throw new Error(`Search failed: ${response.statusText}`)
  }

  return response.json()
}

export async function getHealth() {
  const response = await fetch(`${API_BASE_URL}/health`)
  
  if (!response.ok) {
    throw new Error('Health check failed')
  }

  return response.json()
}
