import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { api, type WorkflowStepResponse, type WorkflowTemplateResponse } from '../api/client'
import i18n from '../i18n'
import { WorkflowTemplateEdit, mainFlowDays } from './WorkflowTemplateEdit'

vi.mock('react-i18next', async original => ({
  ...await original<typeof import('react-i18next')>(),
  useTranslation: () => ({ t: i18n.getFixedT('sv'), i18n }),
}))

const step = (id: number, name: string, days: number, extra: Partial<WorkflowStepResponse> = {}): WorkflowStepResponse =>
  ({ id, name, daysAfterPrevious: days, isOptional: false, isSideBranch: false, sortOrder: id, ...extra })

const template: WorkflowTemplateResponse = {
  id: 6, name: 'Dahlia från frö', description: 'Cirka 75 dagar', createdAt: '',
  steps: [
    step(0, 'Sådd', 0, { eventType: 'SEEDED', description: '20–22 °C i substratet.' }),
    step(1, 'Omskolning', 20, { eventType: 'POTTED_UP' }),
    step(2, 'Avhärdning', 12, { eventType: 'MOVED', isOptional: true }),
    step(3, 'Säljklar', 11),
  ],
}

beforeEach(() => {
  vi.spyOn(api.workflows, 'getTemplate').mockResolvedValue(template)
  vi.spyOn(api.supplies, 'types').mockResolvedValue([])
  vi.spyOn(api.workflows, 'updateStep').mockResolvedValue(template.steps[0])
})
afterEach(() => vi.restoreAllMocks())

function show() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={['/workflows/6/edit']}>
        <Routes><Route path="/workflows/:id/edit" element={<WorkflowTemplateEdit />} /></Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('WorkflowTemplateEdit', () => {
  it('lists steps under one set of column headings, with description, tags, running day and event', async () => {
    show()
    expect(await screen.findByText('Avhärdning')).toBeInTheDocument()
    expect(screen.getAllByText('Händelse')).toHaveLength(1)
    expect(screen.queryByText('Stegnamn')).toBeNull()
    expect(screen.getByText('20–22 °C i substratet.')).toBeInTheDocument()
    expect(screen.getByText('Valfritt')).toBeInTheDocument()
    expect(screen.getByLabelText('Händelsetyp, Avhärdning')).toHaveDisplayValue('Flyttad')
    expect(screen.getByText('43')).toBeInTheDocument()
  })

  it('saves days edited in the row', async () => {
    show()
    const days = await screen.findByLabelText('Dagar efter föregående, Omskolning')
    fireEvent.change(days, { target: { value: '25' } })
    fireEvent.blur(days)
    await waitFor(() => expect(api.workflows.updateStep).toHaveBeenCalledWith(1, { daysAfterPrevious: 25 }))
  })

  it('clears an event type by sending it empty', async () => {
    show()
    fireEvent.change(await screen.findByLabelText('Händelsetyp, Omskolning'), { target: { value: '' } })
    await waitFor(() => expect(api.workflows.updateStep).toHaveBeenCalledWith(1, { eventType: '' }))
  })

  it('edits a step and its description in the dialog', async () => {
    show()
    fireEvent.click(await screen.findByText('Säljklar'))
    const dialog = await screen.findByRole('dialog')
    fireEvent.change(within(dialog).getByLabelText('Beskrivning'), { target: { value: 'Etikettera.' } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Spara' }))
    await waitFor(() => expect(api.workflows.updateStep).toHaveBeenCalledWith(3, expect.objectContaining({
      name: 'Säljklar', description: 'Etikettera.', daysAfterPrevious: 11, eventType: '',
    })))
  })
})

describe('mainFlowDays', () => {
  it('runs from the first step along the main flow and leaves side branches out', () => {
    expect(mainFlowDays([step(0, 'a', 5), step(1, 'b', 20), step(2, 'c', 7, { isSideBranch: true }), step(3, 'd', 10)]))
      .toEqual([0, 20, null, 30])
  })
})
