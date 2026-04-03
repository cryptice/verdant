import { useRef } from 'react'
import { useFloating, offset, flip, shift, arrow, autoUpdate } from '@floating-ui/react'
import { useTranslation } from 'react-i18next'

interface Props {
  targetElement: HTMLElement | null
  titleKey: string
  descriptionKey: string
  currentIndex: number
  totalSteps: number
  onNext: () => void
  onBack: () => void
  onSkip: () => void
}

export function OnboardingTooltip({
  targetElement, titleKey, descriptionKey,
  currentIndex, totalSteps, onNext, onBack, onSkip,
}: Props) {
  const { t } = useTranslation()
  const arrowRef = useRef<HTMLDivElement>(null)

  const { refs, floatingStyles, middlewareData } = useFloating({
    elements: { reference: targetElement },
    placement: 'bottom',
    middleware: [
      offset(12),
      flip({ fallbackPlacements: ['top', 'right', 'left'] }),
      shift({ padding: 16 }),
      arrow({ element: arrowRef }),
    ],
    whileElementsMounted: autoUpdate,
  })

  if (!targetElement) return null

  const isFirst = currentIndex === 0
  const isLast = currentIndex === totalSteps - 1

  return (
    <div
      ref={refs.setFloating}
      style={floatingStyles}
      className="z-[60] bg-white rounded-xl border border-divider shadow-xl p-4 w-72 max-w-[90vw]"
      onClick={(e) => e.stopPropagation()}
    >
      {/* Arrow */}
      <div
        ref={arrowRef}
        className="absolute w-3 h-3 bg-white border border-divider rotate-45 -z-10"
        style={{
          left: middlewareData.arrow?.x,
          top: middlewareData.arrow?.y,
        }}
      />

      {/* Step counter */}
      {totalSteps > 1 && (
        <p className="text-xs text-text-muted mb-1.5">
          {t('onboarding.tooltip.stepOf', { current: currentIndex + 1, total: totalSteps })}
        </p>
      )}

      {/* Content */}
      <h3 className="font-semibold text-sm mb-1">{t(titleKey)}</h3>
      <p className="text-sm text-text-secondary">{t(descriptionKey)}</p>

      {/* Actions */}
      <div className={`flex items-center mt-3 pt-2 border-t border-divider/50 ${totalSteps > 1 ? 'justify-between' : 'justify-end'}`}>
        {totalSteps > 1 && (
          <button
            onClick={onSkip}
            className="text-xs text-text-muted hover:text-text-secondary transition-colors"
          >
            {t('onboarding.tooltip.skip')}
          </button>
        )}
        <div className="flex gap-2">
          {!isFirst && (
            <button
              onClick={onBack}
              className="text-xs px-2.5 py-1 rounded-lg text-text-secondary hover:bg-surface transition-colors"
            >
              {t('onboarding.tooltip.back')}
            </button>
          )}
          <button
            onClick={onNext}
            className="text-xs px-2.5 py-1.5 rounded-lg bg-accent text-white hover:bg-accent-hover transition-colors font-medium"
          >
            {totalSteps === 1 || isLast ? t('onboarding.tooltip.done') : t('onboarding.tooltip.next')}
          </button>
        </div>
      </div>
    </div>
  )
}
