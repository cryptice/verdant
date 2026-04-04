import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from 'react'
import { useAuth } from './AuthContext'
import { setActiveOrgId } from '../api/client'
import type { UserOrgMembership } from '../api/client'

interface OrgContextValue {
  /** The active organization, or null if user has no orgs */
  activeOrg: UserOrgMembership | null
  /** All organizations the user belongs to */
  organizations: UserOrgMembership[]
  /** Switch to a different organization */
  switchOrg: (orgId: number) => void
  /** Whether the user needs to create/join an org before using the app */
  needsOrg: boolean
  /** Whether we're still loading */
  loading: boolean
}

const OrgContext = createContext<OrgContextValue | null>(null)

export function OrgProvider({ children }: { children: ReactNode }) {
  const { user, loading: authLoading } = useAuth()
  const [activeOrgId, setActiveOrgIdState] = useState<number | null>(() => {
    const stored = localStorage.getItem('verdant_org_id')
    return stored ? Number(stored) : null
  })

  const organizations = user?.organizations ?? []
  const needsOrg = !authLoading && !!user && organizations.length === 0

  // Resolve active org from the user's membership list
  const activeOrg = organizations.find(o => o.orgId === activeOrgId)
    ?? organizations[0]
    ?? null

  // Sync activeOrgId when user data changes
  useEffect(() => {
    if (!user) return
    if (organizations.length === 0) {
      setActiveOrgId(null)
      setActiveOrgIdState(null)
      return
    }
    // If stored orgId is not in the user's orgs, default to first
    const validOrg = organizations.find(o => o.orgId === activeOrgId) ?? organizations[0]
    if (validOrg.orgId !== activeOrgId) {
      setActiveOrgId(validOrg.orgId)
      setActiveOrgIdState(validOrg.orgId)
    }
  }, [user, organizations, activeOrgId])

  const switchOrg = useCallback((orgId: number) => {
    setActiveOrgId(orgId)
    setActiveOrgIdState(orgId)
  }, [])

  return (
    <OrgContext.Provider value={{
      activeOrg,
      organizations,
      switchOrg,
      needsOrg,
      loading: authLoading,
    }}>
      {children}
    </OrgContext.Provider>
  )
}

export function useOrg() {
  const ctx = useContext(OrgContext)
  if (!ctx) throw new Error('useOrg must be used within OrgProvider')
  return ctx
}
