import { useState, useEffect, useCallback, useRef } from 'react'
import type { PageTooltipConfig, TooltipStep } from './types'

interface TooltipTourState {
  currentIndex: number
  totalSteps: number
  currentTooltip: TooltipStep | null
  targetElement: HTMLElement | null
  next: () => void
  back: () => void
  skip: () => void
}

export function useTooltipTour(config: PageTooltipConfig | null, onComplete: () => void): TooltipTourState {
  const [currentIndex, setCurrentIndex] = useState(0)
  const onCompleteRef = useRef(onComplete)
  onCompleteRef.current = onComplete
  const [targetElement, setTargetElement] = useState<HTMLElement | null>(null)

  useEffect(() => {
    setCurrentIndex(0)
  }, [config?.stepId])

  useEffect(() => {
    if (!config || currentIndex >= config.tooltips.length) {
      setTargetElement(null)
      return
    }

    const tooltip = config.tooltips[currentIndex]
    let attempts = 0
    const maxAttempts = 10
    const interval = setInterval(() => {
      const el = document.querySelector<HTMLElement>(tooltip.targetSelector)
      if (el) {
        setTargetElement(el)
        clearInterval(interval)
      } else if (++attempts >= maxAttempts) {
        setTargetElement(null)
        clearInterval(interval)
      }
    }, 200)

    return () => clearInterval(interval)
  }, [config, currentIndex])

  const next = useCallback(() => {
    if (!config) return
    if (currentIndex >= config.tooltips.length - 1) {
      onCompleteRef.current()
    } else {
      setCurrentIndex(i => i + 1)
    }
  }, [config, currentIndex])

  const back = useCallback(() => {
    setCurrentIndex(i => Math.max(0, i - 1))
  }, [])

  const skip = useCallback(() => {
    onCompleteRef.current()
  }, [])

  return {
    currentIndex,
    totalSteps: config?.tooltips.length ?? 0,
    currentTooltip: config?.tooltips[currentIndex] ?? null,
    targetElement,
    next,
    back,
    skip,
  }
}
