import { useQuery } from '@tanstack/react-query'
import { api, type Garden } from '../api/client'
import ErrorDisplay from '../components/ErrorDisplay'

export default function Gardens() {
  const { data: gardens, isLoading, error } = useQuery({
    queryKey: ['admin', 'gardens'],
    queryFn: api.admin.getGardens
  })

  if (isLoading) return <div className="flex justify-center py-12"><div className="text-[#787774] text-sm">Loading...</div></div>
  if (error) return <ErrorDisplay error={error} onRetry={() => window.location.reload()} />

  return (
    <div>
      <div className="mb-6">
        <h2 className="text-2xl font-semibold text-[#37352F]">Gardens</h2>
        <p className="text-sm text-[#787774] mt-1">{gardens?.length || 0} gardens</p>
      </div>

      <div className="border border-[#E9E9E7] rounded-lg overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-[#E9E9E7] bg-[#FBFBFA]">
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">Garden</th>
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">Description</th>
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">Created</th>
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">Updated</th>
            </tr>
          </thead>
          <tbody>
            {gardens?.map((garden: Garden) => (
              <tr key={garden.id} className="border-b border-[#E9E9E7] last:border-0 hover:bg-[#FBFBFA] transition-colors">
                <td className="px-4 py-2.5">
                  <div className="flex items-center gap-2">
                    {garden.emoji && <span className="text-lg">{garden.emoji}</span>}
                    <span className="text-sm font-medium text-[#37352F]">{garden.name}</span>
                  </div>
                </td>
                <td className="px-4 py-2.5 text-sm text-[#787774] max-w-xs truncate">
                  {garden.description || '\u2014'}
                </td>
                <td className="px-4 py-2.5 text-sm text-[#787774]">
                  {new Date(garden.createdAt).toLocaleDateString()}
                </td>
                <td className="px-4 py-2.5 text-sm text-[#787774]">
                  {new Date(garden.updatedAt).toLocaleDateString()}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
