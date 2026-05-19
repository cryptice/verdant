import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type Channel, type OutletResponse } from '../api/client'
import { Masthead, Ledger, LedgerFilters, Chip } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import type { LedgerFilterOption } from '../components/faltet'

const ALL_CHANNELS: Channel[] = ['FLORIST', 'FARMERS_MARKET', 'CSA', 'WEDDING', 'WHOLESALE', 'DIRECT', 'OTHER']

const CHANNEL_TONE: Record<Channel, LedgerFilterOption<Channel>['tone']> = {
  FLORIST: 'clay',
  FARMERS_MARKET: 'mustard',
  CSA: 'sage',
  WEDDING: 'berry',
  WHOLESALE: 'sky',
  DIRECT: 'forest',
  OTHER: 'forest',
}

export function OutletList() {
  const qc = useQueryClient()
  const { t } = useTranslation()
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['outlets'],
    queryFn: () => api.outlets.list(),
  })

  const [channels, setChannels] = useState<Set<Channel>>(new Set(ALL_CHANNELS))
  const [showAdd, setShowAdd] = useState(false)
  const [editItem, setEditItem] = useState<OutletResponse | null>(null)
  const [deleteItem, setDeleteItem] = useState<OutletResponse | null>(null)

  const [formName, setFormName] = useState('')
  const [formChannel, setFormChannel] = useState<Channel>('DIRECT')
  const [formContactInfo, setFormContactInfo] = useState('')
  const [formNotes, setFormNotes] = useState('')
  const [formError, setFormError] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const resetForm = () => {
    setFormName('')
    setFormChannel('DIRECT')
    setFormContactInfo('')
    setFormNotes('')
    setFormError(null)
  }

  const openAdd = () => { resetForm(); setShowAdd(true) }

  const openEdit = (o: OutletResponse) => {
    setFormName(o.name)
    setFormChannel(o.channel)
    setFormContactInfo(o.contactInfo ?? '')
    setFormNotes(o.notes ?? '')
    setFormError(null)
    setEditItem(o)
  }

  const createMut = useMutation({
    mutationFn: () => api.outlets.create({
      name: formName,
      channel: formChannel,
      contactInfo: formContactInfo || undefined,
      notes: formNotes || undefined,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['outlets'] })
      setShowAdd(false)
      resetForm()
    },
    onError: (err) => { setFormError(err instanceof Error ? err.message : String(err)) },
  })

  const updateMut = useMutation({
    mutationFn: () => api.outlets.update(editItem!.id, {
      name: formName,
      channel: formChannel,
      contactInfo: formContactInfo || undefined,
      notes: formNotes || undefined,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['outlets'] })
      setEditItem(null)
      resetForm()
    },
    onError: (err) => { setFormError(err instanceof Error ? err.message : String(err)) },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.outlets.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['outlets'] })
      setDeleteItem(null)
      setDeleteError(null)
    },
    onError: (err) => { setDeleteError(err instanceof Error ? err.message : String(err)) },
  })

  if (isLoading) {
    return (
      <div className="flex justify-center p-16">
        <div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" />
      </div>
    )
  }
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  const filtered = (data ?? []).filter(o => channels.has(o.channel))

  const formFields = (
    <div className="space-y-4">
      <div>
        <label className="field-label">{t('outlets.name')} *</label>
        <input type="text" value={formName} onChange={e => setFormName(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{t('outlets.channel')} *</label>
        <select value={formChannel} onChange={e => setFormChannel(e.target.value as Channel)} className="input">
          {ALL_CHANNELS.map(ch => (
            <option key={ch} value={ch}>{t(`channels.${ch}`)}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="field-label">{t('outlets.contactInfo')}</label>
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
        left={t('nav.outlets')}
        center="— Försäljningskanaler —"
        right={
          <button onClick={openAdd} className="btn-primary">
            {t('outlets.newOutlet')}
          </button>
        }
      />

      <div className="page-body">
        <LedgerFilters
          options={ALL_CHANNELS.map(ch => ({
            id: ch,
            label: t(`channels.${ch}`),
            tone: CHANNEL_TONE[ch],
          }))}
          value={channels}
          onChange={setChannels}
          storageKey="verdant-outlet-filters"
        />

        <Ledger
          paginated
          pageSize={50}
          columns={[
            {
              key: 'id',
              label: '№',
              width: '60px',
              render: (_o, i) => (
                <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 22, color: 'var(--color-berry)' }}>
                  {String(i + 1).padStart(2, '0')}
                </span>
              ),
            },
            {
              key: 'name',
              label: t('outlets.name'),
              width: '1.5fr',
              render: (o: OutletResponse) => (
                <span style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>{o.name}</span>
              ),
            },
            {
              key: 'channel',
              label: t('outlets.channel'),
              width: '140px',
              render: (o: OutletResponse) => (
                <Chip tone={CHANNEL_TONE[o.channel] ?? 'forest'}>
                  {t(`channels.${o.channel}`)}
                </Chip>
              ),
            },
            {
              key: 'contactInfo',
              label: t('outlets.contactInfo'),
              width: '1fr',
              render: (o: OutletResponse) => (
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
                  {o.contactInfo ?? '—'}
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
          rowKey={(o: OutletResponse) => o.id}
          onRowClick={(o: OutletResponse) => openEdit(o)}
        />
      </div>

      <Dialog
        open={showAdd}
        onClose={() => { setShowAdd(false); resetForm() }}
        title={t('outlets.newOutlet')}
        actions={
          <>
            <button onClick={() => { setShowAdd(false); resetForm() }} className="px-4 py-2 text-sm text-text-secondary">
              {t('common.cancel')}
            </button>
            <button
              onClick={() => createMut.mutate()}
              disabled={!formName || createMut.isPending}
              className="btn-primary text-sm"
            >
              {createMut.isPending ? t('common.saving') : t('common.add')}
            </button>
          </>
        }
      >
        {formFields}
      </Dialog>

      <Dialog
        open={editItem !== null}
        onClose={() => { setEditItem(null); resetForm() }}
        title={t('outlets.editOutlet')}
        actions={
          <>
            <button onClick={() => { setEditItem(null); resetForm() }} className="px-4 py-2 text-sm text-text-secondary">
              {t('common.cancel')}
            </button>
            <button
              onClick={() => updateMut.mutate()}
              disabled={!formName || updateMut.isPending}
              className="btn-primary text-sm"
            >
              {updateMut.isPending ? t('common.saving') : t('common.save')}
            </button>
          </>
        }
      >
        {formFields}
        <button
          onClick={() => { setEditItem(null); resetForm(); setDeleteItem(editItem) }}
          className="text-sm text-error hover:underline mt-4"
        >
          {t('outlets.deleteOutlet')}
        </button>
      </Dialog>

      <Dialog
        open={deleteItem !== null}
        onClose={() => { setDeleteItem(null); setDeleteError(null) }}
        title={t('outlets.deleteOutlet')}
        actions={
          <>
            <button onClick={() => { setDeleteItem(null); setDeleteError(null) }} className="px-4 py-2 text-sm text-text-secondary">
              {t('common.cancel')}
            </button>
            <button
              onClick={() => deleteItem && deleteMut.mutate(deleteItem.id)}
              className="px-4 py-2 text-sm text-error font-semibold"
            >
              {t('common.delete')}
            </button>
          </>
        }
      >
        <p className="text-text-secondary">{t('outlets.deleteConfirm')}</p>
        {deleteError && <p className="text-error text-sm mt-2">{deleteError}</p>}
      </Dialog>
    </div>
  )
}
