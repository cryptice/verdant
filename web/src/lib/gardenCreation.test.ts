import { afterEach, describe, expect, it, vi } from 'vitest'
import { api, type GardenResponse } from '../api/client'
import { createGardenWithDestination } from './gardenCreation'

const garden: GardenResponse = { id: 7, name: 'Garden', createdAt: '', updatedAt: '' }
afterEach(() => vi.restoreAllMocks())

describe('garden creation destination', () => {
  it.each([0, 1, 2])('routes correctly with %i existing gardens', async count => {
    vi.spyOn(api.gardens, 'list').mockResolvedValue(Array.from({ length: count }, (_, id) => ({ ...garden, id })))
    const create = vi.spyOn(api.gardens, 'create').mockResolvedValue(garden)
    const result = await createGardenWithDestination({ name: 'Garden' })
    expect(result.destination).toBe(count === 0 ? '/' : '/garden/7')
    expect(create).toHaveBeenCalledOnce()
  })

  it('does not create when the existing garden count cannot be read', async () => {
    vi.spyOn(api.gardens, 'list').mockRejectedValue(new Error('Offline'))
    const create = vi.spyOn(api.gardens, 'create')
    await expect(createGardenWithDestination({ name: 'Garden' })).rejects.toThrow('Offline')
    expect(create).not.toHaveBeenCalled()
  })

  it('does not return a destination when creation fails', async () => {
    vi.spyOn(api.gardens, 'list').mockResolvedValue([])
    vi.spyOn(api.gardens, 'create').mockRejectedValue(new Error('Could not save'))
    await expect(createGardenWithDestination({ name: 'Garden' })).rejects.toThrow('Could not save')
  })
})
