import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type ScheduledTaskResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'

const activityIcons: Record<string, string> = {
  SOW: '🌰', POT_UP: '🪴', PLANT: '🌳', HARVEST: '🌾', RECOVER: '💚', DISCARD: '🗑️',
}

export function TaskList() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t } = useTranslation()
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['tasks'],
    queryFn: api.tasks.list,
  })

  const [deleteTask, setDeleteTask] = useState<ScheduledTaskResponse | null>(null)

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.tasks.delete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tasks'] }); setDeleteTask(null) },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  const today = new Date().toISOString().split('T')[0]

  return (
    <div>
      <PageHeader title={t('tasks.title')} action={{ label: t('tasks.newTask'), onClick: () => navigate('/task/new') }} />
      <div className="px-4 py-4 space-y-3">
        {data && data.length === 0 && (
          <p className="text-text-secondary text-sm text-center py-4">{t('tasks.noTasks')}</p>
        )}

        {data?.map(task => {
          const isOverdue = task.deadline < today
          const isDueToday = task.deadline === today
          const isCompleted = task.status === 'COMPLETED'

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
                  <p className="text-sm text-text-secondary">{task.speciesName}</p>
                  <p className="text-xs text-text-secondary">{t('tasks.remaining', { remaining: task.remainingCount, total: task.targetCount })}</p>
                </div>
              </div>
              <div className="flex gap-2 mt-3">
                {!isCompleted && (
                  <button
                    onClick={() => navigate(`/sow?taskId=${task.id}&speciesId=${task.speciesId}`)}
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
