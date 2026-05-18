import { Dialog } from '../Dialog'

export interface DeleteConfirmDialogProps {
  open: boolean
  onClose: () => void
  onConfirm: () => void
  isPending: boolean
  mutError: string | null
  t: (key: string, opts?: Record<string, unknown>) => string
}

export function DeleteConfirmDialog({
  open, onClose, onConfirm, isPending, mutError, t,
}: DeleteConfirmDialogProps) {
  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={t('common.delete')}
      actions={
        <>
          <button onClick={onClose} className="btn-secondary">{t('common.cancel')}</button>
          <button
            onClick={onConfirm}
            className="px-4 py-2 text-sm text-error font-semibold"
          >
            {isPending ? t('common.deleting') : t('common.delete')}
          </button>
        </>
      }
    >
      <p className="text-text-secondary">{t('common.delete')}?</p>
      {mutError && <p className="text-error text-sm mt-2">{mutError}</p>}
    </Dialog>
  )
}
