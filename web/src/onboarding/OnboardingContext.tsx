import { createContext, useContext, useState, useCallback, useEffect, useMemo, useRef, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../auth/AuthContext'
import { api } from '../api/client'
import { ONBOARDING_STEPS, getStepsForSection } from './steps'
import type { OnboardingStep, OnboardingState, OnboardingSection, PageTooltipConfig } from './types'

interface OnboardingContextValue {
  isActive: boolean
  completedCount: number
  totalCount: number
  isStepComplete: (stepId: string) => boolean
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
  /** ID of the step that was just completed (for animation), cleared after a delay */
  lastCompletedStepId: string | null
  /** Get incomplete onboarding steps that match the given route */
  getHintsForRoute: (pathname: string) => OnboardingStep[]
}

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
  const queryClient = useQueryClient()

  const [state, setState] = useState<OnboardingState>(() =>
    parseOnboardingState(user?.onboarding)
  )
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [minimized, setMinimized] = useState(false)
  const [activeTour, setActiveTour] = useState<PageTooltipConfig | null>(null)
  const [lastCompletedStepId, setLastCompletedStepId] = useState<string | null>(null)

  // Sync state when user data changes (e.g., on login)
  useEffect(() => {
    setState(parseOnboardingState(user?.onboarding))
  }, [user?.onboarding])

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
    // Show celebration: open drawer and highlight the completed step
    setLastCompletedStepId(stepId)
    setDrawerOpen(true)
    // Clear the highlight after animation plays
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

  // Check query cache for pre-existing data on mount
  useEffect(() => {
    if (!user) return
    const newCompleted: string[] = []
    for (const step of ONBOARDING_STEPS) {
      if (state.completedSteps.includes(step.id)) continue
      if (step.queryKey) {
        const data = queryClient.getQueryData(step.queryKey)
        if (data && (Array.isArray(data) ? data.length > 0 : true)) {
          newCompleted.push(step.id)
        }
      }
    }
    if (newCompleted.length > 0) {
      const updated = [...state.completedSteps, ...newCompleted]
      setState(prev => ({ ...prev, completedSteps: updated }))
      syncToBackend({ completedSteps: updated, dismissed: state.dismissed })
    }
  }, [user]) // eslint-disable-line react-hooks/exhaustive-deps

  // Listen for query invalidations triggered by user-initiated mutations.
  // Uses a ref so the flag persists across re-renders without recreating subscriptions.
  const mutationInProgress = useRef(false)
  const completedStepsRef = useRef(state.completedSteps)
  completedStepsRef.current = state.completedSteps

  useEffect(() => {
    const mutUnsub = queryClient.getMutationCache().subscribe((event) => {
      if (event.type !== 'updated') return
      const status = event.mutation?.state.status
      // Ignore the onboarding sync mutation
      const endpoint = event.mutation?.state.variables as { completedSteps?: unknown } | undefined
      if (endpoint && 'completedSteps' in (endpoint ?? {})) return

      if (status === 'pending') {
        mutationInProgress.current = true
      }
      if (status === 'success') {
        setTimeout(() => { mutationInProgress.current = false }, 2000)
      }
    })

    const queryUnsub = queryClient.getQueryCache().subscribe((event) => {
      if (!mutationInProgress.current) return
      if (event.type !== 'updated') return

      const actionType = event.action.type
      if (actionType !== 'success' && actionType !== 'invalidate') return

      const queryKey = event.query.queryKey

      for (const step of ONBOARDING_STEPS) {
        if (step.completionType !== 'mutation') continue
        if (completedStepsRef.current.includes(step.id)) continue
        if (!step.mutationQueryKeys) continue

        const matches = step.mutationQueryKeys.some(mk =>
          mk.length <= queryKey.length && mk.every((k, i) => k === queryKey[i])
        )
        if (matches) {
          if (actionType === 'invalidate') {
            // Query was invalidated by a mutation — step is complete
            completeStep(step.id)
          } else if (actionType === 'success' && event.query.state.fetchStatus === 'idle') {
            const data = event.query.state.data
            if (data && (Array.isArray(data) ? data.length > 0 : true)) {
              completeStep(step.id)
            }
          }
        }
      }
    })

    return () => { mutUnsub(); queryUnsub() }
  }, [queryClient, completeStep])

  const startStep = useCallback((stepId: string) => {
    const step = ONBOARDING_STEPS.find(s => s.id === stepId)
    if (!step) return
    setDrawerOpen(false)
    const targetRoute = step.resolveRoute?.(queryClient) ?? step.route
    const currentPath = window.location.pathname
    const alreadyOnPage = currentPath === targetRoute ||
      step.extraRoutePrefixes?.some(p => currentPath.startsWith(p))
    if (!alreadyOnPage) {
      navigate(targetRoute)
    }
    import('./tooltipConfigs').then(({ getTooltipConfig }) => {
      const config = getTooltipConfig(stepId)
      if (config) {
        // Delay to let drawer close animation (300ms) finish and page render
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

  const sectionProgress = useCallback((section: OnboardingSection) => {
    const steps = getStepsForSection(section)
    const completed = steps.filter(s => state.completedSteps.includes(s.id)).length
    return { completed, total: steps.length }
  }, [state.completedSteps])

  const completedCount = state.completedSteps.length
  const totalCount = ONBOARDING_STEPS.length
  const isActive = !state.dismissed && completedCount < totalCount

  const getHintsForRoute = useCallback((pathname: string) => {
    if (!isActive) return []
    return ONBOARDING_STEPS.filter(s =>
      !state.completedSteps.includes(s.id) &&
      (s.route === pathname || s.extraRoutePrefixes?.some(p => pathname.startsWith(p)))
    )
  }, [isActive, state.completedSteps])

  const value = useMemo<OnboardingContextValue>(() => ({
    isActive,
    completedCount,
    totalCount,
    isStepComplete,
    sectionProgress,
    completeStep,
    startStep,
    minimizeForSession,
    dismissPermanently,
    drawerOpen,
    setDrawerOpen,
    activeTour,
    clearActiveTour,
    minimized,
    lastCompletedStepId,
    getHintsForRoute,
  }), [isActive, completedCount, totalCount, isStepComplete, sectionProgress, completeStep,
       startStep, minimizeForSession, dismissPermanently, drawerOpen, activeTour, clearActiveTour, minimized, lastCompletedStepId, getHintsForRoute])

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
