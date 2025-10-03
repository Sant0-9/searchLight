'use client'

import { SearchResult } from '@/app/apiClient'

interface ResultsProps {
  results: SearchResult[]
}

export default function Results({ results }: ResultsProps) {
  if (results.length === 0) {
    return (
      <div className="text-center py-12 text-gray-500">
        No results found. Try a different query.
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {results.map((result, index) => (
        <div
          key={result.id}
          className="bg-white rounded-lg shadow hover:shadow-md transition-shadow p-6 border border-gray-200"
        >
          <div className="flex items-start justify-between mb-2">
            <h3 className="text-xl font-semibold text-gray-900 flex-1">
              {index + 1}. {result.title || 'Untitled'}
            </h3>
            <div className="flex gap-2 ml-4">
              <span className="px-3 py-1 bg-green-100 text-green-800 rounded-full text-sm font-medium">
                {result.score.toFixed(3)}
              </span>
            </div>
          </div>

          <p className="text-gray-700 mb-3 leading-relaxed">
            {result.snippet}
          </p>

          <div className="flex items-center gap-4 text-sm text-gray-500">
            {result.url && (
              <a
                href={result.url}
                target="_blank"
                rel="noopener noreferrer"
                className="text-blue-600 hover:underline flex items-center gap-1"
              >
                 View Source
              </a>
            )}
            
            <span className="px-2 py-1 bg-gray-100 rounded text-xs">
              {result.source}
            </span>
            
            {result.timestamp && (
              <span className="text-xs">
                 {new Date(result.timestamp).toLocaleDateString()}
              </span>
            )}
            
            <span className="text-xs text-gray-400">
              Chunk {result.chunkIndex}
            </span>
          </div>
        </div>
      ))}
    </div>
  )
}
