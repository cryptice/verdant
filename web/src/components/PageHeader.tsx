import { Breadcrumb, type BreadcrumbItem } from './Breadcrumb'

interface Props {
  title: string
  icon?: string
  breadcrumbs?: BreadcrumbItem[]
  editAction?: () => void
  action?: { label: string; onClick: () => void; 'data-onboarding'?: string }
}

export function PageHeader({ title, icon, breadcrumbs, editAction, action }: Props) {
  return (
    <div className="mb-6">
      {breadcrumbs && breadcrumbs.length > 0 && (
        <Breadcrumb items={breadcrumbs} />
      )}
      <div className="flex items-center gap-2">
        {icon && <span className="text-3xl leading-none">{icon}</span>}
        <h1 className="text-xl font-semibold text-text-primary">{title}</h1>
        {editAction && (
          <button
            onClick={editAction}
            className="text-text-muted hover:text-text-secondary transition-colors p-0.5 cursor-pointer"
            aria-label="Edit"
          >
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M9.5 2.5 L11.5 4.5 L4.5 11.5 L2 12 L2.5 9.5 Z" />
              <line x1="8" y1="4" x2="10" y2="6" />
            </svg>
          </button>
        )}
        {action && (
          <button onClick={action.onClick} className="btn-primary ml-auto" data-onboarding={action['data-onboarding']}>
            {action.label}
          </button>
        )}
      </div>
    </div>
  )
}
