import { useEffect, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api, type ScheduledTaskResponse } from '../api/client'
import { Masthead } from '../components/faltet'
import { Dialog } from '../components/Dialog'
import { maintenanceActivityLabelSv, todayIsoLocal } from '../lib/maintenance'

type ActivityFilter = 'harvest' | 'sowing' | 'watering' | 'planting' | 'maintenance'
const FILTERS: ActivityFilter[] = ['harvest', 'sowing', 'watering', 'planting', 'maintenance']
const TONE: Record<ActivityFilter, string> = {
  harvest:     'var(--color-accent)',
  sowing:      'var(--color-mustard)',
  watering:    'var(--color-sky)',
  planting:    'var(--color-sage)',
  maintenance: 'var(--color-berry)',
}

// Map backend activityType enum values to filter categories. Maintenance
// activities not listed here (WEED, FERTILIZE, MOW, RAKE, PRUNE, EDGE,
// SWEEP, TOP_UP, CLEAN, INSPECT) fall through to 'maintenance' below.
const ACTIVITY_TO_FILTER: Record<string, ActivityFilter> = {
  HARVEST: 'harvest',
  SOW:     'sowing',
  SOAK:    'watering',
  WATER:   'watering',
  PLANT:   'planting',
  POT_UP:  'planting',
  RECOVER: 'maintenance',
  DISCARD: 'maintenance',
}

function activityFilter(activityType: string): ActivityFilter {
  return ACTIVITY_TO_FILTER[activityType] ?? 'maintenance'
}

function loadFilters(): ActivityFilter[] {
  try {
    const raw = localStorage.getItem('verdant-task-filters')
    const parsed = raw ? (JSON.parse(raw) as ActivityFilter[]) : FILTERS
    const valid = parsed.filter((f) => FILTERS.includes(f))
    return valid.length ? valid : FILTERS
  } catch {
    return FILTERS
  }
}

/**
 * A task carried out on a bed or a garden area rather than on a species —
 * every maintenance task, rule-backed or hand-made. Completing one is what
 * writes the bed/area event that moves a maintenance rule's derived clock.
 */
export function isPlaceScoped(task: ScheduledTaskResponse): boolean {
  return task.bedId != null || task.gardenAreaId != null
}

export function taskTitle(task: ScheduledTaskResponse): string {
  if (task.speciesName) return task.speciesName
  const place = task.gardenAreaName ?? task.bedName
  const activity = maintenanceActivityLabelSv(task.activityType)
  return place ? `${activity} · ${place}` : activity
}

export function TaskList() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t } = useTranslation()

  const [filters, setFilters] = useState<ActivityFilter[]>(loadFilters)
  const [drawerTask, setDrawerTask] = useState<ScheduledTaskResponse | null>(null)
  const [deleteTask, setDeleteTask] = useState<ScheduledTaskResponse | null>(null)

  useEffect(() => {
    localStorage.setItem('verdant-task-filters', JSON.stringify(filters))
  }, [filters])

  const { data: tasks = [] } = useQuery({
    queryKey: ['tasks'],
    queryFn: api.tasks.list,
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.tasks.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tasks'] })
      setDeleteTask(null)
    },
  })

  // The only thing that moves a task off PENDING. For a rule-backed task the
  // server also records the work as a bed/area event, which is what lets the
  // scheduler create the next one — without this the rule fires exactly once.
  const completeMut = useMutation({
    mutationFn: (task: ScheduledTaskResponse) =>
      // Floor at 1 to match Android: a PENDING task always has remainingCount
      // > 0 today, but processedCount is @Min(1) and would 400 if that slipped.
      api.tasks.complete(task.id, null, Math.max(task.remainingCount, 1)),
    onSuccess: (_result, task) => {
      qc.invalidateQueries({ queryKey: ['tasks'] })
      if (task.gardenAreaId != null) {
        qc.invalidateQueries({ queryKey: ['maintenance-rules', 'AREA', task.gardenAreaId] })
        qc.invalidateQueries({ queryKey: ['area-events', task.gardenAreaId] })
      } else if (task.bedId != null) {
        qc.invalidateQueries({ queryKey: ['maintenance-rules', 'BED', task.bedId] })
        qc.invalidateQueries({ queryKey: ['bed-events', task.bedId] })
      }
      setDrawerTask(null)
    },
  })

  // Reset so a failed completion's error never greets the next task opened.
  const openDrawer = (task: ScheduledTaskResponse) => {
    completeMut.reset()
    setDrawerTask(task)
  }

  const today = todayIsoLocal()
  const isToday  = (d: string) => d === today
  const isFuture = (d: string) => d > today
  const isPast   = (d: string) => d < today

  // Completed tasks stay in the API's response (it sorts them last rather than
  // dropping them), so the page has to exclude them itself — otherwise the
  // overdue bucket below fills with every task ever finished.
  const filtered  = tasks.filter(
    (task) => task.status === 'PENDING' && filters.includes(activityFilter(task.activityType)),
  )
  // Without this bucket a task vanishes from the page the day after its
  // deadline — including a maintenance task whose rule stays blocked until
  // it is completed or deleted.
  const overdue   = filtered.filter((task) => isPast(task.deadline))
  const todays    = filtered.filter((task) => isToday(task.deadline))
  const upcoming  = filtered.filter((task) => isFuture(task.deadline))

  const toggleFilter = (a: ActivityFilter) => {
    setFilters((cur) => {
      const has = cur.includes(a)
      if (has && cur.length === 1) return cur // at-least-one rule
      return has ? cur.filter((x) => x !== a) : [...cur, a]
    })
  }

  return (
    <div>
      <Masthead
        left={t('nav.tasks')}
        center={t('tasks.masthead.center')}
        right={
          <button
            onClick={() => navigate('/task/new')}
            className="hide-on-mobile"
            style={{
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              padding: '6px 12px',
              border: '1px solid var(--color-ink)',
              borderRadius: 999,
              background: 'transparent',
              cursor: 'pointer',
            }}
          >
            {t('tasks.newTask')}
          </button>
        }
      />

      {/* Mobile FAB — replaces the top button at narrow viewports */}
      <button
        onClick={() => navigate('/task/new')}
        className="mobile-fab"
        aria-label={t('tasks.newTask')}
      >
        +
      </button>

      <div className="page-body">
        {/* Filter pills */}
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 28 }}>
          {FILTERS.map((a) => {
            const active = filters.includes(a)
            const color  = TONE[a]
            return (
              <button
                key={a}
                onClick={() => toggleFilter(a)}
                style={{
                  fontFamily: 'var(--font-mono)',
                  fontSize: 10,
                  letterSpacing: 1.4,
                  textTransform: 'uppercase',
                  padding: '6px 12px',
                  borderRadius: 999,
                  border: `1px solid ${color}`,
                  background: active ? color : 'transparent',
                  color: active ? 'var(--color-cream)' : color,
                  cursor: 'pointer',
                }}
              >
                {t(`tasks.filters.${a}`)}
              </button>
            )
          })}
        </div>

        {/* Försenat */}
        {overdue.length > 0 && (
          <>
            <SectionHeader title={t('tasks.overdue')} count={overdue.length} />
            {overdue.map((task) => (
              <TaskRow key={task.id} task={task} onOpen={() => openDrawer(task)} />
            ))}
            <div style={{ height: 40 }} />
          </>
        )}

        {/* Idag */}
        <SectionHeader title={t('tasks.today')} count={todays.length} />
        {todays.map((task) => (
          <TaskRow key={task.id} task={task} onOpen={() => openDrawer(task)} />
        ))}

        <div style={{ height: 40 }} />

        {/* Kommande */}
        <SectionHeader title={t('tasks.upcoming')} count={upcoming.length} />
        {upcoming.map((task) => (
          <TaskRow key={task.id} task={task} onOpen={() => openDrawer(task)} />
        ))}
      </div>

      {/* Task detail drawer */}
      <Dialog
        open={drawerTask !== null}
        onClose={() => setDrawerTask(null)}
        title={drawerTask ? taskTitle(drawerTask) : ''}
        actions={
          drawerTask ? (
            <>
              <button
                onClick={() => { navigate(`/task/${drawerTask.id}/edit`); setDrawerTask(null) }}
                className="px-4 py-2 text-sm text-text-secondary border border-divider rounded-xl"
              >
                {t('common.edit')}
              </button>
              <button
                onClick={() => {
                  if (drawerTask.activityType === 'SOW') {
                    const params = new URLSearchParams({ taskId: String(drawerTask.id) })
                    if (drawerTask.speciesId) params.set('speciesId', String(drawerTask.speciesId))
                    navigate(`/sow?${params}`)
                  } else if (drawerTask.bedId != null) {
                    navigate(`/bed/${drawerTask.bedId}`)
                  } else if (drawerTask.gardenAreaId != null) {
                    navigate(`/area/${drawerTask.gardenAreaId}`)
                  }
                  setDrawerTask(null)
                }}
                className={
                  // Going to the place is where photos and history live, but it
                  // does not close the task — so for a place-scoped task it is
                  // the secondary action and "mark done" is the primary one.
                  isPlaceScoped(drawerTask)
                    ? 'px-4 py-2 text-sm text-text-secondary border border-divider rounded-xl'
                    : 'btn-primary text-sm py-2 px-4'
                }
              >
                {t('tasks.perform')}
              </button>
              {isPlaceScoped(drawerTask) && (
                <button
                  onClick={() => completeMut.mutate(drawerTask)}
                  disabled={completeMut.isPending}
                  className="btn-primary text-sm py-2 px-4"
                >
                  {t('maintenance.markDone')}
                </button>
              )}
              <button
                onClick={() => { setDeleteTask(drawerTask); setDrawerTask(null) }}
                className="px-4 py-2 text-sm text-error"
              >
                {t('common.delete')}
              </button>
            </>
          ) : undefined
        }
      >
        {drawerTask && (
          <div style={{ fontFamily: 'var(--font-body)', fontSize: 14, lineHeight: 1.6 }}>
            <p style={{ fontFamily: 'var(--font-display)', fontSize: 16, marginBottom: 12 }}>
              {t('tasks.drawer.placeholder')}
            </p>
            <p><strong>{t('tasks.remaining', { remaining: drawerTask.remainingCount, total: drawerTask.targetCount })}</strong></p>
            <p style={{ color: 'var(--color-forest)', marginTop: 4 }}>{drawerTask.deadline}</p>
            {isPlaceScoped(drawerTask) && (
              <p style={{ color: 'var(--color-forest)', marginTop: 12 }}>
                {t('tasks.drawer.placeHint')}
              </p>
            )}
            {/* Only a rule-backed task is logged on completion: recordMaintenance
                is gated on maintenanceRuleId, so promising it for a hand-made
                bed task would be a lie. */}
            {drawerTask.maintenanceRuleId != null && (
              <p style={{ color: 'var(--color-forest)', marginTop: 8 }}>
                {t('tasks.drawer.maintenanceHint')}
              </p>
            )}
            {/* Inline rather than a snackbar: the drawer stays open on failure,
                and a native modal's top layer would hide a fixed-position toast. */}
            {completeMut.isError && (
              <p className="text-error" style={{ marginTop: 12 }}>
                {t('maintenance.markDoneError')}
              </p>
            )}
          </div>
        )}
      </Dialog>

      {/* Delete confirmation */}
      <Dialog
        open={deleteTask !== null}
        onClose={() => setDeleteTask(null)}
        title={t('tasks.deleteTaskTitle')}
        actions={
          <>
            <button onClick={() => setDeleteTask(null)} className="px-4 py-2 text-sm text-text-secondary">
              {t('common.cancel')}
            </button>
            <button
              onClick={() => deleteTask && deleteMut.mutate(deleteTask.id)}
              className="px-4 py-2 text-sm text-error font-semibold"
            >
              {t('common.delete')}
            </button>
          </>
        }
      >
        <p className="text-text-secondary">
          {deleteTask?.maintenanceRuleId != null
            ? t('tasks.deleteMaintenanceTaskConfirm')
            : t('tasks.deleteTaskConfirm')}
        </p>
      </Dialog>
    </div>
  )
}

function SectionHeader({ title, count }: { title: string; count: number }) {
  return (
    <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 12, marginBottom: 10 }}>
      <h2
        style={{
          fontFamily: 'var(--font-display)',
          fontStyle: 'italic',
          fontSize: 30,
          fontWeight: 300,
          margin: 0,
          fontVariationSettings: '"SOFT" 100, "opsz" 144',
        }}
      >
        {title}<span style={{ color: 'var(--color-accent)' }}>.</span>
      </h2>
      <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase' }}>
        {count}
      </span>
    </div>
  )
}

function TaskRow({ task, onOpen }: { task: ScheduledTaskResponse; onOpen: () => void }) {
  const filter = activityFilter(task.activityType)
  const color  = TONE[filter]
  return (
    <button
      onClick={onOpen}
      style={{
        display: 'grid',
        gridTemplateColumns: '40px 1.5fr 160px 80px',
        gap: 18,
        padding: '16px 0',
        borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 25%, transparent)',
        width: '100%',
        background: 'transparent',
        border: 'none',
        borderBottomWidth: 1,
        borderBottomStyle: 'solid',
        borderBottomColor: 'color-mix(in srgb, var(--color-ink) 25%, transparent)',
        textAlign: 'left',
        cursor: 'pointer',
        alignItems: 'center',
      }}
    >
      <span
        style={{
          fontFamily: 'var(--font-display)',
          fontStyle: 'italic',
          fontSize: 26,
          color,
          fontVariationSettings: '"SOFT" 100, "opsz" 144',
        }}
      >
        №
      </span>
      <div>
        <div style={{ fontFamily: 'var(--font-display)', fontSize: 18 }}>
          {task.bedId != null
            ? maintenanceActivityLabelSv(task.activityType)
            : (task.speciesName ?? maintenanceActivityLabelSv(task.activityType))}
        </div>
        {task.originGroupName && (
          <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
            {task.originGroupName}
          </div>
        )}
        <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
          {task.bedId != null
            ? [task.gardenName, task.bedName].filter(Boolean).join(' · ')
            : (task.gardenAreaName ?? task.activityType.replace(/_/g, ' '))}
        </div>
      </div>
      <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase' }}>
        {task.deadline}
      </div>
      <div style={{ textAlign: 'right', fontFamily: 'var(--font-mono)', fontSize: 16 }}>→</div>
    </button>
  )
}
