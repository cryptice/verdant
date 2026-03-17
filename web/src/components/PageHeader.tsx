import { useNavigate } from 'react-router-dom'

interface Props {
  title: string
  back?: boolean
  action?: { label: string; onClick: () => void }
}

export function PageHeader({ title, back, action }: Props) {
  const navigate = useNavigate()
  return (
    <header className="sticky top-0 z-10 bg-cream-dark px-4 py-3 flex items-center gap-3">
      {back && (
        <button onClick={() => navigate(-1)} className="text-text-primary text-xl leading-none">
          ←
        </button>
      )}
      <h1 className="text-lg font-bold flex-1">{title}</h1>
      {action && (
        <button onClick={action.onClick} className="text-green-primary font-semibold text-sm">
          {action.label}
        </button>
      )}
    </header>
  )
}
