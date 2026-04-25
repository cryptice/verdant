import { useEffect, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api, type ScheduledTaskResponse } from '../api/client'
import { Masthead } from '../components/faltet'
import { Dialog } from '../components/Dialog'

type ActivityFilter = 'harvest' | 'sowing' | 'watering' | 'planting' | 'maintenance'
const FILTERS: ActivityFilter[] = ['harvest', 'sowing', 'watering', 'planting', 'maintenance']
const TONE: Record<ActivityFilter, string> = {
  harvest:     'var(--color-accent)',
  sowing:      'var(--color-mustard)',
  watering:    'var(--color-sky)',
  planting:    'var(--color-sage)',
  maintenance: 'var(--color-berry)',
}

// Map backend activityType enum values to filter categories
const ACTIVITY_TO_FILTER: Record<string, ActivityFilter> = {
  HARVEST: 'harvest',
  SOW:     'sowing',
  SOAK:    'watering',
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

function taskTitle(task: ScheduledTaskResponse): string {
  return task.speciesName ?? task.activityType
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

  const today = new Date().toISOString().slice(0, 10)
  const isToday  = (d: string) => d === today
  const isFuture = (d: string) => d > today

  const filtered  = tasks.filter((task) => filters.includes(activityFilter(task.activityType)))
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

      {/* Mobile FAB — replaces the top button at narrow viewports.
          Styles inline + a media-query class so the rule cannot be lost
          to any CSS-tooling reorder. */}
      <button
        onClick={() => navigate('/task/new')}
        className="mobile-fab"
        aria-label={t('tasks.newTask')}
        style={{
          position: 'fixed',
          right: 'calc(20px + env(safe-area-inset-right))',
          bottom: 'calc(20px + env(safe-area-inset-bottom))',
          zIndex: 35,
          width: 56,
          height: 56,
          borderRadius: 999,
          border: 'none',
          background: 'var(--color-accent)',
          color: 'var(--color-cream)',
          fontFamily: 'var(--font-mono)',
          fontSize: 28,
          lineHeight: 1,
          cursor: 'pointer',
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 4px 14px rgba(30, 36, 29, 0.25)',
        }}
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

        {/* Idag */}
        <SectionHeader title={t('tasks.today')} count={todays.length} />
        {todays.map((task) => (
          <TaskRow key={task.id} task={task} onOpen={() => setDrawerTask(task)} />
        ))}

        <div style={{ height: 40 }} />

        {/* Kommande */}
        <SectionHeader title={t('tasks.upcoming')} count={upcoming.length} />
        {upcoming.map((task) => (
          <TaskRow key={task.id} task={task} onOpen={() => setDrawerTask(task)} />
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
                  }
                  setDrawerTask(null)
                }}
                className="btn-primary text-sm py-2 px-4"
              >
                {t('tasks.perform')}
              </button>
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
        <p className="text-text-secondary">{t('tasks.deleteTaskConfirm')}</p>
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
          {task.speciesName ?? task.activityType}
        </div>
        {task.originGroupName && (
          <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
            {task.originGroupName}
          </div>
        )}
        <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
          {task.activityType.replace(/_/g, ' ')}
        </div>
      </div>
      <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase' }}>
        {task.deadline}
      </div>
      <div style={{ textAlign: 'right', fontFamily: 'var(--font-mono)', fontSize: 16 }}>→</div>
    </button>
  )
}
