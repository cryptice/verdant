const statusConfig: Record<string, { label: string; color: string }> = {
  SEEDED: { label: 'Seeded', color: 'bg-amber-800/15 text-amber-800' },
  POTTED_UP: { label: 'Potted Up', color: 'bg-orange-600/15 text-orange-600' },
  PLANTED_OUT: { label: 'Growing', color: 'bg-green-600/15 text-green-600' },
  GROWING: { label: 'Growing', color: 'bg-green-600/15 text-green-600' },
  HARVESTED: { label: 'Harvested', color: 'bg-blue-700/15 text-blue-700' },
  RECOVERED: { label: 'Recovered', color: 'bg-teal-600/15 text-teal-600' },
  REMOVED: { label: 'Removed', color: 'bg-gray-500/15 text-gray-500' },
}

export function StatusBadge({ status }: { status: string }) {
  const cfg = statusConfig[status] ?? { label: status, color: 'bg-gray-200 text-gray-600' }
  return (
    <span className={`inline-block px-2.5 py-0.5 rounded-full text-xs font-medium ${cfg.color}`}>
      {cfg.label}
    </span>
  )
}
