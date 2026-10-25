import { fireEvent, render, screen, within } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import type { ScheduledTaskResponse } from '../api/client'
import i18n from '../i18n'
import { TaskCompletionSummary } from './TaskCompletionSummary'

vi.mock('react-i18next', async original => ({
  ...await original<typeof import('react-i18next')>(),
  useTranslation: () => ({ t: i18n.getFixedT('sv') }),
}))
const task: ScheduledTaskResponse = {
  id: 1, speciesId: 1991, speciesName: 'Chilipeppar – Naga Black',
  bedId: null, bedName: null, gardenName: null, activityType: 'SOW',
  earliestDate: '2026-10-19', deadline: '2026-10-20', targetCount: 15, remainingCount: 5,
  notes: 'Så och märk med sortnamn och sådatum. Håll jämn fukt under lock med lufthål.',
  status: 'PENDING', acceptableSpecies: [], createdAt: '', updatedAt: '',
}
it('pairs fixed labels with their values and expands the description separately', () => {
  render(<TaskCompletionSummary task={task} />)
  expect(screen.queryByText('Uppgift att slutföra')).not.toBeInTheDocument()
  const table = screen.getByRole('table', { name: 'Fasta uppgiftsdetaljer' })
  for (const [label, value] of [['Art', 'Chilipeppar – Naga Black'], ['Antal frön', '15'], ['Återstår', '5'], ['Tidigast', '2026-10-19'], ['Senast', '2026-10-20']]) {
    const row = within(table).getByRole('rowheader', { name: label }).closest('tr')!
    expect(within(row).getByRole('cell', { name: value })).toBeInTheDocument()
  }
  expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
  const expand = screen.getByRole('button', { name: 'Visa hela beskrivningen' })
  expect(expand).toHaveAttribute('aria-expanded', 'false')
  fireEvent.click(expand)
  const collapse = screen.getByRole('button', { name: 'Visa mindre' })
  expect(collapse).toHaveAttribute('aria-expanded', 'true')
  expect(screen.getByText(task.notes!)).toBeInTheDocument()
  fireEvent.click(collapse)
  expect(screen.getByRole('button', { name: 'Visa hela beskrivningen' })).toHaveAttribute('aria-expanded', 'false')
})
it('keeps the generic planned-quantity label for tasks that are not sowing', () => {
  render(<TaskCompletionSummary task={{ ...task, activityType: 'POT_UP' }} />)
  const table = screen.getByRole('table', { name: 'Fasta uppgiftsdetaljer' })
  expect(within(table).getByRole('rowheader', { name: 'Planerat antal' })).toBeInTheDocument()
  expect(within(table).queryByRole('rowheader', { name: 'Antal frön' })).not.toBeInTheDocument()
})
