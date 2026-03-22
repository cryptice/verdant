import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import { LandingPage } from './pages/LandingPage'
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
import { SeasonList } from './pages/SeasonList'
import { CustomerList } from './pages/CustomerList'
import { PestDiseaseLog } from './pages/PestDiseaseLog'
import { VarietyTrials } from './pages/VarietyTrials'
import { BouquetRecipes } from './pages/BouquetRecipes'
import { CropCalendar } from './pages/CropCalendar'
import { SuccessionSchedules } from './pages/SuccessionSchedules'
import { ProductionTargets } from './pages/ProductionTargets'
import { Analytics } from './pages/Analytics'
import { Account } from './pages/Account'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { token, loading } = useAuth()
  if (loading) return null
  if (!token) return <Navigate to="/login" replace />
  return <>{children}</>
}

function PublicOnly({ children }: { children: React.ReactNode }) {
  const { token, loading } = useAuth()
  if (loading) return null
  if (token) return <Navigate to="/" replace />
  return <>{children}</>
}

export function App() {
  const { token, loading } = useAuth()

  return (
    <Routes>
      <Route path="/login" element={<PublicOnly><LandingPage /></PublicOnly>} />
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
        <Route path="seasons" element={<SeasonList />} />
        <Route path="customers" element={<CustomerList />} />
        <Route path="pest-disease" element={<PestDiseaseLog />} />
        <Route path="trials" element={<VarietyTrials />} />
        <Route path="bouquets" element={<BouquetRecipes />} />
        <Route path="calendar" element={<CropCalendar />} />
        <Route path="successions" element={<SuccessionSchedules />} />
        <Route path="targets" element={<ProductionTargets />} />
        <Route path="analytics" element={<Analytics />} />
        <Route path="account" element={<Account />} />
      </Route>
      {/* Unauthenticated root shows landing page, authenticated shows dashboard via ProtectedRoute */}
      {!loading && !token && <Route path="*" element={<Navigate to="/login" replace />} />}
    </Routes>
  )
}
