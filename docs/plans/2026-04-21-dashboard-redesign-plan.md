# Verdant Dashboard Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the four pixel-locked Fältet screens (Dashboard, Uppgifter, Bädd, Redigera art) plus the token/primitive/shell foundation the remaining ~30 screens will inherit via compat aliases.

**Architecture:** Replace the current Tailwind `@theme` block and `@utility` primitives with Fältet values; compat aliases map the old token names to new values so every existing page adopts cream + ink + Fraunces without per-page porting. Six shared primitives live under `web/src/components/faltet/`. A new grouped `Sidebar` replaces the flat nav. The four target pages are replaced in place; `SpeciesEditForm` is extracted and shared between a modal (from Bed row-click) and the `SpeciesDetail` page. Modal + bottom-sheet transitions use `framer-motion`. Design spec: `docs/plans/2026-04-21-dashboard-redesign-design.md`.

**Tech Stack:** React 19 + TypeScript + Vite + Tailwind v4 (@theme + @utility), TanStack Query, react-router-dom, react-i18next, framer-motion (new dep).

**Important notes:**
- Solo-dev, commits to `main`. No worktrees per `CLAUDE.md`.
- Data layer stays untouched; only rendering changes.
- Fonts via Google CDN in this spec; self-hosting is deferred.
- Real photography deferred; `PhotoPlaceholder` is used everywhere.
- Other ~30 screens keep their current layouts until spec 2. They render on the new palette + font via compat aliases.

---

## Phasing overview

| Task | Name | User-visible? |
|---|---|---|
| 1 | Tokens + fonts + utility overhaul | Yes — whole app re-skins on commit |
| 2 | Primitives (Chip, Rule, Stat, Field, PhotoPlaceholder, Masthead) | No (lib only) |
| 3 | Sidebar + Layout | Yes — sidebar reflows globally |
| 4 | Dashboard page | Yes |
| 5 | Uppgifter / TaskList page | Yes |
| 6 | Bädd + Redigera art | Yes |

---

## Task 1 — Tokens + fonts + utility overhaul

**Goal:** Replace the Tailwind token surface so every page inherits Fältet palette + Fraunces typography without touching page code.
**Deliverable:** `index.css` rewritten; `framer-motion` on the classpath; existing `btn-primary` / `btn-secondary` / `card` / `input` utilities render in Fältet shape.
**Dependencies:** none.

**Files:**
- Modify: `web/src/index.css` (current 155 lines)
- Modify: `web/package.json` (add `framer-motion`)
- Modify: `web/package-lock.json` (auto-generated)

### Step 1: Add framer-motion dependency

```bash
cd /Users/erik/development/verdant/web
npm install framer-motion@^11
```

Expected: installs without peer-dep warnings. `framer-motion` appears in `dependencies`.

### Step 2: Replace `web/src/index.css` wholesale

Overwrite the file with:

```css
/* Google Fonts — Fraunces variable (opsz 9..144, SOFT 0..100, wght 300..700, ital 0,1) + Inter */
@import url('https://fonts.googleapis.com/css2?family=Fraunces:ital,opsz,wght,SOFT@0,9..144,300..700,0..100;1,9..144,300..700,0..100&family=Inter:wght@400..700&display=swap');
@import "tailwindcss";

@theme {
  /* Fältet palette */
  --color-cream:   #F5EFE2;
  --color-paper:   #FBF7EC;
  --color-ink:     #1E241D;
  --color-forest:  #2F3D2E;
  --color-sage:    #6B8F6A;
  --color-clay:    #B6553C;
  --color-mustard: #C89A2B;
  --color-berry:   #7A2E44;
  --color-sky:     #4A7A8C;
  --color-butter:  #F2D27A;
  --color-blush:   #E9B8A8;

  /* Compat aliases — old utility-class lookups resolve to Fältet values */
  --color-bg:             var(--color-cream);
  --color-surface:        var(--color-cream);
  --color-sidebar:        var(--color-cream);
  --color-divider:        color-mix(in srgb, var(--color-ink) 20%, transparent);
  --color-text-primary:   var(--color-ink);
  --color-text-secondary: var(--color-forest);
  --color-text-muted:     color-mix(in srgb, var(--color-forest) 60%, transparent);
  --color-accent:         var(--color-clay);
  --color-accent-hover:   color-mix(in srgb, var(--color-clay) 85%, var(--color-ink));
  --color-accent-light:   var(--color-paper);
  --color-warm:           var(--color-paper);
  --color-error:          var(--color-clay);

  /* Typography */
  --font-display: "Fraunces", Georgia, serif;
  --font-body:    "Inter", -apple-system, BlinkMacSystemFont, sans-serif;
  --font-mono:    ui-monospace, "SF Mono", Menlo, monospace;
}

body {
  margin: 0;
  font-family: var(--font-body);
  background-color: var(--color-cream);
  color: var(--color-ink);
  -webkit-font-smoothing: antialiased;
  font-size: 14px;
}

@utility btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background-color: var(--color-ink);
  color: var(--color-cream);
  padding: 10px 22px;
  border-radius: 0;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 1.8px;
  text-transform: uppercase;
  border: 1px solid var(--color-ink);
  cursor: pointer;
  transition: background-color 0.15s ease;
  &:hover { background-color: var(--color-accent-hover); }
  &:disabled { opacity: 0.45; cursor: not-allowed; }
}

@utility btn-secondary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background-color: transparent;
  color: var(--color-ink);
  padding: 10px 18px;
  border-radius: 0;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 1.8px;
  text-transform: uppercase;
  border: 1px solid var(--color-ink);
  cursor: pointer;
  transition: background-color 0.15s ease;
  &:hover { background-color: color-mix(in srgb, var(--color-ink) 4%, transparent); }
  &:disabled { opacity: 0.45; cursor: not-allowed; }
}

@utility card {
  background-color: var(--color-paper);
  border: 1px solid var(--color-ink);
  border-radius: 0;
  padding: 1rem 1.125rem;
  box-shadow: none;
}

@utility input {
  background-color: transparent;
  border: none;
  border-bottom: 1px solid var(--color-ink);
  border-radius: 0;
  padding: 0.5rem 0;
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 300;
  color: var(--color-ink);
  outline: none;
  &:focus { border-bottom-color: var(--color-clay); }
}

.field-label {
  display: block;
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 1.4px;
  text-transform: uppercase;
  color: var(--color-forest);
  opacity: 0.7;
  margin-bottom: 4px;
}
```

Save the file.

### Step 3: Verify typecheck

```bash
cd /Users/erik/development/verdant/web && npx tsc --noEmit
```

Expected: exit 0.

### Step 4: Verify dev server boots

```bash
cd /Users/erik/development/verdant/web && npm run build 2>&1 | tail -5
```

Expected: `built in …` with no errors. Warnings about unused CSS utilities are OK.

### Step 5: Commit

```bash
git add web/src/index.css web/package.json web/package-lock.json
git commit -m "feat: Fältet token overhaul + framer-motion dependency

Replace the Tailwind @theme block with the Fältet palette (cream / ink /
clay / mustard / sage / sky / berry / butter / blush) and add compat
aliases for old token names so existing pages keep resolving.
Typography switches to Fraunces display + Inter body + mono meta.
btn-primary, btn-secondary, card, input utilities get their Fältet
shape: zero radius, 1px ink borders, no shadows, mono uppercase labels
on buttons, input as a Fraunces display on a bottom rule."
```

---

## Task 2 — Primitives

**Goal:** Six shared components under `web/src/components/faltet/` — the building blocks every screen uses.
**Deliverable:** `Chip`, `Rule`, `Stat`, `Field`, `PhotoPlaceholder`, `Masthead`, barrel export, one vitest test per primitive.
**Dependencies:** Task 1.

**Files:**
- Create: `web/src/components/faltet/Chip.tsx`
- Create: `web/src/components/faltet/Rule.tsx`
- Create: `web/src/components/faltet/Stat.tsx`
- Create: `web/src/components/faltet/Field.tsx`
- Create: `web/src/components/faltet/PhotoPlaceholder.tsx`
- Create: `web/src/components/faltet/Masthead.tsx`
- Create: `web/src/components/faltet/index.ts`
- Create: `web/src/components/faltet/Chip.test.tsx`
- Create: `web/src/components/faltet/Stat.test.tsx`
- Create: `web/src/components/faltet/Field.test.tsx`

### Step 1: Verify vitest is available

```bash
cd /Users/erik/development/verdant/web && cat package.json | grep -E 'vitest|@testing-library'
```

If absent: `npm install -D vitest @testing-library/react @testing-library/jest-dom jsdom @vitejs/plugin-react` then add to `package.json` scripts: `"test": "vitest run"`.

(If the project already has a different test runner, skip writing vitest tests — instead add a single `npm run build` smoke assertion per primitive file.)

### Step 2: Write `Chip.tsx`

```tsx
// web/src/components/faltet/Chip.tsx
import type { ReactNode } from 'react'

type Tone = 'clay' | 'mustard' | 'berry' | 'sky' | 'sage' | 'forest'

const TONE_VAR: Record<Tone, string> = {
  clay:    'var(--color-clay)',
  mustard: 'var(--color-mustard)',
  berry:   'var(--color-berry)',
  sky:     'var(--color-sky)',
  sage:    'var(--color-sage)',
  forest:  'var(--color-forest)',
}

export function Chip({ tone = 'forest', children }: { tone?: Tone; children: ReactNode }) {
  const color = TONE_VAR[tone]
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 6,
        fontFamily: 'var(--font-mono)',
        fontSize: 10,
        letterSpacing: 1.4,
        textTransform: 'uppercase',
        color,
        padding: '4px 8px',
        border: `1px solid ${color}`,
        borderRadius: 999,
      }}
    >
      {children}
    </span>
  )
}
```

### Step 3: Write `Rule.tsx`

```tsx
// web/src/components/faltet/Rule.tsx
type Variant = 'ink' | 'soft'

export function Rule({ variant = 'ink', inline = false }: { variant?: Variant; inline?: boolean }) {
  const borderColor =
    variant === 'ink'
      ? 'var(--color-ink)'
      : 'color-mix(in srgb, var(--color-ink) 20%, transparent)'
  if (inline) {
    return <span style={{ flex: 1, height: 1, borderTop: `1px solid ${borderColor}` }} />
  }
  return <hr style={{ border: 0, borderTop: `1px solid ${borderColor}`, margin: 0 }} />
}
```

### Step 4: Write `Stat.tsx`

```tsx
// web/src/components/faltet/Stat.tsx
type Hue = 'sage' | 'clay' | 'mustard' | 'sky' | 'berry'
type Size = 'large' | 'medium' | 'small'

const VALUE_SIZE: Record<Size, number> = { large: 88, medium: 56, small: 32 }
const UNIT_SIZE: Record<Size, number> = { large: 28, medium: 18, small: 14 }
const HUE_VAR: Record<Hue, string> = {
  sage:    'var(--color-sage)',
  clay:    'var(--color-clay)',
  mustard: 'var(--color-mustard)',
  sky:     'var(--color-sky)',
  berry:   'var(--color-berry)',
}

export function Stat({
  value,
  unit,
  label,
  delta,
  hue = 'sage',
  size = 'large',
}: {
  value: number | string
  unit?: string
  label: string
  delta?: string
  hue?: Hue
  size?: Size
}) {
  const hueVar = HUE_VAR[hue]
  return (
    <div>
      <div
        style={{
          fontFamily: 'var(--font-display)',
          fontSize: VALUE_SIZE[size],
          lineHeight: 0.95,
          fontWeight: 300,
          letterSpacing: -1.2,
          color: 'var(--color-ink)',
          fontVariationSettings: '"SOFT" 100, "opsz" 144',
        }}
      >
        {value}
        {unit && (
          <span style={{ fontSize: UNIT_SIZE[size], marginLeft: 4, color: hueVar, fontStyle: 'italic' }}>
            {unit}
          </span>
        )}
      </div>
      <div
        style={{
          fontFamily: 'var(--font-mono)',
          fontSize: 11,
          letterSpacing: 1.8,
          textTransform: 'uppercase',
          color: 'var(--color-forest)',
          marginTop: 10,
          display: 'flex',
          gap: 10,
          alignItems: 'center',
        }}
      >
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          <span style={{ width: 6, height: 6, borderRadius: 999, background: hueVar }} />
          {label}
        </span>
        {delta && <span style={{ color: 'var(--color-clay)' }}>▲ {delta}</span>}
      </div>
    </div>
  )
}
```

### Step 5: Write `Field.tsx`

```tsx
// web/src/components/faltet/Field.tsx
type Accent = 'clay' | 'mustard' | 'sage' | 'sky' | 'berry'

const ACCENT_VAR: Record<Accent, string> = {
  clay:    'var(--color-clay)',
  mustard: 'var(--color-mustard)',
  sage:    'var(--color-sage)',
  sky:     'var(--color-sky)',
  berry:   'var(--color-berry)',
}

type Props = {
  label: string
  value?: string
  accent?: Accent
} & (
  | { editable?: false; onChange?: never }
  | { editable: true; onChange: (v: string) => void; placeholder?: string }
)

export function Field(props: Props) {
  const color = props.accent ? ACCENT_VAR[props.accent] : 'var(--color-ink)'
  return (
    <label style={{ display: 'block' }}>
      <span
        style={{
          display: 'block',
          fontFamily: 'var(--font-mono)',
          fontSize: 9,
          letterSpacing: 1.4,
          textTransform: 'uppercase',
          color: 'var(--color-forest)',
          opacity: 0.7,
          marginBottom: 4,
        }}
      >
        {props.label}
      </span>
      {props.editable ? (
        <input
          value={props.value ?? ''}
          onChange={(e) => props.onChange(e.target.value)}
          placeholder={'placeholder' in props ? props.placeholder : undefined}
          style={{
            display: 'block',
            width: '100%',
            background: 'transparent',
            border: 'none',
            borderBottom: '1px solid var(--color-ink)',
            borderRadius: 0,
            padding: '4px 0',
            fontFamily: 'var(--font-display)',
            fontSize: 20,
            fontWeight: 300,
            color,
            outline: 'none',
          }}
        />
      ) : (
        <div
          style={{
            borderBottom: '1px solid var(--color-ink)',
            padding: '4px 0',
            fontFamily: 'var(--font-display)',
            fontSize: 20,
            fontWeight: 300,
            color,
          }}
        >
          {props.value ?? '—'}
        </div>
      )}
    </label>
  )
}
```

### Step 6: Write `PhotoPlaceholder.tsx`

```tsx
// web/src/components/faltet/PhotoPlaceholder.tsx
type Tone = 'sage' | 'blush' | 'butter'
type Aspect = 'wide' | 'tall' | 'square'

const TONE_RGB: Record<Tone, string> = {
  sage:   '107, 143, 106',
  blush:  '233, 184, 168',
  butter: '242, 210, 122',
}

const ASPECT_PADDING: Record<Aspect, string> = {
  wide:   '56.25%', // 16:9
  tall:   '140%',   // portrait
  square: '100%',
}

export function PhotoPlaceholder({
  tone = 'sage',
  label,
  aspect = 'wide',
}: {
  tone?: Tone
  label: string
  aspect?: Aspect
}) {
  const rgb = TONE_RGB[tone]
  return (
    <div
      style={{
        position: 'relative',
        width: '100%',
        paddingTop: ASPECT_PADDING[aspect],
        background: `radial-gradient(ellipse at 30% 30%, rgba(${rgb},0.35), rgba(${rgb},0.12) 60%, var(--color-cream))`,
        border: '1px solid var(--color-ink)',
      }}
    >
      <span
        style={{
          position: 'absolute',
          left: 12,
          bottom: 12,
          fontFamily: 'var(--font-mono)',
          fontSize: 9,
          letterSpacing: 1.4,
          textTransform: 'uppercase',
          color: 'var(--color-forest)',
          opacity: 0.7,
        }}
      >
        {label}
      </span>
    </div>
  )
}
```

### Step 7: Write `Masthead.tsx`

```tsx
// web/src/components/faltet/Masthead.tsx
import type { ReactNode } from 'react'

export function Masthead({
  left,
  center,
  right,
}: {
  left: ReactNode
  center?: ReactNode
  right?: ReactNode
}) {
  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: '1fr auto 1fr',
        alignItems: 'center',
        padding: '14px 22px',
        background: 'var(--color-cream)',
        borderBottom: '1px solid var(--color-ink)',
      }}
    >
      <div
        style={{
          fontFamily: 'var(--font-mono)',
          fontSize: 10,
          letterSpacing: 1.8,
          textTransform: 'uppercase',
          color: 'var(--color-forest)',
        }}
      >
        {left}
      </div>
      <div
        style={{
          fontFamily: 'var(--font-display)',
          fontStyle: 'italic',
          fontSize: 14,
          color: 'var(--color-forest)',
          textAlign: 'center',
          fontVariationSettings: '"SOFT" 100, "opsz" 144',
        }}
      >
        {center}
      </div>
      <div style={{ textAlign: 'right' }}>{right}</div>
    </div>
  )
}
```

### Step 8: Barrel export

```ts
// web/src/components/faltet/index.ts
export { Chip } from './Chip'
export { Rule } from './Rule'
export { Stat } from './Stat'
export { Field } from './Field'
export { PhotoPlaceholder } from './PhotoPlaceholder'
export { Masthead } from './Masthead'
```

### Step 9: Component tests

If vitest is present, write three representative tests (others follow the same pattern):

```tsx
// web/src/components/faltet/Chip.test.tsx
import { render, screen } from '@testing-library/react'
import { describe, expect, test } from 'vitest'
import { Chip } from './Chip'

describe('Chip', () => {
  test('renders children with default forest tone', () => {
    render(<Chip>Test</Chip>)
    const el = screen.getByText('Test')
    expect(el).toHaveStyle({ color: 'var(--color-forest)' })
  })

  test('applies clay tone color', () => {
    render(<Chip tone="clay">Harvest</Chip>)
    const el = screen.getByText('Harvest')
    expect(el).toHaveStyle({ color: 'var(--color-clay)' })
  })
})
```

```tsx
// web/src/components/faltet/Stat.test.tsx
import { render, screen } from '@testing-library/react'
import { describe, expect, test } from 'vitest'
import { Stat } from './Stat'

describe('Stat', () => {
  test('renders value and label', () => {
    render(<Stat value={42} unit="×" label="aktiva bäddar" />)
    expect(screen.getByText('42')).toBeInTheDocument()
    expect(screen.getByText('×')).toBeInTheDocument()
    expect(screen.getByText('aktiva bäddar')).toBeInTheDocument()
  })

  test('renders delta with up arrow when provided', () => {
    render(<Stat value={10} label="X" delta="+24%" />)
    expect(screen.getByText('▲ +24%')).toBeInTheDocument()
  })
})
```

```tsx
// web/src/components/faltet/Field.test.tsx
import { render, screen, fireEvent } from '@testing-library/react'
import { describe, expect, test, vi } from 'vitest'
import { Field } from './Field'

describe('Field', () => {
  test('renders readonly value', () => {
    render(<Field label="Sort" value="Café au Lait" />)
    expect(screen.getByText('Sort')).toBeInTheDocument()
    expect(screen.getByText('Café au Lait')).toBeInTheDocument()
  })

  test('editable calls onChange', () => {
    const onChange = vi.fn()
    render(<Field label="Sort" editable value="" onChange={onChange} />)
    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'Dahlia' } })
    expect(onChange).toHaveBeenCalledWith('Dahlia')
  })
})
```

### Step 10: Verify

```bash
cd /Users/erik/development/verdant/web && npx tsc --noEmit && npx vitest run
```

Both exit 0. (If vitest isn't set up and you deferred test writing, just typecheck.)

### Step 11: Commit

```bash
git add web/src/components/faltet/
git commit -m "feat: Fältet primitives — Chip, Rule, Stat, Field, PhotoPlaceholder, Masthead"
```

---

## Task 3 — Sidebar + Layout

**Goal:** New grouped sidebar replacing the flat 15-item list; Layout shell uses it; pages render via `<Outlet />`.
**Deliverable:** `Sidebar.tsx` with five nav groups + language switcher; `Layout.tsx` simplified to `<Sidebar /> + <Outlet />`.
**Dependencies:** Tasks 1–2.

**Files:**
- Create: `web/src/components/faltet/Sidebar.tsx`
- Modify: `web/src/components/Layout.tsx` (replace nav rendering)
- Modify: `web/src/i18n/sv.json` (add `app.subtitle`, sidebar group labels)
- Modify: `web/src/i18n/en.json` (same keys)

### Step 1: Sidebar component

```tsx
// web/src/components/faltet/Sidebar.tsx
import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

type NavItem = { to: string; label: string }
type NavGroup = { header: string; items: NavItem[] }

function useGroups(): NavGroup[] {
  const { t } = useTranslation()
  return [
    {
      header: t('sidebar.groups.odling'),
      items: [
        { to: '/',               label: t('nav.dashboard') },
        { to: '/gardens',        label: t('nav.gardens') },
        { to: '/species',        label: t('nav.species') },
        { to: '/species-groups', label: t('nav.speciesGroups') },
        { to: '/plants',         label: t('nav.plants') },
        { to: '/workflows',      label: t('nav.workflows') },
        { to: '/successions',    label: t('nav.successions') },
      ],
    },
    {
      header: t('sidebar.groups.uppgifter'),
      items: [
        { to: '/tasks',     label: t('nav.tasks') },
        { to: '/calendar',  label: t('nav.calendar') },
        { to: '/targets',   label: t('nav.targets') },
        { to: '/seed-stock', label: t('nav.seeds') },
        { to: '/supplies',  label: t('nav.supplies') },
      ],
    },
    {
      header: t('sidebar.groups.sales'),
      items: [
        { to: '/analytics?tab=harvest', label: t('nav.harvest') },
        { to: '/customers',             label: t('nav.customers') },
        { to: '/bouquets',              label: t('nav.bouquets') },
      ],
    },
    {
      header: t('sidebar.groups.analysis'),
      items: [
        { to: '/trials',       label: t('nav.trials') },
        { to: '/pest-disease', label: t('nav.pestDisease') },
        { to: '/analytics',    label: t('nav.analytics') },
      ],
    },
    {
      header: t('sidebar.groups.account'),
      items: [
        { to: '/guide',        label: t('nav.guide') },
        { to: '/org/settings', label: t('nav.orgSettings') },
        { to: '/account',      label: t('nav.account') },
      ],
    },
  ]
}

export function Sidebar() {
  const { t, i18n } = useTranslation()
  const location = useLocation()
  const groups = useGroups()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const currentPath = location.pathname + location.search

  const content = (
    <nav
      style={{
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        width: 220,
        background: 'var(--color-cream)',
        borderRight: '1px solid var(--color-ink)',
        padding: '24px 16px',
        boxSizing: 'border-box',
      }}
    >
      <div>
        <div
          style={{
            fontFamily: 'var(--font-display)',
            fontStyle: 'italic',
            fontSize: 26,
            fontWeight: 300,
            color: 'var(--color-ink)',
            lineHeight: 1,
          }}
        >
          Verdant<span style={{ color: 'var(--color-clay)' }}>.</span>
        </div>
        <div
          style={{
            fontFamily: 'var(--font-mono)',
            fontSize: 10,
            letterSpacing: '0.08em',
            textTransform: 'uppercase',
            color: 'var(--color-forest)',
            marginTop: 6,
          }}
        >
          {t('app.subtitle')}
        </div>
      </div>

      <div style={{ height: 1, background: 'var(--color-ink)', margin: '16px 0' }} />

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflowY: 'auto' }}>
        {groups.slice(0, 4).map((group) => (
          <GroupBlock key={group.header} group={group} currentPath={currentPath} />
        ))}

        <div style={{ marginTop: 'auto' }}>
          <GroupBlock group={groups[4]} currentPath={currentPath} />
        </div>
      </div>

      <div
        style={{
          display: 'flex',
          gap: 12,
          marginTop: 16,
          paddingTop: 12,
          borderTop: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
          fontFamily: 'var(--font-mono)',
          fontSize: 10,
          letterSpacing: 1.4,
          textTransform: 'uppercase',
        }}
      >
        {['sv', 'en'].map((lng) => (
          <button
            key={lng}
            onClick={() => {
              localStorage.setItem('verdant-lang', lng)
              i18n.changeLanguage(lng)
            }}
            style={{
              background: 'transparent',
              border: 'none',
              cursor: 'pointer',
              color: i18n.language === lng ? 'var(--color-clay)' : 'var(--color-forest)',
              padding: 0,
              fontFamily: 'inherit',
              fontSize: 'inherit',
              letterSpacing: 'inherit',
              textTransform: 'inherit',
            }}
          >
            {lng.toUpperCase()}
          </button>
        ))}
      </div>
    </nav>
  )

  return (
    <>
      {/* Mobile hamburger — visible <768px */}
      <button
        onClick={() => setDrawerOpen(true)}
        className="md:hidden"
        style={{
          position: 'fixed',
          top: 10,
          left: 10,
          zIndex: 30,
          background: 'var(--color-cream)',
          border: '1px solid var(--color-ink)',
          padding: '6px 10px',
          cursor: 'pointer',
          fontFamily: 'var(--font-mono)',
          fontSize: 16,
        }}
        aria-label="Menu"
      >
        ≡
      </button>

      {/* Desktop sidebar */}
      <div className="hidden md:block" style={{ height: '100vh', position: 'sticky', top: 0 }}>
        {content}
      </div>

      {/* Mobile drawer */}
      {drawerOpen && (
        <div
          onClick={() => setDrawerOpen(false)}
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(30,36,29,0.55)',
            zIndex: 40,
          }}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            style={{ position: 'absolute', top: 0, left: 0, height: '100vh' }}
          >
            {content}
          </div>
        </div>
      )}
    </>
  )
}

function GroupBlock({ group, currentPath }: { group: NavGroup; currentPath: string }) {
  return (
    <div style={{ marginBottom: 18 }}>
      <div
        style={{
          fontFamily: 'var(--font-mono)',
          fontSize: 9,
          letterSpacing: 1.4,
          textTransform: 'uppercase',
          color: 'var(--color-forest)',
          opacity: 0.7,
          padding: '12px 0 6px',
        }}
      >
        § {group.header}
      </div>
      {group.items.map((item) => {
        const active = currentPath === item.to || (item.to !== '/' && currentPath.startsWith(item.to))
        return (
          <Link
            key={item.to}
            to={item.to}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '10px 0',
              borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
              fontFamily: 'var(--font-display)',
              fontStyle: 'italic',
              fontSize: 16,
              color: active ? 'var(--color-clay)' : 'var(--color-ink)',
              textDecoration: 'none',
              fontVariationSettings: '"SOFT" 100, "opsz" 144',
            }}
          >
            <span>{item.label}</span>
            {active && <span style={{ color: 'var(--color-clay)' }}>●</span>}
          </Link>
        )
      })}
    </div>
  )
}
```

### Step 2: Replace Layout.tsx nav rendering

Open `web/src/components/Layout.tsx`, remove the existing nav/drawer implementation, replace with:

```tsx
// web/src/components/Layout.tsx (simplified)
import { Outlet } from 'react-router-dom'
import { Sidebar } from './faltet/Sidebar'

export function Layout() {
  return (
    <div style={{ display: 'flex', minHeight: '100vh', background: 'var(--color-cream)' }}>
      <Sidebar />
      <main style={{ flex: 1, minWidth: 0 }}>
        <Outlet />
      </main>
    </div>
  )
}
```

If the existing `Layout.tsx` carries other responsibilities (auth gate, onboarding wrapper, etc.), preserve those — this step only replaces the nav chrome.

### Step 3: Add i18n keys

Append to `web/src/i18n/sv.json` (merge into existing structure):

```json
{
  "app": {
    "subtitle": "Est. 2026"
  },
  "sidebar": {
    "groups": {
      "odling": "Odling",
      "uppgifter": "Uppgifter",
      "sales": "Skörd & Försäljning",
      "analysis": "Analys",
      "account": "Konto"
    }
  },
  "nav": {
    "dashboard":     "Översikt",
    "gardens":       "Trädgårdar",
    "species":       "Arter",
    "speciesGroups": "Artgrupper",
    "plants":        "Plantor",
    "workflows":     "Arbetsflöden",
    "successions":   "Successioner",
    "tasks":         "Uppgifter",
    "calendar":      "Kalender",
    "targets":       "Målmål",
    "seeds":         "Frölager",
    "supplies":      "Förbrukning",
    "harvest":       "Skörd",
    "customers":     "Kunder",
    "bouquets":      "Buketter",
    "trials":        "Försök",
    "pestDisease":   "Skadedjur",
    "analytics":     "Analys",
    "guide":         "Guide",
    "orgSettings":   "Org-inställningar",
    "account":       "Konto"
  }
}
```

And in `web/src/i18n/en.json`:

```json
{
  "app": {
    "subtitle": "Est. 2026"
  },
  "sidebar": {
    "groups": {
      "odling":     "Growing",
      "uppgifter":  "Tasks",
      "sales":      "Harvest & Sales",
      "analysis":   "Analytics",
      "account":    "Account"
    }
  },
  "nav": {
    "dashboard":     "Overview",
    "gardens":       "Gardens",
    "species":       "Species",
    "speciesGroups": "Species groups",
    "plants":        "Plants",
    "workflows":     "Workflows",
    "successions":   "Successions",
    "tasks":         "Tasks",
    "calendar":      "Calendar",
    "targets":       "Targets",
    "seeds":         "Seeds",
    "supplies":      "Supplies",
    "harvest":       "Harvest",
    "customers":     "Customers",
    "bouquets":      "Bouquets",
    "trials":        "Trials",
    "pestDisease":   "Pest & disease",
    "analytics":     "Analytics",
    "guide":         "Guide",
    "orgSettings":   "Org settings",
    "account":       "Account"
  }
}
```

If equivalent keys already exist with different names, reuse them rather than duplicating — grep first.

### Step 4: Verify

```bash
cd /Users/erik/development/verdant/web && npx tsc --noEmit && npm run build 2>&1 | tail -5
```

Both succeed.

### Step 5: Commit

```bash
git add web/src/components/faltet/Sidebar.tsx \
        web/src/components/Layout.tsx \
        web/src/i18n/sv.json \
        web/src/i18n/en.json
git commit -m "feat: Fältet sidebar with grouped nav + language switcher"
```

---

## Task 4 — Dashboard page

**Goal:** Replace `Dashboard.tsx` with the Fältet layout (asymmetric hero, three content columns, harvest band).
**Deliverable:** New Dashboard at `/` that reads the same data hooks and renders per spec §5.1.
**Dependencies:** Tasks 1–3.

**Files:**
- Modify: `web/src/pages/Dashboard.tsx` (replace entirely)
- Modify: `web/src/i18n/sv.json` (add `dashboard.*`)
- Modify: `web/src/i18n/en.json` (same)

### Step 1: New Dashboard.tsx

```tsx
// web/src/pages/Dashboard.tsx
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { Masthead, Stat, Rule, PhotoPlaceholder, Chip } from '../components/faltet'

export function Dashboard() {
  const { t } = useTranslation()

  const { data: beds } = useQuery({ queryKey: ['beds'], queryFn: () => api.beds.list() })
  const { data: trays } = useQuery({ queryKey: ['tray-summary'], queryFn: () => api.plants.traySummary() })
  const { data: tasks } = useQuery({
    queryKey: ['tasks', 'dashboard'],
    queryFn: () => api.tasks.list({ limit: 6 }),
  })

  const activeBedsCount = beds?.length ?? 0

  return (
    <div>
      <Masthead left={t('nav.dashboard')} center={t('dashboard.masthead.center')} />

      <div style={{ padding: '28px 40px' }}>
        {/* Hero */}
        <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 40, alignItems: 'center' }}>
          <Stat
            size="large"
            value={activeBedsCount}
            unit="×"
            label={t('dashboard.hero.label')}
            hue="sage"
          />
          <PhotoPlaceholder tone="sage" aspect="wide" label={t('dashboard.hero.photoLabel')} />
        </div>

        <div style={{ margin: '28px 0' }}>
          <Rule variant="ink" />
        </div>

        {/* Three content columns */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, 1fr)',
            gap: 0,
          }}
        >
          <section style={{ padding: '0 22px 0 0', borderRight: '1px solid var(--color-ink)' }}>
            <ColumnHeader title={t('dashboard.trays.title')} />
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
                <span>{row.speciesName ?? row.name}</span>
                <span style={{ textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>
                  {row.count}
                </span>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, textAlign: 'right' }}>
                  {row.sownAt ?? '—'}
                </span>
              </div>
            ))}
          </section>

          <section style={{ padding: '0 22px', borderRight: '1px solid var(--color-ink)' }}>
            <ColumnHeader title={t('dashboard.tasks.title')} right={<Link to="/tasks" style={{ color: 'var(--color-clay)', textDecoration: 'none' }}>→</Link>} />
            {tasks?.slice(0, 6).map((t) => (
              <div
                key={t.id}
                style={{
                  display: 'grid',
                  gridTemplateColumns: '40px 1fr 80px',
                  gap: 10,
                  padding: '10px 0',
                  borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                  alignItems: 'center',
                }}
              >
                <span
                  style={{
                    fontFamily: 'var(--font-display)',
                    fontStyle: 'italic',
                    fontSize: 20,
                    color: 'var(--color-clay)',
                    fontVariationSettings: '"SOFT" 100, "opsz" 144',
                  }}
                >
                  №
                </span>
                <span style={{ fontFamily: 'var(--font-display)', fontSize: 16 }}>{t.title}</span>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, textAlign: 'right' }}>
                  {t.dueDate ?? '—'}
                </span>
              </div>
            ))}
          </section>

          <section style={{ padding: '0 0 0 22px' }}>
            <ColumnHeader title={t('dashboard.beds.title')} />
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
              {beds?.map((b) => (
                <Chip key={b.id} tone={b.status === 'full' ? 'sage' : b.status === 'partial' ? 'mustard' : 'forest'}>
                  № {b.id} · {b.name}
                </Chip>
              ))}
            </div>
          </section>
        </div>

        <div style={{ margin: '28px 0 16px' }}>
          <Rule variant="ink" />
        </div>

        {/* Harvest totals band */}
        <div
          style={{
            background: 'var(--color-ink)',
            color: 'var(--color-cream)',
            padding: '22px 28px',
            position: 'relative',
            overflow: 'hidden',
          }}
        >
          <div
            style={{
              position: 'absolute',
              top: -40,
              right: -40,
              width: 140,
              height: 140,
              borderRadius: '50%',
              background: 'var(--color-butter)',
              opacity: 0.2,
            }}
          />
          <div
            style={{
              fontFamily: 'var(--font-display)',
              fontStyle: 'italic',
              fontSize: 26,
              fontVariationSettings: '"SOFT" 100, "opsz" 144',
            }}
          >
            {t('dashboard.harvest.headline', { stems: 142, year: 2025 })}{' '}
            <span style={{ color: 'var(--color-blush)' }}>{t('dashboard.harvest.season', { year: 2025 })}</span>.
          </div>
          <div
            style={{
              marginTop: 12,
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              display: 'flex',
              gap: 18,
            }}
          >
            <span style={{ color: 'var(--color-sage)' }}>{t('dashboard.harvest.bestWeek', { week: 32 })}</span>
            <span style={{ color: 'var(--color-blush)' }}>+24 % vs 2024 ▲</span>
          </div>
        </div>
      </div>
    </div>
  )
}

function ColumnHeader({ title, right }: { title: string; right?: React.ReactNode }) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 10,
        padding: '0 0 10px',
        borderBottom: '1px solid var(--color-ink)',
        marginBottom: 6,
      }}
    >
      <span
        style={{
          fontFamily: 'var(--font-display)',
          fontStyle: 'italic',
          fontSize: 22,
          fontVariationSettings: '"SOFT" 100, "opsz" 144',
        }}
      >
        {title}<span style={{ color: 'var(--color-clay)' }}>.</span>
      </span>
      <Rule inline variant="soft" />
      {right}
    </div>
  )
}
```

If `api.dashboard.get()`, `api.plants.traySummary()`, `api.tasks.list(...)`, or `api.beds.list()` have different return shapes than the fields referenced above (e.g. `row.sownAt`, `t.dueDate`, `b.status`), adjust — the rendering is the contract, not the field names. The engineer should check the actual response types and rename field accesses as needed.

### Step 2: i18n keys

Append to `web/src/i18n/sv.json`:

```json
{
  "dashboard": {
    "masthead": { "center": "— Fältliggaren —" },
    "hero": {
      "label":      "aktiva bäddar",
      "photoLabel": "NORRA FÄLTET · JULI"
    },
    "trays": { "title": "Brickor" },
    "tasks": { "title": "Uppgifter" },
    "beds":  { "title": "Bäddar" },
    "harvest": {
      "headline": "{{stems}} stjälkar skördade från fältet",
      "season":   "säsong {{year}}",
      "bestWeek": "bästa vecka: v.{{week}}"
    }
  }
}
```

And `en.json`:

```json
{
  "dashboard": {
    "masthead": { "center": "— The Field Ledger —" },
    "hero": {
      "label":      "active beds",
      "photoLabel": "NORTH FIELD · JULY"
    },
    "trays": { "title": "Trays" },
    "tasks": { "title": "Tasks" },
    "beds":  { "title": "Beds" },
    "harvest": {
      "headline": "{{stems}} stems harvested from the field",
      "season":   "season {{year}}",
      "bestWeek": "best week: w.{{week}}"
    }
  }
}
```

### Step 3: Verify + commit

```bash
cd /Users/erik/development/verdant/web && npx tsc --noEmit
```

Green.

```bash
git add web/src/pages/Dashboard.tsx web/src/i18n/sv.json web/src/i18n/en.json
git commit -m "feat: Fältet Dashboard — asymmetric hero, three columns, harvest band"
```

---

## Task 5 — Uppgifter / TaskList page

**Goal:** Replace `TaskList.tsx` with the Fältet ledger (filter pills + Idag + Kommande sections).
**Deliverable:** New TaskList at `/tasks` with filter persistence, side drawer for row clicks.
**Dependencies:** Tasks 1–3.

**Files:**
- Modify: `web/src/pages/TaskList.tsx` (replace)
- Modify: `web/src/i18n/sv.json` + `en.json` (add `tasks.*`)

### Step 1: New TaskList.tsx

```tsx
// web/src/pages/TaskList.tsx
import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { Masthead, Rule } from '../components/faltet'
import { Dialog } from '../components/Dialog'

type ActivityType = 'harvest' | 'sowing' | 'watering' | 'planting' | 'maintenance'
const ACTIVITIES: ActivityType[] = ['harvest', 'sowing', 'watering', 'planting', 'maintenance']
const TONE: Record<ActivityType, string> = {
  harvest:     'var(--color-clay)',
  sowing:      'var(--color-mustard)',
  watering:    'var(--color-sky)',
  planting:    'var(--color-sage)',
  maintenance: 'var(--color-berry)',
}

function loadFilters(): ActivityType[] {
  try {
    const raw = localStorage.getItem('verdant-task-filters')
    const parsed = raw ? (JSON.parse(raw) as ActivityType[]) : ACTIVITIES
    return parsed.length ? parsed : ACTIVITIES
  } catch {
    return ACTIVITIES
  }
}

function activityFromTaskType(taskType: string): ActivityType {
  const t = taskType.toLowerCase()
  if (t.includes('harvest') || t.includes('skörd')) return 'harvest'
  if (t.includes('sow') || t.includes('sådd'))     return 'sowing'
  if (t.includes('water') || t.includes('vattn'))  return 'watering'
  if (t.includes('plant') || t.includes('planter')) return 'planting'
  return 'maintenance'
}

export function TaskList() {
  const { t } = useTranslation()
  const [filters, setFilters] = useState<ActivityType[]>(loadFilters)
  const [drawerTask, setDrawerTask] = useState<null | { id: number; title: string }>(null)

  useEffect(() => {
    localStorage.setItem('verdant-task-filters', JSON.stringify(filters))
  }, [filters])

  const { data: tasks = [] } = useQuery({ queryKey: ['tasks', 'all'], queryFn: () => api.tasks.list({}) })

  const today = new Date().toISOString().slice(0, 10)
  const isToday = (d?: string) => d === today
  const isFuture = (d?: string) => !!d && d > today

  const filtered = tasks.filter((task) => filters.includes(activityFromTaskType(task.type ?? '')))
  const todays = filtered.filter((t) => isToday(t.dueDate ?? undefined))
  const upcoming = filtered.filter((t) => isFuture(t.dueDate ?? undefined))

  const toggleFilter = (a: ActivityType) => {
    setFilters((cur) => {
      const has = cur.includes(a)
      if (has && cur.length === 1) return cur // at-least-one rule
      return has ? cur.filter((x) => x !== a) : [...cur, a]
    })
  }

  return (
    <div>
      <Masthead left={t('nav.tasks')} center={t('tasks.masthead.center')} />

      <div style={{ padding: '28px 40px' }}>
        {/* Filter pills */}
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 28 }}>
          {ACTIVITIES.map((a) => {
            const active = filters.includes(a)
            const color = TONE[a]
            return (
              <button
                key={a}
                onClick={() => toggleFilter(a)}
                style={{
                  fontFamily: 'var(--font-mono)',
                  fontSize: 10,
                  letterSpacing: 1.4,
                  textTransform: 'uppercase',
                  padding: '6px 12px',
                  borderRadius: 999,
                  border: `1px solid ${color}`,
                  background: active ? color : 'transparent',
                  color: active ? 'var(--color-cream)' : color,
                  cursor: 'pointer',
                }}
              >
                {t(`tasks.filters.${a}`)}
              </button>
            )
          })}
        </div>

        {/* Idag */}
        <SectionHeader title={t('tasks.today')} count={todays.length} />
        {todays.map((task) => (
          <TaskRow key={task.id} task={task} onOpen={() => setDrawerTask({ id: task.id, title: task.title })} />
        ))}

        <div style={{ height: 40 }} />

        {/* Kommande */}
        <SectionHeader title={t('tasks.upcoming')} count={upcoming.length} />
        {upcoming.map((task) => (
          <TaskRow key={task.id} task={task} onOpen={() => setDrawerTask({ id: task.id, title: task.title })} />
        ))}
      </div>

      {/* Side drawer (reuse Dialog) */}
      <Dialog open={drawerTask !== null} onClose={() => setDrawerTask(null)} title={drawerTask?.title ?? ''}>
        <p style={{ fontFamily: 'var(--font-display)', fontSize: 16 }}>
          {t('tasks.drawer.placeholder')}
        </p>
      </Dialog>
    </div>
  )
}

function SectionHeader({ title, count }: { title: string; count: number }) {
  return (
    <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, marginBottom: 10 }}>
      <h2
        style={{
          fontFamily: 'var(--font-display)',
          fontStyle: 'italic',
          fontSize: 30,
          fontWeight: 300,
          margin: 0,
          fontVariationSettings: '"SOFT" 100, "opsz" 144',
        }}
      >
        {title}<span style={{ color: 'var(--color-clay)' }}>.</span>
      </h2>
      <Rule inline variant="ink" />
      <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase' }}>
        {count}
      </span>
    </div>
  )
}

function TaskRow({ task, onOpen }: { task: any; onOpen: () => void }) {
  const activity = activityFromTaskType(task.type ?? '')
  const color = TONE[activity]
  return (
    <button
      onClick={onOpen}
      style={{
        display: 'grid',
        gridTemplateColumns: '60px 1.5fr 140px 120px 80px',
        gap: 18,
        padding: '16px 0',
        borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 25%, transparent)',
        width: '100%',
        background: 'transparent',
        border: 'none',
        textAlign: 'left',
        cursor: 'pointer',
        alignItems: 'center',
      }}
      className="task-row"
    >
      <span
        style={{
          fontFamily: 'var(--font-display)',
          fontStyle: 'italic',
          fontSize: 26,
          color,
          fontVariationSettings: '"SOFT" 100, "opsz" 144',
        }}
      >
        №
      </span>
      <div>
        <div style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>{task.title}</div>
        <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
          {task.type}
        </div>
      </div>
      <div style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 16 }}>
        {task.bedName ?? ''} <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, marginLeft: 6 }}>№ {task.bedId ?? '—'}</span>
      </div>
      <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase' }}>
        {task.dueDate ?? ''}
      </div>
      <div style={{ textAlign: 'right', fontFamily: 'var(--font-mono)', fontSize: 16 }}>→</div>
    </button>
  )
}
```

Field accesses (`task.type`, `task.bedName`, `task.bedId`, `task.dueDate`) should match the project's actual Task shape — inspect `api.tasks.list()` return type and adapt. The `activityFromTaskType` mapping is best-effort; if the project has a proper enum, use it directly.

### Step 2: i18n keys

Append to `sv.json`:

```json
{
  "tasks": {
    "masthead": { "center": "— Dagens rader —" },
    "today":    "Idag",
    "upcoming": "Kommande",
    "filters": {
      "harvest":     "Skörd",
      "sowing":      "Sådd",
      "watering":    "Vattna",
      "planting":    "Plantera",
      "maintenance": "Underhåll"
    },
    "drawer": { "placeholder": "Uppgiftsdetaljer kommer snart." }
  }
}
```

And `en.json`:

```json
{
  "tasks": {
    "masthead": { "center": "— Today's rows —" },
    "today":    "Today",
    "upcoming": "Upcoming",
    "filters": {
      "harvest":     "Harvest",
      "sowing":      "Sowing",
      "watering":    "Water",
      "planting":    "Plant",
      "maintenance": "Care"
    },
    "drawer": { "placeholder": "Task details coming soon." }
  }
}
```

### Step 3: Verify + commit

```bash
cd /Users/erik/development/verdant/web && npx tsc --noEmit
git add web/src/pages/TaskList.tsx web/src/i18n/sv.json web/src/i18n/en.json
git commit -m "feat: Fältet Uppgifter — filter pills + Idag/Kommande ledger + drawer stub"
```

---

## Task 6 — Bädd + Redigera art

**Goal:** Replace `BedDetail.tsx` with the Fältet bed ledger; extract `SpeciesEditForm` from existing species logic; wrap in a framer-motion modal (from Bed row clicks) and a thin page wrapper at `/species/:id`.
**Deliverable:** Bed detail renders the hero + stats + plant ledger + danger callout; row clicks open the modal; `/species/:id` page uses the shared form; mobile bottom-sheet with drag-to-dismiss.
**Dependencies:** Tasks 1–3.

**Files:**
- Modify: `web/src/pages/BedDetail.tsx` (replace entirely)
- Modify: `web/src/pages/SpeciesDetail.tsx` (thin wrapper)
- Create: `web/src/components/faltet/SpeciesEditForm.tsx`
- Create: `web/src/components/faltet/SpeciesEditModal.tsx`
- Modify: `web/src/i18n/sv.json` + `en.json` (add `bed.*`, `species.*`)

This task is the largest; split into five commits as you go.

### Step 1: Extract `SpeciesEditForm.tsx`

Read the current `web/src/pages/SpeciesDetail.tsx` (384 lines) to identify:
- which fields the form manages (name, latin binomial, cultivation windows, seed providers, notes, photos, workflow-step suggestions added in the fertilizer feature)
- the existing save mutation (PATCH species)
- the seed-provider sub-components

Create `web/src/components/faltet/SpeciesEditForm.tsx` that accepts `{ speciesId, onSaved? }` and renders:

```tsx
// web/src/components/faltet/SpeciesEditForm.tsx
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../../api/client'
import { Field, Rule, Chip, PhotoPlaceholder } from './index'

export function SpeciesEditForm({ speciesId, onSaved }: { speciesId: number; onSaved?: () => void }) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const { data: species } = useQuery({
    queryKey: ['species', speciesId],
    queryFn: () => api.species.get(speciesId),
  })

  const [draft, setDraft] = useState<Record<string, any>>({})
  const value = (k: string) => (k in draft ? draft[k] : (species as any)?.[k] ?? '')
  const set = (k: string, v: any) => setDraft((d) => ({ ...d, [k]: v }))

  const saveMut = useMutation({
    mutationFn: () => api.species.update(speciesId, draft),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['species', speciesId] })
      onSaved?.()
    },
  })

  if (!species) return null

  return (
    <div style={{ padding: '22px 28px' }}>
      {/* Hero */}
      <div style={{ display: 'grid', gridTemplateColumns: '96px 1fr auto', gap: 20, alignItems: 'start' }}>
        <div style={{ width: 96, height: 96 }}>
          <PhotoPlaceholder tone="blush" aspect="square" label="ART · Foto" />
        </div>
        <div>
          <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 10 }}>
            {(species as any).plantType && <Chip tone="berry">{(species as any).plantType}</Chip>}
            <Chip tone="sage">Full sol</Chip>
          </div>
          <div
            style={{
              fontFamily: 'var(--font-display)',
              fontSize: 44,
              fontWeight: 300,
              letterSpacing: -1,
              fontVariationSettings: '"SOFT" 100, "opsz" 144',
            }}
          >
            {(species as any).commonName}
            {(species as any).variantName && (
              <span style={{ fontStyle: 'italic', color: 'var(--color-clay)' }}> ‘{(species as any).variantName}’</span>
            )}
          </div>
          <div
            style={{
              fontFamily: 'var(--font-display)',
              fontStyle: 'italic',
              fontSize: 14,
              color: 'var(--color-sage)',
              marginTop: 4,
            }}
          >
            {(species as any).latinBinomial ?? ''}
          </div>
        </div>
        <div
          style={{
            fontFamily: 'var(--font-display)',
            fontStyle: 'italic',
            fontSize: 22,
            color: 'var(--color-mustard)',
          }}
        >
          № {String(species.id).padStart(3, '0')}
        </div>
      </div>

      <div style={{ margin: '22px 0' }}><Rule variant="ink" /></div>

      {/* Form grid */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px 28px' }}>
        <Field label={t('species.fields.sortSv')} editable value={value('variantNameSv')} onChange={(v) => set('variantNameSv', v)} accent="clay" />
        <Field label={t('species.fields.sortEn')} editable value={value('variantName')}   onChange={(v) => set('variantName', v)}   accent="clay" />
        <Field label={t('species.fields.sowStart')}    editable value={value('sowStart')}    onChange={(v) => set('sowStart', v)}    accent="mustard" />
        <Field label={t('species.fields.sowEnd')}      editable value={value('sowEnd')}      onChange={(v) => set('sowEnd', v)}      accent="mustard" />
        <Field label={t('species.fields.plantStart')}  editable value={value('plantStart')}  onChange={(v) => set('plantStart', v)}  accent="sage" />
        <Field label={t('species.fields.harvestStart')} editable value={value('harvestStart')} onChange={(v) => set('harvestStart', v)} accent="clay" />
      </div>

      <div style={{ margin: '28px 0 16px', fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.6 }}>
        {t('species.section.cultivation')}
      </div>

      <div
        style={{
          border: '1px solid var(--color-ink)',
          background: 'var(--color-paper)',
          padding: '14px 16px',
          minHeight: 120,
        }}
      >
        <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7, marginBottom: 8 }}>
          {t('species.notes.label')}
        </div>
        <textarea
          value={value('cultivationNotes')}
          onChange={(e) => set('cultivationNotes', e.target.value)}
          style={{
            width: '100%',
            minHeight: 80,
            background: 'transparent',
            border: 'none',
            outline: 'none',
            resize: 'vertical',
            fontFamily: 'var(--font-display)',
            fontStyle: 'italic',
            fontSize: 16,
            color: 'var(--color-ink)',
          }}
        />
      </div>

      {/* Save button is owned by caller (modal footer / page footer). Expose via ref? */}
      <div style={{ marginTop: 28, display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
        <button onClick={() => saveMut.mutate()} disabled={saveMut.isPending || Object.keys(draft).length === 0} className="btn-primary">
          {saveMut.isPending ? t('species.edit.saving') : t('species.edit.save')}
        </button>
      </div>
    </div>
  )
}
```

Adjust to match the actual `api.species.get()` / `api.species.update()` return shapes. If cultivation-window field names differ (e.g., `sowingStartDate` vs `sowStart`), match the real ones.

Commit 6a:

```bash
git add web/src/components/faltet/SpeciesEditForm.tsx
git commit -m "feat: extract SpeciesEditForm with Fältet layout"
```

### Step 2: Create `SpeciesEditModal.tsx`

```tsx
// web/src/components/faltet/SpeciesEditModal.tsx
import { motion, AnimatePresence, PanInfo } from 'framer-motion'
import { useTranslation } from 'react-i18next'
import { useEffect, useState } from 'react'
import { SpeciesEditForm } from './SpeciesEditForm'
import { Masthead } from './Masthead'

export function SpeciesEditModal({
  speciesId,
  onClose,
}: {
  speciesId: number | null
  onClose: () => void
}) {
  const { t } = useTranslation()
  const [isMobile, setIsMobile] = useState(window.innerWidth < 768)

  useEffect(() => {
    const onResize = () => setIsMobile(window.innerWidth < 768)
    window.addEventListener('resize', onResize)
    return () => window.removeEventListener('resize', onResize)
  }, [])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  const handleDragEnd = (_: any, info: PanInfo) => {
    if (info.offset.y > 80) onClose()
  }

  return (
    <AnimatePresence>
      {speciesId !== null && (
        <>
          <motion.div
            key="scrim"
            onClick={onClose}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.18, ease: 'easeOut' }}
            style={{
              position: 'fixed',
              inset: 0,
              background: 'rgba(30,36,29,0.55)',
              backdropFilter: 'blur(1.5px)',
              zIndex: 100,
            }}
          />
          <motion.div
            key="modal"
            initial={isMobile ? { y: '100%' } : { scale: 0.97, y: 6, opacity: 0 }}
            animate={isMobile ? { y: '0%' } : { scale: 1, y: 0, opacity: 1 }}
            exit={isMobile ? { y: '100%' } : { scale: 0.97, y: 6, opacity: 0 }}
            transition={{
              duration: isMobile ? 0.26 : 0.22,
              ease: [0.22, 1, 0.36, 1],
            }}
            drag={isMobile ? 'y' : false}
            dragConstraints={{ top: 0, bottom: 0 }}
            onDragEnd={handleDragEnd}
            style={{
              position: 'fixed',
              zIndex: 110,
              background: 'var(--color-paper)',
              border: '1px solid var(--color-ink)',
              boxShadow: '24px 24px 0 rgba(30,36,29,0.15)',
              ...(isMobile
                ? { left: 0, right: 0, bottom: 0, maxHeight: '86vh', overflowY: 'auto' }
                : {
                    left: '50%',
                    top: '50%',
                    transform: 'translate(-50%, -50%)',
                    width: 760,
                    maxHeight: '86vh',
                    overflowY: 'auto',
                  }),
            }}
          >
            {isMobile && (
              <div style={{ display: 'flex', justifyContent: 'center', padding: '10px 0 0' }}>
                <div style={{ width: 44, height: 3, borderRadius: 999, background: 'var(--color-ink)', opacity: 0.4 }} />
              </div>
            )}
            <Masthead
              left={t('species.masthead.left')}
              center={t('species.masthead.center')}
              right={
                <button
                  onClick={onClose}
                  style={{
                    background: 'transparent',
                    border: 'none',
                    fontFamily: 'var(--font-mono)',
                    fontSize: 10,
                    letterSpacing: 1.4,
                    textTransform: 'uppercase',
                    color: 'var(--color-clay)',
                    cursor: 'pointer',
                  }}
                >
                  ✕ {t('common.close')}
                </button>
              }
            />
            {speciesId !== null && (
              <SpeciesEditForm speciesId={speciesId} onSaved={onClose} />
            )}
          </motion.div>
        </>
      )}
    </AnimatePresence>
  )
}
```

Commit 6b:

```bash
git add web/src/components/faltet/SpeciesEditModal.tsx
git commit -m "feat: Fältet SpeciesEditModal with framer-motion transitions + mobile drag-to-dismiss"
```

### Step 3: Update `SpeciesDetail.tsx` as thin page wrapper

```tsx
// web/src/pages/SpeciesDetail.tsx
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Masthead } from '../components/faltet'
import { SpeciesEditForm } from '../components/faltet/SpeciesEditForm'

export function SpeciesDetail() {
  const { id } = useParams<{ id: string }>()
  const { t } = useTranslation()
  return (
    <div>
      <Masthead left={t('species.masthead.left')} center={t('species.masthead.center')} />
      <SpeciesEditForm speciesId={Number(id)} />
    </div>
  )
}
```

Commit 6c:

```bash
git add web/src/pages/SpeciesDetail.tsx
git commit -m "refactor: SpeciesDetail wraps shared SpeciesEditForm"
```

### Step 4: New BedDetail.tsx

```tsx
// web/src/pages/BedDetail.tsx
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useParams, Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { Masthead, Chip, Rule, Stat, PhotoPlaceholder, Field } from '../components/faltet'
import { SpeciesEditModal } from '../components/faltet/SpeciesEditModal'

export function BedDetail() {
  const { id } = useParams<{ id: string }>()
  const bedId = Number(id)
  const { t } = useTranslation()

  const { data: bed } = useQuery({ queryKey: ['bed', bedId], queryFn: () => api.beds.get(bedId) })
  const { data: plants } = useQuery({ queryKey: ['bed-plants', bedId], queryFn: () => api.beds.plants(bedId) })
  const { data: garden } = useQuery({
    queryKey: ['garden', bed?.gardenId],
    queryFn: () => api.gardens.get(bed!.gardenId),
    enabled: !!bed,
  })

  const [modalSpecies, setModalSpecies] = useState<number | null>(null)

  if (!bed) return null

  const area = bed.lengthMeters && bed.widthMeters ? (bed.lengthMeters * bed.widthMeters).toFixed(1) : '—'

  return (
    <div>
      <Masthead
        left={
          <span>
            {t('nav.gardens')} / {garden?.name ?? '…'} /{' '}
            <span style={{ color: 'var(--color-clay)' }}>Bädd № {bed.id}</span>
          </span>
        }
        center={t('bed.masthead.center')}
      />

      <div style={{ padding: '28px 40px' }}>
        {/* Hero row */}
        <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: 40, alignItems: 'start' }}>
          <div>
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 18 }}>
              <Chip tone="mustard">Bädd № {bed.id}</Chip>
              {bed.sunExposure && <Chip tone="sage">{t(`bed.sun.${bed.sunExposure.toLowerCase()}`)}</Chip>}
              {bed.irrigationType && <Chip tone="sky">{t(`bed.irrigation.${bed.irrigationType.toLowerCase()}`)}</Chip>}
              {bed.raisedBed && <Chip tone="berry">{t('bed.raised')}</Chip>}
            </div>
            <h1
              style={{
                fontFamily: 'var(--font-display)',
                fontSize: 80,
                fontWeight: 300,
                lineHeight: 1,
                letterSpacing: -1.5,
                margin: 0,
                fontVariationSettings: '"SOFT" 100, "opsz" 144',
              }}
            >
              Bädd.{String(bed.id).padStart(2, '0')} <span style={{ color: 'var(--color-mustard)' }}>—</span>
              <br />
              <span style={{ fontStyle: 'italic', color: 'var(--color-clay)' }}>{bed.name}.</span>
            </h1>
            {bed.description && (
              <p
                style={{
                  marginTop: 16,
                  fontFamily: 'Georgia, var(--font-display)',
                  fontSize: 15,
                  lineHeight: 1.6,
                  color: 'var(--color-forest)',
                }}
              >
                {bed.description}
              </p>
            )}
          </div>
          <div>
            <PhotoPlaceholder tone="sage" aspect="tall" label={`BÄDD.${String(bed.id).padStart(2, '0')}`} />
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 0, marginTop: 12, border: '1px solid var(--color-ink)' }}>
              <MetaCell label={t('bed.meta.length')} value={bed.lengthMeters ? `${bed.lengthMeters} m` : '—'} />
              <MetaCell label={t('bed.meta.width')}  value={bed.widthMeters  ? `${bed.widthMeters} m`  : '—'} />
              <MetaCell label={t('bed.meta.orient')} value={bed.aspect ?? '—'} />
              <MetaCell label={t('bed.meta.area')}   value={`${area} m²`} />
            </div>
          </div>
        </div>

        {/* Stats band */}
        <div style={{ margin: '40px 0', padding: '20px 0', borderTop: '1px solid var(--color-ink)', borderBottom: '1px solid var(--color-ink)', display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 18 }}>
          <Stat size="medium" value={plants?.filter((p: any) => p.status === 'PLANTED_OUT').length ?? 0} label={t('bed.stats.active')}       hue="sage" />
          <Stat size="medium" value={0}   unit="st" label={t('bed.stats.harvested')} hue="clay" />
          <Stat size="medium" value="—"            label={t('bed.stats.daysToHarvest')} hue="mustard" />
          <Stat size="medium" value={plants?.length ?? 0} label={t('bed.stats.plants')} hue="sky" />
          <Stat size="medium" value={0}   unit="%" label={t('bed.stats.utilization')} hue="berry" />
        </div>

        {/* Plantor section */}
        <SectionHeader
          title={t('bed.plants.title')}
          meta={`${new Set(plants?.map((p: any) => p.speciesId)).size} ${t('bed.plants.metaSuffix')}`}
          actions={
            <>
              <Link to={`/sow?bedId=${bedId}`} style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 16, color: 'var(--color-clay)', textDecoration: 'none' }}>
                + {t('bed.plants.sow')}
              </Link>
            </>
          }
        />

        {/* Plant rows */}
        <div style={{ display: 'grid', gridTemplateColumns: '50px 1.5fr 1fr 70px 1fr 100px', gap: 18, padding: '10px 0', borderBottom: '1px solid var(--color-ink)', fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
          <span>№</span>
          <span>{t('bed.plants.col.species')}</span>
          <span>{t('bed.plants.col.variant')}</span>
          <span>{t('bed.plants.col.count')}</span>
          <span>{t('bed.plants.col.status')}</span>
          <span>{t('bed.plants.col.timeline')}</span>
        </div>
        {plants?.map((p: any, i: number) => (
          <button
            key={p.id}
            onClick={() => p.speciesId && setModalSpecies(p.speciesId)}
            style={{
              display: 'grid',
              gridTemplateColumns: '50px 1.5fr 1fr 70px 1fr 100px',
              gap: 18,
              padding: '12px 0',
              borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
              background: 'transparent',
              border: 'none',
              width: '100%',
              textAlign: 'left',
              cursor: 'pointer',
              alignItems: 'center',
            }}
          >
            <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 22, color: 'var(--color-clay)' }}>
              {String(i + 1).padStart(2, '0')}
            </span>
            <div>
              <div style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>{p.speciesName ?? p.name}</div>
            </div>
            <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', color: 'var(--color-clay)' }}>
              {p.variantName ?? ''}
            </span>
            <span style={{ fontFamily: 'var(--font-display)', fontSize: 20, fontVariantNumeric: 'tabular-nums' }}>
              {p.seedCount ?? 1}
            </span>
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase' }}>
              {p.status}
            </span>
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10 }}>
              {p.sownDate ?? '—'} → {p.plantedDate ?? '—'}
            </span>
          </button>
        ))}

        {/* Bottom row */}
        <div style={{ display: 'grid', gridTemplateColumns: '1.6fr 1fr', gap: 22, marginTop: 40 }}>
          <div style={{ background: 'var(--color-ink)', color: 'var(--color-cream)', padding: '22px 28px', position: 'relative', overflow: 'hidden' }}>
            <div style={{ position: 'absolute', top: -40, right: -40, width: 140, height: 140, borderRadius: '50%', background: 'var(--color-butter)', opacity: 0.2 }} />
            <div style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 26, fontVariationSettings: '"SOFT" 100, "opsz" 144' }}>
              {t('bed.harvest.headline', { stems: 142 })}{' '}
              <span style={{ color: 'var(--color-blush)' }}>{t('bed.harvest.season', { year: 2025 })}</span>.
            </div>
            <div style={{ marginTop: 12, fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase', display: 'flex', gap: 18 }}>
              <span style={{ color: 'var(--color-sage)' }}>{t('bed.harvest.bestWeek', { week: 32 })}</span>
              <span style={{ color: 'var(--color-blush)' }}>+24 % vs 2024 ▲</span>
            </div>
          </div>
          <div style={{ border: '1px solid color-mix(in srgb, var(--color-clay) 40%, transparent)', padding: '22px 28px' }}>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-clay)', marginBottom: 10 }}>
              {t('bed.danger.title')}
            </div>
            <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 15 }}>
              {t('bed.danger.warning')}
            </p>
            <button
              onClick={() => { if (confirm(t('bed.danger.confirm') ?? '')) api.beds.delete(bedId) }}
              style={{
                marginTop: 10,
                background: 'transparent',
                border: 'none',
                fontFamily: 'var(--font-mono)',
                fontSize: 10,
                letterSpacing: 1.4,
                textTransform: 'uppercase',
                color: 'var(--color-clay)',
                cursor: 'pointer',
              }}
            >
              → {t('bed.danger.delete', { id: String(bedId).padStart(2, '0') })}
            </button>
          </div>
        </div>
      </div>

      <SpeciesEditModal speciesId={modalSpecies} onClose={() => setModalSpecies(null)} />
    </div>
  )
}

function MetaCell({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ padding: '10px 14px', borderTop: '1px solid var(--color-ink)', borderLeft: '1px solid var(--color-ink)' }}>
      <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
        {label}
      </div>
      <div style={{ fontFamily: 'var(--font-display)', fontSize: 22 }}>{value}</div>
    </div>
  )
}

function SectionHeader({ title, meta, actions }: { title: string; meta?: string; actions?: React.ReactNode }) {
  return (
    <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, margin: '40px 0 12px' }}>
      <h2 style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 30, fontWeight: 300, margin: 0, fontVariationSettings: '"SOFT" 100, "opsz" 144' }}>
        {title}<span style={{ color: 'var(--color-clay)' }}>.</span>
      </h2>
      {meta && <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase' }}>{meta}</span>}
      <Rule inline variant="ink" />
      {actions}
    </div>
  )
}
```

Fields on `bed` (`sunExposure`, `irrigationType`, `raisedBed`, `aspect`, etc.) come from the bed-conditions feature. Field name cases must match the actual response.

Commit 6d:

```bash
git add web/src/pages/BedDetail.tsx
git commit -m "feat: Fältet Bädd — hero, stats band, plant ledger, danger callout"
```

### Step 5: i18n keys + milestone

Append to `sv.json`:

```json
{
  "bed": {
    "masthead": { "center": "— Bäddliggaren —" },
    "sun": {
      "full_sun":      "Full sol",
      "partial_sun":   "Mest sol",
      "partial_shade": "Halvskugga",
      "full_shade":    "Full skugga"
    },
    "irrigation": {
      "drip":        "Droppbevattning",
      "sprinkler":   "Spridare",
      "soaker_hose": "Droppslang",
      "manual":      "Manuell",
      "none":        "Ingen"
    },
    "raised": "Upphöjd",
    "meta": {
      "length": "Längd",
      "width":  "Bredd",
      "orient": "Orient",
      "area":   "Yta"
    },
    "stats": {
      "active":         "aktiva plantor",
      "harvested":      "årets skörd",
      "daysToHarvest":  "dagar till skörd",
      "plants":         "plantor totalt",
      "utilization":    "utnyttjande"
    },
    "plants": {
      "title":       "Plantor",
      "metaSuffix":  "arter · grupperat",
      "sow":         "Så fröer",
      "col": {
        "species":  "Art",
        "variant":  "Sort",
        "count":    "Antal",
        "status":   "Status",
        "timeline": "Sådd → Plant"
      }
    },
    "harvest": {
      "headline": "{{stems}} stjälkar skördade från denna bädd",
      "season":   "säsong {{year}}",
      "bestWeek": "bästa vecka: v.{{week}}"
    },
    "danger": {
      "title":   "Farozon",
      "warning": "Permanent radering. Alla plantor i bädden tas bort.",
      "delete":  "Radera bädd.{{id}}",
      "confirm": "Är du säker?"
    }
  },
  "species": {
    "masthead": {
      "left":   "Art · Redigera",
      "center": "— Artkortet —"
    },
    "fields": {
      "sortSv":       "Sort · SV",
      "sortEn":       "Sort · EN",
      "sowStart":     "Sådd — start",
      "sowEnd":       "Sådd — slut",
      "plantStart":   "Plantera",
      "harvestStart": "Skörd"
    },
    "section": { "cultivation": "§ Odling" },
    "notes":   { "label":       "Odlingsanteckningar" },
    "edit": {
      "save":   "Spara ändringar →",
      "saving": "Sparar…"
    }
  },
  "common": { "close": "Stäng" }
}
```

And `en.json`:

```json
{
  "bed": {
    "masthead": { "center": "— The Bed Ledger —" },
    "sun": {
      "full_sun":      "Full sun",
      "partial_sun":   "Partial sun",
      "partial_shade": "Partial shade",
      "full_shade":    "Full shade"
    },
    "irrigation": {
      "drip":        "Drip",
      "sprinkler":   "Sprinkler",
      "soaker_hose": "Soaker hose",
      "manual":      "Manual",
      "none":        "None"
    },
    "raised": "Raised",
    "meta": {
      "length": "Length",
      "width":  "Width",
      "orient": "Aspect",
      "area":   "Area"
    },
    "stats": {
      "active":         "active plants",
      "harvested":      "harvested this year",
      "daysToHarvest":  "days to harvest",
      "plants":         "plants total",
      "utilization":    "utilization"
    },
    "plants": {
      "title":       "Plants",
      "metaSuffix":  "species · grouped",
      "sow":         "Sow seeds",
      "col": {
        "species":  "Species",
        "variant":  "Variant",
        "count":    "Count",
        "status":   "Status",
        "timeline": "Sown → Planted"
      }
    },
    "harvest": {
      "headline": "{{stems}} stems harvested from this bed",
      "season":   "season {{year}}",
      "bestWeek": "best week: w.{{week}}"
    },
    "danger": {
      "title":   "Danger zone",
      "warning": "Permanent deletion. All plants in this bed are removed.",
      "delete":  "Delete bed.{{id}}",
      "confirm": "Are you sure?"
    }
  },
  "species": {
    "masthead": {
      "left":   "Species · Edit",
      "center": "— The Species Card —"
    },
    "fields": {
      "sortSv":       "Variant · SV",
      "sortEn":       "Variant · EN",
      "sowStart":     "Sow — start",
      "sowEnd":       "Sow — end",
      "plantStart":   "Plant out",
      "harvestStart": "Harvest"
    },
    "section": { "cultivation": "§ Cultivation" },
    "notes":   { "label":       "Cultivation notes" },
    "edit": {
      "save":   "Save changes →",
      "saving": "Saving…"
    }
  },
  "common": { "close": "Close" }
}
```

Commit 6e + final milestone:

```bash
git add web/src/i18n/sv.json web/src/i18n/en.json
git commit -m "feat: bed + species i18n keys for Fältet"
git commit --allow-empty -m "milestone: Fältet dashboard redesign (spec 1 of 3) complete"
```

---

## Verification summary

After task 6:

- `web/src/index.css` carries the Fältet palette + compat aliases + updated utilities.
- `web/src/components/faltet/` contains `Chip`, `Rule`, `Stat`, `Field`, `PhotoPlaceholder`, `Masthead`, `Sidebar`, `SpeciesEditForm`, `SpeciesEditModal` + barrel.
- `web/src/components/Layout.tsx` is the slim Fältet shell.
- `web/src/pages/{Dashboard,TaskList,BedDetail,SpeciesDetail}.tsx` are replaced.
- `web/src/i18n/{sv,en}.json` carry the new namespaces (`app.subtitle`, `sidebar.*`, `nav.*`, `dashboard.*`, `tasks.*`, `bed.*`, `species.*`, `common.close`).
- `framer-motion@^11` on the classpath.
- `npx tsc --noEmit` green.
- `npm run build` green.

**Deferred (spec 2 or 3):**
- Port remaining ~25 screens (GardenList, GardenDetail, SpeciesList, SpeciesGroups, PlantedSpeciesList/Detail, SeasonList, CustomerList, PestDiseaseLog, VarietyTrials, BouquetRecipes, SuccessionSchedules, ProductionTargets, Supplies, ApplySupply, SeedInventory, SowActivity, BedForm, TaskForm, PlantDetail, WorkflowTemplates, WorkflowTemplateEdit, Account, OrgSettings, Guide, LandingPage, PrivacyPolicy).
- CropCalendar / Analytics / WorkflowProgress — need design decisions not in the handoff.
- Real photography to replace `PhotoPlaceholder`.
- Self-hosted Fraunces + Inter woff2 (replaces Google CDN).
- Visual regression harness.
- Accessibility audit (color contrast on pills, focus states on nav items).
