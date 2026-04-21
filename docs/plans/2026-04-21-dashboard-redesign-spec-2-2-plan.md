# Verdant Dashboard Redesign Spec 2.2 — Forms, Details, Settings, Public Pages Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the remaining 16 Verdant web pages to Fältet — 8 forms, 3 detail pages, 3 settings/meta, 2 public/unauth — concluding the web-wide rollout (except spec 3's three design-dependent screens).

**Architecture:** No new primitives. Reuse existing Fältet primitives (`Masthead`, `Chip`, `Rule`, `Stat`, `Field`, `PhotoPlaceholder`, `Ledger`, `LedgerFilters`) and established patterns from specs 1 + 2.1. Adds one "editorial" chrome pattern for pages rendered outside the `<Layout>` shell (LandingPage, PrivacyPolicy, OrgSetup) or with book-like content layouts (Guide).

**Tech Stack:** React 19 + TypeScript + Vite + Tailwind v4, TanStack Query, react-router-dom, react-i18next. No new dependencies.

**Important notes:**
- Solo-dev, commits to `main`.
- Data layer untouched. Preserve every existing hook, mutation, dialog, and routing target.
- i18n: most page copy already exists in `sv.json`/`en.json`. Add only the editorial taglines listed in §4.
- Reference design spec: `docs/plans/2026-04-21-dashboard-redesign-spec-2-2-design.md`.

---

## Pattern reference

Every page uses one of four established patterns:

### Form pattern

Source: `web/src/components/faltet/SpeciesEditForm.tsx` (spec 1). Used for all 8 forms + 2 of 3 settings.

1. `<Masthead left={nav label} center={italic Fraunces tagline} right={optional cancel link}>`
2. Optional hero row: `96px 1fr auto` grid — `PhotoPlaceholder` square / chips + Fraunces 44 title + italic Fraunces 14 sage meta / right-aligned italic Fraunces 22 mustard identifier.
3. Form grid — `grid-template-columns: 1fr 1fr; gap: 20px 28px` desktop, `1fr` mobile. Each field = `<Field editable label value onChange accent?>`.
4. Section divider — mono 9 small-caps `§ Sektion` 0.6 opacity + `<Rule variant="soft" />`.
5. Full-width notes block — 1px-ink-bordered paper box with `<textarea>` styled Fraunces italic 16.
6. Sticky footer — cream bg, 1px ink top border. Left (optional): mono `↵ radera …` clay text button. Right: `.btn-secondary` Avbryt + `.btn-primary` Spara.

### Detail pattern

Source: `web/src/pages/BedDetail.tsx` (spec 1). Used for all 3 detail pages.

1. Masthead with breadcrumb `left` (last crumb clay).
2. Hero row — `1.2fr 1fr` desktop, stacked mobile. Left: chip row + Fraunces 60–80 title with tone-colored period + Fraunces Georgia 15 paragraph. Right: `<PhotoPlaceholder tone aspect="tall">` + 2×2 meta grid.
3. Stats band — 5-up flex, top/bottom 1px ink rules, `<Stat size="medium" hue>` cells.
4. Content section(s) — italic Fraunces 30 heading + clay period + `<Rule inline />` + right-aligned action links.
5. Optional bottom row — dark-ink card on left (Fraunces italic 26 + mono meta), 1px clay/40 Farozon callout on right.

### Settings pattern

Form pattern without hero. Destructive actions live in a Farozon-style callout at the bottom of the page. Used for Account, OrgSettings.

### Editorial pattern

New. Used for LandingPage, PrivacyPolicy, OrgSetup, Guide (last one inside Layout shell; first three outside).

1. Top strip (for pages outside `<Layout>`): Verdant wordmark Fraunces italic 32 + clay period, `Est. 2026 — Småland` mono subtitle on left. Right: login or language switcher. 1px ink bottom.
2. Content area: `max-width: 860px` centered, cream bg. Body prose in Fraunces Georgia (15–16px, `line-height: 1.6`, `color: var(--color-forest)`). Section headers mono 9 small-caps `§ …` 0.7 opacity.
3. Unicode glyphs only. No emoji icons.

---

## Phasing overview

| Task | Name | Pages |
|---|---|---|
| 1 | Simple forms | BedForm, GardenForm, SpeciesGroupEdit, OrgSetup |
| 2 | Activity forms | TaskForm, ApplySupply, SowActivity |
| 3 | Workflow template editor | WorkflowTemplateEdit |
| 4 | Detail pages | GardenDetail, PlantedSpeciesDetail, PlantDetail |
| 5 | Settings + meta + public | Account, OrgSettings, Guide, LandingPage, PrivacyPolicy |
| 6 | Milestone | Empty commit |

Each task: read current page → replace rendering → preserve data layer / mutations / dialogs → verify `cd web && npx tsc --noEmit && npm run build` green → commit.

---

## Task 1 — Simple forms

**Goal:** Port 4 simple-shape forms to the form pattern.

**Files:** replace in place:
- `web/src/pages/BedForm.tsx`
- `web/src/pages/GardenForm.tsx`
- `web/src/pages/SpeciesGroupEdit.tsx`
- `web/src/pages/OrgSetup.tsx`

### Step 1: `BedForm.tsx`

Read the current file to note: existing conditions section (soil/sun/drainage/aspect/irrigation/protection/raisedBed/pH) added by the bed-conditions feature. Keep that logic intact.

New shape:

```tsx
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import { useState, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { Masthead, Field, Rule } from '../components/faltet'

export function BedForm() {
  const { gardenId, bedId } = useParams<{ gardenId: string; bedId?: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t } = useTranslation()
  const isEdit = !!bedId

  const { data: bed } = useQuery({
    queryKey: ['bed', bedId],
    queryFn: () => api.beds.get(Number(bedId)),
    enabled: isEdit,
  })

  const [draft, setDraft] = useState<Record<string, any>>({})
  const value = (k: string) => (k in draft ? draft[k] : (bed as any)?.[k] ?? '')
  const set = (k: string, v: any) => setDraft((d) => ({ ...d, [k]: v }))
  const [conditionsOpen, setConditionsOpen] = useState(false)

  const saveMut = useMutation({
    mutationFn: () => isEdit
      ? api.beds.update(Number(bedId), draft)
      : api.beds.create(Number(gardenId), { name: draft.name, ...draft }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['garden', Number(gardenId)] })
      navigate(`/garden/${gardenId}`)
    },
  })

  return (
    <div>
      <Masthead
        left={<span>{t('nav.gardens')} / <span style={{ color: 'var(--color-clay)' }}>{isEdit ? t('bed.editBedTitle') : t('bed.newBedTitle')}</span></span>}
        center={t('form.masthead.center')}
      />

      <div style={{ padding: '28px 40px', paddingBottom: 120 }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px 28px' }}>
          <Field label={t('common.nameLabel')} editable value={value('name')} onChange={(v) => set('name', v)} />
          <Field label={t('bed.meta.length')} editable value={String(value('lengthMeters') ?? '')} onChange={(v) => set('lengthMeters', v ? Number(v) : null)} accent="mustard" />
          <Field label={t('bed.meta.width')} editable value={String(value('widthMeters') ?? '')} onChange={(v) => set('widthMeters', v ? Number(v) : null)} accent="mustard" />
        </div>

        <div style={{ marginTop: 28 }}>
          <button
            onClick={() => setConditionsOpen((o) => !o)}
            style={{ background: 'transparent', border: 'none', fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.6, cursor: 'pointer' }}
          >
            {conditionsOpen ? '▼' : '▶'} § {t('bed.conditions.sectionTitle')}
          </button>
          <div style={{ marginTop: 8 }}><Rule variant="soft" /></div>

          {conditionsOpen && (
            <div style={{ marginTop: 20, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px 28px' }}>
              {/* Preserve every existing condition field. Use <Field editable> with appropriate accent. */}
              {/* Example for soil type — render others identically: */}
              <Field
                label={t('bed.conditions.soilType')}
                editable
                value={String(value('soilType') ?? '')}
                onChange={(v) => set('soilType', v || null)}
              />
              {/* ... continue for: soilPh (accent clay when out of range), sunExposure, drainage,
                     aspect, irrigationType, protection, raisedBed (checkbox not Field).
                     Match the dropdown/input patterns from the existing BedForm. */}
            </div>
          )}
        </div>
      </div>

      <div style={{ position: 'sticky', bottom: 0, background: 'var(--color-cream)', borderTop: '1px solid var(--color-ink)', padding: '14px 40px', display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
        <button className="btn-secondary" onClick={() => navigate(`/garden/${gardenId}`)}>{t('common.cancel')}</button>
        <button className="btn-primary" onClick={() => saveMut.mutate()} disabled={saveMut.isPending || !value('name')?.trim()}>
          {saveMut.isPending ? t('common.saving') : isEdit ? t('common.save') : t('bed.createBed')}
        </button>
      </div>
    </div>
  )
}
```

Preserve the existing conditions dropdowns / chip rows exactly — read the current `BedForm.tsx` and lift those into the collapsed section. The skeleton above shows the shape; the real page needs every condition field wired.

### Step 2: `GardenForm.tsx`

Simple single-column form: name, description, emoji picker. Emoji picker is 32 buttons in a 4-col grid (current logic). Wrap in Fältet form chrome.

```tsx
<div>
  <Masthead
    left={<span>{t('nav.gardens')} / <span style={{ color: 'var(--color-clay)' }}>{isEdit ? t('garden.editGardenTitle') : t('garden.newGardenTitle')}</span></span>}
    center={t('form.masthead.center')}
  />
  <div style={{ padding: '28px 40px', paddingBottom: 120 }}>
    <Field label={t('common.iconLabel')} /* emoji picker below */ />
    {/* Preserve existing emoji grid */}
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(8, 1fr)', gap: 4, marginTop: 8 }}>
      {GARDEN_ICONS.map((icon) => (
        <button
          key={icon}
          onClick={() => set('emoji', value('emoji') === icon ? '' : icon)}
          style={{
            fontSize: 22,
            padding: 4,
            background: value('emoji') === icon ? 'var(--color-paper)' : 'transparent',
            border: `1px solid ${value('emoji') === icon ? 'var(--color-ink)' : 'transparent'}`,
            cursor: 'pointer',
          }}
        >{icon}</button>
      ))}
    </div>
    <div style={{ marginTop: 20 }}>
      <Field label={t('common.nameLabel')} editable value={value('name')} onChange={(v) => set('name', v)} />
    </div>
    <div style={{ marginTop: 20 }}>
      <Field label={t('common.descriptionLabel')} editable value={value('description')} onChange={(v) => set('description', v)} />
    </div>
  </div>
  <StickyFooter /* Avbryt + Spara/Skapa */ />
</div>
```

Preserve the `GARDEN_ICONS` list from the current file verbatim.

### Step 3: `SpeciesGroupEdit.tsx`

Masthead + form with group name + species membership. Existing page has a species checklist; keep it, restyle as a ledger-like list with checkboxes on the left:

```tsx
<div>
  <Masthead left={<span>{t('nav.speciesGroups')} / <span style={{ color: 'var(--color-clay)' }}>{group?.name ?? t('speciesGroups.newTitle')}</span></span>} center={t('form.masthead.center')} />
  <div style={{ padding: '28px 40px', paddingBottom: 120 }}>
    <Field label={t('common.nameLabel')} editable value={value('name')} onChange={(v) => set('name', v)} />

    <div style={{ marginTop: 28, fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.6 }}>
      § {t('speciesGroups.members')}
    </div>
    <div style={{ marginTop: 8 }}><Rule variant="soft" /></div>

    {/* Species checklist - preserve existing logic */}
    <div style={{ marginTop: 14 }}>
      {allSpecies?.map((s) => {
        const checked = selectedSpeciesIds.has(s.id)
        return (
          <label key={s.id} style={{ display: 'grid', gridTemplateColumns: '24px 1fr', gap: 12, padding: '10px 0', borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)', cursor: 'pointer' }}>
            <input type="checkbox" checked={checked} onChange={() => toggleSpecies(s.id)} />
            <span style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>
              {s.commonNameSv ?? s.commonName}
              {s.variantName && <span style={{ fontStyle: 'italic', color: 'var(--color-clay)' }}> '{s.variantName}'</span>}
            </span>
          </label>
        )
      })}
    </div>
  </div>
  <StickyFooter />
</div>
```

Preserve the selection Set state and the save mutation.

### Step 4: `OrgSetup.tsx`

Editorial pattern (pre-shell, no sidebar):

```tsx
export function OrgSetup() {
  const { t } = useTranslation()
  const [name, setName] = useState('')
  const [slug, setSlug] = useState('')
  const createMut = useMutation(/* api.orgs.create(...) */)

  return (
    <div style={{ minHeight: '100vh', background: 'var(--color-cream)' }}>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, padding: '22px 40px', borderBottom: '1px solid var(--color-ink)' }}>
        <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 32, fontWeight: 300, color: 'var(--color-ink)' }}>
          Verdant<span style={{ color: 'var(--color-clay)' }}>.</span>
        </span>
        <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: '0.08em', textTransform: 'uppercase', color: 'var(--color-forest)' }}>
          {t('app.subtitle')}
        </span>
      </div>

      <div style={{ maxWidth: 560, margin: '60px auto', padding: '0 24px' }}>
        <h1 style={{ fontFamily: 'var(--font-display)', fontSize: 48, fontWeight: 300, letterSpacing: -1, fontVariationSettings: '"SOFT" 100, "opsz" 144', margin: 0 }}>
          {t('orgSetup.hero.headline')}
        </h1>
        <p style={{ fontFamily: 'Georgia, var(--font-display)', fontSize: 16, lineHeight: 1.6, color: 'var(--color-forest)', marginTop: 22 }}>
          {t('orgSetup.intro')}
        </p>

        <div style={{ marginTop: 40, display: 'grid', gap: '20px' }}>
          <Field label={t('orgSettings.nameLabel')} editable value={name} onChange={setName} />
          <Field label={t('orgSettings.slugLabel')} editable value={slug} onChange={setSlug} />
        </div>

        <div style={{ marginTop: 40, textAlign: 'right' }}>
          <button className="btn-primary" onClick={() => createMut.mutate({ name, slug })} disabled={!name.trim()}>
            {t('orgSetup.submit')}
          </button>
        </div>
      </div>
    </div>
  )
}
```

### Step 5: i18n

Add to `sv.json` and `en.json`:

```json
"form": {
  "masthead": { "center": "— Formulär —" / "— Form —" },
  "sections": {
    "conditions": "Villkor" / "Conditions",
    "scheduling": "Schema"  / "Schedule",
    "details":    "Detaljer" / "Details"
  }
},
"orgSetup": {
  "hero":   { "headline": "Din första trädgård." / "Your first garden." },
  "intro":  "Välj ett namn för din organisation så kommer vi igång." / "Choose a name for your organisation and we'll get started.",
  "submit": "Skapa →" / "Create →"
},
"speciesGroups": {
  "members":  "Medlemmar" / "Members",
  "newTitle": "Ny grupp" / "New group"
}
```

### Step 6: Verify + commit

```bash
cd web && npx tsc --noEmit && npm run build 2>&1 | tail -3
git add web/src/pages/BedForm.tsx \
        web/src/pages/GardenForm.tsx \
        web/src/pages/SpeciesGroupEdit.tsx \
        web/src/pages/OrgSetup.tsx \
        web/src/i18n/sv.json web/src/i18n/en.json
git commit -m "feat: Fältet form ports — BedForm, GardenForm, SpeciesGroupEdit, OrgSetup"
```

---

## Task 2 — Activity forms

**Goal:** Port 3 activity-flavored forms. Each has multi-section structure.

**Files:** replace `web/src/pages/TaskForm.tsx`, `ApplySupply.tsx`, `SowActivity.tsx`.

### Step 1: `TaskForm.tsx`

Fields: task type dropdown, species picker, bed picker, due date, notes. Sections:

```tsx
<div>
  <Masthead left={<span>{t('nav.tasks')} / <span style={{ color: 'var(--color-clay)' }}>{isEdit ? t('tasks.editTitle') : t('tasks.newTitle')}</span></span>} center={t('form.masthead.center')} />
  <div style={{ padding: '28px 40px', paddingBottom: 120 }}>
    {/* § Detaljer */}
    <SectionLabel>§ {t('form.sections.details')}</SectionLabel>
    <div style={{ marginTop: 14, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px 28px' }}>
      <Field label={t('tasks.form.type')} editable value={value('activityType') ?? ''} onChange={(v) => set('activityType', v)} accent="clay" />
      <Field label={t('tasks.form.deadline')} editable value={String(value('deadline') ?? '')} onChange={(v) => set('deadline', v)} accent="mustard" />
    </div>

    {/* § Schema */}
    <div style={{ marginTop: 28 }}><SectionLabel>§ {t('form.sections.scheduling')}</SectionLabel></div>
    <div style={{ marginTop: 14, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px 28px' }}>
      {/* Preserve species picker + bed picker components as-is; wrap each in a div with a field-label above */}
      <div>
        <span className="field-label">{t('common.speciesLabel')}</span>
        <SpeciesAutocomplete value={value('speciesId')} onChange={(v) => set('speciesId', v)} />
      </div>
      <div>
        <span className="field-label">{t('tasks.form.bed')}</span>
        <BedPicker value={value('bedId')} onChange={(v) => set('bedId', v)} />
      </div>
    </div>

    {/* Notes */}
    <div style={{ marginTop: 28, border: '1px solid var(--color-ink)', background: 'var(--color-paper)', padding: '14px 16px' }}>
      <div className="field-label" style={{ marginBottom: 8 }}>{t('common.notesLabel')}</div>
      <textarea value={value('notes') ?? ''} onChange={(e) => set('notes', e.target.value)} style={{ width: '100%', minHeight: 80, background: 'transparent', border: 'none', outline: 'none', fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 16 }} />
    </div>
  </div>
  <StickyFooter />
</div>

function SectionLabel({ children }: { children: React.ReactNode }) {
  return <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.6 }}>{children}</div>
}
```

Preserve `SpeciesAutocomplete` and whatever bed-picker the current page uses.

### Step 2: `ApplySupply.tsx`

This page was added recently (fertilizer feature) and already uses some Fältet primitives. Read the current file and migrate any remaining inline Tailwind styling to:
- Form grid with `<Field editable>` for every input
- `<Chip>` for category filter indicators
- `<Masthead>` for the page header
- Sticky footer with `.btn-primary` / `.btn-secondary`

Preserve the two-scope radio (BED vs PLANTS), supply picker, quantity input with unit label + red hint on exceed, plant checklist in PLANTS mode. All mutations and query params (`bedId`, `plantIds`, `stepId`, `supplyTypeId`, `quantity`) stay.

### Step 3: `SowActivity.tsx`

Three sections: Species picker → Bed picker → Seed count + sowing date.

```tsx
<div>
  <Masthead left={t('nav.tasks')} center={t('sowing.masthead.center')} />
  <div style={{ padding: '28px 40px', paddingBottom: 120 }}>
    <SectionLabel>§ 1. {t('sowing.section.species')}</SectionLabel>
    <div style={{ marginTop: 14 }}>
      <SpeciesAutocomplete /* existing picker */ />
    </div>

    <div style={{ marginTop: 28 }}><SectionLabel>§ 2. {t('sowing.section.bed')}</SectionLabel></div>
    <div style={{ marginTop: 14 }}>
      {/* Existing bed picker */}
    </div>

    <div style={{ marginTop: 28 }}><SectionLabel>§ 3. {t('sowing.section.details')}</SectionLabel></div>
    <div style={{ marginTop: 14, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px 28px' }}>
      <Field label={t('sowing.seedCount')} editable value={String(value('seedCount') ?? '')} onChange={(v) => set('seedCount', Number(v) || null)} accent="mustard" />
      <Field label={t('sowing.date')} editable value={String(value('sowDate') ?? '')} onChange={(v) => set('sowDate', v)} accent="sage" />
    </div>
  </div>
  <StickyFooter />
</div>
```

Preserve autocomplete + bed picker + sow mutation.

### Step 4: i18n

Add:

```json
"sowing": {
  "masthead": { "center": "— Sådd —" / "— Sowing —" },
  "section": {
    "species": "Art"     / "Species",
    "bed":     "Bädd"    / "Bed",
    "details": "Detaljer" / "Details"
  },
  "seedCount": "Antal frön" / "Seed count",
  "date":      "Datum"      / "Date"
},
"tasks": {
  "editTitle": "Redigera uppgift" / "Edit task",
  "newTitle":  "Ny uppgift"        / "New task",
  "form": {
    "type":     "Typ"    / "Type",
    "deadline": "Deadline" / "Deadline",
    "bed":      "Bädd"     / "Bed"
  }
}
```

### Step 5: Verify + commit

```bash
cd web && npx tsc --noEmit && npm run build 2>&1 | tail -3
git add web/src/pages/TaskForm.tsx web/src/pages/ApplySupply.tsx web/src/pages/SowActivity.tsx web/src/i18n/*.json
git commit -m "feat: Fältet activity forms — TaskForm, ApplySupply, SowActivity"
```

---

## Task 3 — Workflow template editor

**Goal:** Port `WorkflowTemplateEdit` (304 lines) — ordered-list editor for workflow steps.

**File:** replace `web/src/pages/WorkflowTemplateEdit.tsx`.

### Step 1: Read existing structure

Identify in the current file:
- Template-level fields (name, description).
- Step list with drag/reorder.
- Add-step / delete-step controls.
- Conditional `suggestedSupplyTypeId` + `suggestedQuantity` fields on `APPLIED_SUPPLY` steps (fertilizer feature).

Preserve every mutation and every state.

### Step 2: Fältet shell

```tsx
<div>
  <Masthead
    left={<span>{t('nav.workflows')} / <span style={{ color: 'var(--color-clay)' }}>{template?.name ?? t('workflows.newTitle')}</span></span>}
    center={t('workflows.masthead.center')}
  />
  <div style={{ padding: '28px 40px', paddingBottom: 120 }}>
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px 28px' }}>
      <Field label={t('common.nameLabel')} editable value={value('name')} onChange={(v) => set('name', v)} />
      <Field label={t('common.descriptionLabel')} editable value={value('description')} onChange={(v) => set('description', v)} />
    </div>

    <div style={{ marginTop: 40, display: 'flex', alignItems: 'baseline', gap: 12 }}>
      <h2 style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 30, fontWeight: 300, margin: 0 }}>
        {t('workflows.steps')}<span style={{ color: 'var(--color-clay)' }}>.</span>
      </h2>
      <Rule inline variant="ink" />
      <button className="btn-secondary" onClick={addStep}>+ {t('workflows.addStep')}</button>
    </div>

    {/* Step rows — each row is a Fältet card */}
    {steps.map((step, i) => (
      <div key={step.id} style={{ display: 'grid', gridTemplateColumns: '40px 1.5fr 100px 140px 40px', gap: 18, padding: '14px 0', borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)', alignItems: 'center' }}>
        <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 22, color: 'var(--color-forest)' }}>
          {String(i + 1).padStart(2, '0')}
        </span>
        <Field label={t('workflows.stepName')} editable value={step.name} onChange={(v) => updateStep(step.id, { name: v })} />
        <Field label={t('workflows.daysAfter')} editable value={String(step.daysAfterPrevious ?? '')} onChange={(v) => updateStep(step.id, { daysAfterPrevious: Number(v) || null })} accent="mustard" />
        <div>
          <span className="field-label">{t('workflows.eventType')}</span>
          <select
            value={step.eventType ?? ''}
            onChange={(e) => updateStep(step.id, { eventType: e.target.value })}
            className="input"
          >
            {EVENT_TYPES.map((et) => <option key={et} value={et}>{t(`eventType.${et}`)}</option>)}
          </select>
        </div>
        <button onClick={() => removeStep(step.id)} style={{ background: 'transparent', border: 'none', color: 'var(--color-clay)', cursor: 'pointer', fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase' }}>
          ↵
        </button>

        {step.eventType === 'APPLIED_SUPPLY' && (
          <div style={{ gridColumn: '1 / -1', marginTop: 10, paddingLeft: 58, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 18 }}>
            <div>
              <span className="field-label">{t('supplyApplication.suggestedSupply')}</span>
              <select
                value={step.suggestedSupplyTypeId ?? ''}
                onChange={(e) => updateStep(step.id, { suggestedSupplyTypeId: e.target.value ? Number(e.target.value) : null })}
                className="input"
              >
                <option value="">{t('common.none')}</option>
                {supplyTypes?.filter((t: any) => t.category === 'FERTILIZER').map((st: any) => (
                  <option key={st.id} value={st.id}>{st.name}</option>
                ))}
              </select>
            </div>
            <Field
              label={t('supplyApplication.suggestedQuantity')}
              editable
              value={String(step.suggestedQuantity ?? '')}
              onChange={(v) => updateStep(step.id, { suggestedQuantity: v ? Number(v) : null })}
            />
          </div>
        )}
      </div>
    ))}
  </div>
  <StickyFooter />
</div>
```

Preserve the reorder-up / reorder-down / delete actions. If the current page uses HTML5 drag-and-drop, keep that logic; otherwise keep the existing arrow-button reordering.

### Step 3: i18n

```json
"workflows": {
  "newTitle":    "Ny mall"     / "New template",
  "masthead":    { "center": "— Mallen —" / "— The Template —" },
  "steps":       "Steg"     / "Steps",
  "addStep":     "Lägg till steg" / "Add step",
  "stepName":    "Namn"      / "Name",
  "daysAfter":   "Dagar efter föregående" / "Days after previous",
  "eventType":   "Händelsetyp" / "Event type"
}
```

### Step 4: Verify + commit

```bash
cd web && npx tsc --noEmit && npm run build 2>&1 | tail -3
git add web/src/pages/WorkflowTemplateEdit.tsx web/src/i18n/*.json
git commit -m "feat: Fältet port — WorkflowTemplateEdit"
```

---

## Task 4 — Detail pages

**Goal:** Port GardenDetail, PlantedSpeciesDetail, PlantDetail to the detail pattern.

**Files:** replace `web/src/pages/{GardenDetail,PlantedSpeciesDetail,PlantDetail}.tsx`.

### Step 1: `GardenDetail.tsx` (416 lines — largest)

Shape:

```tsx
<div>
  <Masthead
    left={<span>{t('nav.gardens')} / <span style={{ color: 'var(--color-clay)' }}>{garden?.name}</span></span>}
    center={t('garden.masthead.center')}
    right={<Link to={`/garden/${gardenId}/edit`} className="btn-secondary">{t('common.edit')}</Link>}
  />
  <div style={{ padding: '28px 40px' }}>
    {/* Hero */}
    <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: 40, alignItems: 'start' }}>
      <div>
        <h1 style={{ fontFamily: 'var(--font-display)', fontSize: 80, fontWeight: 300, letterSpacing: -1.5, lineHeight: 1, margin: 0, fontVariationSettings: '"SOFT" 100, "opsz" 144' }}>
          {garden?.emoji && <span style={{ marginRight: 16 }}>{garden.emoji}</span>}
          {garden?.name}<span style={{ color: 'var(--color-clay)' }}>.</span>
        </h1>
        {garden?.description && (
          <p style={{ marginTop: 16, fontFamily: 'Georgia, var(--font-display)', fontSize: 15, lineHeight: 1.6, color: 'var(--color-forest)' }}>
            {garden.description}
          </p>
        )}
      </div>
      <PhotoPlaceholder tone="sage" aspect="tall" label={`${garden?.name ?? ''}`.toUpperCase()} />
    </div>

    {/* Stats band */}
    <div style={{ margin: '40px 0', padding: '20px 0', borderTop: '1px solid var(--color-ink)', borderBottom: '1px solid var(--color-ink)', display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 18 }}>
      <Stat size="medium" value={beds?.length ?? 0}   label={t('garden.stats.activeBeds')}   hue="sage" />
      <Stat size="medium" value={plantCount}          label={t('garden.stats.activePlants')} hue="mustard" />
      <Stat size="medium" value={harvestStemsThisYear} unit="st" label={t('garden.stats.harvested')} hue="clay" />
    </div>

    {/* Beds section */}
    <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, marginBottom: 14 }}>
      <h2 style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 30, fontWeight: 300, margin: 0 }}>
        {t('garden.beds')}<span style={{ color: 'var(--color-clay)' }}>.</span>
      </h2>
      <Rule inline variant="ink" />
      <Link to={`/garden/${gardenId}/bed/new`} style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 16, color: 'var(--color-clay)', textDecoration: 'none' }}>
        + {t('garden.newBed')}
      </Link>
    </div>

    {beds?.length === 0 && <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', color: 'var(--color-forest)' }}>{t('garden.noBedsYet')}</p>}

    {beds?.map((bed, i) => (
      <Link
        key={bed.id}
        to={`/bed/${bed.id}`}
        style={{ display: 'grid', gridTemplateColumns: '50px 1.5fr auto 40px', gap: 18, padding: '14px 0', borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)', alignItems: 'center', textDecoration: 'none', color: 'var(--color-ink)' }}
      >
        <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 22, color: 'var(--color-sage)' }}>
          {String(i + 1).padStart(2, '0')}
        </span>
        <span style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>{bed.name}</span>
        <div style={{ display: 'flex', gap: 6 }}>
          {bed.sunExposure && <Chip tone="sage">{t(`bed.sun.${bed.sunExposure.toLowerCase()}`)}</Chip>}
          {bed.drainage && <Chip tone="sky">{bed.drainage}</Chip>}
          {bed.protection && <Chip tone="berry">{t(`bed.protection.${bed.protection.toLowerCase()}`)}</Chip>}
        </div>
        <span style={{ color: 'var(--color-clay)', fontFamily: 'var(--font-mono)' }}>→</span>
      </Link>
    ))}

    {/* Bottom row — harvest + danger */}
    <div style={{ marginTop: 40, display: 'grid', gridTemplateColumns: '1.6fr 1fr', gap: 22 }}>
      <HarvestStatCard /* same pattern as BedDetail's bottom card */ />
      <DangerCallout
        titleKey="garden.danger.title"
        warningKey="garden.danger.warning"
        deleteLabel={t('garden.danger.delete')}
        onDelete={() => confirmAndDelete()}
      />
    </div>
  </div>
</div>
```

Preserve all existing queries: `api.gardens.get(id)`, `api.gardens.beds(id)`, plant count aggregation, harvest stats, delete mutation.

### Step 2: `PlantedSpeciesDetail.tsx`

Simple page — Masthead + hero + ledger of plants:

```tsx
<div>
  <Masthead left={<span>{t('nav.plants')} / <span style={{ color: 'var(--color-clay)' }}>{species?.commonName}</span></span>} />
  <div style={{ padding: '28px 40px' }}>
    <h1 style={{ fontFamily: 'var(--font-display)', fontSize: 60, fontWeight: 300, letterSpacing: -1, margin: 0 }}>
      {species?.commonName}
      {species?.variantName && <span style={{ fontStyle: 'italic', color: 'var(--color-clay)' }}> '{species.variantName}'</span>}
    </h1>
    {species?.scientificName && (
      <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 14, color: 'var(--color-sage)', marginTop: 6 }}>
        {species.scientificName}
      </p>
    )}

    <div style={{ marginTop: 28 }}>
      <Ledger
        columns={[
          { key: 'id',     label: '№', width: '60px', render: (p, i) => <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 22, color: 'var(--color-sage)' }}>{String(i + 1).padStart(2, '0')}</span> },
          { key: 'bedName', label: t('plant.col.bed'), width: '1.5fr' },
          { key: 'status',  label: t('plant.col.status'), width: '120px' },
          { key: 'goto', label: '', width: '40px', align: 'right', render: () => '→' },
        ]}
        rows={plants}
        rowKey={(p: any) => p.id}
        onRowClick={(p: any) => navigate(`/plant/${p.id}`)}
      />
    </div>
  </div>
</div>
```

### Step 3: `PlantDetail.tsx`

Already has the GDD strip from spec 1. Finish porting:

```tsx
<div>
  <Masthead left={<span>{t('nav.plants')} / <span style={{ color: 'var(--color-clay)' }}>{plant?.name ?? t('plant.untitled')}</span></span>} />
  <div style={{ padding: '28px 40px' }}>
    {/* Hero */}
    <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: 40, alignItems: 'start' }}>
      <div>
        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 14 }}>
          {plant?.status && <Chip tone="sage">{t(`status.${plant.status}`)}</Chip>}
          {plant?.bedName && <Chip tone="mustard">{plant.bedName}</Chip>}
        </div>
        <h1 style={{ fontFamily: 'var(--font-display)', fontSize: 60, fontWeight: 300, letterSpacing: -1, margin: 0 }}>
          {plant?.name ?? plant?.speciesName}
        </h1>
        {plant?.variantName && (
          <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 18, color: 'var(--color-clay)', marginTop: 6 }}>
            '{plant.variantName}'
          </p>
        )}
      </div>
    </div>

    {/* GDD strip — preserved from existing implementation */}
    <div style={{ marginTop: 28 }}>
      <GDDStrip plantId={Number(id)} />
    </div>

    {/* Event timeline */}
    <div style={{ marginTop: 40, display: 'flex', alignItems: 'baseline', gap: 12 }}>
      <h2 style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 30, fontWeight: 300, margin: 0 }}>
        {t('plant.events')}<span style={{ color: 'var(--color-clay)' }}>.</span>
      </h2>
      <Rule inline variant="ink" />
      <button onClick={() => setShowAddEvent(true)} className="btn-secondary">+ {t('plant.addEvent')}</button>
    </div>

    <div style={{ marginTop: 14 }}>
      {events?.map((e, i) => (
        <EventRow key={e.id} event={e} index={i} />
      ))}
    </div>
  </div>

  {/* Existing add-event dialog */}
</div>
```

Preserve the `GDDStrip` component (already Fältet-styled in spec 1), the existing event timeline renderer with its `APPLIED_SUPPLY` case, and the add-event dialog.

### Step 4: i18n

```json
"garden": {
  "masthead": { "center": "— Trädgårdsliggaren —" / "— The Garden Ledger —" },
  "stats": {
    "activeBeds":   "aktiva bäddar"     / "active beds",
    "activePlants": "aktiva plantor"    / "active plants",
    "harvested":    "årets skörd"       / "harvested this year"
  },
  "danger": {
    "title":   "Farozon" / "Danger zone",
    "warning": "Permanent radering. Alla bäddar och plantor raderas." / "Permanent deletion. All beds and plants are removed.",
    "delete":  "Radera trädgård" / "Delete garden"
  }
},
"plant": {
  "untitled": "Utan namn" / "Untitled",
  "events":   "Händelser" / "Events",
  "addEvent": "Ny händelse" / "New event",
  "col": {
    "bed":    "Bädd"   / "Bed",
    "status": "Status" / "Status"
  }
}
```

### Step 5: Verify + commit

```bash
cd web && npx tsc --noEmit && npm run build 2>&1 | tail -3
git add web/src/pages/GardenDetail.tsx web/src/pages/PlantedSpeciesDetail.tsx web/src/pages/PlantDetail.tsx web/src/i18n/*.json
git commit -m "feat: Fältet detail ports — GardenDetail, PlantedSpeciesDetail, PlantDetail"
```

---

## Task 5 — Settings + meta + public

**Goal:** Port 5 pages: Account, OrgSettings (settings pattern), Guide (editorial-within-shell), LandingPage + PrivacyPolicy (editorial).

**Files:** replace `web/src/pages/{Account,OrgSettings,Guide,LandingPage,PrivacyPolicy}.tsx`.

### Step 1: `Account.tsx`

Settings pattern. Existing page has: advanced mode toggle, language preference, sign out, delete account.

```tsx
<div>
  <Masthead left={t('nav.account')} center={t('account.masthead.center')} />
  <div style={{ padding: '28px 40px', paddingBottom: 120 }}>
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px 28px' }}>
      <div>
        <span className="field-label">{t('account.advancedMode')}</span>
        <label style={{ display: 'flex', gap: 10, alignItems: 'center', marginTop: 6 }}>
          <input type="checkbox" checked={advanced} onChange={() => toggleAdvanced()} />
          <span style={{ fontFamily: 'var(--font-display)', fontSize: 16 }}>{t('account.advancedModeDescription')}</span>
        </label>
      </div>
      <div>
        <span className="field-label">{t('language.label')}</span>
        <select value={i18n.language} onChange={(e) => i18n.changeLanguage(e.target.value)} className="input">
          <option value="sv">Svenska</option>
          <option value="en">English</option>
        </select>
      </div>
    </div>

    <div style={{ marginTop: 40 }}>
      <button onClick={() => logout()} className="btn-secondary">{t('account.signOut')}</button>
    </div>

    {/* Farozon callout */}
    <div style={{ marginTop: 60, padding: '22px 28px', border: '1px solid color-mix(in srgb, var(--color-clay) 40%, transparent)' }}>
      <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-clay)', marginBottom: 10 }}>
        {t('common.dangerZone')}
      </div>
      <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 15 }}>
        {t('account.deleteAccountConfirm')}
      </p>
      <button onClick={() => setShowDelete(true)} style={{ background: 'transparent', border: 'none', fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-clay)', cursor: 'pointer' }}>
        → {t('account.deleteAccount')}
      </button>
    </div>
  </div>

  {/* Existing confirm dialog preserved */}
</div>
```

### Step 2: `OrgSettings.tsx`

Same settings pattern. Fields: org name, slug, member list. Destructive: leave org or delete org.

Preserve the member-list component from the existing page; wrap its rows in ledger-style hairlines.

### Step 3: `Guide.tsx`

Editorial within shell (authenticated, uses Layout):

```tsx
<div>
  <Masthead left={t('nav.guide')} center={t('guide.masthead.center')} />
  <div style={{ maxWidth: 860, margin: '40px auto', padding: '0 40px' }}>
    <h1 style={{ fontFamily: 'var(--font-display)', fontSize: 56, fontWeight: 300, letterSpacing: -1, margin: 0 }}>
      {t('guide.hero.headline')}
    </h1>

    {/* Preserve existing sections — each becomes a § Tema header + Fraunces Georgia body */}
    <section style={{ marginTop: 40 }}>
      <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7, marginBottom: 8 }}>
        § {t('guide.section.gettingStarted')}
      </div>
      <Rule variant="soft" />
      <p style={{ fontFamily: 'Georgia, var(--font-display)', fontSize: 16, lineHeight: 1.7, color: 'var(--color-forest)', marginTop: 14 }}>
        {t('guide.body.gettingStarted')}
      </p>
    </section>

    {/* more sections … */}
  </div>
</div>
```

Preserve every section's existing copy (just restyle presentation).

### Step 4: `LandingPage.tsx`

Editorial pattern outside Layout. Note: `App.tsx` routes `/login` to `<LandingPage />` — no changes needed there.

```tsx
export function LandingPage() {
  const { login, token } = useAuth()
  const navigate = useNavigate()
  const { t } = useTranslation()
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (token) navigate('/', { replace: true })
  }, [token, navigate])

  // Preserve Google initialize + credential handler from existing file

  return (
    <div style={{ minHeight: '100vh', background: 'var(--color-cream)' }}>
      {/* Top strip */}
      <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', padding: '22px 40px', borderBottom: '1px solid var(--color-ink)' }}>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 12 }}>
          <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 32, fontWeight: 300 }}>
            Verdant<span style={{ color: 'var(--color-clay)' }}>.</span>
          </span>
          <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: '0.08em', textTransform: 'uppercase', color: 'var(--color-forest)' }}>
            {t('app.subtitle')}
          </span>
        </div>
        {/* Login button rendered by Google into a div */}
        <div id="google-signin-btn" />
      </div>

      {/* Hero */}
      <div style={{ maxWidth: 860, margin: '80px auto 60px', padding: '0 40px' }}>
        <h1 style={{ fontFamily: 'var(--font-display)', fontSize: 80, fontWeight: 300, letterSpacing: -1.5, lineHeight: 1, margin: 0, fontVariationSettings: '"SOFT" 100, "opsz" 144' }}>
          {t('landing.hero.headline').split('.')[0]}
          <span style={{ color: 'var(--color-clay)' }}>.</span>
          <br />
          <span style={{ fontStyle: 'italic' }}>{t('landing.hero.headline').split('.')[1]}</span>
          <span style={{ color: 'var(--color-clay)' }}>.</span>
        </h1>
        <p style={{ fontFamily: 'Georgia, var(--font-display)', fontSize: 18, lineHeight: 1.6, color: 'var(--color-forest)', marginTop: 28 }}>
          {t('landing.hero.sub')}
        </p>
      </div>

      <Rule variant="ink" />

      {/* Features — 4-up grid, no emoji */}
      <div style={{ maxWidth: 1100, margin: '60px auto', padding: '0 40px' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 28 }}>
          {['gardens', 'plants', 'seeds', 'tasks'].map((key, i) => (
            <div key={key}>
              <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
                № {String(i + 1).padStart(2, '0')}
              </div>
              <div style={{ fontFamily: 'var(--font-display)', fontSize: 22, fontWeight: 300, marginTop: 6 }}>
                {t(`landing.features.${key}.title`)}
              </div>
              <p style={{ fontFamily: 'Georgia, var(--font-display)', fontSize: 15, lineHeight: 1.6, color: 'var(--color-forest)', marginTop: 8 }}>
                {t(`landing.features.${key}.description`)}
              </p>
            </div>
          ))}
        </div>
      </div>

      {error && (
        <p style={{ color: 'var(--color-clay)', textAlign: 'center', fontFamily: 'var(--font-display)', fontStyle: 'italic' }}>
          {error}
        </p>
      )}
    </div>
  )
}
```

### Step 5: `PrivacyPolicy.tsx`

Minimal editorial:

```tsx
<div style={{ minHeight: '100vh', background: 'var(--color-cream)' }}>
  <div style={{ padding: '22px 40px', borderBottom: '1px solid var(--color-ink)' }}>
    <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 32, fontWeight: 300 }}>
      Verdant<span style={{ color: 'var(--color-clay)' }}>.</span>
    </span>
  </div>

  <div style={{ maxWidth: 860, margin: '60px auto', padding: '0 40px' }}>
    <h1 style={{ fontFamily: 'var(--font-display)', fontSize: 56, fontWeight: 300, letterSpacing: -1, margin: 0 }}>
      {t('privacy.title')}
    </h1>

    {/* Preserve existing body copy — each section as § Header + Georgia paragraph */}
  </div>
</div>
```

### Step 6: i18n

```json
"account": {
  "masthead": { "center": "— Konto —" / "— Account —" }
},
"orgSettings": {
  "masthead": { "center": "— Organisation —" / "— Organisation —" }
},
"guide": {
  "hero":    { "headline": "Handbok." / "Handbook." },
  "section": {
    "gettingStarted": "Kom igång" / "Getting started",
    "daily":          "Dagligen"  / "Daily",
    "harvest":        "Skörd"     / "Harvest"
  }
},
"landing": {
  "hero": {
    "headline": "Odla vackert. Skörda mer." / "Grow beautifully. Harvest more.",
    "sub":      "Trädgårdsplanering för kommersiell snittblomsodling." / "Garden planning for commercial cut-flower growing."
  },
  "features": {
    "gardens":   { "title": "Trädgårdar", "description": "…" },
    "plants":    { "title": "Plantor",    "description": "…" },
    "seeds":     { "title": "Frölager",   "description": "…" },
    "tasks":     { "title": "Uppgifter",  "description": "…" }
  }
},
"privacy": {
  "title": "Integritetspolicy" / "Privacy policy"
}
```

Populate feature descriptions with the actual copy from the current `LandingPage.tsx` (existing i18n keys — grep first).

### Step 7: Verify + commit

```bash
cd web && npx tsc --noEmit && npm run build 2>&1 | tail -3
git add web/src/pages/Account.tsx web/src/pages/OrgSettings.tsx web/src/pages/Guide.tsx web/src/pages/LandingPage.tsx web/src/pages/PrivacyPolicy.tsx web/src/i18n/*.json
git commit -m "feat: Fältet ports — Account, OrgSettings, Guide, LandingPage, PrivacyPolicy"
```

---

## Task 6 — Milestone

### Step 1: Final verification

```bash
cd web && npx tsc --noEmit && npm run test && npm run build 2>&1 | tail -3
```

All green.

### Step 2: Milestone commit

```bash
git commit --allow-empty -m "milestone: Fältet dashboard redesign spec 2.2 complete

Full web-wide Fältet visual rollout done (except spec 3's three
design-dependent screens: CropCalendar, Analytics, WorkflowProgress).
16 pages ported in spec 2.2: 8 forms, 3 detail pages, 3 settings/meta,
2 public/unauth pages."
```

---

## Verification summary

After task 6:
- 16 pages ported.
- `npx tsc --noEmit` green.
- `npm run test` green.
- `npm run build` green.
- Spec 1 + 2.1 + 2.2 = full Fältet rollout except spec 3.

**Deferred:**
- Spec 3: CropCalendar, Analytics, WorkflowProgress — need design input.
- Real photography.
- Self-hosted fonts.
- Visual regression.
- Accessibility audit.
- Pagination restoration (carry-forward from spec 2.1).
