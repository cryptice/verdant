import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useState, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type ScheduledTaskResponse, type SpeciesResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { Pagination } from '../components/Pagination'

const PAGE_SIZE = 50

const activityIcons: Record<string, string> = {
  SOW: '🌰', POT_UP: '🪴', PLANT: '🌳', HARVEST: '🌾', RECOVER: '💚', DISCARD: '🗑️',
}

function speciesDisplayName(s: { commonName: string; variantName?: string; commonNameSv?: string; variantNameSv?: string }, lang: string) {
  const main = lang === 'sv' ? (s.commonNameSv ?? s.commonName) : s.commonName
  const variant = lang === 'sv' ? (s.variantNameSv ?? s.variantName) : s.variantName
  return variant ? `${main} — ${variant}` : main
}

function groupByMainName(
  items: { commonName: string; variantName?: string; commonNameSv?: string; variantNameSv?: string }[],
  lang: string,
) {
  const grouped = new Map<string, string[]>()
  for (const s of items) {
    const main = lang === 'sv' ? (s.commonNameSv ?? s.commonName) : s.commonName
    const variant = lang === 'sv' ? (s.variantNameSv ?? s.variantName) : s.variantName
    const variants = grouped.get(main) ?? []
    if (variant) variants.push(variant)
    grouped.set(main, variants)
  }
  return grouped
}

function GroupedSpeciesLines({ grouped }: { grouped: Map<string, string[]> }) {
  return (
    <div className="text-xs text-text-secondary mt-0.5 space-y-0.5">
      {Array.from(grouped.entries()).map(([main, variants]) => (
        <p key={main}>{main}{variants.length > 0 ? `: ${variants.join(', ')}` : ''}</p>
      ))}
    </div>
  )
}

function ClickableGroupedSpecies({ species, lang, onClickSpecies }: {
  species: SpeciesResponse[]
  lang: string
  onClickSpecies: (speciesId: number) => void
}) {
  const grouped = new Map<string, { id: number; variant: string | undefined }[]>()
  for (const s of species) {
    const main = lang === 'sv' ? (s.commonNameSv ?? s.commonName) : s.commonName
    const variant = lang === 'sv' ? (s.variantNameSv ?? s.variantName) : s.variantName
    const list = grouped.get(main) ?? []
    list.push({ id: s.id, variant: variant ?? undefined })
    grouped.set(main, list)
  }
  return (
    <div className="text-xs text-text-secondary mt-0.5 space-y-0.5">
      {Array.from(grouped.entries()).map(([main, entries]) => (
        <p key={main}>
          {entries.some(e => !e.variant) ? (
            <button onClick={() => entries.forEach(e => onClickSpecies(e.id))} className="cursor-pointer underline text-accent">{main}</button>
          ) : (
            <>{main}: </>
          )}
          {entries.filter(e => e.variant).map((e, i) => (
            <span key={e.id}>
              {i > 0 && ', '}
              <button onClick={() => onClickSpecies(e.id)} className="cursor-pointer underline text-accent">{e.variant}</button>
            </span>
          ))}
        </p>
      ))}
    </div>
  )
}

export function TaskList() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t, i18n } = useTranslation()
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['tasks'],
    queryFn: api.tasks.list,
  })

  // Fetch species to resolve current group memberships
  const { data: allSpecies } = useQuery({
    queryKey: ['species'],
    queryFn: api.species.list,
  })

  // Build map: groupId -> set of speciesIds currently in that group
  const currentGroupMembers = useMemo(() => {
    const map = new Map<number, Set<number>>()
    if (allSpecies) {
      for (const s of allSpecies) {
        for (const g of s.groups) {
          const set = map.get(g.id) ?? new Set()
          set.add(s.id)
          map.set(g.id, set)
        }
      }
    }
    return map
  }, [allSpecies])

  // Build map: groupId -> full species objects for display of new species
  const currentGroupSpecies = useMemo(() => {
    const map = new Map<number, SpeciesResponse[]>()
    if (allSpecies) {
      for (const s of allSpecies) {
        for (const g of s.groups) {
          const list = map.get(g.id) ?? []
          list.push(s)
          map.set(g.id, list)
        }
      }
    }
    return map
  }, [allSpecies])

  const [deleteTask, setDeleteTask] = useState<ScheduledTaskResponse | null>(null)
  const [page, setPage] = useState(0)

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.tasks.delete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tasks'] }); setDeleteTask(null) },
  })

  const syncMut = useMutation({
    mutationFn: (id: number) => api.tasks.syncGroup(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tasks'] }) },
  })

  const addSpeciesMut = useMutation({
    mutationFn: ({ taskId, speciesId }: { taskId: number; speciesId: number }) =>
      api.tasks.addSpecies(taskId, speciesId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tasks'] }) },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  const today = new Date().toISOString().split('T')[0]
  const lang = i18n.language

  return (
    <div>
      <PageHeader title={t('tasks.title')} action={{ label: t('tasks.newTask'), onClick: () => navigate('/task/new') }} />
      <div className="px-4 py-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {data && data.length === 0 && (
          <p className="text-text-secondary text-sm text-center py-4">{t('tasks.noTasks')}</p>
        )}

        {data?.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE).map(task => {
          const isOverdue = task.deadline < today
          const isDueToday = task.deadline === today
          const isCompleted = task.status === 'COMPLETED'

          // Compute group sync diff for group tasks
          let addedToGroup: SpeciesResponse[] = []
          let removedFromGroup: typeof task.acceptableSpecies = []
          const hasGroupDiff = !!(task.originGroupId && allSpecies)
          if (task.originGroupId && allSpecies) {
            const currentIds = currentGroupMembers.get(task.originGroupId) ?? new Set()
            const taskIds = new Set(task.acceptableSpecies.map(s => s.speciesId))
            // Species in group but not in task
            addedToGroup = (currentGroupSpecies.get(task.originGroupId) ?? [])
              .filter(s => !taskIds.has(s.id))
            // Species in task but not in group
            removedFromGroup = task.acceptableSpecies
              .filter(s => !currentIds.has(s.speciesId))
          }

          return (
            <div key={task.id} className={`card ${isCompleted ? 'opacity-60' : ''}`}>
              <div className="flex items-start gap-3">
                <span className="text-xl">{activityIcons[task.activityType] ?? '📋'}</span>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <p className="font-medium text-sm">{t(`activityType.${task.activityType}`, { defaultValue: task.activityType.replace(/_/g, ' ') })}</p>
                    <span className={`text-xs font-medium px-2 py-0.5 rounded-lg ${
                      isOverdue ? 'bg-error/15 text-error' :
                      isDueToday ? 'bg-orange-100 text-orange-700' :
                      'bg-surface text-text-secondary'
                    }`}>
                      {task.deadline}
                    </span>
                  </div>
                  <p className="text-sm text-text-secondary">
                    {task.originGroupName ? (
                      <span className="inline-flex items-center gap-1">
                        {task.originGroupName}
                        <span className="text-xs bg-accent/15 text-accent px-1 py-0.5 rounded">{t('common.group')}</span>
                      </span>
                    ) : (
                      task.speciesName
                    )}
                  </p>
                  {task.acceptableSpecies.length > 1 && (
                    <GroupedSpeciesLines grouped={groupByMainName(task.acceptableSpecies, lang)} />
                  )}

                  {/* Group sync diff */}
                  {hasGroupDiff && addedToGroup.length > 0 && (
                    <div className="mt-1.5 border border-accent/30 rounded-lg px-2 py-1.5 bg-accent/5">
                      <p className="text-xs text-accent font-medium mb-0.5">{t('tasks.newInGroup')}</p>
                      {!isCompleted ? (
                        <>
                          <ClickableGroupedSpecies
                            species={addedToGroup}
                            lang={lang}
                            onClickSpecies={speciesId => addSpeciesMut.mutate({ taskId: task.id, speciesId })}
                          />
                          {addedToGroup.length > 1 && (
                            <button
                              onClick={() => syncMut.mutate(task.id)}
                              disabled={syncMut.isPending}
                              className="text-xs text-accent font-medium mt-1 cursor-pointer underline"
                            >
                              {syncMut.isPending ? t('common.saving') : t('tasks.addAllFromGroup')}
                            </button>
                          )}
                        </>
                      ) : (
                        <GroupedSpeciesLines grouped={groupByMainName(
                          addedToGroup.map(s => ({
                            commonName: s.commonName,
                            variantName: s.variantName ?? undefined,
                            commonNameSv: s.commonNameSv ?? undefined,
                            variantNameSv: s.variantNameSv ?? undefined,
                          })),
                          lang,
                        )} />
                      )}
                    </div>
                  )}
                  {hasGroupDiff && removedFromGroup.length > 0 && (
                    <div className="mt-1.5 border border-orange-300/50 rounded-lg px-2 py-1.5 bg-orange-50">
                      <p className="text-xs text-orange-700 font-medium mb-0.5">{t('tasks.removedFromGroup')}</p>
                      <div className="text-xs text-orange-600 space-y-0.5">
                        {removedFromGroup.map(s => (
                          <p key={s.speciesId}>{speciesDisplayName(s, lang)}</p>
                        ))}
                      </div>
                    </div>
                  )}

                  <p className="text-xs text-text-secondary">{t('tasks.remaining', { remaining: task.remainingCount, total: task.targetCount })}</p>
                </div>
              </div>
              <div className="flex gap-2 mt-3">
                {!isCompleted && (
                  <button
                    onClick={() => {
                      const params = new URLSearchParams({ taskId: String(task.id) })
                      if (task.speciesId) params.set('speciesId', String(task.speciesId))
                      navigate(`/sow?${params}`)
                    }}
                    className="btn-primary text-xs py-1.5 px-3 flex-1"
                  >
                    {t('tasks.perform')}
                  </button>
                )}
                <button
                  onClick={() => navigate(`/task/${task.id}/edit`)}
                  className="text-xs py-1.5 px-3 border border-divider rounded-xl text-text-secondary"
                >
                  {t('common.edit')}
                </button>
                <button
                  onClick={() => setDeleteTask(task)}
                  className="text-xs py-1.5 px-3 text-error"
                >
                  {t('common.delete')}
                </button>
              </div>
            </div>
          )
        })}
        {data && <Pagination page={page} pageSize={PAGE_SIZE} total={data.length} onPageChange={setPage} />}
      </div>

      <Dialog open={deleteTask !== null} onClose={() => setDeleteTask(null)} title={t('tasks.deleteTaskTitle')} actions={
        <>
          <button onClick={() => setDeleteTask(null)} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => deleteTask && deleteMut.mutate(deleteTask.id)} className="px-4 py-2 text-sm text-error font-semibold">{t('common.delete')}</button>
        </>
      }>
        <p className="text-text-secondary">{t('tasks.deleteTaskConfirm')}</p>
      </Dialog>

    </div>
  )
}
