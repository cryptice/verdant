import { createContext, useContext, useState, useCallback, useEffect, useMemo, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
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
  const queryClient = useQueryClient()

  const [state, setState] = useState<OnboardingState>(() =>
    parseOnboardingState(user?.onboarding)
  )
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [minimized, setMinimized] = useState(false)
  const [activeTour, setActiveTour] = useState<PageTooltipConfig | null>(null)

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
  }, [syncToBackend])

  // Visit-type steps are completed via the tooltip tour in OnboardingRoot,
  // not by auto-detecting route visits. This prevents accidental completion
  // when users navigate normally through the sidebar.

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

  // Listen for mutation successes to detect completed action steps.
  // We track invalidated query keys so we only mark steps complete when
  // a mutation triggers a refetch, not on initial page-load fetches.
  useEffect(() => {
    const recentlyInvalidated = new Set<string>()

    // When a mutation succeeds, record which queries it invalidates
    const mutUnsub = queryClient.getMutationCache().subscribe((event) => {
      if (event.type !== 'updated' || event.mutation?.state.status !== 'success') return
      // After a mutation, queries get invalidated. Mark a short window.
      for (const step of ONBOARDING_STEPS) {
        if (step.mutationQueryKeys) {
          for (const mk of step.mutationQueryKeys) {
            recentlyInvalidated.add(mk.join(','))
          }
        }
      }
      // Clear after a short delay
      setTimeout(() => recentlyInvalidated.clear(), 2000)
    })

    // When queries refetch after invalidation, check if they match onboarding steps
    const queryUnsub = queryClient.getQueryCache().subscribe((event) => {
      if (event.type !== 'updated' || event.action.type !== 'success') return
      if (recentlyInvalidated.size === 0) return

      const queryKey = event.query.queryKey
      for (const step of ONBOARDING_STEPS) {
        if (step.completionType !== 'mutation') continue
        if (state.completedSteps.includes(step.id)) continue
        if (!step.mutationQueryKeys) continue

        const matches = step.mutationQueryKeys.some(mk =>
          recentlyInvalidated.has(mk.join(',')) &&
          mk.length <= queryKey.length && mk.every((k, i) => k === queryKey[i])
        )
        if (matches) {
          completeStep(step.id)
        }
      }
    })

    return () => { mutUnsub(); queryUnsub() }
  }, [queryClient, state.completedSteps, completeStep])

  const startStep = useCallback((stepId: string) => {
    const step = ONBOARDING_STEPS.find(s => s.id === stepId)
    if (!step) return
    setDrawerOpen(false)
    navigate(step.route)
    import('./tooltipConfigs').then(({ getTooltipConfig }) => {
      const config = getTooltipConfig(stepId)
      if (config) {
        setTimeout(() => setActiveTour(config), 100)
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
      !state.completedSteps.includes(s.id) && s.route === pathname
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
    getHintsForRoute,
  }), [isActive, completedCount, totalCount, isStepComplete, sectionProgress, completeStep,
       startStep, minimizeForSession, dismissPermanently, drawerOpen, activeTour, clearActiveTour, minimized, getHintsForRoute])

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
