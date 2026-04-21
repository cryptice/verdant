// web/src/components/faltet/Ledger.test.tsx
import { render, screen, fireEvent } from '@testing-library/react'
import { describe, expect, test, vi } from 'vitest'
import { I18nextProvider } from 'react-i18next'
import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import { Ledger } from './Ledger'

i18n.use(initReactI18next).init({
  lng: 'en',
  resources: {
    en: {
      translation: {
        common: { ledger: { empty: 'No rows yet' } },
        pagination: { of: 'of', previous: 'Previous page', next: 'Next page' },
      },
    },
  },
  interpolation: { escapeValue: false },
})

function withI18n(node: React.ReactNode) {
  return <I18nextProvider i18n={i18n}>{node}</I18nextProvider>
}

type Row = { id: number; name: string; count: number }

describe('Ledger', () => {
  const columns = [
    { key: 'name', label: 'Name' },
    { key: 'count', label: 'Count', align: 'right' as const },
  ]

  test('renders header labels and row values', () => {
    const rows: Row[] = [{ id: 1, name: 'Alpha', count: 12 }]
    render(withI18n(<Ledger columns={columns} rows={rows} rowKey={(r) => r.id} />))
    expect(screen.getByText('Name')).toBeInTheDocument()
    expect(screen.getByText('Alpha')).toBeInTheDocument()
    expect(screen.getByText('12')).toBeInTheDocument()
  })

  test('uses custom render function when provided', () => {
    const customColumns = [
      { key: 'name', label: 'Name', render: (r: Row) => <span>{r.name.toUpperCase()}</span> },
    ]
    const rows: Row[] = [{ id: 1, name: 'beta', count: 1 }]
    render(withI18n(<Ledger columns={customColumns} rows={rows} rowKey={(r) => r.id} />))
    expect(screen.getByText('BETA')).toBeInTheDocument()
  })

  test('calls onRowClick when row clicked', () => {
    const onRowClick = vi.fn()
    const rows: Row[] = [{ id: 1, name: 'Alpha', count: 12 }]
    render(withI18n(<Ledger columns={columns} rows={rows} rowKey={(r) => r.id} onRowClick={onRowClick} />))
    fireEvent.click(screen.getByText('Alpha').closest('button')!)
    expect(onRowClick).toHaveBeenCalledWith(rows[0])
  })

  test('shows empty state when no rows', () => {
    render(withI18n(<Ledger columns={columns} rows={[]} rowKey={(r: Row) => r.id} />))
    expect(screen.getByText('No rows yet')).toBeInTheDocument()
  })

  test('renders sectionHeaders between groups', () => {
    const rows: Row[] = [
      { id: 1, name: 'A', count: 1 },
      { id: 2, name: 'B', count: 2 },
    ]
    const sectionHeaders = (row: Row, i: number) =>
      i === 0 || row.count !== rows[i - 1].count ? <div>§ SECTION {row.count}</div> : null
    render(withI18n(<Ledger columns={columns} rows={rows} rowKey={(r) => r.id} sectionHeaders={sectionHeaders} />))
    expect(screen.getByText('§ SECTION 1')).toBeInTheDocument()
    expect(screen.getByText('§ SECTION 2')).toBeInTheDocument()
  })

  test('paginated prop slices rows', () => {
    const rows = Array.from({ length: 7 }, (_, i) => ({ id: i, name: `Row ${i}`, count: i }))
    render(withI18n(<Ledger columns={columns} rows={rows} rowKey={(r) => r.id} paginated pageSize={5} />))
    expect(screen.getByText('Row 0')).toBeInTheDocument()
    expect(screen.getByText('Row 4')).toBeInTheDocument()
    expect(screen.queryByText('Row 5')).not.toBeInTheDocument()
  })

  test('next button advances the page slice', async () => {
    const rows = Array.from({ length: 7 }, (_, i) => ({ id: i, name: `Row ${i}`, count: i }))
    render(withI18n(<Ledger columns={columns} rows={rows} rowKey={(r) => r.id} paginated pageSize={5} />))
    const nextBtn = screen.getByLabelText('Next page')
    fireEvent.click(nextBtn)
    expect(screen.getByText('Row 5')).toBeInTheDocument()
    expect(screen.getByText('Row 6')).toBeInTheDocument()
    expect(screen.queryByText('Row 0')).not.toBeInTheDocument()
  })
})
