import { useState } from 'react'
import { apiRequest } from '../api/client'

interface SeedResult {
  speciesCount: number
  groupCount: number
  tagCount: number
  gardenCount: number
  bedCount: number
  plantCount: number
  eventCount: number
  seedInventoryCount: number
  taskCount: number
}

export default function DevSeed() {
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<SeedResult | null>(null)
  const [error, setError] = useState<string | null>(null)

  const handleSeed = async () => {
    setLoading(true)
    setError(null)
    setResult(null)
    try {
      const data = await apiRequest<SeedResult>('/api/dev/seed', { method: 'POST' })
      setResult(data)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to seed data')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-bold text-gray-900 mb-2">Dev Tools</h1>
      <p className="text-gray-500 mb-8">Create test data for development. Only works on localhost.</p>

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-2">Seed Test Data</h2>
        <p className="text-sm text-gray-500 mb-4">
          Creates 20 species (5 groups, 5 tags), 2 gardens with 5 beds, 20 plants with lifecycle
          events, seed inventory for 8 species, and 6 scheduled tasks. Data is created for the
          currently logged-in user.
        </p>

        <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 mb-4">
          <p className="text-sm text-amber-800">
            Warning: Running this multiple times will create duplicate data.
          </p>
        </div>

        <button
          onClick={handleSeed}
          disabled={loading}
          className="px-6 py-2.5 bg-green-600 text-white rounded-lg font-medium hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {loading ? 'Seeding...' : 'Seed All Test Data'}
        </button>

        {error && (
          <div className="mt-4 bg-red-50 border border-red-200 rounded-lg p-3">
            <p className="text-sm text-red-800">{error}</p>
          </div>
        )}

        {result && (
          <div className="mt-4 bg-green-50 border border-green-200 rounded-lg p-4">
            <p className="text-sm font-medium text-green-800 mb-3">Test data created successfully!</p>
            <div className="grid grid-cols-3 gap-3">
              {[
                ['Species', result.speciesCount],
                ['Groups', result.groupCount],
                ['Tags', result.tagCount],
                ['Gardens', result.gardenCount],
                ['Beds', result.bedCount],
                ['Plants', result.plantCount],
                ['Events', result.eventCount],
                ['Seed Batches', result.seedInventoryCount],
                ['Tasks', result.taskCount],
              ].map(([label, count]) => (
                <div key={label as string} className="bg-white rounded-lg p-2 text-center">
                  <div className="text-lg font-bold text-green-700">{count}</div>
                  <div className="text-xs text-gray-500">{label}</div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
