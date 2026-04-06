import { useState } from 'react'
import { apiRequest } from '../api/client'
import { useTranslation } from 'react-i18next'

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
  const { t } = useTranslation()

  const handleSeed = async () => {
    setLoading(true)
    setError(null)
    setResult(null)
    try {
      const data = await apiRequest<SeedResult>('/api/dev/seed', { method: 'POST' })
      setResult(data)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : t('devSeed.seedFailed'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-semibold text-[#37352F] mb-1">{t('devSeed.title')}</h1>
      <p className="text-sm text-[#787774] mb-8">{t('devSeed.subtitle')}</p>

      <div className="border border-[#E9E9E7] rounded-lg p-6">
        <h2 className="text-base font-semibold text-[#37352F] mb-2">{t('devSeed.seedTitle')}</h2>
        <p className="text-sm text-[#787774] mb-4">
          {t('devSeed.seedDescription')}
        </p>

        <div className="bg-[#FBF3DB] border border-[#F1E5BC] rounded-md p-3 mb-4">
          <p className="text-sm text-[#73641C]">
            {t('devSeed.duplicateWarning')}
          </p>
        </div>

        <button
          onClick={handleSeed}
          disabled={loading}
          className="px-4 py-2 bg-[#2EAADC] text-white rounded-md text-sm font-medium hover:bg-[#2898C4] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {loading ? t('devSeed.seeding') : t('devSeed.seedButton')}
        </button>

        {error && (
          <div className="mt-4 bg-[#FBE4E4] border border-[#F5C6C6] rounded-md p-3">
            <p className="text-sm text-[#E03E3E]">{error}</p>
          </div>
        )}

        {result && (
          <div className="mt-4 bg-[#DBEDDB] border border-[#C4DFC4] rounded-md p-4">
            <p className="text-sm font-medium text-[#0F7B0F] mb-3">{t('devSeed.seedSuccess')}</p>
            <div className="grid grid-cols-3 gap-2">
              {([
                [t('devSeed.labelSpecies'), result.speciesCount],
                [t('devSeed.labelGroups'), result.groupCount],
                [t('devSeed.labelTags'), result.tagCount],
                [t('devSeed.labelGardens'), result.gardenCount],
                [t('devSeed.labelBeds'), result.bedCount],
                [t('devSeed.labelPlants'), result.plantCount],
                [t('devSeed.labelEvents'), result.eventCount],
                [t('devSeed.labelSeedBatches'), result.seedInventoryCount],
                [t('devSeed.labelTasks'), result.taskCount],
              ] as [string, number][]).map(([label, count]) => (
                <div key={label} className="bg-white/70 rounded-md p-2 text-center">
                  <div className="text-base font-semibold text-[#37352F]">{count}</div>
                  <div className="text-xs text-[#787774]">{label}</div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
