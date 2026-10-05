import { api } from '../api/client'

/** Read before creating so a failed follow-up read cannot turn a saved garden into a retry. */
export async function createGardenWithDestination(data: Parameters<typeof api.gardens.create>[0]) {
  const existingGardens = await api.gardens.list()
  const garden = await api.gardens.create(data)
  return { garden, destination: existingGardens.length === 0 ? '/' : `/garden/${garden.id}` }
}
