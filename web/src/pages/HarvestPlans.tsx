import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../api/client'
import { harvestPlans, type HarvestPlanPreview, type HarvestPlanResponse, type PlanningSpecies, type ProductionProfile, type SellableUnit } from '../api/harvestPlans'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'

const activities = ['PURCHASE', 'SOW', 'POT_UP', 'PLANT', 'PINCH', 'SUPPORT', 'WATER', 'FERTILIZE', 'HARVEST', 'TODO']
const unitTypes = ['SEED', 'PLUG', 'BULB', 'TUBER', 'PLANT']

function ProfileEditor({ species, onClose, onSaved }: { species: PlanningSpecies; onClose: () => void; onSaved: () => void }) {
  const { t } = useTranslation()
  const [profile, setProfile] = useState<ProductionProfile>(species.profile)
  const save = useMutation({ mutationFn: () => harvestPlans.profile(species.speciesId, profile), onSuccess: onSaved })
  const updateStep = (index: number, patch: Partial<ProductionProfile['steps'][number]>) => setProfile(p => ({ ...p, steps: p.steps.map((s, i) => i === index ? { ...s, ...patch } : s) }))
  return <Dialog open onClose={onClose} title={species.speciesName}>
    <form className="space-y-4" onSubmit={e => { e.preventDefault(); save.mutate() }}>
      <p>{t('planning.profileHelp')}</p>
      <fieldset disabled={save.isPending} className="space-y-4">
        <label className="block">{t('planning.unit')}<select className="input" value={profile.sellableUnit} onChange={e => setProfile({ ...profile, sellableUnit: e.target.value as SellableUnit })}>{['STEM', 'FLOWER'].map(u => <option key={u} value={u}>{t(`planning.units.${u}`)}</option>)}</select></label>
        <label className="block">{t('planning.yield')}<input className="input" type="number" min="0.01" max="1000000" step="any" required value={profile.outputPerPlant ?? ''} onChange={e => setProfile({ ...profile, outputPerPlant: e.target.value === '' ? null : Number(e.target.value) })} /></label>
        <label className="block">{t('planning.success')}<input className="input" type="number" min="0.01" max="100" step="any" required value={profile.establishmentPercent ?? ''} onChange={e => setProfile({ ...profile, establishmentPercent: e.target.value === '' ? null : Number(e.target.value) })} /></label>
        <label className="block">{t('planning.startingUnit')}<select className="input" value={profile.startingUnit} onChange={e => setProfile({ ...profile, startingUnit: e.target.value })}>{unitTypes.map(u => <option key={u} value={u}>{t(`planning.units.${u}`)}</option>)}</select></label>
        <p>{t('planning.scheduleHelp')}</p>
        {profile.steps.map((step, index) => <fieldset key={step.key} className="border p-3 space-y-2">
          <legend>{index + 1}</legend>
          <label className="block">{t('planning.stepName')}<input className="input" required maxLength={255} value={step.name} onChange={e => updateStep(index, { name: e.target.value })} /></label>
          <label className="block">{t('planning.activity')}<select className="input" value={step.activityType} onChange={e => updateStep(index, { activityType: e.target.value })}>{activities.map(a => <option key={a} value={a}>{t(`activityType.${a}`, a)}</option>)}</select></label>
          <label className="block">{t('planning.daysBefore')}<input className="input" type="number" min="0" max="3650" required value={step.daysBeforeHarvest ?? ''} onChange={e => updateStep(index, { daysBeforeHarvest: e.target.value === '' ? null : Number(e.target.value) })} /></label>
          <label className="block">{t('planning.quantityBasis')}<select className="input" value={step.quantityBasis} onChange={e => updateStep(index, { quantityBasis: e.target.value as typeof step.quantityBasis })}>{['START', 'PLANT', 'OUTPUT'].map(b => <option key={b} value={b}>{t(`planning.basis.${b}`)}</option>)}</select></label>
          <button type="button" className="btn-secondary" onClick={() => setProfile({ ...profile, steps: profile.steps.filter((_, i) => i !== index) })}>{t('common.delete')}</button>
        </fieldset>)}
        <button type="button" className="btn-secondary" disabled={profile.steps.length >= 30} onClick={() => setProfile({ ...profile, steps: [...profile.steps.slice(0, -1), { key: crypto.randomUUID(), name: '', activityType: 'TODO', daysBeforeHarvest: null, quantityBasis: 'PLANT' }, ...profile.steps.slice(-1)] })}>{t('planning.addStep')}</button>
        <button className="btn-primary" type="submit">{t('common.save')}</button>
      </fieldset>
      {save.error && <ErrorDisplay error={save.error} />}
    </form>
  </Dialog>
}

function PlanDetail({ plan, onChanged }: { plan: HarvestPlanResponse; onChanged: () => void }) {
  const { t } = useTranslation()
  const [amounts, setAmounts] = useState<Record<number, string>>({})
  const [confirmCancel, setConfirmCancel] = useState(false)
  const change = useMutation({ mutationFn: (action: () => Promise<HarvestPlanResponse>) => action(), onSuccess: onChanged, onError: onChanged })
  return <section className="space-y-4 border p-4">
    <h2>{plan.snapshot.request.quantity} {t(`planning.units.${plan.snapshot.request.sellableUnit}`)} · {plan.snapshot.request.harvestDate}</h2>
    <p>{t('planning.progressHelp')}</p>
    {plan.status === 'CANCELLED' && <p>{t('planning.cancelled')}</p>}
    {plan.snapshot.allocations.map(a => <div key={a.speciesId} className="space-y-3">
      <h3>{a.speciesName} · {a.outputQuantity} {t(`planning.units.${a.profile.sellableUnit}`)}</h3>
      {a.steps.map(step => {
        const task = plan.tasks.find(task => task.stepKey === step.key)
        const amount = Number(amounts[task?.id ?? 0] ?? task?.remainingCount ?? 0)
        return <div key={step.key} className="border-t py-3">
          <p>{step.date} · {step.name} · {step.quantity} {t(`planning.units.${step.unit}`)}</p>
          {task && <p>{t('planning.remaining', { count: task.remainingCount })} · {t(`planning.status.${task.status}`)}</p>}
          {task && task.status === 'PENDING' && plan.status === 'ACTIVE' && <div className="flex gap-2 items-end">
            <label>{t('planning.processed')}<input className="input" type="number" min="1" max={task.remainingCount} value={amounts[task.id] ?? task.remainingCount} disabled={change.isPending} onChange={e => setAmounts({ ...amounts, [task.id]: e.target.value })} /></label>
            <button className="btn-secondary" disabled={change.isPending || !Number.isInteger(amount) || amount < 1 || amount > task.remainingCount} onClick={() => change.mutate(() => harvestPlans.complete(plan.id, task.id, task.remainingCount, amount))}>{t('planning.recordProgress')}</button>
          </div>}
        </div>
      })}
    </div>)}
    {change.error && <ErrorDisplay error={change.error} />}
    {plan.status === 'ACTIVE' && <button className="btn-secondary" disabled={change.isPending} onClick={() => setConfirmCancel(true)}>{t('planning.cancelPlan')}</button>}
    <Dialog open={confirmCancel} onClose={() => setConfirmCancel(false)} title={t('planning.cancelPlan')}>
      <p>{t('planning.cancelHelp')}</p>
      <button className="btn-primary" disabled={change.isPending} onClick={() => change.mutate(async () => { const result = await harvestPlans.cancel(plan.id); setConfirmCancel(false); return result })}>{t('planning.cancelPlan')}</button>
    </Dialog>
  </section>
}

export function HarvestPlans() {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [params, setParams] = useSearchParams()
  const selectedId = Number(params.get('plan')) || undefined
  const [page, setPage] = useState(0)
  const plans = useQuery({ queryKey: ['harvest-plans', page], queryFn: () => harvestPlans.list(page * 50) })
  const detail = useQuery({ queryKey: ['harvest-plan', selectedId], queryFn: () => harvestPlans.get(selectedId!), enabled: !!selectedId })
  const seasons = useQuery({ queryKey: ['seasons'], queryFn: api.seasons.list })
  const [species, setSpecies] = useState<SpeciesResponse | null>(null)
  const [group, setGroup] = useState<{ id: number; name: string } | null>(null)
  const [seasonId, setSeasonId] = useState('')
  const [quantity, setQuantity] = useState('300')
  const [date, setDate] = useState('')
  const [unit, setUnit] = useState<SellableUnit>('STEM')
  const [fixed, setFixed] = useState<Record<number, string>>({})
  const [preview, setPreview] = useState<HarvestPlanPreview | null>(null)
  const [requestKey, setRequestKey] = useState(() => crypto.randomUUID())
  const [edit, setEdit] = useState<PlanningSpecies | null>(null)
  const candidates = useQuery({ queryKey: ['planning-species', species?.id, group?.id], queryFn: () => harvestPlans.species(species?.id, group?.id), enabled: !!species || !!group })
  const invalidate = () => {
    void qc.invalidateQueries({ queryKey: ['harvest-plans'] })
    void qc.invalidateQueries({ queryKey: ['harvest-plan'] })
    void qc.invalidateQueries({ queryKey: ['tasks'] })
  }
  const resetPreview = () => { setPreview(null); setRequestKey(crypto.randomUUID()); calculate.reset(); save.reset() }
  const calculate = useMutation({ mutationFn: () => harvestPlans.preview({ seasonId: Number(seasonId), speciesId: species?.id, groupId: group?.id, quantity: Number(quantity), harvestDate: date, sellableUnit: unit, allocations: Object.fromEntries(Object.entries(fixed).filter(([, value]) => value !== '').map(([id, value]) => [id, Number(value)])) }), onSuccess: setPreview })
  const save = useMutation({ mutationFn: () => harvestPlans.create(requestKey, preview!), onSuccess: plan => { invalidate(); setParams({ plan: String(plan.id) }); setPreview(null); setRequestKey(crypto.randomUUID()) } })
  const busy = calculate.isPending || save.isPending
  return <div className="space-y-6">
    <h1 className="text-3xl">{t('planning.title')}</h1>
    <p>{t('planning.intro')}</p>
    {detail.isFetching && selectedId && <p>{t('planning.loading')}</p>}
    {detail.error && <ErrorDisplay error={detail.error} onRetry={() => { void detail.refetch() }} />}
    {detail.data && <PlanDetail key={detail.data.id} plan={detail.data} onChanged={invalidate} />}
    <form className="space-y-4" onSubmit={e => { e.preventDefault(); setPreview(null); calculate.mutate() }}>
      <fieldset disabled={busy} className="space-y-4">
        <label className="block">{t('planning.season')}<select className="input" required value={seasonId} onChange={e => { setSeasonId(e.target.value); resetPreview() }}><option value="">{t('planning.choose')}</option>{seasons.data?.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</select></label>
        <label className="block">{t('planning.species')}</label>
        <SpeciesAutocomplete value={species} showGroups onChange={sp => { setSpecies(sp); setGroup(null); setFixed({}); resetPreview() }} onGroupSelect={(id, name) => { setSpecies(null); setGroup({ id, name }); setFixed({}); resetPreview() }} />
        {group && <p>{group.name}</p>}
        <div className="grid gap-4 sm:grid-cols-3">
          <label>{t('planning.quantity')}<input className="input" type="number" min="1" max="1000000" required value={quantity} onChange={e => { setQuantity(e.target.value); resetPreview() }} /></label>
          <label>{t('planning.unit')}<select className="input" value={unit} onChange={e => { setUnit(e.target.value as SellableUnit); resetPreview() }}>{['STEM', 'FLOWER'].map(u => <option key={u} value={u}>{t(`planning.units.${u}`)}</option>)}</select></label>
          <label>{t('planning.harvestDate')}<input className="input" type="date" required value={date} onChange={e => { setDate(e.target.value); resetPreview() }} /></label>
        </div>
        <p>{t('planning.mixHelp')}</p>
        {candidates.isFetching && <p>{t('planning.loading')}</p>}
        {candidates.data?.map(sp => <div key={sp.speciesId} className="flex flex-wrap items-end gap-3 border-b py-3">
          <label className="grow">{sp.speciesName}<input className="input" type="number" min="0" max={quantity} placeholder={t('planning.suggested')} value={fixed[sp.speciesId] ?? ''} onChange={e => { setFixed({ ...fixed, [sp.speciesId]: e.target.value }); resetPreview() }} /></label>
          <button type="button" className="btn-secondary" onClick={() => { setEdit(sp); resetPreview() }}>{t('planning.configure')}</button>
        </div>)}
        <button className="btn-primary" disabled={!seasonId || (!species && !group) || candidates.isFetching || !!candidates.error}>{t('planning.preview')}</button>
      </fieldset>
    </form>
    {[seasons.error, candidates.error, calculate.error, save.error].filter(Boolean).map((error, i) => <ErrorDisplay key={i} error={error} />)}
    {preview && <section className="space-y-3 border p-4" aria-label={t('planning.preview')}>
      {preview.issues.map((issue, i) => <p key={i}>{candidates.data?.find(sp => sp.speciesId === issue.speciesId)?.speciesName} {issue.message}</p>)}
      {preview.allocations.map(a => <div key={a.speciesId}>
        <h3>{a.speciesName} · {a.outputQuantity} {t(`planning.units.${unit}`)}</h3>
        <p>{a.plantsNeeded} {t('planning.units.PLANT')} · {a.startingQuantity} {t(`planning.units.${a.profile.startingUnit}`)}</p>
        <ol>{a.steps.map(s => <li key={s.key}>{s.date} · {s.name} · {s.quantity} {t(`planning.units.${s.unit}`)}</li>)}</ol>
      </div>)}
      <button className="btn-primary" disabled={!preview.canSave || busy} onClick={() => save.mutate()}>{t('planning.savePlan')}</button>
    </section>}
    {edit && <ProfileEditor key={edit.speciesId} species={edit} onClose={() => setEdit(null)} onSaved={() => { setEdit(null); resetPreview(); void qc.invalidateQueries({ queryKey: ['planning-species'] }) }} />}
    <h2>{t('planning.saved')}</h2>
    {plans.isLoading && <p>{t('planning.loading')}</p>}
    {plans.error && <ErrorDisplay error={plans.error} onRetry={() => { void plans.refetch() }} />}
    {plans.data?.length === 0 && <p>{t('planning.empty')}</p>}
    {plans.data?.map(plan => <button key={plan.id} className="block w-full text-left border-b py-4" onClick={() => setParams({ plan: String(plan.id) })}>{plan.snapshot.request.harvestDate} · {plan.snapshot.request.quantity} {t(`planning.units.${plan.snapshot.request.sellableUnit}`)} · {plan.snapshot.allocations.map(a => a.speciesName).join(', ')} {plan.status === 'CANCELLED' ? t('planning.cancelled') : ''}</button>)}
    <div className="flex gap-3"><button className="btn-secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>{t('planning.previous')}</button><button className="btn-secondary" disabled={(plans.data?.length ?? 0) < 50} onClick={() => setPage(page + 1)}>{t('planning.next')}</button></div>
  </div>
}
