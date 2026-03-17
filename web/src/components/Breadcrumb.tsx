import { Link } from 'react-router-dom'

export interface BreadcrumbItem {
  label: string
  to: string
}

export function Breadcrumb({ items }: { items: BreadcrumbItem[] }) {
  if (items.length === 0) return null
  return (
    <nav className="flex items-center gap-1 text-xs text-text-secondary mb-1.5 flex-wrap">
      {items.map((item, i) => (
        <span key={i} className="flex items-center gap-1">
          {i > 0 && <span className="text-text-muted select-none">›</span>}
          <Link to={item.to} className="hover:text-text-primary transition-colors">
            {item.label}
          </Link>
        </span>
      ))}
    </nav>
  )
}
