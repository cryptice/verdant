import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { DashboardBedStat } from './Dashboard'
import i18n from '../i18n'
import type { GardenSummary } from '../api/client'

vi.mock('react-i18next', async original => ({
  ...await original<typeof import('react-i18next')>(),
  useTranslation: () => ({ t: i18n.getFixedT('sv') }),
}))

const gardens: GardenSummary[] = [
  { id: 42, name: 'First', bedCount: 1, plantCount: 0 },
  { id: 7, name: 'Second', bedCount: 0, plantCount: 0 },
]
function Location() {
  const location = useLocation()
  return <output data-testid="location">{location.pathname}{location.hash}</output>
}

describe('dashboard bed widget', () => {
  it.each([[0, 'Bäddar'], [1, 'Bädd'], [2, 'Bäddar']] as const)(
    'labels %i beds correctly and opens the first garden beds section', (count, label) => {
      render(<MemoryRouter><DashboardBedStat count={count} gardens={gardens} /><Location /></MemoryRouter>)
      fireEvent.click(screen.getByRole('button', { name: `${count} ${label}` }))
      expect(screen.getByTestId('location')).toHaveTextContent('/garden/42#beds')
    },
  )

  it.each([undefined, []])('has no broken navigation while gardens are missing: %j', gardens => {
    render(<MemoryRouter><DashboardBedStat count={0} gardens={gardens} /></MemoryRouter>)
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
    expect(screen.getByText('Bäddar')).toBeInTheDocument()
  })

  it('has singular and plural labels in English too', () => {
    const t = i18n.getFixedT('en')
    expect(t('dashboard.bedLabel', { count: 1 })).toBe('Bed')
    expect(t('dashboard.bedLabel', { count: 2 })).toBe('Beds')
  })
})
