import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { I18nextProvider } from 'react-i18next'
import i18n from '../i18n'
import { GardenBedsPrompt } from './GardenBedsPrompt'
import type { GardenSummary } from '../api/client'

const garden: GardenSummary = { id: 7, name: 'My garden', bedCount: 0, plantCount: 0 }
const prompt = (gardens: GardenSummary[] | undefined) => (
  <I18nextProvider i18n={i18n}><MemoryRouter><GardenBedsPrompt gardens={gardens} /></MemoryRouter></I18nextProvider>
)

describe('dashboard bed setup prompt', () => {
  it('names the garden and links directly to its bed form, then disappears after a bed is added', () => {
    const { rerender } = render(prompt([garden]))
    expect(screen.getByRole('heading')).toHaveTextContent('My garden')
    expect(screen.getByRole('link')).toHaveAttribute('href', '/garden/7/bed/new')
    rerender(prompt([{ ...garden, bedCount: 1 }]))
    expect(screen.queryByRole('link')).not.toBeInTheDocument()
  })

  it.each([undefined, [], [garden, { ...garden, id: 8 }], [garden, { ...garden, id: 8, bedCount: 2 }]])(
    'does not show for loading, zero or multiple gardens: %j', gardens => {
      render(prompt(gardens))
      expect(screen.queryByRole('link')).not.toBeInTheDocument()
    },
  )
})
