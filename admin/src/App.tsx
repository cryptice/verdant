import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import Login from './pages/Login'
import Users from './pages/Users'
import Gardens from './pages/Gardens'
import { SpeciesListPage, SpeciesDetailPage, SpeciesEditPage, SpeciesCreatePage } from './pages/Species'
import DevSeed from './pages/DevSeed'
import ResetData from './pages/ResetData'
import Outlets from './pages/Outlets'
import Providers from './pages/Providers'
import ProtectedRoute from './components/ProtectedRoute'
import { PlanningGroups, PlanningGroupDetail } from './pages/PlanningGroups'
import { PlanningSchedules, PlanningScheduleDetail } from './pages/PlanningSchedules'
import PlanningAssignments from './pages/PlanningAssignments'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/" element={<Navigate to="/species" replace />} />
          <Route path="/users" element={<Users />} />
          <Route path="/gardens" element={<Gardens />} />
          <Route path="/species" element={<SpeciesListPage />} />
          <Route path="/species/new" element={<SpeciesCreatePage />} />
          <Route path="/species/:id" element={<SpeciesDetailPage />} />
          <Route path="/species/:id/edit" element={<SpeciesEditPage />} />
          <Route path="/groups" element={<PlanningGroups />} />
          <Route path="/groups/:id" element={<PlanningGroupDetail />} />
          <Route path="/schedules" element={<PlanningSchedules />} />
          <Route path="/schedules/:key" element={<PlanningScheduleDetail />} />
          <Route path="/planning/species" element={<PlanningAssignments />} />
          <Route path="/providers" element={<Providers />} />
          <Route path="/outlets" element={<Outlets />} />
          <Route path="/reset" element={<ResetData />} />
          <Route path="/dev" element={<DevSeed />} />
        </Route>
      </Route>
    </Routes>
  )
}
