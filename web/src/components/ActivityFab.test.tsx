import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, within } from '@testing-library/react'
import i18n from '../i18n'
import { ActivityFab } from './ActivityFab'

vi.mock('react-i18next', async original => ({
  ...await original<typeof import('react-i18next')>(),
  useTranslation: () => ({ t: i18n.getFixedT('sv') }),
}))
const navigate = vi.fn()
vi.mock('react-router-dom', () => ({ useNavigate: () => navigate }))

describe('ActivityFab', () => {
  it.each([['Lägg till uppgift', '/task/new?type=TODO'], ['Så', '/sow']])('opens the activity choices and goes to %s', (choice, path) => {
    render(<ActivityFab />)
    fireEvent.click(screen.getByRole('button', { name: 'Aktivitet' }))
    const dialog = screen.getByRole('dialog', { name: 'Vad vill du göra?' })
    fireEvent.click(within(dialog).getByRole('button', { name: new RegExp(`^${choice}`) }))
    expect(navigate).toHaveBeenCalledWith(path)
  })
})
