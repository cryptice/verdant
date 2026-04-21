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
