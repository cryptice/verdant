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
