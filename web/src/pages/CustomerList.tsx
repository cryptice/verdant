import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type CustomerResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { Pagination } from '../components/Pagination'
import { OnboardingHint } from '../onboarding/OnboardingHint'
import { useOnboarding } from '../onboarding/OnboardingContext'

const PAGE_SIZE = 50

const CHANNELS = ['FLORIST', 'FARMERS_MARKET', 'CSA', 'WEDDING', 'WHOLESALE', 'DIRECT', 'OTHER'] as const

export function CustomerList() {
  const qc = useQueryClient()
  const { t } = useTranslation()
  const { completeStep } = useOnboarding()
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['customers'],
    queryFn: () => api.customers.list(),
  })

  const [page, setPage] = useState(0)
  const [showAdd, setShowAdd] = useState(false)
  const [editItem, setEditItem] = useState<CustomerResponse | null>(null)
  const [deleteItem, setDeleteItem] = useState<CustomerResponse | null>(null)

  // Form state
  const [formName, setFormName] = useState('')
  const [formChannel, setFormChannel] = useState<string>('DIRECT')
  const [formContactInfo, setFormContactInfo] = useState('')
  const [formNotes, setFormNotes] = useState('')

  const [formError, setFormError] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const resetForm = () => {
    setFormName(''); setFormChannel('DIRECT'); setFormContactInfo(''); setFormNotes(''); setFormError(null)
  }

  const openAdd = () => { resetForm(); setShowAdd(true) }

  const openEdit = (c: CustomerResponse) => {
    setFormName(c.name)
    setFormChannel(c.channel)
    setFormContactInfo(c.contactInfo ?? '')
    setFormNotes(c.notes ?? '')
    setFormError(null)
    setEditItem(c)
  }

  const createMut = useMutation({
    mutationFn: () => api.customers.create({
      name: formName,
      channel: formChannel,
      contactInfo: formContactInfo || undefined,
      notes: formNotes || undefined,
    }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['customers'] }); setShowAdd(false); resetForm(); completeStep('add_customer') },
    onError: (err) => { setFormError(err instanceof Error ? err.message : String(err)) },
  })

  const updateMut = useMutation({
    mutationFn: () => api.customers.update(editItem!.id, {
      name: formName,
      channel: formChannel,
      contactInfo: formContactInfo || undefined,
      notes: formNotes || undefined,
    }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['customers'] }); setEditItem(null); resetForm() },
    onError: (err) => { setFormError(err instanceof Error ? err.message : String(err)) },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.customers.delete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['customers'] }); setDeleteItem(null); setDeleteError(null) },
    onError: (err) => { setDeleteError(err instanceof Error ? err.message : String(err)) },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  const channelBadge = (channel: string) => {
    const colors: Record<string, string> = {
      FLORIST: 'bg-purple-100 text-purple-700',
      FARMERS_MARKET: 'bg-green-100 text-green-700',
      CSA: 'bg-blue-100 text-blue-700',
      WEDDING: 'bg-pink-100 text-pink-700',
      WHOLESALE: 'bg-amber-100 text-amber-700',
      DIRECT: 'bg-teal-100 text-teal-700',
      OTHER: 'bg-gray-100 text-gray-700',
    }
    return (
      <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${colors[channel] ?? colors.OTHER}`}>
        {t(`channels.${channel}`)}
      </span>
    )
  }

  const formFields = (
    <div className="space-y-4">
      <div>
        <label className="field-label">{t('customers.name')} *</label>
        <input type="text" value={formName} onChange={e => setFormName(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{t('customers.channel')} *</label>
        <select value={formChannel} onChange={e => setFormChannel(e.target.value)} className="input">
          {CHANNELS.map(ch => (
            <option key={ch} value={ch}>{t(`channels.${ch}`)}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="field-label">{t('customers.contactInfo')}</label>
        <textarea value={formContactInfo} onChange={e => setFormContactInfo(e.target.value)} placeholder={t('common.optional')} rows={2} className="input" />
      </div>
      <div>
        <label className="field-label">{t('common.notesLabel')}</label>
        <textarea value={formNotes} onChange={e => setFormNotes(e.target.value)} placeholder={t('common.optional')} rows={2} className="input" />
      </div>
      {formError && <p className="text-error text-sm">{formError}</p>}
    </div>
  )

  return (
    <div>
      <PageHeader title={t('customers.title')} action={{ label: t('customers.newCustomer'), onClick: openAdd, 'data-onboarding': 'add-customer-btn' }} />
      <OnboardingHint />
      <div className="px-4 py-4">
        {data && data.length === 0 && (
          <p className="text-text-secondary text-sm text-center py-4">{t('customers.noCustomers')}</p>
        )}

        {data && data.length > 0 && (<>
          <div className="border border-divider rounded-xl overflow-hidden bg-bg shadow-sm">
            <table className="w-full">
              <thead>
                <tr className="border-b border-divider bg-surface">
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('customers.name')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('customers.channel')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('customers.contactInfo')}</th>
                </tr>
              </thead>
              <tbody>
                {data.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE).map(c => (
                  <tr
                    key={c.id}
                    className="border-b border-divider last:border-0 hover:bg-surface cursor-pointer transition-colors"
                    onClick={() => openEdit(c)}
                  >
                    <td className="px-4 py-2.5 text-sm">{c.name}</td>
                    <td className="px-4 py-2.5 text-sm">{channelBadge(c.channel)}</td>
                    <td className="px-4 py-2.5 text-sm text-text-secondary truncate max-w-[200px]">{c.contactInfo ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={page} pageSize={PAGE_SIZE} total={data.length} onPageChange={setPage} />
        </>)}
      </div>

      <Dialog open={showAdd} onClose={() => { setShowAdd(false); resetForm() }} title={t('customers.newCustomer')} actions={
        <>
          <button onClick={() => { setShowAdd(false); resetForm() }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button
            onClick={() => createMut.mutate()}
            disabled={!formName || createMut.isPending}
            className="btn-primary text-sm"
          >
            {createMut.isPending ? t('common.saving') : t('common.add')}
          </button>
        </>
      }>
        {formFields}
      </Dialog>

      <Dialog open={editItem !== null} onClose={() => { setEditItem(null); resetForm() }} title={t('customers.editCustomer')} actions={
        <>
          <button onClick={() => { setEditItem(null); resetForm() }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button
            onClick={() => updateMut.mutate()}
            disabled={!formName || updateMut.isPending}
            className="btn-primary text-sm"
          >
            {updateMut.isPending ? t('common.saving') : t('common.save')}
          </button>
        </>
      }>
        {formFields}
        <button
          onClick={() => { setEditItem(null); resetForm(); setDeleteItem(editItem) }}
          className="text-sm text-error hover:underline mt-4"
        >
          {t('customers.deleteCustomer')}
        </button>
      </Dialog>

      <Dialog open={deleteItem !== null} onClose={() => { setDeleteItem(null); setDeleteError(null) }} title={t('customers.deleteCustomer')} actions={
        <>
          <button onClick={() => { setDeleteItem(null); setDeleteError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => deleteItem && deleteMut.mutate(deleteItem.id)} className="px-4 py-2 text-sm text-error font-semibold">{t('common.delete')}</button>
        </>
      }>
        <p className="text-text-secondary">{t('customers.deleteConfirm')}</p>
        {deleteError && <p className="text-error text-sm mt-2">{deleteError}</p>}
      </Dialog>
    </div>
  )
}
