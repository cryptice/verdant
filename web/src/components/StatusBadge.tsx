import { useTranslation } from 'react-i18next'

const statusColors: Record<string, string> = {
  SEEDED: 'bg-amber-800/15 text-amber-800',
  POTTED_UP: 'bg-orange-600/15 text-orange-600',
  PLANTED_OUT: 'bg-green-600/15 text-green-600',
  GROWING: 'bg-green-600/15 text-green-600',
  HARVESTED: 'bg-blue-700/15 text-blue-700',
  RECOVERED: 'bg-teal-600/15 text-teal-600',
  REMOVED: 'bg-gray-500/15 text-gray-500',
}

export function StatusBadge({ status }: { status: string }) {
  const { t } = useTranslation()
  const color = statusColors[status] ?? 'bg-gray-200 text-gray-600'
  const label = t(`status.${status}`, { defaultValue: status })
  return (
    <span className={`inline-block px-2.5 py-0.5 rounded-full text-xs font-medium ${color}`}>
      {label}
    </span>
  )
}
