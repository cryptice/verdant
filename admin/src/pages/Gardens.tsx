import { useQuery } from '@tanstack/react-query'
import { api, type Garden } from '../api/client'

export default function Gardens() {
  const { data: gardens, isLoading, error } = useQuery({
    queryKey: ['admin', 'gardens'],
    queryFn: api.admin.getGardens
  })

  if (isLoading) return <div className="flex justify-center py-12"><div className="text-gray-500">Loading...</div></div>
  if (error) return <div className="text-red-600">Error loading gardens</div>

  return (
    <div>
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-gray-800">Gardens</h2>
        <p className="text-gray-500">{gardens?.length || 0} gardens</p>
      </div>

      <div className="bg-white rounded-xl shadow-sm overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b bg-gray-50">
              <th className="text-left px-6 py-3 text-sm font-medium text-gray-500">ID</th>
              <th className="text-left px-6 py-3 text-sm font-medium text-gray-500">Garden</th>
              <th className="text-left px-6 py-3 text-sm font-medium text-gray-500">Description</th>
              <th className="text-left px-6 py-3 text-sm font-medium text-gray-500">Created</th>
              <th className="text-left px-6 py-3 text-sm font-medium text-gray-500">Updated</th>
            </tr>
          </thead>
          <tbody>
            {gardens?.map((garden: Garden) => (
              <tr key={garden.id} className="border-b last:border-0 hover:bg-gray-50">
                <td className="px-6 py-4 text-sm text-gray-600">{garden.id}</td>
                <td className="px-6 py-4">
                  <div className="flex items-center gap-2">
                    <span className="text-xl">{garden.emoji || ''}</span>
                    <span className="font-medium text-gray-800">{garden.name}</span>
                  </div>
                </td>
                <td className="px-6 py-4 text-sm text-gray-600 max-w-xs truncate">
                  {garden.description || '\u2014'}
                </td>
                <td className="px-6 py-4 text-sm text-gray-500">
                  {new Date(garden.createdAt).toLocaleDateString()}
                </td>
                <td className="px-6 py-4 text-sm text-gray-500">
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
