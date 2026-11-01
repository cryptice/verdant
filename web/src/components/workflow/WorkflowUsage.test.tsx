import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { api, type SpeciesResponse } from '../../api/client'
import i18n from '../../i18n'
import { WorkflowUsage } from './WorkflowUsage'

vi.mock('react-i18next', async original => ({
  ...await original<typeof import('react-i18next')>(),
  useTranslation: () => ({ t: i18n.getFixedT('sv'), i18n: { ...i18n, language: 'sv' } }),
}))

const variety = (id: number, commonNameSv: string, variantNameSv: string, groups: string[], workflowTemplateId: number | null) => ({
  id, commonName: commonNameSv, commonNameSv, variantNameSv, workflowTemplateId,
  groups: groups.map((name, i) => ({ id: i + 1, name })),
} as SpeciesResponse)

afterEach(() => vi.restoreAllMocks())

function show(species: SpeciesResponse[]) {
  vi.spyOn(api.species, 'list').mockResolvedValue(species)
  render(
    <MemoryRouter><QueryClientProvider client={new QueryClient()}>
      <WorkflowUsage templateId={6} />
    </QueryClientProvider></MemoryRouter>,
  )
}

describe('WorkflowUsage', () => {
  it('lists the groups on the workflow, each opening to its varieties', async () => {
    show([
      variety(1, 'Pompondahlia', 'Lilliput', ['Dahlior'], 6),
      variety(2, 'Halskråsdahlia', 'Sunny Reggae', ['Dahlior', 'Dahlior – halskrås'], 6),
      variety(3, 'Dahlia', 'Figaro mix', [], 6),
      variety(4, 'Zinnia', 'Diablo', ['Zinnior'], 9),
    ])

    expect(await screen.findByText(/Används av · 3 sorter/)).toBeInTheDocument()
    const groups = screen.getAllByRole('group')
    expect(groups.map(g => g.querySelector('summary span')?.textContent)).toEqual(['Dahlior', 'Dahlior – halskrås', 'Utan grupp'])
    expect(screen.queryByText('Zinnior')).toBeNull()

    fireEvent.click(screen.getByText('Dahlior'))
    expect(groups[0]).toHaveAttribute('open')
    expect(within(groups[0]).getAllByRole('link').map(a => [a.textContent, a.getAttribute('href')])).toEqual([
      ['Halskråsdahlia – Sunny Reggae', '/species/2'],
      ['Pompondahlia – Lilliput', '/species/1'],
    ])
  })

  it('says when no variety uses the workflow', async () => {
    show([variety(4, 'Zinnia', 'Diablo', ['Zinnior'], 9)])
    expect(await screen.findByText(/Inga sorter använder arbetsflödet än/)).toBeInTheDocument()
  })
})
