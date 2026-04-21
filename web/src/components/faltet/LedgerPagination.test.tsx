// web/src/components/faltet/LedgerPagination.test.tsx
import { render, screen, fireEvent } from '@testing-library/react'
import { describe, expect, test, vi } from 'vitest'
import { I18nextProvider } from 'react-i18next'
import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import { LedgerPagination } from './LedgerPagination'

i18n.use(initReactI18next).init({
  lng: 'en',
  resources: {
    en: {
      translation: {
        pagination: { of: 'of', previous: 'Previous page', next: 'Next page' },
      },
    },
  },
  interpolation: { escapeValue: false },
})

function withI18n(node: React.ReactNode) {
  return <I18nextProvider i18n={i18n}>{node}</I18nextProvider>
}

describe('LedgerPagination', () => {
  test('renders counter and arrows', () => {
    render(withI18n(<LedgerPagination page={0} pageSize={10} total={25} onChange={() => {}} />))
    expect(screen.getByText('1–10 of 25')).toBeInTheDocument()
    expect(screen.getByLabelText('Previous page')).toBeInTheDocument()
    expect(screen.getByLabelText('Next page')).toBeInTheDocument()
  })

  test('disables previous on first page', () => {
    render(withI18n(<LedgerPagination page={0} pageSize={10} total={25} onChange={() => {}} />))
    expect(screen.getByLabelText('Previous page')).toBeDisabled()
  })

  test('disables next on last page', () => {
    render(withI18n(<LedgerPagination page={2} pageSize={10} total={25} onChange={() => {}} />))
    expect(screen.getByLabelText('Next page')).toBeDisabled()
  })

  test('onChange fires on click', () => {
    const onChange = vi.fn()
    render(withI18n(<LedgerPagination page={0} pageSize={10} total={25} onChange={onChange} />))
    fireEvent.click(screen.getByLabelText('Next page'))
    expect(onChange).toHaveBeenCalledWith(1)
  })

  test('returns null when total is 0', () => {
    const { container } = render(withI18n(<LedgerPagination page={0} pageSize={10} total={0} onChange={() => {}} />))
    expect(container.firstChild).toBeNull()
  })
})
