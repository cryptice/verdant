import type { SupplyTypeResponse, SupplyInventoryResponse } from '../../api/client'

export function formatUnit(quantity: number, unit: string, t: (key: string) => string): string {
  return `${quantity} ${t(`supplyUnit.${unit}`)}`
}

export function deriveTypeName(category: string, properties: Record<string, unknown>, t: (key: string) => string): string {
  const cat = t(`supplyCategory.${category}`)
  const parts: string[] = []
  switch (category) {
    case 'SOIL':
      if (properties.type) parts.push(String(properties.type))
      break
    case 'POT': {
      const shape = properties.shape ? t(`supplies.${properties.shape}`) : ''
      const dims = [properties.widthMm, properties.heightMm].filter(Boolean).join('x')
      if (shape) parts.push(shape.toLowerCase())
      if (dims) parts.push(`${dims} mm`)
      break
    }
    case 'FERTILIZER':
      if (properties.npk) parts.push(`NPK ${properties.npk}`)
      break
    case 'TRAY': {
      const rows = properties.rows as number | undefined
      const cols = properties.columns as number | undefined
      if (rows != null && cols != null) {
        parts.push(rows === 0 && cols === 0 ? t('supplies.pureTray') : `${rows}x${cols}`)
      }
      if (properties.volumePerPlugMl) parts.push(`${properties.volumePerPlugMl} ml`)
      break
    }
    case 'LABEL': {
      if (properties.material) parts.push(String(properties.material))
      const dims = [properties.widthMm, properties.heightMm].filter(Boolean).join('x')
      if (dims) parts.push(`${dims} mm`)
      break
    }
  }
  return parts.length > 0 ? `${cat}, ${parts.join(' ')}` : cat
}

export function displayTypeName(type: { name: string; category: string; properties: Record<string, unknown> }, t: (key: string) => string): string {
  return type.name || deriveTypeName(type.category, type.properties, t)
}

export function formatCost(costCents?: number): string {
  if (costCents == null) return ''
  return `${(costCents / 100).toFixed(2)} kr`
}

export function formatTypeLabel(type: SupplyTypeResponse, t: (key: string) => string): string {
  if (!type.name) return displayTypeName(type, t)
  const props = type.properties ?? {}
  switch (type.category) {
    case 'POT': {
      const nameNorm = type.name.toLowerCase().replace(/\s/g, '')
      const shape = props.shape ? t(`supplies.${props.shape as string}`) : ''
      const dims = [props.widthMm, props.heightMm].filter(Boolean).join('x')
      const parts = [
        shape && !nameNorm.includes(shape.toLowerCase()) ? shape : '',
        dims && !nameNorm.includes(dims.replace(/\s/g, '')) ? `${dims} mm` : '',
      ].filter(Boolean)
      return parts.length > 0 ? `${type.name} (${parts.join(' ')})` : type.name
    }
    case 'TRAY': {
      const grid = props.rows && props.columns ? `${props.rows}x${props.columns}` : ''
      const vol = props.volumePerPlugMl ? `${props.volumePerPlugMl}ml` : ''
      const nameNorm = type.name.replace(/\s/g, '')
      const parts = [grid, vol].filter(p => p && !nameNorm.includes(p.replace(/\s/g, '')))
      const extra = parts.join(' ')
      return extra ? `${type.name} (${extra})` : type.name
    }
    case 'FERTILIZER': {
      const npk = props.npk as string | undefined
      if (!npk || type.name.toLowerCase().includes(npk.toLowerCase())) return type.name
      return `${type.name} (${npk})`
    }
    case 'SOIL': {
      const soilType = props.type as string | undefined
      if (!soilType || type.name.toLowerCase().includes(soilType.toLowerCase())) return type.name
      return `${type.name} — ${soilType}`
    }
    default:
      return type.name
  }
}

export interface GroupedType {
  type: SupplyTypeResponse
  label: string
  totalQuantity: number
  batches: SupplyInventoryResponse[]
}

export function groupByCategory(
  types: SupplyTypeResponse[],
  batches: SupplyInventoryResponse[],
  t: (key: string) => string
): Map<string, GroupedType[]> {
  const batchesByType = new Map<number, SupplyInventoryResponse[]>()
  for (const b of batches) {
    const arr = batchesByType.get(b.supplyTypeId) ?? []
    arr.push(b)
    batchesByType.set(b.supplyTypeId, arr)
  }

  const groups = new Map<string, GroupedType[]>()
  for (const type of types) {
    const typeBatches = batchesByType.get(type.id) ?? []
    const totalQuantity = typeBatches.reduce((sum, b) => sum + b.quantity, 0)
    const entry: GroupedType = { type, label: formatTypeLabel(type, t), totalQuantity, batches: typeBatches }
    const arr = groups.get(type.category) ?? []
    arr.push(entry)
    groups.set(type.category, arr)
  }
  return groups
}

export function mmWarning(value: unknown, label: string, minMm: number, maxMm: number, t: (key: string, opts?: Record<string, unknown>) => string): string | null {
  const v = Number(value)
  if (!v || v <= 0) return null
  if (v < minMm) return t('supplies.warnTooSmall', { field: label, value: v, hint: `${v * 10} mm` })
  if (v > maxMm) return t('supplies.warnTooLarge', { field: label, value: v })
  return null
}

export function mlWarning(value: unknown, minMl: number, maxMl: number, t: (key: string, opts?: Record<string, unknown>) => string): string | null {
  const v = Number(value)
  if (!v || v <= 0) return null
  if (v < minMl) return t('supplies.warnVolumeTooSmall', { value: v })
  if (v > maxMl) return t('supplies.warnVolumeTooLarge', { value: v })
  return null
}

export function getRequiredFields(category: string): string[] {
  switch (category) {
    case 'SOIL': return ['type']
    case 'POT': return ['shape', 'widthMm', 'heightMm']
    case 'FERTILIZER': return []
    case 'TRAY': return ['rows', 'columns']
    case 'LABEL': return []
    default: return []
  }
}

const NPK_PATTERN = /^\d+(\.\d+)?-\d+(\.\d+)?-\d+(\.\d+)?$/

export function isValidNpk(value: string): boolean {
  return NPK_PATTERN.test(value.trim())
}

export function arePropertiesValid(category: string, props: Record<string, unknown>): boolean {
  const required = getRequiredFields(category)
  for (const field of required) {
    const val = props[field]
    if (val == null || val === '') return false
  }
  if (category === 'FERTILIZER' && props.npk) {
    if (!isValidNpk(String(props.npk))) return false
  }
  return true
}
