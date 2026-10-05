import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { planning, buttonClass, inputClass, linkClass, type SharedGroup, type PlanningSpecies } from '../api/planning'
import { PlanningConfirm, PlanningError, PlanningField } from '../components/PlanningFields'

export function PlanningGroups() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [query, setQuery] = useState('')
  const [name, setName] = useState('')
  const groups = useQuery({ queryKey: ['admin', 'planning', 'groups'], queryFn: planning.groups })
  const create = useMutation({ mutationFn: () => planning.createGroup(name.trim()), onSuccess: group => {
    qc.invalidateQueries({ queryKey: ['admin', 'planning'] }); navigate(`/groups/${group.id}`)
  } })
  return <div className="space-y-5 max-w-5xl">
    <h1 className="text-xl font-semibold">{t('planning.groups')}</h1>
    <p className="text-sm text-[#787774]">{t('planning.groupsHelp')}</p>
    <form className="flex items-end gap-2" onSubmit={e => { e.preventDefault(); create.mutate() }}>
      <div className="flex-1"><PlanningField label={t('planning.newGroup')}><input className={inputClass} required maxLength={255} value={name} onChange={e => setName(e.target.value)} /></PlanningField></div>
      <button className={buttonClass} disabled={create.isPending || !name.trim()}>{t('common.add')}</button>
    </form>
    <PlanningError error={groups.error || create.error} />
    <PlanningField label={t('planning.searchGroups')}><input type="search" className={inputClass} value={query} onChange={e => setQuery(e.target.value)} /></PlanningField>
    {groups.isPending ? <p>{t('common.loading')}</p> : <ul className="divide-y border border-[#E9E9E7] rounded">
      {groups.data?.filter(g => g.name.toLocaleLowerCase().includes(query.toLocaleLowerCase())).map(g => <li key={g.id}>
        <Link to={`/groups/${g.id}`} className="flex justify-between gap-3 p-3 hover:bg-[#FBFBFA]"><span className={linkClass}>{g.name}</span><span className="text-sm text-[#787774]">{t('planning.memberCount', { count: g.speciesIds.length })}</span></Link>
      </li>)}
    </ul>}
  </div>
}

export function PlanningGroupDetail() {
  const { id } = useParams()
  const { t } = useTranslation()
  const groups = useQuery({ queryKey: ['admin', 'planning', 'groups'], queryFn: planning.groups })
  const species = useQuery({ queryKey: ['admin', 'planning', 'species'], queryFn: planning.species })
  const group = groups.data?.find(g => g.id === Number(id))
  if (groups.error || species.error) return <PlanningError error={groups.error || species.error} />
  if (groups.isPending || species.isPending) return <p>{t('common.loading')}</p>
  if (!group) return <p>{t('planning.notFound')}</p>
  return <GroupEditor key={group.id} group={group} species={species.data ?? []} />
}

function GroupEditor({ group, species }: { group: SharedGroup; species: PlanningSpecies[] }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [name, setName] = useState(group.name)
  const [query, setQuery] = useState('')
  const [selected, setSelected] = useState<number[]>([])
  const [confirm, setConfirm] = useState<number | 'delete' | null>(null)
  const change = useMutation({ mutationFn: (action: () => Promise<unknown>) => action(), onSuccess: () => {
    qc.invalidateQueries({ queryKey: ['admin', 'planning'] }); qc.invalidateQueries({ queryKey: ['admin', 'species'] }); setSelected([]); setConfirm(null)
  } })
  const members = species.filter(s => group.speciesIds.includes(s.id))
  const available = species.filter(s => !group.speciesIds.includes(s.id) && `${s.name} ${s.scientificName ?? ''}`.toLocaleLowerCase().includes(query.toLocaleLowerCase()))
  return <div className="max-w-5xl space-y-5">
    <Link className={linkClass} to="/groups">← {t('planning.groups')}</Link>
    <h1 className="text-xl font-semibold">{group.name}</h1>
    <PlanningError error={change.error} />
    <form className="flex items-end gap-2" onSubmit={e => { e.preventDefault(); change.mutate(() => planning.renameGroup(group.id, name.trim())) }}>
      <div className="flex-1"><PlanningField label={t('planning.name')}><input className={inputClass} required maxLength={255} value={name} onChange={e => setName(e.target.value)} /></PlanningField></div>
      <button className={buttonClass} disabled={change.isPending || !name.trim() || name.trim() === group.name}>{t('common.save')}</button>
    </form>
    <div className="flex flex-wrap justify-between gap-2"><h2 className="font-semibold">{t('planning.memberCount', { count: members.length })}</h2><Link className={linkClass} to={`/planning/species?group=${group.id}`}>{t('planning.manageSchedules')}</Link></div>
    {members.length > 200 && <p className="text-sm text-amber-800">{t('planning.groupLimit')}</p>}
    <ul className="divide-y max-h-96 overflow-y-auto border rounded border-[#E9E9E7]">
      {members.map(s => <li key={s.id} className="flex justify-between items-center gap-2 p-3"><Link className={linkClass} to={`/species/${s.id}`}>{s.name}</Link><button type="button" className={buttonClass} disabled={change.isPending} onClick={() => setConfirm(s.id)}>{t('common.remove')}</button></li>)}
    </ul>
    {typeof confirm === 'number' && <PlanningConfirm pending={change.isPending} onCancel={() => setConfirm(null)} onConfirm={() => change.mutate(() => planning.removeMember(group.id, confirm))}>{t('planning.removeMemberConfirm', { name: species.find(s => s.id === confirm)?.name })}</PlanningConfirm>}
    <section className="space-y-3">
      <h2 className="font-semibold">{t('planning.addMembers')}</h2>
      <PlanningField label={t('planning.searchSpecies')}><input className={inputClass} type="search" value={query} onChange={e => setQuery(e.target.value)} /></PlanningField>
      <ul className="max-h-64 overflow-y-auto border rounded border-[#E9E9E7] divide-y">{available.map(s => <li key={s.id}><label className="flex gap-3 items-center p-2 text-sm"><input type="checkbox" disabled={change.isPending} checked={selected.includes(s.id)} onChange={e => setSelected(e.target.checked ? [...selected, s.id] : selected.filter(id => id !== s.id))} />{s.name}</label></li>)}</ul>
      <button className={buttonClass} disabled={change.isPending || !selected.length} onClick={() => change.mutate(() => planning.addMembers(group.id, selected))}>{t('planning.addSelected', { count: selected.length })}</button>
    </section>
    {confirm === 'delete' ? <PlanningConfirm pending={change.isPending} onCancel={() => setConfirm(null)} onConfirm={() => change.mutate(async () => { await planning.deleteGroup(group.id); navigate('/groups') })}>{t('planning.deleteGroupConfirm', { name: group.name })}</PlanningConfirm> : <button className={`${buttonClass} text-red-700`} disabled={change.isPending} onClick={() => setConfirm('delete')}>{t('planning.deleteGroup')}</button>}
  </div>
}
