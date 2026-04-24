import { createContext, useContext, useState, useCallback, useEffect, useMemo, useRef, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { api } from '../api/client'
import { ONBOARDING_STEPS, getStepsForSection } from './steps'
import type { OnboardingStep, OnboardingState, OnboardingSection, PageTooltipConfig } from './types'

interface OnboardingContextValue {
  isActive: boolean
  enabled: boolean
  setEnabled: (enabled: boolean) => void
  completedCount: number
  totalCount: number
  isStepComplete: (stepId: string) => boolean
  isStepBlocked: (stepId: string) => boolean
  sectionProgress: (section: OnboardingSection) => { completed: number; total: number }
  completeStep: (stepId: string) => void
  startStep: (stepId: string) => void
  minimizeForSession: () => void
  dismissPermanently: () => void
  drawerOpen: boolean
  setDrawerOpen: (open: boolean) => void
  activeTour: PageTooltipConfig | null
  clearActiveTour: () => void
  minimized: boolean
  lastCompletedStepId: string | null
  getHintsForRoute: (pathname: string) => OnboardingStep[]
}

const ENABLED_KEY = 'verdant-onboarding-enabled'

const OnboardingContext = createContext<OnboardingContextValue | null>(null)

function parseOnboardingState(json: string | undefined | null): OnboardingState {
  if (!json) return { completedSteps: [], dismissed: false }
  try {
    const parsed = JSON.parse(json)
    return {
      completedSteps: parsed.completedSteps ?? [],
      dismissed: parsed.dismissed ?? false,
    }
  } catch {
    return { completedSteps: [], dismissed: false }
  }
}

export function OnboardingProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [state, setState] = useState<OnboardingState>(() =>
    parseOnboardingState(user?.onboarding)
  )
  const [drawerOpen, setDrawerOpenInternal] = useState(false)
  const [minimized, setMinimized] = useState(false)
  const [activeTour, setActiveTour] = useState<PageTooltipConfig | null>(null)
  const [lastCompletedStepId, setLastCompletedStepId] = useState<string | null>(null)

  // Onboarding is disabled by default — the user can re-enable it from the
  // Account page. Persisted in localStorage so the choice sticks per device.
  const [enabled, setEnabledState] = useState<boolean>(
    () => typeof window !== 'undefined' && localStorage.getItem(ENABLED_KEY) === 'true',
  )
  const setEnabled = useCallback((next: boolean) => {
    if (typeof window !== 'undefined') {
      localStorage.setItem(ENABLED_KEY, next ? 'true' : 'false')
    }
    setEnabledState(next)
  }, [])

  // Ignore drawer-open calls while onboarding is disabled so nothing can
  // pop the drawer back up behind the user's back.
  const setDrawerOpen = useCallback((open: boolean) => {
    if (open && !enabled) return
    setDrawerOpenInternal(open)
  }, [enabled])

  // Track the "suppressed" signal in a ref so completeStep's closure always
  // sees the latest value without having to be recreated (and re-running the
  // auto-complete effect below every time it flips).
  const suppressedRef = useRef(false)

  // Sync state when user data changes (e.g., on login)
  useEffect(() => {
    setState(parseOnboardingState(user?.onboarding))
  }, [user?.onboarding])

  useEffect(() => {
    suppressedRef.current = !enabled || state.dismissed || minimized
  }, [enabled, state.dismissed, minimized])

  const syncToBackend = useCallback((s: OnboardingState) => {
    api.user.updateOnboarding({ completedSteps: s.completedSteps, dismissed: s.dismissed }).catch(() => {
      // Silently fail — onboarding is non-critical
    })
  }, [])

  const completeStep = useCallback((stepId: string) => {
    setState(prev => {
      if (prev.completedSteps.includes(stepId)) return prev
      const updated = { ...prev, completedSteps: [...prev.completedSteps, stepId] }
      syncToBackend(updated)
      return updated
    })
    // If onboarding is dismissed or minimized, just record the completion
    // silently — don't re-open the drawer or flash a toast.
    if (suppressedRef.current) return
    setLastCompletedStepId(stepId)
    setDrawerOpen(true)
    setTimeout(() => setLastCompletedStepId(null), 3000)
  }, [syncToBackend])

  // Auto-complete visit-type steps when the user navigates to the page
  useEffect(() => {
    for (const step of ONBOARDING_STEPS) {
      if (step.completionType !== 'visit') continue
      if (state.completedSteps.includes(step.id)) continue
      if (location.pathname === step.route) {
        completeStep(step.id)
      }
    }
  }, [location.pathname, state.completedSteps, completeStep])

  // Auto-complete create_org step when user has organizations
  useEffect(() => {
    if (!user) return
    if (user.organizations.length > 0 && !state.completedSteps.includes('create_org')) {
      completeStep('create_org')
    }
  }, [user, state.completedSteps, completeStep])

  const startStep = useCallback((stepId: string) => {
    const step = ONBOARDING_STEPS.find(s => s.id === stepId)
    if (!step) return
    setDrawerOpen(false)
    if (window.location.pathname !== step.route) {
      navigate(step.route)
    }
    import('./tooltipConfigs').then(({ getTooltipConfig }) => {
      const config = getTooltipConfig(stepId)
      if (config) {
        setTimeout(() => setActiveTour(config), 400)
      }
    })
  }, [navigate])

  const minimizeForSession = useCallback(() => {
    setMinimized(true)
    setDrawerOpen(false)
  }, [])

  const dismissPermanently = useCallback(() => {
    const updated = { ...state, dismissed: true }
    setState(updated)
    setDrawerOpen(false)
    syncToBackend(updated)
  }, [state, syncToBackend])

  const clearActiveTour = useCallback(() => setActiveTour(null), [])

  const isStepComplete = useCallback((stepId: string) =>
    state.completedSteps.includes(stepId), [state.completedSteps])

  const isStepBlocked = useCallback((stepId: string) => {
    const step = ONBOARDING_STEPS.find(s => s.id === stepId)
    if (!step?.requires) return false
    return !state.completedSteps.includes(step.requires)
  }, [state.completedSteps])

  const sectionProgress = useCallback((section: OnboardingSection) => {
    const steps = getStepsForSection(section)
    const completed = steps.filter(s => state.completedSteps.includes(s.id)).length
    return { completed, total: steps.length }
  }, [state.completedSteps])

  const completedCount = state.completedSteps.length
  const totalCount = ONBOARDING_STEPS.length
  const isActive = enabled && !state.dismissed && completedCount < totalCount

  const getHintsForRoute = useCallback((pathname: string) => {
    if (!isActive) return []
    return ONBOARDING_STEPS.filter(s =>
      !state.completedSteps.includes(s.id) && s.route === pathname
    )
  }, [isActive, state.completedSteps])

  const value = useMemo<OnboardingContextValue>(() => ({
    isActive, enabled, setEnabled, completedCount, totalCount,
    isStepComplete, isStepBlocked, sectionProgress,
    completeStep, startStep,
    minimizeForSession, dismissPermanently,
    drawerOpen, setDrawerOpen,
    activeTour, clearActiveTour,
    minimized, lastCompletedStepId,
    getHintsForRoute,
  }), [isActive, enabled, setEnabled, completedCount, totalCount, isStepComplete, isStepBlocked, sectionProgress,
       completeStep, startStep, minimizeForSession, dismissPermanently,
       drawerOpen, setDrawerOpen, activeTour, clearActiveTour, minimized, lastCompletedStepId, getHintsForRoute])

  return (
    <OnboardingContext.Provider value={value}>
      {children}
    </OnboardingContext.Provider>
  )
}

export function useOnboarding() {
  const ctx = useContext(OnboardingContext)
  if (!ctx) throw new Error('useOnboarding must be used within OnboardingProvider')
  return ctx
}
