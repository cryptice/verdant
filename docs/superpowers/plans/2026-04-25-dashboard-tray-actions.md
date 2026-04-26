# Dashboard Tray Actions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make tray entries on the web Dashboard clickable to open a dialog where the user can pot up, plant out, or discard plants from that tray group.

**Architecture:** Single-dialog flow with two steps — action chooser, then per-action form. Backend reuses the existing `POST /api/plants/batch-event` endpoint; the only backend change is adding `speciesId` to `TraySummaryEntry`. Web adds a new `TrayActionDialog` component and wires the existing dashboard tray rows as click triggers.

**Tech Stack:** Quarkus + Kotlin (backend), React + TypeScript + react-query + react-i18next (web). Uses the existing `Dialog` component (`web/src/components/Dialog.tsx`) and `api.plants.batchEvent` client.

---

## File Structure

**Backend (modify):**
- `backend/src/main/kotlin/app/verdant/dto/PlantDtos.kt` — add `speciesId: Long?` to `TraySummaryEntry`.
- `backend/src/main/kotlin/app/verdant/repository/PlantRepository.kt` — update `traySummary` SQL.

**Web (create):**
- `web/src/components/TrayActionDialog.tsx` — new dialog component with action chooser + form steps.

**Web (modify):**
- `web/src/api/client.ts` — add `speciesId?: number` to `TraySummaryEntry` interface.
- `web/src/pages/Dashboard.tsx` — make tray rows clickable, render `TrayActionDialog`.
- `web/src/i18n/en.json` — add `dashboard.trays.action.*` keys.
- `web/src/i18n/sv.json` — add Swedish translations.

---

## Task 1: Backend — add `speciesId` to `TraySummaryEntry`

**Files:**
- Modify: `backend/src/main/kotlin/app/verdant/dto/PlantDtos.kt:113-117`
- Modify: `backend/src/main/kotlin/app/verdant/repository/PlantRepository.kt:203-226`

- [ ] **Step 1: Update the DTO**

Edit `backend/src/main/kotlin/app/verdant/dto/PlantDtos.kt:113-117` to add `speciesId`:

```kotlin
data class TraySummaryEntry(
    val speciesId: Long?,
    val speciesName: String,
    val status: String,
    val count: Int,
)
```

- [ ] **Step 2: Update the SQL and mapping in the repository**

Edit `backend/src/main/kotlin/app/verdant/repository/PlantRepository.kt:203-226`. Replace the entire `traySummary` function body with:

```kotlin
fun traySummary(orgId: Long): List<app.verdant.dto.TraySummaryEntry> =
    ds.connection.use { conn ->
        conn.prepareStatement(
            """SELECT s.id AS species_id,
                      COALESCE(s.common_name_sv, s.common_name) as species_name,
                      p.status,
                      COUNT(*) as count
               FROM plant p
               LEFT JOIN species s ON p.species_id = s.id
               WHERE p.org_id = ? AND p.bed_id IS NULL AND p.status != 'REMOVED'
               GROUP BY s.id, s.common_name_sv, s.common_name, p.status
               ORDER BY species_name, p.status"""
        ).use { ps ->
            ps.setLong(1, orgId)
            ps.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) add(
                        app.verdant.dto.TraySummaryEntry(
                            speciesId = rs.getObject("species_id") as? Long,
                            speciesName = rs.getString("species_name") ?: "Unknown",
                            status = rs.getString("status"),
                            count = rs.getInt("count"),
                        )
                    )
                }
            }
        }
    }
```

- [ ] **Step 3: Compile the backend**

Run from repo root:

```bash
cd backend && ./gradlew compileKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/dto/PlantDtos.kt backend/src/main/kotlin/app/verdant/repository/PlantRepository.kt
git commit -m "feat(backend): include speciesId in tray summary entries"
```

---

## Task 2: Web — extend `TraySummaryEntry` interface

**Files:**
- Modify: `web/src/api/client.ts:175`

- [ ] **Step 1: Add `speciesId` to the interface**

Edit `web/src/api/client.ts:175`. Replace:

```ts
export interface TraySummaryEntry { speciesName: string; status: string; count: number }
```

with:

```ts
export interface TraySummaryEntry { speciesId?: number; speciesName: string; status: string; count: number }
```

- [ ] **Step 2: Type-check**

Run from repo root:

```bash
cd web && npm run typecheck
```

Expected: no errors.

(If `npm run typecheck` doesn't exist, use `cd web && npx tsc --noEmit` instead — discover the right command from `web/package.json` "scripts" section.)

- [ ] **Step 3: Commit**

```bash
git add web/src/api/client.ts
git commit -m "feat(web): expose speciesId on TraySummaryEntry"
```

---

## Task 3: Web — add i18n keys for tray actions

**Files:**
- Modify: `web/src/i18n/en.json` (under `dashboard.trays`)
- Modify: `web/src/i18n/sv.json` (under `dashboard.trays`)

- [ ] **Step 1: Add English keys**

Edit `web/src/i18n/en.json:155-157`. Replace the `"trays"` block with:

```json
    "trays": {
      "title": "Trays",
      "action": {
        "chooseAction": "Take action on {{species}}",
        "potUpTitle": "Pot up {{species}}",
        "plantOutTitle": "Plant out {{species}}",
        "discardTitle": "Discard {{species}}",
        "potUp": "Pot up",
        "plantOut": "Plant out",
        "discard": "Discard",
        "count": "Count",
        "countError": "Count must be between 1 and {{max}}",
        "targetBed": "Target bed",
        "targetBedRequired": "Target bed is required",
        "selectBed": "Select a bed",
        "notes": "Notes",
        "discardConfirm": "Discard {{count}} {{species}} plants from the tray?"
      }
    },
```

- [ ] **Step 2: Add Swedish keys**

Read `web/src/i18n/sv.json` around line 155 to find the `"trays"` block. Replace it with the Swedish equivalent:

```json
    "trays": {
      "title": "Brätten",
      "action": {
        "chooseAction": "Vidta åtgärd för {{species}}",
        "potUpTitle": "Skola om {{species}}",
        "plantOutTitle": "Plantera ut {{species}}",
        "discardTitle": "Kasta {{species}}",
        "potUp": "Skola om",
        "plantOut": "Plantera ut",
        "discard": "Kasta",
        "count": "Antal",
        "countError": "Antal måste vara mellan 1 och {{max}}",
        "targetBed": "Målbädd",
        "targetBedRequired": "Målbädd krävs",
        "selectBed": "Välj en bädd",
        "notes": "Anteckningar",
        "discardConfirm": "Kasta {{count}} plantor av {{species}} från brättet?"
      }
    },
```

(If the Swedish "trays" title is currently something different from "Brätten", preserve the existing Swedish word — only add the `action` block.)

- [ ] **Step 3: Verify both files are valid JSON**

```bash
cd web && node -e "JSON.parse(require('fs').readFileSync('src/i18n/en.json'))" && node -e "JSON.parse(require('fs').readFileSync('src/i18n/sv.json'))"
```

Expected: no output, exit code 0.

- [ ] **Step 4: Commit**

```bash
git add web/src/i18n/en.json web/src/i18n/sv.json
git commit -m "feat(web): add i18n keys for dashboard tray actions"
```

---

## Task 4: Web — create `TrayActionDialog` component

**Files:**
- Create: `web/src/components/TrayActionDialog.tsx`

This dialog handles three actions on a tray group: pot up, plant out, discard. State machine: `'choose' → 'pot_up' | 'plant_out' | 'discard'`. Submit calls `api.plants.batchEvent` and invalidates the relevant queries on success.

- [ ] **Step 1: Create the component**

Create `web/src/components/TrayActionDialog.tsx` with this exact content:

```tsx
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import { Dialog } from './Dialog'

export interface TrayActionEntry {
  speciesId: number
  speciesName: string
  status: string
  count: number
}

type Step = 'choose' | 'pot_up' | 'plant_out' | 'discard'

interface Props {
  open: boolean
  entry: TrayActionEntry | null
  onClose: () => void
}

export function TrayActionDialog({ open, entry, onClose }: Props) {
  const { t } = useTranslation()
  const qc = useQueryClient()

  const [step, setStep] = useState<Step>('choose')
  const [count, setCount] = useState<string>('')
  const [notes, setNotes] = useState<string>('')
  const [targetBedId, setTargetBedId] = useState<string>('')
  const [submitError, setSubmitError] = useState<string | null>(null)

  // Reset state whenever the dialog opens with a new entry
  useEffect(() => {
    if (entry) {
      setStep('choose')
      setCount(String(entry.count))
      setNotes('')
      setTargetBedId('')
      setSubmitError(null)
    }
  }, [entry])

  const { data: beds } = useQuery({
    queryKey: ['beds'],
    queryFn: () => api.beds.list(),
    enabled: step === 'plant_out',
  })

  const eventTypeFor = (s: Step): string =>
    s === 'pot_up' ? 'POTTED_UP' : s === 'plant_out' ? 'PLANTED_OUT' : 'REMOVED'

  const mut = useMutation({
    mutationFn: () => {
      if (!entry) throw new Error('No entry')
      return api.plants.batchEvent({
        speciesId: entry.speciesId,
        status: entry.status,
        eventType: eventTypeFor(step),
        count: Number(count),
        notes: notes.trim() ? notes.trim() : undefined,
        targetBedId: step === 'plant_out' ? Number(targetBedId) : undefined,
      })
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tray-summary'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      qc.invalidateQueries({ queryKey: ['beds'] })
      qc.invalidateQueries({ queryKey: ['plants'] })
      onClose()
    },
    onError: (err: unknown) => {
      setSubmitError(err instanceof Error ? err.message : String(err))
    },
  })

  if (!entry) {
    return <Dialog open={open} onClose={onClose} title="">{null}</Dialog>
  }

  const species = entry.speciesName
  const max = entry.count

  const countNum = Number(count)
  const countValid = Number.isInteger(countNum) && countNum >= 1 && countNum <= max
  const targetBedValid = step !== 'plant_out' || (targetBedId !== '' && Number(targetBedId) > 0)
  const canSubmit = step !== 'choose' && countValid && targetBedValid && !mut.isPending

  const titleKey =
    step === 'choose' ? 'dashboard.trays.action.chooseAction'
    : step === 'pot_up' ? 'dashboard.trays.action.potUpTitle'
    : step === 'plant_out' ? 'dashboard.trays.action.plantOutTitle'
    : 'dashboard.trays.action.discardTitle'

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={t(titleKey, { species })}
      actions={
        step === 'choose' ? (
          <button onClick={onClose} className="px-4 py-2 text-sm text-text-secondary">
            {t('common.cancel')}
          </button>
        ) : (
          <>
            <button
              onClick={() => setStep('choose')}
              className="px-4 py-2 text-sm text-text-secondary"
            >
              ← {t('common.back')}
            </button>
            <button onClick={onClose} className="px-4 py-2 text-sm text-text-secondary">
              {t('common.cancel')}
            </button>
            <button
              onClick={() => { setSubmitError(null); mut.mutate() }}
              disabled={!canSubmit}
              className="btn-primary text-sm"
            >
              {mut.isPending ? t('common.saving') : t('common.save')}
            </button>
          </>
        )
      }
    >
      {step === 'choose' && (
        <div className="flex flex-col gap-2">
          {entry.status === 'SEEDED' && (
            <button
              onClick={() => setStep('pot_up')}
              className="btn-secondary w-full text-left"
            >
              {t('dashboard.trays.action.potUp')}
            </button>
          )}
          {(entry.status === 'SEEDED' || entry.status === 'POTTED_UP') && (
            <button
              onClick={() => setStep('plant_out')}
              className="btn-secondary w-full text-left"
            >
              {t('dashboard.trays.action.plantOut')}
            </button>
          )}
          <button
            onClick={() => setStep('discard')}
            className="btn-secondary w-full text-left"
          >
            {t('dashboard.trays.action.discard')}
          </button>
        </div>
      )}

      {step !== 'choose' && (
        <div className="space-y-4">
          {step === 'discard' && (
            <p className="text-text-secondary text-sm">
              {t('dashboard.trays.action.discardConfirm', { count: max, species })}
            </p>
          )}

          <div>
            <label className="field-label">{t('dashboard.trays.action.count')}</label>
            <input
              type="number"
              min={1}
              max={max}
              value={count}
              onChange={(e) => setCount(e.target.value)}
              className="input"
            />
            {!countValid && count !== '' && (
              <p className="text-xs text-error mt-1">
                {t('dashboard.trays.action.countError', { max })}
              </p>
            )}
          </div>

          {step === 'plant_out' && (
            <div>
              <label className="field-label">{t('dashboard.trays.action.targetBed')}</label>
              <select
                value={targetBedId}
                onChange={(e) => setTargetBedId(e.target.value)}
                className="input"
              >
                <option value="">{t('dashboard.trays.action.selectBed')}</option>
                {(beds ?? []).map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.gardenName ? `${b.gardenName} · ${b.name}` : b.name}
                  </option>
                ))}
              </select>
              {!targetBedValid && targetBedId === '' && (
                <p className="text-xs text-error mt-1">
                  {t('dashboard.trays.action.targetBedRequired')}
                </p>
              )}
            </div>
          )}

          <div>
            <label className="field-label">{t('dashboard.trays.action.notes')}</label>
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder={t('common.optional')}
              rows={2}
              className="input"
            />
          </div>

          {submitError && (
            <p className="text-sm text-error">{submitError}</p>
          )}
        </div>
      )}
    </Dialog>
  )
}
```

- [ ] **Step 2: Type-check**

```bash
cd web && npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add web/src/components/TrayActionDialog.tsx
git commit -m "feat(web): add TrayActionDialog for dashboard tray actions"
```

---

## Task 5: Web — wire dashboard rows to open the dialog

**Files:**
- Modify: `web/src/pages/Dashboard.tsx`

The current tray rows are non-interactive `<div>`s at `Dashboard.tsx:108-138`. Convert each row to a button-like clickable element when `row.speciesId` is present, and render the dialog at the bottom of the component.

- [ ] **Step 1: Add imports and state to `Dashboard.tsx`**

At the top of `web/src/pages/Dashboard.tsx`, after the existing imports, add:

```tsx
import { useState } from 'react'
import { TrayActionDialog, type TrayActionEntry } from '../components/TrayActionDialog'
```

Inside the `Dashboard` component (after the `useOnboarding` line), add:

```tsx
const [trayDialogOpen, setTrayDialogOpen] = useState(false)
const [activeTrayEntry, setActiveTrayEntry] = useState<TrayActionEntry | null>(null)
```

- [ ] **Step 2: Make tray rows clickable**

Replace the existing tray-row block (currently `Dashboard.tsx:108-138`):

```tsx
{trays?.slice(0, 6).map((row, i) => (
  <div
    key={i}
    style={{
      display: 'grid',
      gridTemplateColumns: '1.5fr 60px 80px',
      gap: 10,
      padding: '10px 0',
      borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
      fontFamily: 'var(--font-display)',
      fontSize: 16,
    }}
  >
    <span>{row.speciesName}</span>
    <span style={{ textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>
      {row.count}
    </span>
    <span
      style={{
        fontFamily: 'var(--font-mono)',
        fontSize: 10,
        textAlign: 'right',
        textTransform: 'uppercase',
        letterSpacing: 1.2,
        color: 'var(--color-forest)',
      }}
    >
      {row.status}
    </span>
  </div>
))}
```

with:

```tsx
{trays?.slice(0, 6).map((row, i) => {
  const clickable = row.speciesId != null
  const handleClick = () => {
    if (!clickable) return
    setActiveTrayEntry({
      speciesId: row.speciesId!,
      speciesName: row.speciesName,
      status: row.status,
      count: row.count,
    })
    setTrayDialogOpen(true)
  }
  return (
    <div
      key={i}
      role={clickable ? 'button' : undefined}
      tabIndex={clickable ? 0 : undefined}
      onClick={handleClick}
      onKeyDown={(e) => {
        if (clickable && (e.key === 'Enter' || e.key === ' ')) {
          e.preventDefault()
          handleClick()
        }
      }}
      style={{
        display: 'grid',
        gridTemplateColumns: '1.5fr 60px 80px',
        gap: 10,
        padding: '10px 0',
        borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
        fontFamily: 'var(--font-display)',
        fontSize: 16,
        cursor: clickable ? 'pointer' : 'default',
        opacity: clickable ? 1 : 0.6,
        transition: 'background 120ms',
      }}
      onMouseEnter={(e) => {
        if (clickable) e.currentTarget.style.background =
          'color-mix(in srgb, var(--color-ink) 4%, transparent)'
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.background = 'transparent'
      }}
    >
      <span>{row.speciesName}</span>
      <span style={{ textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>
        {row.count}
      </span>
      <span
        style={{
          fontFamily: 'var(--font-mono)',
          fontSize: 10,
          textAlign: 'right',
          textTransform: 'uppercase',
          letterSpacing: 1.2,
          color: 'var(--color-forest)',
        }}
      >
        {row.status}
      </span>
    </div>
  )
})}
```

- [ ] **Step 3: Render the dialog at the end of the component**

Find the closing `</div>` of the outermost `Dashboard` return value (the one matching `<div>` at the start of `return (`). Just before that closing `</div>`, add:

```tsx
<TrayActionDialog
  open={trayDialogOpen}
  entry={activeTrayEntry}
  onClose={() => setTrayDialogOpen(false)}
/>
```

- [ ] **Step 4: Type-check**

```bash
cd web && npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 5: Commit**

```bash
git add web/src/pages/Dashboard.tsx
git commit -m "feat(web): make dashboard tray rows clickable to launch action dialog"
```

---

## Task 6: Manual verification

**Files:** none

- [ ] **Step 1: Start the backend**

In one terminal:

```bash
cd backend && ./gradlew quarkusDev
```

Wait for "Quarkus ... started in ... Listening on:" message.

- [ ] **Step 2: Start the web dev server**

In another terminal:

```bash
cd web && npm run dev
```

Note the URL (typically `http://localhost:5173`).

- [ ] **Step 3: Verify tray summary endpoint returns `speciesId`**

In a third terminal (assuming dev auth is set up; if not, log in via the UI first and grab the token from devtools):

```bash
curl -s http://localhost:8080/api/plants/tray-summary -H "Authorization: Bearer $TOKEN" | head
```

Expected: JSON array; each entry has `speciesId` (or `null` for unlinked species), `speciesName`, `status`, `count`.

If you don't have a quick way to hit the API directly, skip this step and move to the UI check.

- [ ] **Step 4: Verify the UI flow**

1. Navigate to the Dashboard. Confirm tray rows render.
2. If you have no tray plants, sow some via the existing flow first so the dashboard column populates.
3. Click a tray row. Dialog opens with title "Take action on {species}".
4. Confirm the action buttons match the row status:
   - `SEEDED` row → Pot up, Plant out, Discard
   - `POTTED_UP` row → Plant out, Discard (no Pot up)
5. Click **Pot up**. Form shows count (defaulted to row count) and notes. Submit. Dialog closes. Dashboard refreshes — row count decreases (or row disappears if all potted), and a new row appears under `POTTED_UP`.
6. Click a `POTTED_UP` row → click **Plant out**. Form shows count, target bed select, and notes. Submit. Plants move to the chosen bed; tray count decreases.
7. Click any tray row → click **Discard**. Form shows the discard confirmation paragraph + count + notes. Submit. Tray count decreases.
8. Verify the **Back** button on each form returns to the action chooser.
9. Verify validation: enter `0` or a count > available; the Save button disables and the count error message appears.

- [ ] **Step 5: Note any issues**

If anything misbehaves, capture the symptom and the file/line; fix in a follow-up task before completing the plan.

---

## Self-review checklist

- Spec coverage: Backend `speciesId` (Task 1), web type update (Task 2), i18n (Task 3), dialog component with chooser + three forms + validation + error handling (Task 4), dashboard wiring (Task 5), manual verification (Task 6). ✓
- All event types covered: `POTTED_UP`, `PLANTED_OUT`, `REMOVED`. ✓
- Status-based button visibility implemented. ✓
- Query invalidation on success: `tray-summary`, `dashboard`, `beds`, `plants`. ✓
- No placeholders. No "implement later". All code shown verbatim. ✓
- Out-of-scope items (photo, multi-select, per-plantedDate granularity) deliberately omitted. ✓
