import { lazy, Suspense } from 'react'
import { Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import { useOrg } from './auth/OrgContext'
import { LandingPage } from './pages/LandingPage'

const PrivacyPolicy = lazy(() => import('./pages/PrivacyPolicy').then(m => ({ default: m.PrivacyPolicy })))
const OrgSetup = lazy(() => import('./pages/OrgSetup').then(m => ({ default: m.OrgSetup })))
const Layout = lazy(() => import('./components/Layout').then(m => ({ default: m.Layout })))
const Dashboard = lazy(() => import('./pages/Dashboard').then(m => ({ default: m.Dashboard })))
const GardenList = lazy(() => import('./pages/GardenList').then(m => ({ default: m.GardenList })))
const GardenDetail = lazy(() => import('./pages/GardenDetail').then(m => ({ default: m.GardenDetail })))
const BedDetail = lazy(() => import('./pages/BedDetail').then(m => ({ default: m.BedDetail })))
const BedForm = lazy(() => import('./pages/BedForm').then(m => ({ default: m.BedForm })))
const PlantDetail = lazy(() => import('./pages/PlantDetail').then(m => ({ default: m.PlantDetail })))
const PlantedSpeciesList = lazy(() => import('./pages/PlantedSpeciesList').then(m => ({ default: m.PlantedSpeciesList })))
const PlantedSpeciesDetail = lazy(() => import('./pages/PlantedSpeciesDetail').then(m => ({ default: m.PlantedSpeciesDetail })))
const TaskList = lazy(() => import('./pages/TaskList').then(m => ({ default: m.TaskList })))
const TaskForm = lazy(() => import('./pages/TaskForm').then(m => ({ default: m.TaskForm })))
const SeedInventory = lazy(() => import('./pages/SeedInventory').then(m => ({ default: m.SeedInventory })))
const SowActivity = lazy(() => import('./pages/SowActivity').then(m => ({ default: m.SowActivity })))
const SpeciesList = lazy(() => import('./pages/SpeciesList').then(m => ({ default: m.SpeciesList })))
const SpeciesDetail = lazy(() => import('./pages/SpeciesDetail').then(m => ({ default: m.SpeciesDetail })))
const SpeciesGroups = lazy(() => import('./pages/SpeciesGroups').then(m => ({ default: m.SpeciesGroups })))
const SpeciesGroupEdit = lazy(() => import('./pages/SpeciesGroupEdit').then(m => ({ default: m.SpeciesGroupEdit })))
const SeasonList = lazy(() => import('./pages/SeasonList').then(m => ({ default: m.SeasonList })))
const TrayLocations = lazy(() => import('./pages/TrayLocations').then(m => ({ default: m.TrayLocations })))
const TrayLocationDetail = lazy(() => import('./pages/TrayLocationDetail').then(m => ({ default: m.TrayLocationDetail })))
const CustomerList = lazy(() => import('./pages/CustomerList').then(m => ({ default: m.CustomerList })))
const PestDiseaseLog = lazy(() => import('./pages/PestDiseaseLog').then(m => ({ default: m.PestDiseaseLog })))
const VarietyTrials = lazy(() => import('./pages/VarietyTrials').then(m => ({ default: m.VarietyTrials })))
const BouquetRecipes = lazy(() => import('./pages/BouquetRecipes').then(m => ({ default: m.BouquetRecipes })))
const Bouquets = lazy(() => import('./pages/Bouquets').then(m => ({ default: m.Bouquets })))
const CropCalendar = lazy(() => import('./pages/CropCalendar').then(m => ({ default: m.CropCalendar })))
const SuccessionSchedules = lazy(() => import('./pages/SuccessionSchedules').then(m => ({ default: m.SuccessionSchedules })))
const ProductionTargets = lazy(() => import('./pages/ProductionTargets').then(m => ({ default: m.ProductionTargets })))
const Analytics = lazy(() => import('./pages/Analytics').then(m => ({ default: m.Analytics })))
const Account = lazy(() => import('./pages/Account').then(m => ({ default: m.Account })))
const OrgSettings = lazy(() => import('./pages/OrgSettings').then(m => ({ default: m.OrgSettings })))
const Guide = lazy(() => import('./pages/Guide').then(m => ({ default: m.Guide })))
const Supplies = lazy(() => import('./pages/Supplies').then(m => ({ default: m.Supplies })))
const WorkflowTemplates = lazy(() => import('./pages/WorkflowTemplates').then(m => ({ default: m.WorkflowTemplates })))
const WorkflowTemplateEdit = lazy(() => import('./pages/WorkflowTemplateEdit').then(m => ({ default: m.WorkflowTemplateEdit })))
const WorkflowProgress = lazy(() => import('./pages/WorkflowProgress').then(m => ({ default: m.WorkflowProgress })))
const ApplySupply = lazy(() => import('./pages/ApplySupply').then(m => ({ default: m.ApplySupply })))

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

function RequireOrg({ children }: { children: React.ReactNode }) {
  const { needsOrg, loading } = useOrg()
  const location = useLocation()
  if (loading) return null
  if (needsOrg && location.pathname !== '/org/setup') return <Navigate to="/org/setup" replace />
  return <>{children}</>
}

export function App() {
  const { token, loading } = useAuth()

  return (
    <Suspense fallback={null}>
      <Routes>
        <Route path="/login" element={<PublicOnly><LandingPage /></PublicOnly>} />
        <Route path="/privacy" element={<PrivacyPolicy />} />
        <Route path="/org/setup" element={<ProtectedRoute><OrgSetup /></ProtectedRoute>} />
        <Route element={<ProtectedRoute><RequireOrg><Layout /></RequireOrg></ProtectedRoute>}>
          <Route index element={<Dashboard />} />
          <Route path="gardens" element={<GardenList />} />
          <Route path="garden/new" element={<Navigate to="/" replace />} />
          <Route path="garden/:id" element={<GardenDetail />} />
          <Route path="garden/:gardenId/bed/new" element={<BedForm />} />
          <Route path="bed/:id" element={<BedDetail />} />
          <Route path="plant/:id" element={<PlantDetail />} />
          <Route path="plants" element={<PlantedSpeciesList />} />
          <Route path="species/:id" element={<SpeciesDetail />} />
          <Route path="species/:speciesId/plants" element={<PlantedSpeciesDetail />} />
          <Route path="tasks" element={<TaskList />} />
          <Route path="task/new" element={<TaskForm />} />
          <Route path="task/:taskId/edit" element={<TaskForm />} />
          <Route path="seed-stock" element={<SeedInventory />} />
          <Route path="supplies" element={<Supplies />} />
          <Route path="sow" element={<SowActivity />} />
          <Route path="activity/apply-supply" element={<ApplySupply />} />
          <Route path="species" element={<SpeciesList />} />
          <Route path="species-groups" element={<SpeciesGroups />} />
          <Route path="species-groups/:id/edit" element={<SpeciesGroupEdit />} />
          <Route path="workflows" element={<WorkflowTemplates />} />
          <Route path="workflows/:id/edit" element={<WorkflowTemplateEdit />} />
          <Route path="workflows/progress/:speciesId" element={<WorkflowProgress />} />
          <Route path="seasons" element={<SeasonList />} />
          <Route path="tray-locations" element={<TrayLocations />} />
          <Route path="tray-locations/:id" element={<TrayLocationDetail />} />
          <Route path="customers" element={<CustomerList />} />
          <Route path="pest-disease" element={<PestDiseaseLog />} />
          <Route path="trials" element={<VarietyTrials />} />
          <Route path="bouquets" element={<Bouquets />} />
          <Route path="bouquet-recipes" element={<BouquetRecipes />} />
          <Route path="calendar" element={<CropCalendar />} />
          <Route path="successions" element={<SuccessionSchedules />} />
          <Route path="targets" element={<ProductionTargets />} />
          <Route path="analytics" element={<Analytics />} />
          <Route path="guide" element={<Guide />} />
          <Route path="org/settings" element={<OrgSettings />} />
          <Route path="account" element={<Account />} />
        </Route>
        {!loading && !token && <Route path="*" element={<Navigate to="/login" replace />} />}
      </Routes>
    </Suspense>
  )
}
