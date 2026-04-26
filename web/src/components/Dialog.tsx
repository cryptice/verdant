import { useEffect, useId, useRef, useState, type ReactNode } from 'react'

interface Props {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
  actions?: ReactNode
  /** When true, dismissing the dialog (Esc, scrim click, X) shows a
   *  confirm-discard prompt before actually closing. */
  isDirty?: boolean
  /** Optional message override for the discard prompt. */
  discardMessage?: string
}

export function Dialog({
  open,
  onClose,
  title,
  children,
  actions,
  isDirty = false,
  discardMessage = 'Ändringar går förlorade. Är du säker?',
}: Props) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  const [confirmDiscard, setConfirmDiscard] = useState(false)

  useEffect(() => {
    const el = ref.current
    if (!el) return
    if (open && !el.open) el.showModal()
    else if (!open && el.open) el.close()
  }, [open])

  const requestClose = () => {
    if (isDirty) setConfirmDiscard(true)
    else onClose()
  }

  return (
    <>
      <dialog
        ref={ref}
        aria-labelledby={titleId}
        onCancel={(e) => {
          // Esc fires onCancel before onClose; intercept to confirm.
          if (isDirty) {
            e.preventDefault()
            setConfirmDiscard(true)
          }
        }}
        onClose={onClose}
        onClick={(e) => {
          // Native scrim click lands on the dialog itself, not a child.
          if (e.target === ref.current) requestClose()
        }}
        className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 rounded-2xl p-0 w-[min(26rem,calc(100vw-2rem))] shadow-2xl backdrop:bg-black/20 backdrop:backdrop-blur-sm bg-bg border border-divider"
      >
        <div className="p-5">
          <h2 id={titleId} className="text-base font-semibold text-text-primary mb-4">{title}</h2>
          {children}
          {actions && <div className="flex justify-end gap-2 mt-5">{actions}</div>}
        </div>
      </dialog>
      {confirmDiscard && (
        <ConfirmDiscardDialog
          message={discardMessage}
          onCancel={() => setConfirmDiscard(false)}
          onConfirm={() => {
            setConfirmDiscard(false)
            onClose()
          }}
        />
      )}
    </>
  )
}

function ConfirmDiscardDialog({
  message,
  onCancel,
  onConfirm,
}: {
  message: string
  onCancel: () => void
  onConfirm: () => void
}) {
  const ref = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    if (!ref.current?.open) ref.current?.showModal()
  }, [])
  return (
    <dialog
      ref={ref}
      onClose={onCancel}
      className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 rounded-2xl p-0 w-[min(22rem,calc(100vw-2rem))] shadow-2xl backdrop:bg-black/20 backdrop:backdrop-blur-sm bg-bg border border-divider"
    >
      <div className="p-5">
        <p className="text-sm text-text-primary mb-4">{message}</p>
        <div className="flex justify-end gap-2">
          <button className="btn-secondary" onClick={onCancel}>Avbryt</button>
          <button
            className="btn-primary"
            style={{ background: 'var(--color-accent)', borderColor: 'var(--color-accent)' }}
            onClick={onConfirm}
          >
            Kasta ändringar
          </button>
        </div>
      </div>
    </dialog>
  )
}
