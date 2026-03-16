import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import Login from './pages/Login'
import Users from './pages/Users'
import Gardens from './pages/Gardens'
import Species from './pages/Species'
import DevSeed from './pages/DevSeed'
import ProtectedRoute from './components/ProtectedRoute'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/" element={<Navigate to="/species" replace />} />
          <Route path="/users" element={<Users />} />
          <Route path="/gardens" element={<Gardens />} />
          <Route path="/species" element={<Species />} />
          <Route path="/dev" element={<DevSeed />} />
        </Route>
      </Route>
    </Routes>
  )
}
