import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import { LoginPage } from './auth/LoginPage'
import { Layout } from './components/Layout'
import { Dashboard } from './pages/Dashboard'
import { GardenDetail } from './pages/GardenDetail'
import { GardenForm } from './pages/GardenForm'
import { BedDetail } from './pages/BedDetail'
import { BedForm } from './pages/BedForm'
import { PlantDetail } from './pages/PlantDetail'
import { PlantedSpeciesList } from './pages/PlantedSpeciesList'
import { PlantedSpeciesDetail } from './pages/PlantedSpeciesDetail'
import { TaskList } from './pages/TaskList'
import { TaskForm } from './pages/TaskForm'
import { SeedInventory } from './pages/SeedInventory'
import { SowActivity } from './pages/SowActivity'
import { SpeciesList } from './pages/SpeciesList'
import { Account } from './pages/Account'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { token, loading } = useAuth()
  if (loading) return null
  if (!token) return <Navigate to="/login" replace />
  return <>{children}</>
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route index element={<Dashboard />} />
        <Route path="garden/new" element={<GardenForm />} />
        <Route path="garden/:id" element={<GardenDetail />} />
        <Route path="garden/:gardenId/bed/new" element={<BedForm />} />
        <Route path="bed/:id" element={<BedDetail />} />
        <Route path="plant/:id" element={<PlantDetail />} />
        <Route path="plants" element={<PlantedSpeciesList />} />
        <Route path="species/:speciesId/plants" element={<PlantedSpeciesDetail />} />
        <Route path="tasks" element={<TaskList />} />
        <Route path="task/new" element={<TaskForm />} />
        <Route path="task/:taskId/edit" element={<TaskForm />} />
        <Route path="seeds" element={<SeedInventory />} />
        <Route path="sow" element={<SowActivity />} />
        <Route path="species" element={<SpeciesList />} />
        <Route path="account" element={<Account />} />
      </Route>
    </Routes>
  )
}
