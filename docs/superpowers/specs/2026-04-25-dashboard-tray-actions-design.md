# Dashboard tray actions — design

## Goal

Make tray entries on the web Dashboard clickable so the user can pick an action (Pot up, Plant out, Discard) to apply to plants in that tray group, mirroring the Android batch flows.

## Background

- `Dashboard.tsx` renders a "Trays" column from `api.plants.traySummary()`. Each row groups tray plants by `speciesName + status` and shows the count.
- Today the rows are display-only.
- The backend already supports the underlying batch operation: `POST /api/plants/batch-event` (`PlantResource.kt:75`, `PlantService.batchEvent`), which mutates plants in a group keyed by `(speciesId, bedId, plantedDate?, status)` and accepts event types `POTTED_UP`, `PLANTED_OUT`, `REMOVED` (and others). `plantedDate` and `bedId` may be null — when both are null, it operates on the matching tray group across all sow dates, oldest plants first.
- Android already has `BatchPotUpScreen` and `BatchPlantOutScreen` as reference implementations.

The current `TraySummaryEntry` DTO does not expose `speciesId`, which the new flow needs in order to call `batchEvent`.

## Backend changes

### `TraySummaryEntry` adds `speciesId`

`backend/src/main/kotlin/app/verdant/dto/PlantDtos.kt:113` — add `speciesId: Long?` to the data class.

`backend/src/main/kotlin/app/verdant/repository/PlantRepository.kt:203` — update `traySummary` SQL to select `s.id` and group by it. Map it onto the new field.

```sql
SELECT s.id AS species_id,
       COALESCE(s.common_name_sv, s.common_name) AS species_name,
       p.status,
       COUNT(*) AS count
FROM plant p
LEFT JOIN species s ON p.species_id = s.id
WHERE p.org_id = ? AND p.bed_id IS NULL AND p.status != 'REMOVED'
GROUP BY s.id, s.common_name_sv, s.common_name, p.status
ORDER BY species_name, p.status
```

`speciesId` is nullable to preserve the existing fallback row when a plant has no linked species (rare). Rows with `speciesId == null` will not be clickable on the web.

No new endpoint is needed — `batchEvent` is sufficient.

## Web changes

### `web/src/api/client.ts`

Add `speciesId?: number` to the `TraySummaryEntry` interface (line 175).

### `web/src/components/TrayActionDialog.tsx` (new)

Props:

```ts
interface TrayActionDialogProps {
  open: boolean
  entry: { speciesId: number; speciesName: string; status: string; count: number } | null
  onClose: () => void
}
```

Internal state:

```ts
type Step = 'choose' | 'pot_up' | 'plant_out' | 'discard'
const [step, setStep] = useState<Step>('choose')
const [count, setCount] = useState<string>('')         // default to entry.count when entry changes
const [notes, setNotes] = useState<string>('')
const [targetBedId, setTargetBedId] = useState<string>('')
```

Reset state to `{ step: 'choose', count: String(entry.count), notes: '', targetBedId: '' }` whenever `entry` changes (via `useEffect`), so the dialog opens fresh per row.

#### Step `choose`

Three buttons rendered conditionally on `entry.status`:

- **Pot up** — visible if `status === 'SEEDED'`. Sets `step = 'pot_up'`.
- **Plant out** — visible if `status === 'SEEDED' || status === 'POTTED_UP'`. Sets `step = 'plant_out'`.
- **Discard** — always visible. Sets `step = 'discard'`.

Title: `t('dashboard.trays.action.chooseAction', { species: entry.speciesName })`.

#### Step `pot_up` / `plant_out` / `discard`

Body fields:

- **Confirmation banner** (discard only): `t('dashboard.trays.action.discardConfirm', { count: entry.count, species: entry.speciesName })`. Light-weight, just a paragraph above the count field — no separate confirm dialog.
- **Count** (all): number input, label = `t('dashboard.trays.action.count')`, default `entry.count`, validated `1..entry.count`. Inline error if invalid.
- **Target bed** (plant_out only): `<select>` populated from `useQuery(['beds'], () => api.beds.list())`. Shows `bed.name` (and garden if useful — match the Android dropdown style if simple). Required.
- **Notes** (all): optional textarea, label = `t('dashboard.trays.action.notes')`, placeholder `t('common.optional')`.

Dialog footer:

- Left: `← t('common.back')` link → `setStep('choose')`.
- Right: `Cancel` (closes), `Save` (submits).

Dialog title:

- `pot_up` → `t('dashboard.trays.action.potUpTitle', { species })`
- `plant_out` → `t('dashboard.trays.action.plantOutTitle', { species })`
- `discard` → `t('dashboard.trays.action.discardTitle', { species })`

#### Submit

```ts
const mut = useMutation({
  mutationFn: () => api.plants.batchEvent({
    speciesId: entry.speciesId,
    status: entry.status,
    eventType: step === 'pot_up' ? 'POTTED_UP'
             : step === 'plant_out' ? 'PLANTED_OUT'
             : 'REMOVED',
    count: Number(count),
    notes: notes || undefined,
    targetBedId: step === 'plant_out' ? Number(targetBedId) : undefined,
  }),
  onSuccess: () => {
    qc.invalidateQueries({ queryKey: ['tray-summary'] })
    qc.invalidateQueries({ queryKey: ['dashboard'] })
    qc.invalidateQueries({ queryKey: ['beds'] })
    onClose()
  },
})
```

`bedId` and `plantedDate` are intentionally omitted from the request, so the backend operates on the full tray group regardless of sow date.

#### Validation

- `count` parses as integer in `1..entry.count`.
- For `plant_out`, `targetBedId` is set.
- Save button disabled while `mut.isPending` or validation fails.

#### Error handling

Surface `mut.error` inline in the dialog body using the same pattern as `PlantDetail.tsx` (text styled with `var(--color-accent)` or use `ErrorDisplay` if it fits the modal layout — match whatever is closest to the existing event dialog).

### `web/src/pages/Dashboard.tsx`

Replace each tray row's outer `<div>` with a `<button>` (or `<div role="button" tabIndex={0}>`) carrying:

- `onClick` → `setActiveEntry({ speciesId, speciesName, status, count })` and `setDialogOpen(true)`.
- Disabled state when `row.speciesId == null`: no click handler, no hover, slightly muted text.
- Visual: keep existing grid layout. Add subtle hover (e.g., `background: color-mix(in srgb, var(--color-ink) 4%, transparent)`) and a pointer cursor — match how `Chip` / other interactive list rows feel in the codebase.

Render the new `<TrayActionDialog>` at the bottom of the Dashboard, controlled by local state.

### i18n

Add to `web/src/i18n/en.json` and `web/src/i18n/sv.json` under `dashboard.trays.action`:

```json
{
  "chooseAction": "Take action on {{species}}",
  "potUpTitle": "Pot up {{species}}",
  "plantOutTitle": "Plant out {{species}}",
  "discardTitle": "Discard {{species}}",
  "potUp": "Pot up",
  "plantOut": "Plant out",
  "discard": "Discard",
  "count": "Count",
  "targetBed": "Target bed",
  "notes": "Notes",
  "discardConfirm": "Discard {{count}} {{species}} plants from the tray?"
}
```

Swedish equivalents follow existing translation conventions in the file.

## Testing

- **Backend:** if `traySummary` already has a test, extend it to assert `speciesId` is populated. If not present, skip — additive field, low risk.
- **Web:** no new component tests unless the repo has a clear precedent for dialog tests; the Dashboard does not test interactivity today, so we will not add a test harness just for this.
- Manual verification: run web dev server, sow some plants in a tray, click each row, confirm Pot up / Plant out / Discard each update the tray summary on success and reflect on the relevant downstream views.

## Out of scope

- Photo capture in the dialog (deferred — admin UI has no photo capture pattern yet).
- Multi-row select.
- Per-plantedDate granularity — the dialog operates on the full tray group; if the user has two sow dates of the same species in the same status, the action takes from oldest first.
- Workflow-step auto-progression — already handled by `PlantService.batchEvent`.
- Optimistic updates — invalidate-and-refetch is sufficient.

## Open questions / verifications during implementation

- Confirm the visual treatment of the clickable row matches existing Faltet patterns. If a `ListRow` primitive exists, use it; otherwise inline the styling.
- Confirm `api.beds.list()` returns enough info for the select label (name, optionally garden). If not, adjust to a richer endpoint.
