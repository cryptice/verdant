import { useQuery } from '@tanstack/react-query'
import { api, type Garden } from '../api/client'
import ErrorDisplay from '../components/ErrorDisplay'
import { useTranslation } from 'react-i18next'

export default function Gardens() {
  const { data: gardens, isLoading, error } = useQuery({
    queryKey: ['admin', 'gardens'],
    queryFn: api.admin.getGardens
  })
  const { t } = useTranslation()

  if (isLoading) return <div className="flex justify-center py-12"><div className="text-[#787774] text-sm">{t('common.loading')}</div></div>
  if (error) return <ErrorDisplay error={error} onRetry={() => window.location.reload()} />

  return (
    <div>
      <div className="mb-5 sm:mb-6">
        <h2 className="text-xl sm:text-2xl font-semibold text-[#37352F]">{t('gardens.title')}</h2>
        <p className="text-sm text-[#787774] mt-1">{t('gardens.count', { count: gardens?.length || 0 })}</p>
      </div>

      {/* Mobile card list */}
      <div className="md:hidden space-y-2">
        {gardens?.map((garden: Garden) => (
          <div key={garden.id} className="border border-[#E9E9E7] rounded-lg p-3 bg-white">
            <div className="flex items-baseline gap-2 mb-1">
              {garden.emoji && <span className="text-lg leading-none">{garden.emoji}</span>}
              <span className="text-sm font-medium text-[#37352F] truncate">{garden.name}</span>
            </div>
            {garden.description && (
              <p className="text-sm text-[#787774] mb-2">{garden.description}</p>
            )}
            <div className="flex flex-wrap gap-x-3 gap-y-0.5 text-xs text-[#A5A29C]">
              <span>{t('gardens.created')}: {new Date(garden.createdAt).toLocaleDateString()}</span>
              <span>{t('gardens.updated')}: {new Date(garden.updatedAt).toLocaleDateString()}</span>
            </div>
          </div>
        ))}
      </div>

      {/* Desktop table */}
      <div className="hidden md:block border border-[#E9E9E7] rounded-lg overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-[#E9E9E7] bg-[#FBFBFA]">
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('gardens.garden')}</th>
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('gardens.description')}</th>
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('gardens.created')}</th>
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('gardens.updated')}</th>
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
