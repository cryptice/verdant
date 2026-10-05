import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { planning, buttonClass, inputClass, linkClass } from '../api/planning'
import { PlanningConfirm, PlanningError, PlanningField } from '../components/PlanningFields'

export default function PlanningAssignments() {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const qc = useQueryClient()
  const [query, setQuery] = useState('')
  const [selected, setSelected] = useState<number[]>([])
  const [target, setTarget] = useState('')
  const [confirm, setConfirm] = useState(false)
  const [saved, setSaved] = useState(false)
  const species = useQuery({ queryKey: ['admin', 'planning', 'species'], queryFn: planning.species })
  const groups = useQuery({ queryKey: ['admin', 'planning', 'groups'], queryFn: planning.groups })
  const schedules = useQuery({ queryKey: ['admin', 'planning', 'schedules'], queryFn: planning.schedules })
  const assign = useMutation({ mutationFn: () => planning.assign(selected, target || null), onSuccess: () => {
    qc.invalidateQueries({ queryKey: ['admin', 'planning'] }); setSelected([]); setConfirm(false); setSaved(true)
  } })
  const group = groups.data?.find(g => String(g.id) === params.get('group'))
  const filtered = (species.data ?? []).filter(sp =>
    (!params.get('group') || group?.speciesIds.includes(sp.id)) &&
    (!params.get('species') || String(sp.id) === params.get('species')) &&
    (!params.get('schedule') || sp.resolvedScheduleKey === params.get('schedule') || sp.assignedScheduleKey === params.get('schedule')) &&
    (!params.get('review') || sp.reviewRequired) &&
    `${sp.name} ${sp.scientificName ?? ''}`.toLocaleLowerCase().includes(query.toLocaleLowerCase()))
  const targetSchedule = schedules.data?.find(s => s.key === target)
  const compatible = filtered.filter(sp => !targetSchedule || sp.startingUnit === targetSchedule.profile.startingUnit)
  const setFilter = (key: string, value: string) => {
    const next = new URLSearchParams(params)
    if (value) next.set(key, value); else next.delete(key)
    setParams(next); setSelected([]); setSaved(false)
  }
  const scheduleName = (key: string) => schedules.data?.find(s => s.key === key)?.name ?? t('planning.noSchedule')
  const loading = species.isPending || schedules.isPending || groups.isPending
  return <div className="space-y-5 max-w-6xl">
    <h1 className="text-xl font-semibold">{t('planning.assignments')}</h1>
    <p className="text-sm text-[#787774]">{t('planning.assignmentHelp')}</p>
    <PlanningError error={species.error || groups.error || schedules.error || assign.error} />
    {saved && <p role="status" className="text-sm text-green-800">{t('planning.saved')}</p>}
    <fieldset disabled={confirm || assign.isPending} className="space-y-4">
      <div className="grid md:grid-cols-3 gap-3">
        <PlanningField label={t('planning.searchSpecies')}><input type="search" className={inputClass} value={query} onChange={e => { setQuery(e.target.value); setSelected([]) }} /></PlanningField>
        <PlanningField label={t('planning.groups')}><select className={inputClass} value={params.get('group') ?? ''} onChange={e => setFilter('group', e.target.value)}><option value="">{t('planning.allGroups')}</option>{groups.data?.map(g => <option key={g.id} value={g.id}>{g.name}</option>)}</select></PlanningField>
        <PlanningField label={t('planning.schedules')}><select className={inputClass} value={params.get('schedule') ?? ''} onChange={e => setFilter('schedule', e.target.value)}><option value="">{t('planning.allSchedules')}</option>{schedules.data?.map(s => <option key={s.key} value={s.key}>{s.name}</option>)}<option value="unassigned">{t('planning.noSchedule')}</option></select></PlanningField>
      </div>
      <div className="flex gap-4 text-sm"><label className="flex gap-2"><input type="checkbox" checked={!!params.get('review')} onChange={e => setFilter('review', e.target.checked ? '1' : '')} />{t('planning.reviewOnly')}</label>{params.get('species') && <button type="button" className={linkClass} onClick={() => setFilter('species', '')}>{t('planning.showAllSpecies')}</button>}</div>
      <section className="border border-[#E9E9E7] rounded p-4 space-y-3 bg-[#FBFBFA]">
        <PlanningField label={t('planning.assignSchedule')}><select className={inputClass} value={target} onChange={e => { setTarget(e.target.value); setSelected([]); setSaved(false) }}><option value="">{t('planning.automatic')}</option>{schedules.data?.map(s => <option key={s.key} value={s.key}>{s.name} · {t(`planning.units.${s.profile.startingUnit}`)}</option>)}</select></PlanningField>
        <p className="text-sm text-[#787774]">{t('planning.compatibilityHelp')}</p>
        <button className={buttonClass} disabled={loading || !selected.length} onClick={() => setConfirm(true)}>{t('planning.applySelected', { count: selected.length })}</button>
      </section>
      {loading ? <p>{t('common.loading')}</p> : <>
        <div className="flex flex-wrap justify-between gap-3 text-sm"><label className="flex gap-2"><input type="checkbox" checked={compatible.length > 0 && compatible.every(sp => selected.includes(sp.id))} onChange={e => setSelected(e.target.checked ? compatible.map(sp => sp.id) : [])} />{t('planning.selectVisible', { count: compatible.length })}</label><span>{t('planning.memberCount', { count: filtered.length })}</span></div>
        <ul className="divide-y border border-[#E9E9E7] rounded max-h-[65vh] overflow-y-auto">{filtered.map(sp => <li key={sp.id} className="p-3 flex gap-3">
          <input type="checkbox" className="self-start mt-1" aria-label={sp.name} disabled={!!targetSchedule && sp.startingUnit !== targetSchedule.profile.startingUnit} checked={selected.includes(sp.id)} onChange={e => setSelected(e.target.checked ? [...selected, sp.id] : selected.filter(id => id !== sp.id))} />
          <div className="space-y-1 min-w-0"><Link to={`/species/${sp.id}`} className={linkClass}>{sp.name}</Link><p className="text-xs text-[#787774]">{sp.scientificName} · {t(`planning.units.${sp.startingUnit}`)}</p>
            <p className="text-sm">{sp.resolvedScheduleKey === 'unassigned' ? t('planning.noSchedule') : <Link className={linkClass} to={`/schedules/${sp.resolvedScheduleKey}`}>{scheduleName(sp.resolvedScheduleKey)}</Link>} · {t(sp.assignedScheduleKey ? 'planning.explicit' : 'planning.automatic')} · {t(sp.reviewRequired ? 'planning.reviewRequired' : 'planning.ready')}</p>
            {sp.assignedScheduleKey && sp.assignedScheduleKey !== sp.resolvedScheduleKey && <p className="text-sm text-amber-800">{t('planning.incompatibleAssignment', { name: scheduleName(sp.assignedScheduleKey) })}</p>}
            {sp.workflowTemplateId && <p className="text-sm text-amber-800">{t('planning.workflowOverride')}</p>}
          </div>
        </li>)}</ul>
      </>}
    </fieldset>
    {confirm && <PlanningConfirm pending={assign.isPending} onCancel={() => setConfirm(false)} onConfirm={() => assign.mutate()}>{t('planning.assignConfirm', { count: selected.length, name: targetSchedule?.name ?? t('planning.automatic') })}</PlanningConfirm>}
  </div>
}
