import { useEffect, useRef, type ReactNode } from 'react'

interface Props {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
  actions?: ReactNode
}

export function Dialog({ open, onClose, title, children, actions }: Props) {
  const ref = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const el = ref.current
    if (!el) return
    if (open && !el.open) el.showModal()
    else if (!open && el.open) el.close()
  }, [open])

  return (
    <dialog
      ref={ref}
      onClose={onClose}
      className="rounded-lg p-0 w-[min(26rem,calc(100vw-2rem))] shadow-xl backdrop:bg-black/25 bg-bg border border-divider"
    >
      <div className="p-5">
        <h2 className="text-base font-semibold text-text-primary mb-4">{title}</h2>
        {children}
        {actions && <div className="flex justify-end gap-2 mt-5">{actions}</div>}
      </div>
    </dialog>
  )
}
