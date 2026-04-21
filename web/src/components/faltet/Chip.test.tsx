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
