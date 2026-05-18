import { useState } from 'react'
import type { PlantResponse } from '../../api/client'
import { groupPlants } from '../../lib/bed'

export function BedPlantGroups({
  plants,
  onSpeciesClick,
}: {
  plants: PlantResponse[]
  onSpeciesClick: (speciesId: number) => void
}) {
  const groups = groupPlants(plants)
  const [expanded, setExpanded] = useState<Set<string>>(new Set())
  const toggle = (key: string) => setExpanded(prev => {
    const next = new Set(prev)
    next.has(key) ? next.delete(key) : next.add(key)
    return next
  })

  return (
    <>
      {groups.map((g, i) => {
        const isOpen = expanded.has(g.key)
        const collapsible = g.plants.length > 1
        return (
          <div key={g.key}>
            <button
              type="button"
              onClick={() => {
                if (collapsible) toggle(g.key)
                else if (g.speciesId != null) onSpeciesClick(g.speciesId)
              }}
              style={{
                display: 'grid',
                gridTemplateColumns: '50px 1.5fr 70px 1fr 120px',
                gap: 18,
                padding: '12px 0',
                background: 'transparent',
                border: 'none',
                borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                width: '100%',
                textAlign: 'left',
                cursor: 'pointer',
                alignItems: 'center',
              }}
            >
              <span
                style={{
                  fontFamily: 'var(--font-display)',
                  fontStyle: 'italic',
                  fontSize: 22,
                  color: 'var(--color-accent)',
                }}
              >
                {String(i + 1).padStart(2, '0')}
              </span>
              <div style={{ fontFamily: 'var(--font-display)', fontSize: 20, display: 'flex', alignItems: 'center', gap: 8 }}>
                {g.speciesName}
                {collapsible && (
                  <span
                    style={{
                      fontFamily: 'var(--font-mono)',
                      fontSize: 10,
                      letterSpacing: 1.2,
                      textTransform: 'uppercase',
                      color: 'var(--color-forest)',
                      opacity: 0.7,
                    }}
                  >
                    ×{g.plants.length} {isOpen ? '▲' : '▼'}
                  </span>
                )}
              </div>
              <span
                style={{
                  fontFamily: 'var(--font-display)',
                  fontSize: 20,
                  fontVariantNumeric: 'tabular-nums',
                }}
              >
                {g.totalSeeds}
              </span>
              <span
                style={{
                  fontFamily: 'var(--font-mono)',
                  fontSize: 10,
                  letterSpacing: 1.4,
                  textTransform: 'uppercase',
                }}
              >
                {g.status}
              </span>
              <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10 }}>
                {g.plantedDate ?? '—'}
              </span>
            </button>
            {collapsible && isOpen && (
              <div style={{ background: 'var(--color-paper)' }}>
                {g.plants.map(p => (
                  <button
                    key={p.id}
                    type="button"
                    onClick={() => p.speciesId != null && onSpeciesClick(p.speciesId)}
                    style={{
                      display: 'grid',
                      gridTemplateColumns: '50px 1.5fr 70px 1fr 120px',
                      gap: 18,
                      padding: '8px 0 8px 24px',
                      background: 'transparent',
                      border: 'none',
                      borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 10%, transparent)',
                      width: '100%',
                      textAlign: 'left',
                      cursor: p.speciesId != null ? 'pointer' : 'default',
                      alignItems: 'center',
                    }}
                  >
                    <span />
                    <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 16, color: 'var(--color-forest)' }}>
                      {p.name}
                    </span>
                    <span style={{ fontFamily: 'var(--font-display)', fontSize: 16, fontVariantNumeric: 'tabular-nums' }}>
                      {p.seedCount ?? 1}
                    </span>
                    <span />
                    <span />
                  </button>
                ))}
              </div>
            )}
          </div>
        )
      })}
    </>
  )
}
