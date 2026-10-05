import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { planning, buttonClass, inputClass, linkClass, units, activities, type SharedSchedule, type LifecycleStep } from '../api/planning'
import { PlanningConfirm, PlanningError, PlanningField, PlanningMonths } from '../components/PlanningFields'

export function PlanningSchedules() {
  const { t } = useTranslation()
  const [query, setQuery] = useState('')
  const [reviewOnly, setReviewOnly] = useState(false)
  const schedules = useQuery({ queryKey: ['admin', 'planning', 'schedules'], queryFn: planning.schedules })
  const species = useQuery({ queryKey: ['admin', 'planning', 'species'], queryFn: planning.species })
  return <div className="space-y-5 max-w-5xl">
    <div className="flex justify-between items-center"><h1 className="text-xl font-semibold">{t('planning.schedules')}</h1><Link className={buttonClass} to="/schedules/new">{t('planning.newSchedule')}</Link></div>
    <p className="text-sm text-[#787774]">{t('planning.schedulesHelp')}</p>
    <Link className={linkClass} to="/planning/species">{t('planning.assignments')}</Link>
    <PlanningError error={schedules.error || species.error} />
    <PlanningField label={t('planning.searchSchedules')}><input className={inputClass} type="search" value={query} onChange={e => setQuery(e.target.value)} /></PlanningField>
    <label className="flex gap-2 text-sm"><input type="checkbox" checked={reviewOnly} onChange={e => setReviewOnly(e.target.checked)} />{t('planning.reviewOnly')}</label>
    {schedules.isPending ? <p>{t('common.loading')}</p> : <ul className="divide-y border border-[#E9E9E7] rounded">
      {schedules.data?.filter(s => (!reviewOnly || s.reviewRequired) && `${s.name} ${s.key} ${s.genera.join(' ')}`.toLocaleLowerCase().includes(query.toLocaleLowerCase())).map(s => <li key={s.key}>
        <Link className="block p-3 hover:bg-[#FBFBFA]" to={`/schedules/${s.key}`}><div className="flex justify-between gap-3"><strong className={`${linkClass} font-medium`}>{s.name}</strong><span className="text-sm">{t(s.reviewRequired ? 'planning.reviewRequired' : 'planning.ready')}</span></div>
          <p className="text-sm text-[#787774] mt-1">{t(`planning.units.${s.profile.startingUnit}`)} · {t('planning.stepCount', { count: s.profile.steps.length })} · {t('planning.matchedCount', { count: species.data?.filter(sp => sp.resolvedScheduleKey === s.key).length ?? 0 })}</p>
        </Link>
      </li>)}
    </ul>}
  </div>
}

export function PlanningScheduleDetail() {
  const { key } = useParams()
  const [params] = useSearchParams()
  const { t } = useTranslation()
  const sourceKey = key === 'new' ? params.get('copy') : key
  const schedule = useQuery({ queryKey: ['admin', 'planning', 'schedule', sourceKey], queryFn: () => planning.schedule(sourceKey!), enabled: !!sourceKey })
  if (sourceKey && schedule.error) return <PlanningError error={schedule.error} />
  if (sourceKey && schedule.isPending) return <p>{t('common.loading')}</p>
  const seed: SharedSchedule = schedule.data ? { ...schedule.data, ...(key === 'new' ? { key: '', name: `${schedule.data.name} (${t('planning.copy')})`, revision: 0, autoMatch: false } : {}) } : {
    key: '', name: '', priority: 1000, revision: 0, autoMatch: false, useSpeciesTiming: true,
    version: new Date().toISOString().slice(0, 10), climate: '', description: '', reviewRequired: true,
    harvestMonths: [], plantingMonths: [], sources: [], genera: [], scientificPrefixes: [], plantTypes: [], startingUnits: ['SEED'],
    profile: { startingUnit: 'SEED', sellableUnit: 'STEM', outputPerPlant: 1, establishmentPercent: 70, steps: [
      { key: 'purchase', name: t('planning.activities.PURCHASE'), activityType: 'PURCHASE', daysBeforeHarvest: null, quantityBasis: 'START' },
      { key: 'start', name: t('planning.activities.SOW'), activityType: 'SOW', daysBeforeHarvest: null, quantityBasis: 'START' },
      { key: 'harvest', name: t('planning.activities.HARVEST'), activityType: 'HARVEST', daysBeforeHarvest: 0, quantityBasis: 'OUTPUT' },
    ] },
  }
  return <ScheduleEditor key={`${key}:${sourceKey}`} initial={seed} isNew={key === 'new'} />
}

function ScheduleEditor({ initial, isNew }: { initial: SharedSchedule; isNew: boolean }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [draft, setDraft] = useState(initial)
  const [genera, setGenera] = useState(initial.genera.join(', '))
  const [prefixes, setPrefixes] = useState(initial.scientificPrefixes.join(', '))
  const [confirm, setConfirm] = useState<'save' | 'delete' | null>(null)
  const species = useQuery({ queryKey: ['admin', 'planning', 'species'], queryFn: planning.species })
  const matched = species.data?.filter(s => s.resolvedScheduleKey === initial.key) ?? []
  const assigned = species.data?.filter(s => s.assignedScheduleKey === initial.key) ?? []
  const split = (text: string) => text.split(',').map(s => s.trim()).filter(Boolean)
  const save = useMutation({ mutationFn: () => {
    const request = { ...draft, genera: split(genera), scientificPrefixes: split(prefixes) }
    return isNew ? planning.createSchedule(request) : planning.updateSchedule(request)
  }, onSuccess: result => {
    setDraft(result)
    qc.setQueryData(['admin', 'planning', 'schedule', result.key], result)
    qc.invalidateQueries({ queryKey: ['admin', 'planning'] }); setConfirm(null); navigate(`/schedules/${result.key}`, { replace: true })
  } })
  const remove = useMutation({ mutationFn: () => planning.deleteSchedule(draft.key, draft.revision), onSuccess: () => {
    qc.invalidateQueries({ queryKey: ['admin', 'planning'] }); navigate('/schedules')
  } })
  const updateStep = (index: number, change: Partial<LifecycleStep>) => setDraft(d => ({ ...d, profile: { ...d.profile, steps: d.profile.steps.map((step, i) => i === index ? { ...step, ...change } : step) } }))
  const moveStep = (index: number, delta: number) => {
    const steps = [...draft.profile.steps]
    ;[steps[index], steps[index + delta]] = [steps[index + delta], steps[index]]
    setDraft({ ...draft, profile: { ...draft.profile, steps } })
  }
  const pending = save.isPending || remove.isPending
  return <div className="space-y-5 max-w-5xl">
    <Link className={linkClass} to="/schedules">← {t('planning.schedules')}</Link>
    <div className="flex flex-wrap justify-between gap-2"><h1 className="text-xl font-semibold">{isNew ? t('planning.newSchedule') : initial.name}</h1>{!isNew && <Link className={buttonClass} to={`/schedules/new?copy=${initial.key}`}>{t('planning.duplicate')}</Link>}</div>
    <p className="text-sm text-[#787774]">{t('planning.scheduleImpact')}</p>
    {!isNew && <Link className={linkClass} to={`/planning/species?schedule=${initial.key}`}>{t('planning.matchedCount', { count: matched.length })} · {t('planning.assignedCount', { count: assigned.length })}</Link>}
    <PlanningError error={save.error || remove.error || species.error} />
    <form onSubmit={e => { e.preventDefault(); setConfirm('save') }} className="space-y-5">
      <fieldset disabled={pending || confirm !== null} className="space-y-5">
        <div className="grid md:grid-cols-2 gap-4">
          <PlanningField label={t('planning.name')}><input className={inputClass} required maxLength={255} value={draft.name} onChange={e => setDraft({ ...draft, name: e.target.value })} /></PlanningField>
          <PlanningField label={t('planning.key')}><input className={inputClass} required pattern="[a-z0-9][a-z0-9-]{0,79}" disabled={!isNew} value={draft.key} onChange={e => setDraft({ ...draft, key: e.target.value })} /></PlanningField>
          <PlanningField label={t('planning.version')}><input className={inputClass} required maxLength={80} value={draft.version} onChange={e => setDraft({ ...draft, version: e.target.value })} /></PlanningField>
          <PlanningField label={t('planning.climate')}><input className={inputClass} required maxLength={1000} value={draft.climate} onChange={e => setDraft({ ...draft, climate: e.target.value })} /></PlanningField>
        </div>
        <PlanningField label={t('planning.description')}><textarea className={inputClass} rows={3} maxLength={10000} value={draft.description} onChange={e => setDraft({ ...draft, description: e.target.value })} /></PlanningField>
        <label className="flex gap-2 text-sm"><input type="checkbox" checked={draft.reviewRequired} onChange={e => setDraft({ ...draft, reviewRequired: e.target.checked })} />{t('planning.reviewRequired')}</label>
        <p className="text-sm text-[#787774]">{t('planning.reviewHelp')}</p>
        <section className="border border-[#E9E9E7] rounded p-4 space-y-4">
          <h2 className="font-semibold">{t('planning.quantities')}</h2>
          <div className="grid md:grid-cols-2 gap-4">
            <PlanningField label={t('planning.startingUnit')}><select className={inputClass} value={draft.profile.startingUnit} onChange={e => {
              const unit = e.target.value as SharedSchedule['profile']['startingUnit']
              setDraft({ ...draft, startingUnits: [unit], profile: { ...draft.profile, startingUnit: unit, steps: draft.profile.steps.map(step => step.quantityBasis === 'START' && ['SOW', 'PLANT'].includes(step.activityType) ? { ...step, activityType: unit === 'SEED' ? 'SOW' : 'PLANT' } : step) } })
            }}>{units.map(unit => <option key={unit} value={unit}>{t(`planning.units.${unit}`)}</option>)}</select></PlanningField>
            <PlanningField label={t('planning.sellableUnit')}><select className={inputClass} value={draft.profile.sellableUnit} onChange={e => setDraft({ ...draft, profile: { ...draft.profile, sellableUnit: e.target.value as 'STEM' | 'FLOWER' } })}>{['STEM', 'FLOWER'].map(unit => <option key={unit} value={unit}>{t(`planning.units.${unit}`)}</option>)}</select></PlanningField>
            <PlanningField label={t('planning.output')}><input className={inputClass} type="number" step="any" min="0.01" max="1000000" required={!draft.reviewRequired} value={draft.profile.outputPerPlant ?? ''} onChange={e => setDraft({ ...draft, profile: { ...draft.profile, outputPerPlant: e.target.value === '' ? null : Number(e.target.value) } })} /></PlanningField>
            <PlanningField label={t('planning.establishment')}><input className={inputClass} type="number" step="any" min="0.01" max="100" required={!draft.reviewRequired} value={draft.profile.establishmentPercent ?? ''} onChange={e => setDraft({ ...draft, profile: { ...draft.profile, establishmentPercent: e.target.value === '' ? null : Number(e.target.value) } })} /></PlanningField>
          </div>
          <label className="flex gap-2 text-sm"><input type="checkbox" checked={draft.useSpeciesTiming} onChange={e => setDraft({ ...draft, useSpeciesTiming: e.target.checked })} />{t('planning.useSpeciesTiming')}</label>
          <p className="text-sm text-[#787774]">{t('planning.quantityHelp')}</p>
        </section>
        <section className="space-y-3">
          <h2 className="font-semibold">{t('planning.lifecycle')}</h2><p className="text-sm text-[#787774]">{t('planning.stepsHelp')}</p>
          {draft.profile.steps.map((step, index) => <div key={step.key} className="border border-[#E9E9E7] rounded p-4 space-y-3">
            <div className="flex justify-between items-center"><span className="text-sm font-semibold">{index + 1}</span><div className="flex gap-2">
              <button type="button" className={buttonClass} disabled={index === 0} aria-label={t('planning.moveUp')} onClick={() => moveStep(index, -1)}>↑</button>
              <button type="button" className={buttonClass} disabled={index === draft.profile.steps.length - 1} aria-label={t('planning.moveDown')} onClick={() => moveStep(index, 1)}>↓</button>
              <button type="button" className={buttonClass} onClick={() => setDraft({ ...draft, profile: { ...draft.profile, steps: draft.profile.steps.filter((_, i) => i !== index) } })}>{t('common.remove')}</button>
            </div></div>
            <div className="grid md:grid-cols-2 gap-3">
              <PlanningField label={t('planning.stepName')}><input className={inputClass} required maxLength={255} value={step.name} onChange={e => updateStep(index, { name: e.target.value })} /></PlanningField>
              <PlanningField label={t('planning.activity')}><select className={inputClass} value={step.activityType} onChange={e => updateStep(index, { activityType: e.target.value })}>{activities.map(a => <option key={a} value={a}>{t(`planning.activities.${a}`)}</option>)}</select></PlanningField>
              <PlanningField label={t('planning.daysBefore')}><input className={inputClass} type="number" min="0" max="3650" required={!draft.reviewRequired} value={step.daysBeforeHarvest ?? ''} onChange={e => updateStep(index, { daysBeforeHarvest: e.target.value === '' ? null : Number(e.target.value) })} /></PlanningField>
              <PlanningField label={t('planning.quantityBasis')}><select className={inputClass} value={step.quantityBasis} onChange={e => updateStep(index, { quantityBasis: e.target.value as LifecycleStep['quantityBasis'] })}>{['START', 'PLANT', 'OUTPUT'].map(b => <option key={b} value={b}>{t(`planning.basis.${b}`)}</option>)}</select></PlanningField>
            </div>
          </div>)}
          <button type="button" className={buttonClass} disabled={draft.profile.steps.length >= 30} onClick={() => setDraft({ ...draft, profile: { ...draft.profile, steps: [...draft.profile.steps.slice(0, -1), { key: crypto.randomUUID(), name: '', activityType: 'TODO', daysBeforeHarvest: null, quantityBasis: 'PLANT' }, ...draft.profile.steps.slice(-1)] } })}>{t('planning.addStep')}</button>
        </section>
        <section className="border border-[#E9E9E7] rounded p-4 space-y-4"><h2 className="font-semibold">{t('planning.season')}</h2><p className="text-sm text-[#787774]">{t('planning.monthsHelp')}</p>
          <PlanningMonths label={t('planning.harvestMonths')} value={draft.harvestMonths} onChange={harvestMonths => setDraft({ ...draft, harvestMonths })} />
          <PlanningMonths label={t('planning.plantingMonths')} value={draft.plantingMonths} onChange={plantingMonths => setDraft({ ...draft, plantingMonths })} />
        </section>
        <section className="border border-[#E9E9E7] rounded p-4 space-y-4"><h2 className="font-semibold">{t('planning.matching')}</h2><p className="text-sm text-[#787774]">{t('planning.matchingHelp')}</p>
          <label className="flex gap-2 text-sm"><input type="checkbox" checked={draft.autoMatch} onChange={e => setDraft({ ...draft, autoMatch: e.target.checked })} />{t('planning.autoMatch')}</label>
          <div className="grid md:grid-cols-2 gap-3">
            <PlanningField label={t('planning.priority')}><input className={inputClass} required type="number" min="0" max="100000" value={draft.priority} onChange={e => setDraft({ ...draft, priority: Number(e.target.value) })} /></PlanningField>
            <PlanningField label={t('planning.genera')}><input className={inputClass} value={genera} onChange={e => setGenera(e.target.value)} /></PlanningField>
            <PlanningField label={t('planning.prefixes')}><input className={inputClass} value={prefixes} onChange={e => setPrefixes(e.target.value)} /></PlanningField>
          </div>
          <fieldset><legend className="text-sm text-[#787774] mb-2">{t('planning.plantTypes')}</legend><div className="flex flex-wrap gap-3">{['ANNUAL', 'BIENNIAL', 'PERENNIAL', 'BULB', 'TUBER'].map(type => <label key={type} className="flex gap-2 text-sm"><input type="checkbox" checked={draft.plantTypes.includes(type)} onChange={e => setDraft({ ...draft, plantTypes: e.target.checked ? [...draft.plantTypes, type] : draft.plantTypes.filter(v => v !== type) })} />{t(`planning.plantType.${type}`)}</label>)}</div></fieldset>
        </section>
        <section className="space-y-3"><h2 className="font-semibold">{t('planning.sources')}</h2>{draft.sources.map((source, index) => <div key={index} className="grid md:grid-cols-[1fr_2fr_auto] items-end gap-3">
          <PlanningField label={t('planning.sourceTitle')}><input className={inputClass} required maxLength={255} value={source.title} onChange={e => setDraft({ ...draft, sources: draft.sources.map((s, i) => i === index ? { ...s, title: e.target.value } : s) })} /></PlanningField>
          <PlanningField label={t('planning.sourceUrl')}><input className={inputClass} required type="url" maxLength={2000} value={source.url} onChange={e => setDraft({ ...draft, sources: draft.sources.map((s, i) => i === index ? { ...s, url: e.target.value } : s) })} /></PlanningField>
          <button type="button" className={buttonClass} onClick={() => setDraft({ ...draft, sources: draft.sources.filter((_, i) => i !== index) })}>{t('common.remove')}</button>
        </div>)}<button type="button" className={buttonClass} disabled={draft.sources.length >= 30} onClick={() => setDraft({ ...draft, sources: [...draft.sources, { title: '', url: '' }] })}>{t('planning.addSource')}</button></section>
        <button className={buttonClass} type="submit">{t('common.save')}</button>
      </fieldset>
      {confirm === 'save' && <PlanningConfirm pending={pending} onCancel={() => setConfirm(null)} onConfirm={() => save.mutate()}>{t('planning.saveScheduleConfirm', { count: matched.length })}</PlanningConfirm>}
    </form>
    {!isNew && (confirm === 'delete' ? <PlanningConfirm pending={pending} onCancel={() => setConfirm(null)} onConfirm={() => remove.mutate()}>{t('planning.deleteScheduleConfirm', { name: initial.name, count: matched.length })}</PlanningConfirm> : <div className="space-y-2"><button className={`${buttonClass} text-red-700`} disabled={pending || confirm !== null || assigned.length > 0 || !species.data} onClick={() => setConfirm('delete')}>{t('planning.deleteSchedule')}</button>{assigned.length > 0 && <p className="text-sm text-[#787774]">{t('planning.deleteAssignedHelp')}</p>}</div>)}
  </div>
}
