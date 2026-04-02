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
  const [wiping, setWiping] = useState(false)
  const [wipeResult, setWipeResult] = useState<string | null>(null)
  const [wipeError, setWipeError] = useState<string | null>(null)
  const [confirmWipe, setConfirmWipe] = useState(false)

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

  const handleWipe = async () => {
    setWiping(true)
    setWipeError(null)
    setWipeResult(null)
    setConfirmWipe(false)
    try {
      const data = await apiRequest<{ message: string }>('/api/dev/wipe', { method: 'POST' })
      setWipeResult(data.message)
    } catch (e: unknown) {
      setWipeError(e instanceof Error ? e.message : 'Failed to wipe data')
    } finally {
      setWiping(false)
    }
  }

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-semibold text-[#37352F] mb-1">Dev Tools</h1>
      <p className="text-sm text-[#787774] mb-8">Create test data for development. Only works on localhost.</p>

      <div className="border border-[#E9E9E7] rounded-lg p-6">
        <h2 className="text-base font-semibold text-[#37352F] mb-2">Seed Test Data</h2>
        <p className="text-sm text-[#787774] mb-4">
          Creates 20 species (5 groups, 5 tags), 2 gardens with 5 beds, 20 plants with lifecycle
          events, seed inventory for 8 species, and 6 scheduled tasks. Data is created for the
          currently logged-in user.
        </p>

        <div className="bg-[#FBF3DB] border border-[#F1E5BC] rounded-md p-3 mb-4">
          <p className="text-sm text-[#73641C]">
            Running this multiple times will create duplicate data.
          </p>
        </div>

        <button
          onClick={handleSeed}
          disabled={loading}
          className="px-4 py-2 bg-[#2EAADC] text-white rounded-md text-sm font-medium hover:bg-[#2898C4] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {loading ? 'Seeding...' : 'Seed All Test Data'}
        </button>

        {error && (
          <div className="mt-4 bg-[#FBE4E4] border border-[#F5C6C6] rounded-md p-3">
            <p className="text-sm text-[#E03E3E]">{error}</p>
          </div>
        )}

        {result && (
          <div className="mt-4 bg-[#DBEDDB] border border-[#C4DFC4] rounded-md p-4">
            <p className="text-sm font-medium text-[#0F7B0F] mb-3">Test data created successfully!</p>
            <div className="grid grid-cols-3 gap-2">
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
                <div key={label as string} className="bg-white/70 rounded-md p-2 text-center">
                  <div className="text-base font-semibold text-[#37352F]">{count}</div>
                  <div className="text-xs text-[#787774]">{label}</div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      <div className="border border-[#E9E9E7] rounded-lg p-6 mt-6">
        <h2 className="text-base font-semibold text-[#37352F] mb-2">Wipe User Data</h2>
        <p className="text-sm text-[#787774] mb-4">
          Deletes all user data: gardens, beds, plants, events, seeds, tasks, seasons, customers,
          orders, listings, trials, bouquets, successions, targets, and user-created species.
          System species and user accounts are preserved.
        </p>

        <div className="bg-[#FBE4E4] border border-[#F5C6C6] rounded-md p-3 mb-4">
          <p className="text-sm text-[#E03E3E] font-medium">
            This action is irreversible and will delete all data for all users.
          </p>
        </div>

        {!confirmWipe ? (
          <button
            onClick={() => setConfirmWipe(true)}
            disabled={wiping}
            className="px-4 py-2 bg-[#E03E3E] text-white rounded-md text-sm font-medium hover:bg-[#C73535] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            Wipe All User Data
          </button>
        ) : (
          <div className="flex items-center gap-3">
            <button
              onClick={handleWipe}
              disabled={wiping}
              className="px-4 py-2 bg-[#E03E3E] text-white rounded-md text-sm font-medium hover:bg-[#C73535] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {wiping ? 'Wiping...' : 'Yes, wipe everything'}
            </button>
            <button
              onClick={() => setConfirmWipe(false)}
              className="px-4 py-2 text-[#787774] text-sm hover:text-[#37352F] transition-colors"
            >
              Cancel
            </button>
          </div>
        )}

        {wipeError && (
          <div className="mt-4 bg-[#FBE4E4] border border-[#F5C6C6] rounded-md p-3">
            <p className="text-sm text-[#E03E3E]">{wipeError}</p>
          </div>
        )}

        {wipeResult && (
          <div className="mt-4 bg-[#DBEDDB] border border-[#C4DFC4] rounded-md p-3">
            <p className="text-sm text-[#0F7B0F]">{wipeResult}</p>
          </div>
        )}
      </div>
    </div>
  )
}
