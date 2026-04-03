import { useState } from 'react'
import { apiRequest } from '../api/client'

export default function ResetData() {
  const [wiping, setWiping] = useState(false)
  const [result, setResult] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [confirmWipe, setConfirmWipe] = useState(false)

  const handleWipe = async () => {
    setWiping(true)
    setError(null)
    setResult(null)
    setConfirmWipe(false)
    try {
      const data = await apiRequest<{ message: string }>('/api/dev/wipe', { method: 'POST' })
      setResult(data.message)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to reset data')
    } finally {
      setWiping(false)
    }
  }

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-semibold text-[#37352F] mb-1">Reset Data</h1>
      <p className="text-sm text-[#787774] mb-8">Remove all user data to start fresh.</p>

      <div className="border border-[#E9E9E7] rounded-lg p-6">
        <h2 className="text-base font-semibold text-[#37352F] mb-2">Wipe All User Data</h2>
        <p className="text-sm text-[#787774] mb-4">
          Deletes all gardens, beds, plants, events, seeds, tasks, seasons, customers,
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

        {error && (
          <div className="mt-4 bg-[#FBE4E4] border border-[#F5C6C6] rounded-md p-3">
            <p className="text-sm text-[#E03E3E]">{error}</p>
          </div>
        )}

        {result && (
          <div className="mt-4 bg-[#DBEDDB] border border-[#C4DFC4] rounded-md p-3">
            <p className="text-sm text-[#0F7B0F]">{result}</p>
          </div>
        )}
      </div>
    </div>
  )
}
