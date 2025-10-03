'use client'

import { useState } from 'react'
import SearchBar from '@/components/SearchBar'
import Results from '@/components/Results'
import { search, SearchParams, SearchResponse } from '@/app/apiClient'

export default function Home() {
  const [results, setResults] = useState<SearchResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSearch = async (params: SearchParams) => {
    setLoading(true)
    setError(null)
    
    try {
      const response = await search(params)
      setResults(response)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Search failed')
      setResults(null)
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="min-h-screen p-8">
      <div className="max-w-6xl mx-auto">
        <header className="mb-12 text-center">
          <h1 className="text-5xl font-bold mb-4 bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent">
             Searchlight
          </h1>
          <p className="text-gray-600 text-lg">
            Hybrid Search Engine • Keyword + Vector Similarity
          </p>
        </header>

        <SearchBar onSearch={handleSearch} loading={loading} />

        {error && (
          <div className="mt-6 p-4 bg-red-50 border border-red-200 rounded-lg text-red-800">
             {error}
          </div>
        )}

        {results && (
          <div className="mt-8">
            <div className="mb-4 text-sm text-gray-600">
              Found {results.total} results in {results.queryTimeMs}ms
              {results.query.hasVector && (
                <span className="ml-2 px-2 py-1 bg-blue-100 text-blue-800 rounded">
                  Vector Search Active (α={results.query.alpha})
                </span>
              )}
            </div>
            <Results results={results.results} />
          </div>
        )}

        {!results && !loading && (
          <div className="mt-20 text-center text-gray-500">
            <svg className="mx-auto h-24 w-24 text-gray-400 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <p className="text-lg">Enter a query to search the knowledge base</p>
          </div>
        )}
      </div>
    </main>
  )
}
