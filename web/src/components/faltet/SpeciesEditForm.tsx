import { useState, type ReactNode } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../../api/client'
import { SpeciesForm } from '../species/SpeciesForm'

export function SpeciesEditForm({
  speciesId,
  onSaved,
  onCancel,
  workflowSection,
}: {
  speciesId: number
  onSaved?: () => void
  onCancel?: () => void
  workflowSection?: ReactNode
}) {
  const { t, i18n } = useTranslation()
  const qc = useQueryClient()
  // Bumping the key remounts the form, which drops unsaved edits.
  const [revision, setRevision] = useState(0)
  const { data: species } = useQuery({
    queryKey: ['species', speciesId],
    queryFn: () => api.species.get(speciesId),
  })

  const saveMut = useMutation({
    mutationFn: (req: Record<string, unknown>) => api.species.update(speciesId, req),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['species'] })
      onSaved?.()
    },
  })

  if (!species) return null

  const name = i18n.language === 'sv' ? (species.commonNameSv ?? species.commonName) : species.commonName
  const variant = i18n.language === 'sv' ? (species.variantNameSv ?? species.variantName) : species.variantName
  const fullName = variant ? `${name} – ${variant}` : name

  return (
    <SpeciesForm
      key={`${species.id}-${revision}`}
      species={species}
      title={species.isSystem ? fullName : t('speciesForm.editTitle', { name: fullName })}
      submitLabel={t('speciesForm.saveChanges')}
      isSubmitting={saveMut.isPending}
      error={saveMut.error?.message ?? null}
      onCancel={onCancel ?? (() => setRevision(r => r + 1))}
      onSubmit={req => saveMut.mutate(req)}
      workflowSection={species.isSystem ? undefined : workflowSection}
    />
  )
}
