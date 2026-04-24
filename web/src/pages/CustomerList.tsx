import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type CustomerResponse } from '../api/client'
import { Masthead, Ledger, LedgerFilters, Chip } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { OnboardingHint } from '../onboarding/OnboardingHint'
import { useOnboarding } from '../onboarding/OnboardingContext'
import type { LedgerFilterOption } from '../components/faltet'

const ALL_CHANNELS = ['FLORIST', 'FARMERS_MARKET', 'CSA', 'WEDDING', 'WHOLESALE', 'DIRECT', 'OTHER'] as const
type Channel = typeof ALL_CHANNELS[number]

const CHANNEL_TONE: Record<Channel, LedgerFilterOption<Channel>['tone']> = {
  FLORIST: 'clay',
  FARMERS_MARKET: 'mustard',
  CSA: 'sage',
  WEDDING: 'berry',
  WHOLESALE: 'sky',
  DIRECT: 'forest',
  OTHER: 'forest',
}

export function CustomerList() {
  const qc = useQueryClient()
  const { t } = useTranslation()
  const { completeStep } = useOnboarding()
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['customers'],
    queryFn: () => api.customers.list(),
  })

  const [channels, setChannels] = useState<Set<Channel>>(new Set(ALL_CHANNELS))
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
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['customers'] })
      setShowAdd(false)
      resetForm()
      completeStep('add_customer')
    },
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

  const filtered = (data ?? []).filter(c => channels.has(c.channel as Channel))

  const formFields = (
    <div className="space-y-4">
      <div>
        <label className="field-label">{t('customers.name')} *</label>
        <input type="text" value={formName} onChange={e => setFormName(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{t('customers.channel')} *</label>
        <select value={formChannel} onChange={e => setFormChannel(e.target.value)} className="input">
          {ALL_CHANNELS.map(ch => (
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
      <Masthead
        left={t('nav.customers')}
        center="— Kundliggaren —"
        right={
          <button
            onClick={openAdd}
            className="btn-primary"
            data-onboarding="add-customer-btn"
          >
            {t('customers.newCustomer')}
          </button>
        }
      />
      <OnboardingHint />

      <div className="page-body">
        <LedgerFilters
          options={ALL_CHANNELS.map(ch => ({
            id: ch,
            label: t(`channels.${ch}`),
            tone: CHANNEL_TONE[ch],
          }))}
          value={channels}
          onChange={setChannels}
          storageKey="verdant-customer-filters"
        />

        <Ledger
          paginated
          pageSize={50}
          columns={[
            {
              key: 'id',
              label: '№',
              width: '60px',
              render: (_c, i) => (
                <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 22, color: 'var(--color-berry)' }}>
                  {String(i + 1).padStart(2, '0')}
                </span>
              ),
            },
            {
              key: 'name',
              label: t('customers.name'),
              width: '1.5fr',
              render: (c: CustomerResponse) => (
                <span style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>
                  {c.name}
                </span>
              ),
            },
            {
              key: 'channel',
              label: t('customers.channel'),
              width: '140px',
              render: (c: CustomerResponse) => {
                const ch = c.channel as Channel
                return (
                  <Chip tone={CHANNEL_TONE[ch] ?? 'forest'}>
                    {t(`channels.${c.channel}`)}
                  </Chip>
                )
              },
            },
            {
              key: 'contactInfo',
              label: t('customers.contactInfo'),
              width: '1fr',
              render: (c: CustomerResponse) => (
                <span
                  style={{
                    fontFamily: 'var(--font-mono)',
                    fontSize: 10,
                    color: 'var(--color-forest)',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                    display: 'block',
                  }}
                >
                  {c.contactInfo ?? '—'}
                </span>
              ),
            },
            {
              key: 'goto',
              label: '',
              width: '40px',
              align: 'right',
              render: () => (
                <span style={{ color: 'var(--color-accent)', fontFamily: 'var(--font-mono)' }}>→</span>
              ),
            },
          ]}
          rows={filtered}
          rowKey={(c: CustomerResponse) => c.id}
          onRowClick={(c: CustomerResponse) => openEdit(c)}
        />
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
