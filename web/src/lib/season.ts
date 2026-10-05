import type { SeasonResponse } from '../api/client'

export function latestSeason(seasons: readonly SeasonResponse[] | undefined): SeasonResponse | undefined {
  return seasons?.reduce<SeasonResponse | undefined>(
    (latest, season) => !latest || season.year > latest.year ? season : latest,
    undefined,
  )
}
