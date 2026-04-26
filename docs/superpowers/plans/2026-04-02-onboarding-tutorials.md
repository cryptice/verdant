# Onboarding Tutorials Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a hybrid onboarding system to the Verdant web app — a floating checklist drawer with contextual tooltips — that guides new users through all app features in progressive sections.

**Architecture:** A React context (`OnboardingContext`) manages checklist state, synced to a JSONB column on the backend `app_user` table. A floating progress button opens a slide-out drawer with 5 collapsible sections (19 steps total). Clicking a checklist item navigates to the relevant page and triggers sequential tooltips with backdrop overlay and interactive cutout. Tooltip "seen" state is localStorage-only. The `@floating-ui/react` library handles tooltip positioning.

**Tech Stack:** React 19, TanStack Query, @floating-ui/react, Tailwind CSS v4, i18next, Quarkus/Kotlin backend with raw JDBC

---

## File Structure

### Backend (new/modified)
| File | Action | Responsibility |
|------|--------|----------------|
| `backend/src/main/resources/db/migration/V2__onboarding.sql` | Create | Add `onboarding_json` TEXT column to `app_user` |
| `backend/src/main/kotlin/app/verdant/entity/User.kt` | Modify | Add `onboardingJson` field |
| `backend/src/main/kotlin/app/verdant/dto/UserDtos.kt` | Modify | Add `onboarding` to `UserResponse`, new `UpdateOnboardingRequest` |
| `backend/src/main/kotlin/app/verdant/repository/UserRepository.kt` | Modify | Read/write `onboarding_json` column |
| `backend/src/main/kotlin/app/verdant/resource/UserResource.kt` | Modify | Add `PUT /api/users/me/onboarding` endpoint |
| `backend/src/main/kotlin/app/verdant/service/AuthService.kt` | Modify | Include `onboardingJson` in `toResponse()` |

### Frontend (new)
| File | Responsibility |
|------|----------------|
| `web/src/onboarding/types.ts` | Onboarding types, step definitions, section structure |
| `web/src/onboarding/steps.ts` | Step configuration: IDs, routes, completion detection config |
| `web/src/onboarding/OnboardingContext.tsx` | React context: state management, completion tracking, API sync |
| `web/src/onboarding/OnboardingFab.tsx` | Floating action button with circular progress ring |
| `web/src/onboarding/OnboardingDrawer.tsx` | Slide-out drawer with sectioned checklist |
| `web/src/onboarding/OnboardingOverlay.tsx` | Backdrop overlay with interactive cutout |
| `web/src/onboarding/OnboardingTooltip.tsx` | Sequential tooltip with navigation controls |
| `web/src/onboarding/useTooltipTour.ts` | Hook: manages active tour state, step progression, element refs |
| `web/src/onboarding/tooltipConfigs.ts` | Per-page tooltip configurations (target selectors, text keys) |

### Frontend (modified)
| File | Action |
|------|--------|
| `web/src/api/client.ts` | Add `onboarding` field to `UserResponse`, add `api.user.updateOnboarding()` |
| `web/src/main.tsx` | Wrap app with `OnboardingProvider` |
| `web/src/components/Layout.tsx` | Render `OnboardingFab` |
| `web/src/i18n/en.json` | Add onboarding translation keys |
| `web/src/i18n/sv.json` | Add onboarding translation keys |

---

## Task 1: Database Migration & Backend Entity

**Files:**
- Create: `backend/src/main/resources/db/migration/V2__onboarding.sql`
- Modify: `backend/src/main/kotlin/app/verdant/entity/User.kt`
- Modify: `backend/src/main/kotlin/app/verdant/repository/UserRepository.kt`

- [ ] **Step 1: Create migration file**

```sql
-- V2__onboarding.sql
ALTER TABLE app_user ADD COLUMN onboarding_json TEXT;
```

- [ ] **Step 2: Add field to User entity**

In `backend/src/main/kotlin/app/verdant/entity/User.kt`, add `onboardingJson` field:

```kotlin
data class User(
    val id: Long? = null,
    val googleSubject: String? = null,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val passwordHash: String? = null,
    val role: Role = Role.USER,
    val language: String = "sv",
    val onboardingJson: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
```

- [ ] **Step 3: Update UserRepository to read/write the new column**

In `UserRepository.kt`, update:

1. `toUser()` mapper — add line:
```kotlin
onboardingJson = getString("onboarding_json"),
```

2. `persist()` — update INSERT SQL and parameter bindings:
```kotlin
conn.prepareStatement(
    """INSERT INTO app_user (google_subject, email, display_name, avatar_url, password_hash, role, language, onboarding_json, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, now(), now())""",
    Statement.RETURN_GENERATED_KEYS
).use { ps ->
    ps.setString(1, user.googleSubject)
    ps.setString(2, user.email)
    ps.setString(3, user.displayName)
    ps.setString(4, user.avatarUrl)
    ps.setString(5, user.passwordHash)
    ps.setString(6, user.role.name)
    ps.setString(7, user.language)
    ps.setString(8, user.onboardingJson)
    ps.executeUpdate()
    // ... rest unchanged
}
```

3. `update()` — update SET clause and parameter bindings:
```kotlin
conn.prepareStatement(
    """UPDATE app_user SET google_subject = ?, email = ?, display_name = ?, avatar_url = ?,
       password_hash = ?, role = ?, language = ?, onboarding_json = ?, updated_at = now() WHERE id = ?"""
).use { ps ->
    ps.setString(1, user.googleSubject)
    ps.setString(2, user.email)
    ps.setString(3, user.displayName)
    ps.setString(4, user.avatarUrl)
    ps.setString(5, user.passwordHash)
    ps.setString(6, user.role.name)
    ps.setString(7, user.language)
    ps.setString(8, user.onboardingJson)
    ps.setLong(9, user.id!!)
    ps.executeUpdate()
}
```

- [ ] **Step 4: Verify backend compiles**

Run: `cd backend && ./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/db/migration/V2__onboarding.sql backend/src/main/kotlin/app/verdant/entity/User.kt backend/src/main/kotlin/app/verdant/repository/UserRepository.kt
git commit -m "feat: add onboarding_json column to app_user table"
```

---

## Task 2: Backend DTOs & API Endpoint

**Files:**
- Modify: `backend/src/main/kotlin/app/verdant/dto/UserDtos.kt`
- Modify: `backend/src/main/kotlin/app/verdant/service/AuthService.kt`
- Modify: `backend/src/main/kotlin/app/verdant/resource/UserResource.kt`

- [ ] **Step 1: Update DTOs**

In `backend/src/main/kotlin/app/verdant/dto/UserDtos.kt`:

```kotlin
package app.verdant.dto

import app.verdant.entity.Role
import java.time.Instant

data class UserResponse(
    val id: Long,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
    val role: Role,
    val language: String,
    val onboarding: String?,
    val createdAt: Instant
)

data class UpdateUserRequest(
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val language: String? = null,
)

data class UpdateOnboardingRequest(
    val completedSteps: List<String>? = null,
    val dismissed: Boolean? = null,
)
```

- [ ] **Step 2: Update toResponse() in AuthService**

In `backend/src/main/kotlin/app/verdant/service/AuthService.kt`, update the extension function:

```kotlin
fun User.toResponse() = UserResponse(
    id = id!!,
    email = email,
    displayName = displayName,
    avatarUrl = avatarUrl,
    role = role,
    language = language,
    onboarding = onboardingJson,
    createdAt = createdAt
)
```

- [ ] **Step 3: Add onboarding endpoint to UserResource**

In `backend/src/main/kotlin/app/verdant/resource/UserResource.kt`, add the new endpoint:

```kotlin
@PUT
@Path("/me/onboarding")
fun updateOnboarding(request: UpdateOnboardingRequest): Any {
    val user = userRepository.findById(jwt.subject.toLong()) ?: throw NotFoundException("User not found")
    val updated = user.copy(
        onboardingJson = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().writeValueAsString(request)
    )
    userRepository.update(updated)
    return updated.toResponse()
}
```

Add import at top of file:
```kotlin
import app.verdant.dto.UpdateOnboardingRequest
```

Note: Jackson is already on the classpath via Quarkus. The frontend sends `{ completedSteps: ["create_season", ...], dismissed: false }` and the backend stores it verbatim as JSON text.

- [ ] **Step 4: Verify backend compiles**

Run: `cd backend && ./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/dto/UserDtos.kt backend/src/main/kotlin/app/verdant/service/AuthService.kt backend/src/main/kotlin/app/verdant/resource/UserResource.kt
git commit -m "feat: add onboarding API endpoint and DTO"
```

---

## Task 3: Install @floating-ui/react

**Files:**
- Modify: `web/package.json` (via npm)

- [ ] **Step 1: Install dependency**

Run: `cd web && npm install @floating-ui/react`
Expected: added 1-3 packages

- [ ] **Step 2: Commit**

```bash
git add web/package.json web/package-lock.json
git commit -m "feat: add @floating-ui/react dependency for onboarding tooltips"
```

---

## Task 4: Onboarding Types & Step Definitions

**Files:**
- Create: `web/src/onboarding/types.ts`
- Create: `web/src/onboarding/steps.ts`

- [ ] **Step 1: Create types file**

Create `web/src/onboarding/types.ts`:

```typescript
export interface OnboardingStep {
  id: string
  section: OnboardingSection
  route: string
  completionType: 'mutation' | 'visit' | 'query'
  /** TanStack Query key to check for existing data (for 'query' and 'mutation' completion) */
  queryKey?: string[]
  /** For 'mutation' type: which query keys signal completion when they're invalidated */
  mutationQueryKeys?: string[][]
}

export type OnboardingSection =
  | 'getting_started'
  | 'growing'
  | 'harvesting_sales'
  | 'planning'
  | 'advanced'

export interface OnboardingState {
  completedSteps: string[]
  dismissed: boolean
}

export interface TooltipStep {
  targetSelector: string
  titleKey: string
  descriptionKey: string
}

export interface PageTooltipConfig {
  stepId: string
  tooltips: TooltipStep[]
}
```

- [ ] **Step 2: Create step definitions file**

Create `web/src/onboarding/steps.ts`:

```typescript
import type { OnboardingStep, OnboardingSection } from './types'

export const ONBOARDING_STEPS: OnboardingStep[] = [
  // Section 1: Getting Started
  { id: 'create_season', section: 'getting_started', route: '/seasons', completionType: 'mutation', queryKey: ['seasons'], mutationQueryKeys: [['seasons']] },
  { id: 'create_garden', section: 'getting_started', route: '/garden/new', completionType: 'mutation', queryKey: ['dashboard'], mutationQueryKeys: [['dashboard']] },
  { id: 'create_bed', section: 'getting_started', route: '/', completionType: 'query', queryKey: ['dashboard'] },

  // Section 2: Growing
  { id: 'browse_species', section: 'growing', route: '/species', completionType: 'visit' },
  { id: 'add_seeds', section: 'growing', route: '/seeds', completionType: 'mutation', queryKey: ['seed-inventory'], mutationQueryKeys: [['seed-inventory']] },
  { id: 'sow_seeds', section: 'growing', route: '/sow', completionType: 'mutation', mutationQueryKeys: [['plants'], ['tray-summary']] },
  { id: 'pot_up', section: 'growing', route: '/plants', completionType: 'mutation', mutationQueryKeys: [['plants']] },
  { id: 'plant_out', section: 'growing', route: '/plants', completionType: 'mutation', mutationQueryKeys: [['plants']] },

  // Section 3: Harvesting & Sales
  { id: 'record_harvest', section: 'harvesting_sales', route: '/plants', completionType: 'mutation', mutationQueryKeys: [['harvest-stats']] },
  { id: 'add_customer', section: 'harvesting_sales', route: '/customers', completionType: 'mutation', queryKey: ['customers'], mutationQueryKeys: [['customers']] },
  { id: 'create_bouquet', section: 'harvesting_sales', route: '/bouquets', completionType: 'mutation', queryKey: ['bouquet-recipes'], mutationQueryKeys: [['bouquet-recipes']] },

  // Section 4: Planning
  { id: 'create_task', section: 'planning', route: '/task/new', completionType: 'mutation', queryKey: ['tasks'], mutationQueryKeys: [['tasks']] },
  { id: 'setup_succession', section: 'planning', route: '/successions', completionType: 'mutation', queryKey: ['succession-schedules'], mutationQueryKeys: [['succession-schedules']] },
  { id: 'set_target', section: 'planning', route: '/targets', completionType: 'mutation', queryKey: ['production-targets'], mutationQueryKeys: [['production-targets']] },
  { id: 'view_calendar', section: 'planning', route: '/calendar', completionType: 'visit' },

  // Section 5: Advanced
  { id: 'start_trial', section: 'advanced', route: '/trials', completionType: 'mutation', queryKey: ['variety-trials'], mutationQueryKeys: [['variety-trials']] },
  { id: 'log_pest', section: 'advanced', route: '/pest-disease', completionType: 'mutation', queryKey: ['pest-disease-logs'], mutationQueryKeys: [['pest-disease-logs']] },
  { id: 'view_analytics', section: 'advanced', route: '/analytics', completionType: 'visit' },
  { id: 'create_listing', section: 'advanced', route: '/market/listings', completionType: 'mutation', queryKey: ['my-listings'], mutationQueryKeys: [['my-listings']] },
]

export const SECTIONS: { id: OnboardingSection; titleKey: string; icon: string }[] = [
  { id: 'getting_started', titleKey: 'onboarding.sections.gettingStarted', icon: '🚀' },
  { id: 'growing', titleKey: 'onboarding.sections.growing', icon: '🌱' },
  { id: 'harvesting_sales', titleKey: 'onboarding.sections.harvestingSales', icon: '🌾' },
  { id: 'planning', titleKey: 'onboarding.sections.planning', icon: '📋' },
  { id: 'advanced', titleKey: 'onboarding.sections.advanced', icon: '⚡' },
]

export function getStepsForSection(section: OnboardingSection): OnboardingStep[] {
  return ONBOARDING_STEPS.filter(s => s.section === section)
}
```

- [ ] **Step 3: Verify types compile**

Run: `cd web && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add web/src/onboarding/types.ts web/src/onboarding/steps.ts
git commit -m "feat: define onboarding step types and configuration"
```

---

## Task 5: Frontend API Client Updates

**Files:**
- Modify: `web/src/api/client.ts`

- [ ] **Step 1: Add onboarding field to UserResponse**

In `web/src/api/client.ts`, update the `UserResponse` interface (line 54-57):

```typescript
export interface UserResponse {
  id: number; email: string; displayName: string; avatarUrl?: string
  role: string; language?: string; onboarding?: string; createdAt: string
}
```

- [ ] **Step 2: Add updateOnboarding API method**

In `web/src/api/client.ts`, inside the `user` namespace (after line 289, `delete` method):

```typescript
  user: {
    me: () => apiRequest<UserResponse>('/api/users/me'),
    update: (data: { displayName?: string; language?: string }) =>
      apiRequest<UserResponse>('/api/users/me', { method: 'PUT', body: JSON.stringify(data) }),
    delete: () => apiRequest<void>('/api/users/me', { method: 'DELETE' }),
    updateOnboarding: (data: { completedSteps?: string[]; dismissed?: boolean }) =>
      apiRequest<UserResponse>('/api/users/me/onboarding', { method: 'PUT', body: JSON.stringify(data) }),
  },
```

- [ ] **Step 3: Verify types compile**

Run: `cd web && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add web/src/api/client.ts
git commit -m "feat: add onboarding field and API method to client"
```

---

## Task 6: Onboarding Context (State Management)

**Files:**
- Create: `web/src/onboarding/OnboardingContext.tsx`

- [ ] **Step 1: Create the context**

Create `web/src/onboarding/OnboardingContext.tsx`:

```typescript
import { createContext, useContext, useState, useCallback, useEffect, useMemo, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../auth/AuthContext'
import { api } from '../api/client'
import { ONBOARDING_STEPS, SECTIONS, getStepsForSection } from './steps'
import type { OnboardingState, OnboardingSection, PageTooltipConfig } from './types'

interface OnboardingContextValue {
  /** Is onboarding active (not dismissed, not all complete) */
  isActive: boolean
  /** Total completed / total steps */
  completedCount: number
  totalCount: number
  /** Check if a specific step is complete */
  isStepComplete: (stepId: string) => boolean
  /** Get completed count for a section */
  sectionProgress: (section: OnboardingSection) => { completed: number; total: number }
  /** Mark a step as complete */
  completeStep: (stepId: string) => void
  /** Start a checklist item: navigate to page + activate tooltips */
  startStep: (stepId: string) => void
  /** Dismiss onboarding for session */
  minimizeForSession: () => void
  /** Dismiss onboarding permanently */
  dismissPermanently: () => void
  /** Whether the drawer is open */
  drawerOpen: boolean
  setDrawerOpen: (open: boolean) => void
  /** Active tooltip tour */
  activeTour: PageTooltipConfig | null
  clearActiveTour: () => void
  /** Whether FAB is hidden for this session */
  minimized: boolean
}

const OnboardingContext = createContext<OnboardingContextValue | null>(null)

const STORAGE_KEY = 'verdant_onboarding_tooltips_seen'

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
  const location = useLocation()

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

  // Auto-complete visit-type steps based on current route
  useEffect(() => {
    const visitSteps = ONBOARDING_STEPS.filter(s => s.completionType === 'visit')
    for (const step of visitSteps) {
      if (!state.completedSteps.includes(step.id) && location.pathname === step.route) {
        completeStep(step.id)
      }
    }
  }, [location.pathname]) // eslint-disable-line react-hooks/exhaustive-deps

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

  const startStep = useCallback((stepId: string) => {
    const step = ONBOARDING_STEPS.find(s => s.id === stepId)
    if (!step) return
    setDrawerOpen(false)
    navigate(step.route)
    // Tooltip tour will be set by the tooltipConfigs lookup after navigation
    // We import it lazily to avoid circular deps
    import('./tooltipConfigs').then(({ getTooltipConfig }) => {
      const config = getTooltipConfig(stepId)
      if (config) {
        // Small delay to let the page render
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
  }), [isActive, completedCount, totalCount, isStepComplete, sectionProgress, completeStep,
       startStep, minimizeForSession, dismissPermanently, drawerOpen, activeTour, clearActiveTour, minimized])

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
```

- [ ] **Step 2: Verify types compile**

Run: `cd web && npx tsc --noEmit`
Expected: Errors about missing `tooltipConfigs` file (expected — we create it in Task 9)

- [ ] **Step 3: Commit**

```bash
git add web/src/onboarding/OnboardingContext.tsx
git commit -m "feat: add OnboardingContext for state management"
```

---

## Task 7: Floating Action Button (Progress Ring)

**Files:**
- Create: `web/src/onboarding/OnboardingFab.tsx`

- [ ] **Step 1: Create the FAB component**

Create `web/src/onboarding/OnboardingFab.tsx`:

```typescript
import { useTranslation } from 'react-i18next'
import { useOnboarding } from './OnboardingContext'

export function OnboardingFab() {
  const { isActive, completedCount, totalCount, drawerOpen, setDrawerOpen, minimized } = useOnboarding()
  const { t } = useTranslation()

  if (!isActive || minimized) return null

  const progress = completedCount / totalCount
  const radius = 20
  const circumference = 2 * Math.PI * radius
  const offset = circumference - progress * circumference

  return (
    <button
      onClick={() => setDrawerOpen(!drawerOpen)}
      className="fixed bottom-6 right-6 z-40 w-14 h-14 rounded-full bg-white border border-divider shadow-lg hover:shadow-xl transition-shadow duration-200 flex items-center justify-center group"
      aria-label={t('onboarding.fab.label')}
      title={t('onboarding.fab.title', { completed: completedCount, total: totalCount })}
    >
      {/* Progress ring */}
      <svg className="absolute inset-0 w-14 h-14 -rotate-90" viewBox="0 0 56 56">
        {/* Background circle */}
        <circle
          cx="28" cy="28" r={radius}
          fill="none"
          stroke="currentColor"
          strokeWidth="3"
          className="text-divider"
        />
        {/* Progress arc */}
        <circle
          cx="28" cy="28" r={radius}
          fill="none"
          stroke="currentColor"
          strokeWidth="3"
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          className="text-accent transition-all duration-500"
        />
      </svg>
      {/* Center icon */}
      <span className="text-lg relative z-10 group-hover:scale-110 transition-transform duration-150">🌿</span>
    </button>
  )
}
```

- [ ] **Step 2: Verify types compile**

Run: `cd web && npx tsc --noEmit`
Expected: No errors (or only the expected tooltipConfigs error from Task 6)

- [ ] **Step 3: Commit**

```bash
git add web/src/onboarding/OnboardingFab.tsx
git commit -m "feat: add floating action button with progress ring"
```

---

## Task 8: Onboarding Drawer (Sectioned Checklist)

**Files:**
- Create: `web/src/onboarding/OnboardingDrawer.tsx`

- [ ] **Step 1: Create the drawer component**

Create `web/src/onboarding/OnboardingDrawer.tsx`:

```typescript
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useOnboarding } from './OnboardingContext'
import { SECTIONS, getStepsForSection } from './steps'

export function OnboardingDrawer() {
  const { t } = useTranslation()
  const {
    drawerOpen, setDrawerOpen, isStepComplete, sectionProgress,
    startStep, completedCount, totalCount,
    minimizeForSession, dismissPermanently,
  } = useOnboarding()
  const [expandedSection, setExpandedSection] = useState<string>(SECTIONS[0].id)
  const [showDismissMenu, setShowDismissMenu] = useState(false)

  const allComplete = completedCount >= totalCount

  if (!drawerOpen) return null

  return (
    <>
      {/* Backdrop */}
      <div className="fixed inset-0 z-40 bg-black/10" onClick={() => setDrawerOpen(false)} />

      {/* Drawer */}
      <div className="fixed right-0 top-0 bottom-0 z-50 w-80 max-w-[90vw] bg-white border-l border-divider shadow-xl flex flex-col">
        {/* Header */}
        <div className="px-4 py-4 border-b border-divider flex items-center justify-between">
          <div>
            <h2 className="font-semibold text-base">{t('onboarding.drawer.title')}</h2>
            <p className="text-xs text-text-secondary mt-0.5">
              {t('onboarding.drawer.progress', { completed: completedCount, total: totalCount })}
            </p>
          </div>
          <div className="flex items-center gap-1">
            <div className="relative">
              <button
                onClick={() => setShowDismissMenu(!showDismissMenu)}
                className="text-text-muted hover:text-text-secondary p-1 rounded-lg hover:bg-surface transition-colors text-sm"
                aria-label={t('onboarding.drawer.options')}
              >
                ···
              </button>
              {showDismissMenu && (
                <div className="absolute right-0 top-8 bg-white border border-divider rounded-xl shadow-lg py-1 w-48 z-10">
                  <button
                    onClick={() => { minimizeForSession(); setShowDismissMenu(false) }}
                    className="w-full text-left px-3 py-2 text-sm text-text-secondary hover:bg-surface transition-colors"
                  >
                    {t('onboarding.drawer.hideSession')}
                  </button>
                  <button
                    onClick={() => { dismissPermanently(); setShowDismissMenu(false) }}
                    className="w-full text-left px-3 py-2 text-sm text-error hover:bg-surface transition-colors"
                  >
                    {t('onboarding.drawer.dismissForever')}
                  </button>
                </div>
              )}
            </div>
            <button
              onClick={() => setDrawerOpen(false)}
              className="text-text-muted hover:text-text-secondary p-1 rounded-lg hover:bg-surface transition-colors"
              aria-label={t('common.close')}
            >
              ✕
            </button>
          </div>
        </div>

        {/* Progress bar */}
        <div className="px-4 pt-3">
          <div className="h-1.5 bg-surface rounded-full overflow-hidden">
            <div
              className="h-full bg-accent rounded-full transition-all duration-500"
              style={{ width: `${(completedCount / totalCount) * 100}%` }}
            />
          </div>
        </div>

        {/* Sections */}
        <div className="flex-1 overflow-y-auto px-4 py-3 space-y-2">
          {allComplete ? (
            <div className="text-center py-8">
              <p className="text-4xl mb-3">🎉</p>
              <p className="font-semibold text-lg">{t('onboarding.drawer.allComplete')}</p>
              <p className="text-sm text-text-secondary mt-1">{t('onboarding.drawer.allCompleteHint')}</p>
              <button
                onClick={dismissPermanently}
                className="btn-primary mt-4"
              >
                {t('onboarding.drawer.finish')}
              </button>
            </div>
          ) : (
            SECTIONS.map(section => {
              const { completed, total } = sectionProgress(section.id)
              const steps = getStepsForSection(section.id)
              const isExpanded = expandedSection === section.id

              return (
                <div key={section.id} className="border border-divider rounded-xl overflow-hidden">
                  <button
                    onClick={() => setExpandedSection(isExpanded ? '' : section.id)}
                    className="w-full flex items-center gap-2.5 px-3 py-2.5 hover:bg-surface/50 transition-colors"
                  >
                    <span className="text-base">{section.icon}</span>
                    <span className="flex-1 text-left text-sm font-medium">{t(section.titleKey)}</span>
                    <span className="text-xs text-text-secondary">{completed}/{total}</span>
                    <span className={`text-xs text-text-muted transition-transform duration-200 ${isExpanded ? 'rotate-180' : ''}`}>
                      ▾
                    </span>
                  </button>
                  {isExpanded && (
                    <div className="border-t border-divider/50 px-3 py-1.5">
                      {steps.map(step => {
                        const complete = isStepComplete(step.id)
                        return (
                          <button
                            key={step.id}
                            onClick={() => !complete && startStep(step.id)}
                            disabled={complete}
                            className={`w-full flex items-center gap-2.5 px-2 py-2 rounded-lg text-sm transition-colors ${
                              complete
                                ? 'text-text-muted'
                                : 'text-text-primary hover:bg-accent-light cursor-pointer'
                            }`}
                          >
                            <span className={`w-5 h-5 rounded-full border-2 flex items-center justify-center shrink-0 text-xs ${
                              complete
                                ? 'border-accent bg-accent text-white'
                                : 'border-divider'
                            }`}>
                              {complete && '✓'}
                            </span>
                            <span className={complete ? 'line-through' : ''}>{t(`onboarding.steps.${step.id}`)}</span>
                          </button>
                        )
                      })}
                    </div>
                  )}
                </div>
              )
            })
          )}
        </div>
      </div>
    </>
  )
}
```

- [ ] **Step 2: Verify types compile**

Run: `cd web && npx tsc --noEmit`
Expected: No errors (or only the expected tooltipConfigs error)

- [ ] **Step 3: Commit**

```bash
git add web/src/onboarding/OnboardingDrawer.tsx
git commit -m "feat: add onboarding drawer with sectioned checklist"
```

---

## Task 9: Tooltip Tour System

**Files:**
- Create: `web/src/onboarding/tooltipConfigs.ts`
- Create: `web/src/onboarding/useTooltipTour.ts`
- Create: `web/src/onboarding/OnboardingOverlay.tsx`
- Create: `web/src/onboarding/OnboardingTooltip.tsx`

- [ ] **Step 1: Create tooltip configurations**

Create `web/src/onboarding/tooltipConfigs.ts`:

```typescript
import type { PageTooltipConfig } from './types'

const configs: PageTooltipConfig[] = [
  {
    stepId: 'create_season',
    tooltips: [
      { targetSelector: '[data-onboarding="season-form"]', titleKey: 'onboarding.tooltips.createSeason.title', descriptionKey: 'onboarding.tooltips.createSeason.description' },
    ],
  },
  {
    stepId: 'create_garden',
    tooltips: [
      { targetSelector: '[data-onboarding="garden-form"]', titleKey: 'onboarding.tooltips.createGarden.title', descriptionKey: 'onboarding.tooltips.createGarden.description' },
    ],
  },
  {
    stepId: 'create_bed',
    tooltips: [
      { targetSelector: '[data-onboarding="garden-card"]', titleKey: 'onboarding.tooltips.createBed.step1.title', descriptionKey: 'onboarding.tooltips.createBed.step1.description' },
      { targetSelector: '[data-onboarding="add-bed-btn"]', titleKey: 'onboarding.tooltips.createBed.step2.title', descriptionKey: 'onboarding.tooltips.createBed.step2.description' },
    ],
  },
  {
    stepId: 'browse_species',
    tooltips: [
      { targetSelector: '[data-onboarding="species-list"]', titleKey: 'onboarding.tooltips.browseSpecies.title', descriptionKey: 'onboarding.tooltips.browseSpecies.description' },
    ],
  },
  {
    stepId: 'add_seeds',
    tooltips: [
      { targetSelector: '[data-onboarding="add-seed-btn"]', titleKey: 'onboarding.tooltips.addSeeds.step1.title', descriptionKey: 'onboarding.tooltips.addSeeds.step1.description' },
      { targetSelector: '[data-onboarding="seed-form"]', titleKey: 'onboarding.tooltips.addSeeds.step2.title', descriptionKey: 'onboarding.tooltips.addSeeds.step2.description' },
    ],
  },
  {
    stepId: 'sow_seeds',
    tooltips: [
      { targetSelector: '[data-onboarding="sow-species"]', titleKey: 'onboarding.tooltips.sowSeeds.step1.title', descriptionKey: 'onboarding.tooltips.sowSeeds.step1.description' },
      { targetSelector: '[data-onboarding="sow-location"]', titleKey: 'onboarding.tooltips.sowSeeds.step2.title', descriptionKey: 'onboarding.tooltips.sowSeeds.step2.description' },
      { targetSelector: '[data-onboarding="sow-submit"]', titleKey: 'onboarding.tooltips.sowSeeds.step3.title', descriptionKey: 'onboarding.tooltips.sowSeeds.step3.description' },
    ],
  },
  {
    stepId: 'pot_up',
    tooltips: [
      { targetSelector: '[data-onboarding="plant-actions"]', titleKey: 'onboarding.tooltips.potUp.title', descriptionKey: 'onboarding.tooltips.potUp.description' },
    ],
  },
  {
    stepId: 'plant_out',
    tooltips: [
      { targetSelector: '[data-onboarding="plant-actions"]', titleKey: 'onboarding.tooltips.plantOut.title', descriptionKey: 'onboarding.tooltips.plantOut.description' },
    ],
  },
  {
    stepId: 'record_harvest',
    tooltips: [
      { targetSelector: '[data-onboarding="plant-actions"]', titleKey: 'onboarding.tooltips.recordHarvest.title', descriptionKey: 'onboarding.tooltips.recordHarvest.description' },
    ],
  },
  {
    stepId: 'add_customer',
    tooltips: [
      { targetSelector: '[data-onboarding="add-customer-btn"]', titleKey: 'onboarding.tooltips.addCustomer.title', descriptionKey: 'onboarding.tooltips.addCustomer.description' },
    ],
  },
  {
    stepId: 'create_bouquet',
    tooltips: [
      { targetSelector: '[data-onboarding="add-bouquet-btn"]', titleKey: 'onboarding.tooltips.createBouquet.title', descriptionKey: 'onboarding.tooltips.createBouquet.description' },
    ],
  },
  {
    stepId: 'create_task',
    tooltips: [
      { targetSelector: '[data-onboarding="task-form"]', titleKey: 'onboarding.tooltips.createTask.title', descriptionKey: 'onboarding.tooltips.createTask.description' },
    ],
  },
  {
    stepId: 'setup_succession',
    tooltips: [
      { targetSelector: '[data-onboarding="add-succession-btn"]', titleKey: 'onboarding.tooltips.setupSuccession.title', descriptionKey: 'onboarding.tooltips.setupSuccession.description' },
    ],
  },
  {
    stepId: 'set_target',
    tooltips: [
      { targetSelector: '[data-onboarding="add-target-btn"]', titleKey: 'onboarding.tooltips.setTarget.title', descriptionKey: 'onboarding.tooltips.setTarget.description' },
    ],
  },
  {
    stepId: 'view_calendar',
    tooltips: [
      { targetSelector: '[data-onboarding="calendar-view"]', titleKey: 'onboarding.tooltips.viewCalendar.title', descriptionKey: 'onboarding.tooltips.viewCalendar.description' },
    ],
  },
  {
    stepId: 'start_trial',
    tooltips: [
      { targetSelector: '[data-onboarding="add-trial-btn"]', titleKey: 'onboarding.tooltips.startTrial.title', descriptionKey: 'onboarding.tooltips.startTrial.description' },
    ],
  },
  {
    stepId: 'log_pest',
    tooltips: [
      { targetSelector: '[data-onboarding="add-pest-btn"]', titleKey: 'onboarding.tooltips.logPest.title', descriptionKey: 'onboarding.tooltips.logPest.description' },
    ],
  },
  {
    stepId: 'view_analytics',
    tooltips: [
      { targetSelector: '[data-onboarding="analytics-view"]', titleKey: 'onboarding.tooltips.viewAnalytics.title', descriptionKey: 'onboarding.tooltips.viewAnalytics.description' },
    ],
  },
  {
    stepId: 'create_listing',
    tooltips: [
      { targetSelector: '[data-onboarding="add-listing-btn"]', titleKey: 'onboarding.tooltips.createListing.title', descriptionKey: 'onboarding.tooltips.createListing.description' },
    ],
  },
]

export function getTooltipConfig(stepId: string): PageTooltipConfig | null {
  return configs.find(c => c.stepId === stepId) ?? null
}
```

- [ ] **Step 2: Create the tooltip tour hook**

Create `web/src/onboarding/useTooltipTour.ts`:

```typescript
import { useState, useEffect, useCallback, useRef } from 'react'
import type { PageTooltipConfig, TooltipStep } from './types'

interface TooltipTourState {
  currentIndex: number
  totalSteps: number
  currentTooltip: TooltipStep | null
  targetElement: HTMLElement | null
  next: () => void
  back: () => void
  skip: () => void
}

export function useTooltipTour(config: PageTooltipConfig | null, onComplete: () => void): TooltipTourState {
  const [currentIndex, setCurrentIndex] = useState(0)
  const onCompleteRef = useRef(onComplete)
  onCompleteRef.current = onComplete
  const [targetElement, setTargetElement] = useState<HTMLElement | null>(null)

  // Reset index when config changes
  useEffect(() => {
    setCurrentIndex(0)
  }, [config?.stepId])

  // Find target element when index or config changes
  useEffect(() => {
    if (!config || currentIndex >= config.tooltips.length) {
      setTargetElement(null)
      return
    }

    const tooltip = config.tooltips[currentIndex]

    // Retry finding element a few times (page may still be rendering)
    let attempts = 0
    const maxAttempts = 10
    const interval = setInterval(() => {
      const el = document.querySelector<HTMLElement>(tooltip.targetSelector)
      if (el) {
        setTargetElement(el)
        clearInterval(interval)
      } else if (++attempts >= maxAttempts) {
        // Element not found — skip this tooltip
        setTargetElement(null)
        clearInterval(interval)
      }
    }, 200)

    return () => clearInterval(interval)
  }, [config, currentIndex])

  const next = useCallback(() => {
    if (!config) return
    if (currentIndex >= config.tooltips.length - 1) {
      onCompleteRef.current()
    } else {
      setCurrentIndex(i => i + 1)
    }
  }, [config, currentIndex])

  const back = useCallback(() => {
    setCurrentIndex(i => Math.max(0, i - 1))
  }, [])

  const skip = useCallback(() => {
    onCompleteRef.current()
  }, [])

  return {
    currentIndex,
    totalSteps: config?.tooltips.length ?? 0,
    currentTooltip: config?.tooltips[currentIndex] ?? null,
    targetElement,
    next,
    back,
    skip,
  }
}
```

- [ ] **Step 3: Create the overlay component**

Create `web/src/onboarding/OnboardingOverlay.tsx`:

```typescript
import { useEffect, useState } from 'react'

interface Props {
  targetElement: HTMLElement | null
  onClick: () => void
}

export function OnboardingOverlay({ targetElement, onClick }: Props) {
  const [rect, setRect] = useState<DOMRect | null>(null)

  useEffect(() => {
    if (!targetElement) { setRect(null); return }

    const update = () => setRect(targetElement.getBoundingClientRect())
    update()

    // Update on scroll/resize
    window.addEventListener('scroll', update, true)
    window.addEventListener('resize', update)
    return () => {
      window.removeEventListener('scroll', update, true)
      window.removeEventListener('resize', update)
    }
  }, [targetElement])

  if (!rect) return null

  const padding = 8

  return (
    <div className="fixed inset-0 z-50" onClick={onClick}>
      <svg className="absolute inset-0 w-full h-full">
        <defs>
          <mask id="onboarding-mask">
            <rect x="0" y="0" width="100%" height="100%" fill="white" />
            <rect
              x={rect.left - padding}
              y={rect.top - padding}
              width={rect.width + padding * 2}
              height={rect.height + padding * 2}
              rx="8"
              fill="black"
            />
          </mask>
        </defs>
        <rect
          x="0" y="0" width="100%" height="100%"
          fill="rgba(0,0,0,0.3)"
          mask="url(#onboarding-mask)"
        />
      </svg>
      {/* Transparent clickable area over the cutout so clicks pass through to the element */}
      <div
        className="absolute"
        style={{
          left: rect.left - padding,
          top: rect.top - padding,
          width: rect.width + padding * 2,
          height: rect.height + padding * 2,
        }}
        onClick={(e) => e.stopPropagation()}
      />
    </div>
  )
}
```

- [ ] **Step 4: Create the tooltip component**

Create `web/src/onboarding/OnboardingTooltip.tsx`:

```typescript
import { useRef } from 'react'
import { useFloating, offset, flip, shift, arrow, autoUpdate } from '@floating-ui/react'
import { useTranslation } from 'react-i18next'

interface Props {
  targetElement: HTMLElement | null
  titleKey: string
  descriptionKey: string
  currentIndex: number
  totalSteps: number
  onNext: () => void
  onBack: () => void
  onSkip: () => void
}

export function OnboardingTooltip({
  targetElement, titleKey, descriptionKey,
  currentIndex, totalSteps, onNext, onBack, onSkip,
}: Props) {
  const { t } = useTranslation()
  const arrowRef = useRef<HTMLDivElement>(null)

  const { refs, floatingStyles, middlewareData } = useFloating({
    elements: { reference: targetElement },
    placement: 'bottom',
    middleware: [
      offset(12),
      flip({ fallbackPlacements: ['top', 'right', 'left'] }),
      shift({ padding: 16 }),
      arrow({ element: arrowRef }),
    ],
    whileElementsMounted: autoUpdate,
  })

  if (!targetElement) return null

  const isFirst = currentIndex === 0
  const isLast = currentIndex === totalSteps - 1

  return (
    <div
      ref={refs.setFloating}
      style={floatingStyles}
      className="z-[60] bg-white rounded-xl border border-divider shadow-xl p-4 w-72 max-w-[90vw]"
      onClick={(e) => e.stopPropagation()}
    >
      {/* Arrow */}
      <div
        ref={arrowRef}
        className="absolute w-3 h-3 bg-white border border-divider rotate-45"
        style={{
          left: middlewareData.arrow?.x,
          top: middlewareData.arrow?.y != null ? middlewareData.arrow.y : undefined,
          ...(floatingStyles.top && parseFloat(String(floatingStyles.top)) > (targetElement?.getBoundingClientRect().top ?? 0)
            ? { top: -6, borderBottom: 'none', borderRight: 'none' }
            : { bottom: -6, borderTop: 'none', borderLeft: 'none' }),
        }}
      />

      {/* Step counter */}
      {totalSteps > 1 && (
        <p className="text-xs text-text-muted mb-1.5">
          {t('onboarding.tooltip.stepOf', { current: currentIndex + 1, total: totalSteps })}
        </p>
      )}

      {/* Content */}
      <h3 className="font-semibold text-sm mb-1">{t(titleKey)}</h3>
      <p className="text-sm text-text-secondary">{t(descriptionKey)}</p>

      {/* Actions */}
      <div className="flex items-center justify-between mt-3 pt-2 border-t border-divider/50">
        <button
          onClick={onSkip}
          className="text-xs text-text-muted hover:text-text-secondary transition-colors"
        >
          {t('onboarding.tooltip.skip')}
        </button>
        <div className="flex gap-2">
          {!isFirst && (
            <button
              onClick={onBack}
              className="text-xs px-2.5 py-1 rounded-lg text-text-secondary hover:bg-surface transition-colors"
            >
              {t('onboarding.tooltip.back')}
            </button>
          )}
          <button
            onClick={onNext}
            className="text-xs px-2.5 py-1.5 rounded-lg bg-accent text-white hover:bg-accent-hover transition-colors font-medium"
          >
            {isLast ? t('onboarding.tooltip.done') : t('onboarding.tooltip.next')}
          </button>
        </div>
      </div>
    </div>
  )
}
```

- [ ] **Step 5: Verify types compile**

Run: `cd web && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 6: Commit**

```bash
git add web/src/onboarding/tooltipConfigs.ts web/src/onboarding/useTooltipTour.ts web/src/onboarding/OnboardingOverlay.tsx web/src/onboarding/OnboardingTooltip.tsx
git commit -m "feat: add tooltip tour system with overlay and floating positioning"
```

---

## Task 10: Wire Everything Together

**Files:**
- Modify: `web/src/main.tsx`
- Modify: `web/src/components/Layout.tsx`

- [ ] **Step 1: Create a combined onboarding root component**

We need a component that renders the FAB, drawer, overlay, and tooltip together, and connects them via the tour hook. Add this to the bottom of `web/src/onboarding/OnboardingContext.tsx` (before the closing of the file) OR create a small wrapper. Since the context already manages `activeTour`, the cleanest approach is a combined renderer.

Create `web/src/onboarding/OnboardingRoot.tsx`:

```typescript
import { OnboardingFab } from './OnboardingFab'
import { OnboardingDrawer } from './OnboardingDrawer'
import { OnboardingOverlay } from './OnboardingOverlay'
import { OnboardingTooltip } from './OnboardingTooltip'
import { useOnboarding } from './OnboardingContext'
import { useTooltipTour } from './useTooltipTour'

export function OnboardingRoot() {
  const { activeTour, clearActiveTour, completeStep, isActive } = useOnboarding()

  const tour = useTooltipTour(activeTour, () => {
    if (activeTour) {
      completeStep(activeTour.stepId)
    }
    clearActiveTour()
  })

  if (!isActive) return null

  return (
    <>
      <OnboardingFab />
      <OnboardingDrawer />
      {activeTour && tour.targetElement && (
        <>
          <OnboardingOverlay targetElement={tour.targetElement} onClick={clearActiveTour} />
          <OnboardingTooltip
            targetElement={tour.targetElement}
            titleKey={tour.currentTooltip!.titleKey}
            descriptionKey={tour.currentTooltip!.descriptionKey}
            currentIndex={tour.currentIndex}
            totalSteps={tour.totalSteps}
            onNext={tour.next}
            onBack={tour.back}
            onSkip={clearActiveTour}
          />
        </>
      )}
    </>
  )
}
```

- [ ] **Step 2: Add OnboardingProvider to main.tsx**

In `web/src/main.tsx`, add the OnboardingProvider inside the existing provider tree. The provider needs to be inside `BrowserRouter` (for navigation) and `QueryClientProvider` (for query cache access) and `AuthProvider` (for user data):

```typescript
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { OnboardingProvider } from './onboarding/OnboardingContext'
import { App } from './App'
import './i18n'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => {
        if (error instanceof Error && 'status' in error) return false
        return failureCount < 2
      },
      staleTime: 30_000,
    },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <OnboardingProvider>
            <App />
          </OnboardingProvider>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
```

- [ ] **Step 3: Add OnboardingRoot to Layout**

In `web/src/components/Layout.tsx`, import and render `OnboardingRoot` at the end of the Layout component's return. Add it after the closing `</div>` of the main container but before the fragment/return closes:

Add import:
```typescript
import { OnboardingRoot } from '../onboarding/OnboardingRoot'
```

In the `Layout` component return, add `<OnboardingRoot />` right before the closing tag:

```typescript
  return (
    <div className="min-h-screen flex bg-surface">
      {/* ... existing sidebar, drawer, main content ... */}
    </div>
    {/* This won't work — we need a fragment */}
  )
```

Actually, wrap the return in a fragment:

```typescript
  return (
    <>
      <div className="min-h-screen flex bg-surface">
        {/* Desktop sidebar */}
        <div className="hidden md:flex md:flex-col md:w-56 md:fixed md:inset-y-0 md:left-0 z-20">
          <SidebarContent />
        </div>

        {/* Mobile drawer overlay */}
        {drawerOpen && (
          <div className="fixed inset-0 z-30 md:hidden">
            <div className="absolute inset-0 bg-black/20" onClick={() => setDrawerOpen(false)} />
            <div className="absolute left-0 top-0 bottom-0 w-56">
              <SidebarContent onClose={() => setDrawerOpen(false)} />
            </div>
          </div>
        )}

        {/* Main content */}
        <div className="flex-1 md:ml-56 flex flex-col min-h-screen">
          {/* Mobile top bar */}
          <header className="md:hidden flex items-center gap-3 px-4 py-3 bg-sidebar border-b border-divider sticky top-0 z-10">
            <button
              onClick={() => setDrawerOpen(true)}
              className="text-text-secondary hover:text-text-primary transition-colors p-0.5"
              aria-label="Open menu"
            >
              <svg width="18" height="18" viewBox="0 0 18 18" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round">
                <line x1="2" y1="4.5" x2="16" y2="4.5" />
                <line x1="2" y1="9" x2="16" y2="9" />
                <line x1="2" y1="13.5" x2="16" y2="13.5" />
              </svg>
            </button>
            <span className="font-semibold text-text-primary text-sm">🌿 Verdant</span>
          </header>

          <main className="flex-1 w-full px-4 md:px-8 py-6 flex flex-col">
            <Outlet />
          </main>
        </div>
      </div>
      <OnboardingRoot />
    </>
  )
```

- [ ] **Step 4: Verify types compile**

Run: `cd web && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 5: Commit**

```bash
git add web/src/onboarding/OnboardingRoot.tsx web/src/main.tsx web/src/components/Layout.tsx
git commit -m "feat: wire onboarding provider and root into app"
```

---

## Task 11: Add data-onboarding Attributes to Existing Pages

**Files:**
- Modify: Multiple page files (add `data-onboarding` attributes to target elements)

This task adds the `data-onboarding` attributes that the tooltip system targets. Each attribute is a simple addition to an existing JSX element. You need to read each page, find the right element, and add the attribute.

- [ ] **Step 1: Add attributes to season, garden, and bed pages**

Read each file first, then add `data-onboarding` attributes:

**`web/src/pages/SeasonList.tsx`** — Find the form or "add season" section and add:
```
data-onboarding="season-form"
```

**`web/src/pages/GardenForm.tsx`** — Find the form wrapper element and add:
```
data-onboarding="garden-form"
```

**`web/src/pages/Dashboard.tsx`** — Find the first garden card Link element and add:
```
data-onboarding="garden-card"
```

**`web/src/pages/GardenDetail.tsx`** — Find the "add bed" button and add:
```
data-onboarding="add-bed-btn"
```

- [ ] **Step 2: Add attributes to species, seeds, and sow pages**

**`web/src/pages/SpeciesList.tsx`** — Find the species list container and add:
```
data-onboarding="species-list"
```

**`web/src/pages/SeedInventory.tsx`** — Find the "add seed" button/FAB and add:
```
data-onboarding="add-seed-btn"
```
Also find the seed form (if inline) and add:
```
data-onboarding="seed-form"
```

**`web/src/pages/SowActivity.tsx`** — Find the species selector, location selector, and submit button and add:
```
data-onboarding="sow-species"
data-onboarding="sow-location"
data-onboarding="sow-submit"
```

- [ ] **Step 3: Add attributes to plant action, customer, bouquet, and task pages**

**`web/src/pages/PlantedSpeciesList.tsx`** or **`web/src/pages/PlantDetail.tsx`** — Find the action buttons area and add:
```
data-onboarding="plant-actions"
```

**`web/src/pages/CustomerList.tsx`** — Find the "add customer" button and add:
```
data-onboarding="add-customer-btn"
```

**`web/src/pages/BouquetRecipes.tsx`** — Find the "add bouquet" button and add:
```
data-onboarding="add-bouquet-btn"
```

**`web/src/pages/TaskForm.tsx`** — Find the form wrapper and add:
```
data-onboarding="task-form"
```

- [ ] **Step 4: Add attributes to planning and advanced pages**

**`web/src/pages/SuccessionSchedules.tsx`** — Find the "add" button and add:
```
data-onboarding="add-succession-btn"
```

**`web/src/pages/ProductionTargets.tsx`** — Find the "add" button and add:
```
data-onboarding="add-target-btn"
```

**`web/src/pages/CropCalendar.tsx`** — Find the main calendar container and add:
```
data-onboarding="calendar-view"
```

**`web/src/pages/VarietyTrials.tsx`** — Find the "add" button and add:
```
data-onboarding="add-trial-btn"
```

**`web/src/pages/PestDiseaseLog.tsx`** — Find the "add" button and add:
```
data-onboarding="add-pest-btn"
```

**`web/src/pages/Analytics.tsx`** — Find the main analytics container and add:
```
data-onboarding="analytics-view"
```

**`web/src/pages/MyListings.tsx`** — Find the "add listing" button and add:
```
data-onboarding="add-listing-btn"
```

- [ ] **Step 5: Verify types compile**

Run: `cd web && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 6: Commit**

```bash
git add web/src/pages/
git commit -m "feat: add data-onboarding attributes to page elements for tooltip targeting"
```

---

## Task 12: Mutation Completion Tracking

**Files:**
- Modify: `web/src/onboarding/OnboardingContext.tsx`

The onboarding context needs to listen for TanStack Query mutation successes to auto-complete action steps. TanStack Query provides a global `MutationCache` that can be configured with an `onSuccess` callback.

- [ ] **Step 1: Add mutation cache listener to main.tsx**

In `web/src/main.tsx`, configure the `QueryClient` with a `MutationCache` that fires a custom event on mutation success:

```typescript
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider, MutationCache } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { OnboardingProvider } from './onboarding/OnboardingContext'
import { App } from './App'
import './i18n'
import './index.css'

const mutationCache = new MutationCache({
  onSuccess: (_data, _variables, _context, mutation) => {
    window.dispatchEvent(new CustomEvent('onboarding:mutation-success', {
      detail: { mutationKey: mutation.options.mutationKey },
    }))
  },
})

const queryClient = new QueryClient({
  mutationCache,
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => {
        if (error instanceof Error && 'status' in error) return false
        return failureCount < 2
      },
      staleTime: 30_000,
    },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <OnboardingProvider>
            <App />
          </OnboardingProvider>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
```

- [ ] **Step 2: Listen for query invalidations in OnboardingContext**

In `web/src/onboarding/OnboardingContext.tsx`, add a `useEffect` that subscribes to the query client's cache to detect when relevant queries get new data. This is more reliable than listening to mutation keys (since many mutations don't set a mutationKey). Instead, watch for query cache changes:

Add inside `OnboardingProvider`, after the existing `useEffect` hooks:

```typescript
  // Listen for query cache updates to detect completed mutation steps
  useEffect(() => {
    const unsubscribe = queryClient.getQueryCache().subscribe((event) => {
      if (event.type !== 'updated' || event.action.type !== 'success') return

      const queryKey = event.query.queryKey
      for (const step of ONBOARDING_STEPS) {
        if (step.completionType !== 'mutation') continue
        if (state.completedSteps.includes(step.id)) continue
        if (!step.mutationQueryKeys) continue

        const matches = step.mutationQueryKeys.some(mk =>
          mk.length <= queryKey.length && mk.every((k, i) => k === queryKey[i])
        )
        if (matches) {
          completeStep(step.id)
        }
      }
    })

    return unsubscribe
  }, [queryClient, state.completedSteps, completeStep])
```

- [ ] **Step 3: Verify types compile**

Run: `cd web && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add web/src/main.tsx web/src/onboarding/OnboardingContext.tsx
git commit -m "feat: add mutation completion tracking via query cache subscription"
```

---

## Task 13: i18n Translation Keys

**Files:**
- Modify: `web/src/i18n/en.json`
- Modify: `web/src/i18n/sv.json`

- [ ] **Step 1: Add English translations**

Add the following keys to `web/src/i18n/en.json` (add an `"onboarding"` top-level key):

```json
"onboarding": {
  "fab": {
    "label": "Setup guide",
    "title": "Setup guide — {{completed}}/{{total}} complete"
  },
  "drawer": {
    "title": "Setup Guide",
    "progress": "{{completed}} of {{total}} complete",
    "options": "Options",
    "hideSession": "Hide for now",
    "dismissForever": "Don't show again",
    "allComplete": "You're all set!",
    "allCompleteHint": "You've explored everything Verdant has to offer.",
    "finish": "Close guide"
  },
  "sections": {
    "gettingStarted": "Getting Started",
    "growing": "Growing",
    "harvestingSales": "Harvesting & Sales",
    "planning": "Planning",
    "advanced": "Advanced"
  },
  "steps": {
    "create_season": "Create a season",
    "create_garden": "Create a garden",
    "create_bed": "Create a bed in your garden",
    "browse_species": "Browse species",
    "add_seeds": "Add seeds to inventory",
    "sow_seeds": "Sow your first seeds",
    "pot_up": "Pot up a plant",
    "plant_out": "Plant out to a bed",
    "record_harvest": "Record a harvest",
    "add_customer": "Add a customer",
    "create_bouquet": "Create a bouquet recipe",
    "create_task": "Create a scheduled task",
    "setup_succession": "Set up a succession schedule",
    "set_target": "Set a production target",
    "view_calendar": "View the crop calendar",
    "start_trial": "Start a variety trial",
    "log_pest": "Log a pest or disease",
    "view_analytics": "View analytics",
    "create_listing": "Create a marketplace listing"
  },
  "tooltip": {
    "stepOf": "Step {{current}} of {{total}}",
    "next": "Next",
    "back": "Back",
    "skip": "Skip",
    "done": "Got it"
  },
  "tooltips": {
    "createSeason": {
      "title": "Create your first season",
      "description": "Seasons help you organise your work by growing period. Add your first one to get started."
    },
    "createGarden": {
      "title": "Name your garden",
      "description": "Fill in the details and create your first garden. You can add beds to it afterwards."
    },
    "createBed": {
      "step1": {
        "title": "Open your garden",
        "description": "Click on your garden to see its details and manage beds."
      },
      "step2": {
        "title": "Add a bed",
        "description": "Click here to create a new bed where your plants will grow."
      }
    },
    "browseSpecies": {
      "title": "Explore species",
      "description": "Browse the species library to see what you can grow. You'll pick from these when sowing."
    },
    "addSeeds": {
      "step1": {
        "title": "Add seeds",
        "description": "Click here to add a new seed batch to your inventory."
      },
      "step2": {
        "title": "Fill in seed details",
        "description": "Enter the species, quantity, and optionally the provider and expiry date."
      }
    },
    "sowSeeds": {
      "step1": {
        "title": "Pick a species",
        "description": "Select which species you want to sow."
      },
      "step2": {
        "title": "Choose where to sow",
        "description": "Pick a bed or sow into a portable tray."
      },
      "step3": {
        "title": "Sow!",
        "description": "Set the count and hit sow to plant your seeds."
      }
    },
    "potUp": {
      "title": "Pot up plants",
      "description": "From the plants list, select a seeded plant and use the pot up action to move it to a pot."
    },
    "plantOut": {
      "title": "Plant out",
      "description": "Select a potted plant and plant it out to a bed in your garden."
    },
    "recordHarvest": {
      "title": "Record a harvest",
      "description": "Select a growing plant and record a harvest with weight or quantity."
    },
    "addCustomer": {
      "title": "Add a customer",
      "description": "Click here to add your first customer. You can track who buys your flowers."
    },
    "createBouquet": {
      "title": "Create a bouquet recipe",
      "description": "Click here to design a bouquet recipe with your favourite flower combinations."
    },
    "createTask": {
      "title": "Plan a task",
      "description": "Fill in the form to schedule a task like sowing, potting up, or harvesting."
    },
    "setupSuccession": {
      "title": "Set up succession planting",
      "description": "Click here to create a staggered sowing schedule for continuous harvests."
    },
    "setTarget": {
      "title": "Set a production target",
      "description": "Click here to set goals for how much of each species you want to produce."
    },
    "viewCalendar": {
      "title": "Your crop calendar",
      "description": "This is your visual timeline showing when to sow, plant, and harvest through the season."
    },
    "startTrial": {
      "title": "Start a variety trial",
      "description": "Click here to set up a trial comparing different varieties of a species."
    },
    "logPest": {
      "title": "Log a pest or disease",
      "description": "Click here to record pest or disease observations to track issues over time."
    },
    "viewAnalytics": {
      "title": "Your analytics",
      "description": "This dashboard shows harvest trends, production stats, and insights from your growing data."
    },
    "createListing": {
      "title": "List on the marketplace",
      "description": "Click here to create a listing and sell your produce to other growers and buyers."
    }
  }
}
```

- [ ] **Step 2: Add Swedish translations**

Add the same structure to `web/src/i18n/sv.json` with Swedish text:

```json
"onboarding": {
  "fab": {
    "label": "Installationsguide",
    "title": "Installationsguide — {{completed}}/{{total}} klara"
  },
  "drawer": {
    "title": "Installationsguide",
    "progress": "{{completed}} av {{total}} klara",
    "options": "Alternativ",
    "hideSession": "Dölj för nu",
    "dismissForever": "Visa inte igen",
    "allComplete": "Nu är du redo!",
    "allCompleteHint": "Du har utforskat allt som Verdant har att erbjuda.",
    "finish": "Stäng guiden"
  },
  "sections": {
    "gettingStarted": "Kom igång",
    "growing": "Odling",
    "harvestingSales": "Skörd & Försäljning",
    "planning": "Planering",
    "advanced": "Avancerat"
  },
  "steps": {
    "create_season": "Skapa en säsong",
    "create_garden": "Skapa en trädgård",
    "create_bed": "Skapa en bädd i din trädgård",
    "browse_species": "Utforska arter",
    "add_seeds": "Lägg till utsäde",
    "sow_seeds": "Så dina första frön",
    "pot_up": "Kruka om en planta",
    "plant_out": "Plantera ut i en bädd",
    "record_harvest": "Registrera en skörd",
    "add_customer": "Lägg till en kund",
    "create_bouquet": "Skapa ett bukettrecept",
    "create_task": "Skapa en schemalagd uppgift",
    "setup_succession": "Skapa ett successionsschema",
    "set_target": "Sätt ett produktionsmål",
    "view_calendar": "Visa odlingskalendern",
    "start_trial": "Starta ett sortförsök",
    "log_pest": "Logga skadedjur eller sjukdom",
    "view_analytics": "Visa statistik",
    "create_listing": "Skapa en marknadsannons"
  },
  "tooltip": {
    "stepOf": "Steg {{current}} av {{total}}",
    "next": "Nästa",
    "back": "Tillbaka",
    "skip": "Hoppa över",
    "done": "Klart"
  },
  "tooltips": {
    "createSeason": {
      "title": "Skapa din första säsong",
      "description": "Säsonger hjälper dig organisera ditt arbete efter odlingsperiod. Lägg till din första för att komma igång."
    },
    "createGarden": {
      "title": "Namnge din trädgård",
      "description": "Fyll i detaljerna och skapa din första trädgård. Du kan lägga till bäddar efteråt."
    },
    "createBed": {
      "step1": {
        "title": "Öppna din trädgård",
        "description": "Klicka på din trädgård för att se detaljer och hantera bäddar."
      },
      "step2": {
        "title": "Lägg till en bädd",
        "description": "Klicka här för att skapa en ny bädd där dina plantor ska växa."
      }
    },
    "browseSpecies": {
      "title": "Utforska arter",
      "description": "Bläddra i artbiblioteket för att se vad du kan odla. Du väljer bland dessa när du sår."
    },
    "addSeeds": {
      "step1": {
        "title": "Lägg till utsäde",
        "description": "Klicka här för att lägga till en ny fröomgång i ditt inventarie."
      },
      "step2": {
        "title": "Fyll i frödetaljer",
        "description": "Ange art, antal och eventuellt leverantör och utgångsdatum."
      }
    },
    "sowSeeds": {
      "step1": {
        "title": "Välj en art",
        "description": "Välj vilken art du vill så."
      },
      "step2": {
        "title": "Välj plats att så",
        "description": "Välj en bädd eller så i en portabel bricka."
      },
      "step3": {
        "title": "Så!",
        "description": "Ange antal och tryck på så för att plantera dina frön."
      }
    },
    "potUp": {
      "title": "Kruka om plantor",
      "description": "Från plantlistan, välj en sådd planta och använd kruka om-åtgärden för att flytta den till en kruka."
    },
    "plantOut": {
      "title": "Plantera ut",
      "description": "Välj en krukad planta och plantera ut den i en bädd i din trädgård."
    },
    "recordHarvest": {
      "title": "Registrera en skörd",
      "description": "Välj en växande planta och registrera en skörd med vikt eller antal."
    },
    "addCustomer": {
      "title": "Lägg till en kund",
      "description": "Klicka här för att lägga till din första kund. Du kan spåra vem som köper dina blommor."
    },
    "createBouquet": {
      "title": "Skapa ett bukettrecept",
      "description": "Klicka här för att designa ett bukettrecept med dina favoritkombinationer av blommor."
    },
    "createTask": {
      "title": "Planera en uppgift",
      "description": "Fyll i formuläret för att schemalägga en uppgift som sådd, uppkrukning eller skörd."
    },
    "setupSuccession": {
      "title": "Skapa successionsplantering",
      "description": "Klicka här för att skapa ett stegvist såddschema för kontinuerlig skörd."
    },
    "setTarget": {
      "title": "Sätt ett produktionsmål",
      "description": "Klicka här för att sätta mål för hur mycket av varje art du vill producera."
    },
    "viewCalendar": {
      "title": "Din odlingskalender",
      "description": "Detta är din visuella tidslinje som visar när du ska så, plantera och skörda under säsongen."
    },
    "startTrial": {
      "title": "Starta ett sortförsök",
      "description": "Klicka här för att starta ett försök som jämför olika sorter av en art."
    },
    "logPest": {
      "title": "Logga skadedjur eller sjukdom",
      "description": "Klicka här för att registrera observationer av skadedjur eller sjukdomar för att spåra problem över tid."
    },
    "viewAnalytics": {
      "title": "Din statistik",
      "description": "Denna instrumentpanel visar skördetrender, produktionsstatistik och insikter från din odlingsdata."
    },
    "createListing": {
      "title": "Annonsera på marknadsplatsen",
      "description": "Klicka här för att skapa en annons och sälja dina produkter till andra odlare och köpare."
    }
  }
}
```

- [ ] **Step 3: Verify the app builds**

Run: `cd web && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add web/src/i18n/en.json web/src/i18n/sv.json
git commit -m "feat: add English and Swedish onboarding translations"
```

---

## Task 14: Manual Smoke Test & Polish

This task is about verifying everything works end-to-end and fixing issues.

- [ ] **Step 1: Start the backend and web app**

Run in separate terminals:
```bash
cd backend && ./gradlew quarkusDev
cd web && npm run dev
```

- [ ] **Step 2: Verify the onboarding FAB appears**

Open `http://localhost:5175` in a browser. After logging in:
- The floating button should appear in the bottom-right corner with a progress ring
- Clicking it should open the drawer with 5 sections and 19 steps
- The first section ("Getting Started") should be expanded by default

- [ ] **Step 3: Test a checklist item flow**

Click "Create a season" in the checklist:
- Drawer should close
- App should navigate to `/seasons`
- A tooltip should appear pointing at the season form
- Complete the action — the step should auto-mark as complete
- The progress ring should update

- [ ] **Step 4: Test dismiss flows**

- Click the "···" menu in the drawer
- Test "Hide for now" — FAB should disappear, reappear on page refresh
- Test "Don't show again" — FAB should disappear and not come back

- [ ] **Step 5: Test tooltip navigation**

Find a multi-step tooltip (e.g., "Sow your first seeds"):
- Verify "Step 1 of 3" counter shows
- Verify Next/Back/Skip buttons work
- Verify the overlay cutout follows the target element

- [ ] **Step 6: Fix any issues found during testing**

Address layout, z-index, positioning, or timing issues.

- [ ] **Step 7: Commit any fixes**

```bash
git add -A
git commit -m "fix: polish onboarding UI after smoke testing"
```

---

## Summary

| Task | Description | Files |
|------|-------------|-------|
| 1 | Database migration & backend entity | V2 migration, User.kt, UserRepository.kt |
| 2 | Backend DTOs & API endpoint | UserDtos.kt, AuthService.kt, UserResource.kt |
| 3 | Install @floating-ui/react | package.json |
| 4 | Onboarding types & step definitions | types.ts, steps.ts |
| 5 | Frontend API client updates | client.ts |
| 6 | Onboarding context (state management) | OnboardingContext.tsx |
| 7 | Floating action button | OnboardingFab.tsx |
| 8 | Onboarding drawer (checklist) | OnboardingDrawer.tsx |
| 9 | Tooltip tour system | tooltipConfigs.ts, useTooltipTour.ts, OnboardingOverlay.tsx, OnboardingTooltip.tsx |
| 10 | Wire everything together | OnboardingRoot.tsx, main.tsx, Layout.tsx |
| 11 | Add data-onboarding attributes | ~15 page files |
| 12 | Mutation completion tracking | main.tsx, OnboardingContext.tsx |
| 13 | i18n translations | en.json, sv.json |
| 14 | Smoke test & polish | Various |
