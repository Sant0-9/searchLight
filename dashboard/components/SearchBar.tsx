'use client'

import { useState } from 'react'
import { SearchParams } from '@/app/apiClient'

interface SearchBarProps {
  onSearch: (params: SearchParams) => void
  loading: boolean
}

export default function SearchBar({ onSearch, loading }: SearchBarProps) {
  const [query, setQuery] = useState('')
  const [topK, setTopK] = useState(10)
  const [alpha, setAlpha] = useState(0.5)
  const [showAdvanced, setShowAdvanced] = useState(false)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (query.trim()) {
      onSearch({ query: query.trim(), topK, alpha })
    }
  }

  return (
    <div className="bg-white rounded-xl shadow-lg p-6">
      <form onSubmit={handleSubmit}>
        <div className="flex gap-3">
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search for documents..."
            className="flex-1 px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
            disabled={loading}
          />
          <button
            type="submit"
            disabled={loading || !query.trim()}
            className="px-8 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed font-medium transition-colors"
          >
            {loading ? ' Searching...' : ' Search'}
          </button>
        </div>

        <div className="mt-4">
          <button
            type="button"
            onClick={() => setShowAdvanced(!showAdvanced)}
            className="text-sm text-gray-600 hover:text-gray-900"
          >
            {showAdvanced ? '' : ''} Advanced Options
          </button>
        </div>

        {showAdvanced && (
          <div className="mt-4 grid grid-cols-2 gap-6 pt-4 border-t border-gray-200">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Results: {topK}
              </label>
              <input
                type="range"
                min="5"
                max="50"
                step="5"
                value={topK}
                onChange={(e) => setTopK(parseInt(e.target.value))}
                className="w-full"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Alpha (Hybrid): {alpha.toFixed(2)}
                <span className="ml-2 text-xs text-gray-500">
                  {alpha === 0 ? '(Keyword Only)' : alpha === 1 ? '(Vector Only)' : '(Hybrid)'}
                </span>
              </label>
              <input
                type="range"
                min="0"
                max="1"
                step="0.1"
                value={alpha}
                onChange={(e) => setAlpha(parseFloat(e.target.value))}
                className="w-full"
              />
              <div className="flex justify-between text-xs text-gray-500 mt-1">
                <span>Keyword</span>
                <span>Vector</span>
              </div>
            </div>
          </div>
        )}
      </form>
    </div>
  )
}
