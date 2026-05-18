import { useTranslation } from 'react-i18next'

export const GROWING_POSITIONS = ['SUNNY', 'PARTIALLY_SUNNY', 'SHADOWY'] as const
export const SOIL_TYPES = ['CLAY', 'SANDY', 'LOAMY', 'CHALKY', 'PEATY', 'SILTY'] as const

export function usePositionLabel() {
  const { t } = useTranslation()
  return {
    SUNNY: t('species.positionSunny'), PARTIALLY_SUNNY: t('species.positionPartialSun'), SHADOWY: t('species.positionShadowy')
  } as Record<string, string>
}

export function useSoilLabel() {
  const { t } = useTranslation()
  return {
    CLAY: t('species.soilCite'), SANDY: t('species.soilSandy'), LOAMY: t('species.soilLoamy'), CHALKY: t('species.soilChalky'), PEATY: t('species.soilPeaty'), SILTY: t('species.soilSilty')
  } as Record<string, string>
}

export interface PendingProvider {
  providerId: number
  imageFrontBase64: string | null
  imageBackBase64: string | null
  productUrl: string | null
}
