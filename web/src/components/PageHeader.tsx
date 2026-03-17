import { useNavigate } from 'react-router-dom'

interface Props {
  title: string
  back?: boolean
  action?: { label: string; onClick: () => void }
}

export function PageHeader({ title, back, action }: Props) {
  const navigate = useNavigate()
  return (
    <div className="flex items-center gap-2 mb-6">
      {back && (
        <button
          onClick={() => navigate(-1)}
          className="text-text-secondary hover:text-text-primary transition-colors mr-1 flex items-center"
          aria-label="Back"
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="10 12 6 8 10 4" />
          </svg>
        </button>
      )}
      <h1 className="text-xl font-semibold text-text-primary flex-1">{title}</h1>
      {action && (
        <button onClick={action.onClick} className="btn-primary">
          {action.label}
        </button>
      )}
    </div>
  )
}
