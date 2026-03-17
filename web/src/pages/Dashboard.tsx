import { useQuery } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Fab } from '../components/Fab'
import { StatusBadge } from '../components/StatusBadge'
import { useAuth } from '../auth/AuthContext'

function formatWeight(grams: number) {
  return grams >= 1000 ? `${(grams / 1000).toFixed(1)} kg` : `${grams} g`
}

export function Dashboard() {
  const { user } = useAuth()
  const navigate = useNavigate()

  const { data: dashboard, error, isLoading, refetch } = useQuery({
    queryKey: ['dashboard'],
    queryFn: api.dashboard,
  })

  const { data: tray } = useQuery({
    queryKey: ['tray-summary'],
    queryFn: api.plants.traySummary,
  })

  const { data: harvests } = useQuery({
    queryKey: ['harvest-stats'],
    queryFn: api.stats.harvests,
  })

  if (isLoading) {
    return (
      <div className="flex items-center justify-center p-16">
        <div className="animate-spin h-8 w-8 border-4 border-green-primary border-t-transparent rounded-full" />
      </div>
    )
  }

  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  return (
    <div className="px-4 py-4 space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Hello, {user?.displayName?.split(' ')[0]}</h1>
        {dashboard && (
          <p className="text-text-secondary text-sm mt-1">
            {dashboard.stats.totalGardens} {dashboard.stats.totalGardens === 1 ? 'garden' : 'gardens'} · {dashboard.stats.totalPlants} {dashboard.stats.totalPlants === 1 ? 'plant' : 'plants'}
          </p>
        )}
      </div>

      {dashboard && dashboard.gardens.length === 0 && (
        <div className="card text-center py-8">
          <p className="text-4xl mb-2">🌱</p>
          <p className="font-medium">No gardens yet</p>
          <p className="text-text-secondary text-sm">Tap + to create your first garden</p>
        </div>
      )}

      {dashboard?.gardens.map(g => (
        <Link key={g.id} to={`/garden/${g.id}`} className="card flex items-center gap-4 no-underline text-inherit">
          <span className="text-3xl">{g.emoji ?? '🌱'}</span>
          <div className="flex-1 min-w-0">
            <p className="font-semibold text-lg">{g.name}</p>
            <p className="text-sm text-text-secondary">
              {g.plantCount} {g.plantCount === 1 ? 'plant' : 'plants'} · {g.bedCount} {g.bedCount === 1 ? 'bed' : 'beds'}
            </p>
          </div>
        </Link>
      ))}

      {tray && tray.length > 0 && (
        <section>
          <h2 className="font-bold text-lg mb-2">Plants in trays</h2>
          <div className="card space-y-2">
            {tray.map((t, i) => (
              <div key={i} className="flex items-center justify-between text-sm">
                <span className="flex-1">{t.speciesName}</span>
                <span className="text-green-primary font-medium w-8 text-right">{t.count}</span>
                <span className="w-20 text-right"><StatusBadge status={t.status} /></span>
              </div>
            ))}
          </div>
        </section>
      )}

      {harvests && harvests.length > 0 && (
        <section>
          <h2 className="font-bold text-lg mb-2">Harvest stats</h2>
          <div className="space-y-3">
            {harvests.map(h => (
              <div key={h.species} className="card">
                <p className="font-bold mb-2">{h.species}</p>
                <div className="flex justify-between text-center text-sm">
                  <div>
                    <p className="font-bold text-green-primary">{formatWeight(h.totalWeightGrams)}</p>
                    <p className="text-text-secondary text-xs">Weight</p>
                  </div>
                  <div>
                    <p className="font-bold text-green-primary">{h.totalQuantity}</p>
                    <p className="text-text-secondary text-xs">Quantity</p>
                  </div>
                  <div>
                    <p className="font-bold text-green-primary">{h.harvestCount}</p>
                    <p className="text-text-secondary text-xs">Harvests</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {(!harvests || harvests.length === 0) && (
        <div className="card text-center py-6">
          <p className="text-2xl text-text-secondary/30 mb-1">🌾</p>
          <p className="text-sm text-text-secondary">No harvests yet</p>
        </div>
      )}

      <Fab onClick={() => navigate('/garden/new')} />
    </div>
  )
}
