import { describe, expect, it, vi } from 'vitest'
import { useState } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import i18n from '../../i18n'
import { PackagePriceFields } from './PackagePriceFields'

vi.mock('react-i18next', async original => ({
  ...await original<typeof import('react-i18next')>(),
  useTranslation: () => ({ t: i18n.getFixedT('sv'), i18n: { ...i18n, language: 'sv' } }),
}))

function Harness({ size = '', perSeed = '' }: { size?: string; perSeed?: string }) {
  const [seeds, setSeeds] = useState(size)
  const [cost, setCost] = useState(perSeed)
  return <PackagePriceFields seedsPerPackage={seeds} costPerSeedSek={cost} onSeedsPerPackage={setSeeds} onCostPerSeed={setCost} />
}

const field = (label: string) => screen.getByLabelText(label) as HTMLInputElement

describe('PackagePriceFields', () => {
  it('shows a stored price per package from the price per seed and package size', () => {
    render(<Harness size="50" perSeed="0.78" />)
    expect(field('Pris per paket (kr)').value).toBe('39')
  })

  it('fills in the price per seed from a package price, and keeps them linked when the size changes', () => {
    render(<Harness />)
    fireEvent.change(field('Frön per paket'), { target: { value: '40' } })
    fireEvent.change(field('Pris per paket (kr)'), { target: { value: '39,50' } })
    expect(field('Kostnad per frö (kr)').value).toBe('0.99')

    fireEvent.change(field('Kostnad per frö (kr)'), { target: { value: '1,2' } })
    expect(field('Pris per paket (kr)').value).toBe('48')

    fireEvent.change(field('Frön per paket'), { target: { value: '' } })
    expect(field('Pris per paket (kr)').value).toBe('')
    expect(field('Kostnad per frö (kr)').value).toBe('1,2')
  })
})
