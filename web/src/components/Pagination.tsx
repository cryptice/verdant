import { useTranslation } from 'react-i18next'

interface Props {
  page: number
  pageSize: number
  total: number
  onPageChange: (page: number) => void
}

export function Pagination({ page, pageSize, total, onPageChange }: Props) {
  const { t } = useTranslation()
  const totalPages = Math.ceil(total / pageSize)
  if (totalPages <= 1) return null

  return (
    <div className="flex items-center justify-between pt-3">
      <p className="text-xs text-text-secondary">
        {t('pagination.showing', { from: page * pageSize + 1, to: Math.min((page + 1) * pageSize, total), total })}
      </p>
      <div className="flex items-center gap-1">
        <button
          onClick={() => onPageChange(page - 1)}
          disabled={page === 0}
          className="px-2.5 py-1 text-sm rounded-lg border border-divider text-text-secondary hover:bg-warm hover:border-divider disabled:opacity-30 disabled:cursor-not-allowed transition-all cursor-pointer"
        >
          ‹
        </button>
        <button
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1}
          className="px-2.5 py-1 text-sm rounded-lg border border-divider text-text-secondary hover:bg-warm hover:border-divider disabled:opacity-30 disabled:cursor-not-allowed transition-all cursor-pointer"
        >
          ›
        </button>
      </div>
    </div>
  )
}
