import { useEffect, useState } from 'react'

interface Props {
  targetElement: HTMLElement | null
  onClick: () => void
}

export function OnboardingOverlay({ targetElement, onClick }: Props) {
  const [rect, setRect] = useState<DOMRect | null>(null)

  useEffect(() => {
    if (!targetElement) { setRect(null); return }

    const update = () => setRect(targetElement.getBoundingClientRect())
    update()

    window.addEventListener('scroll', update, true)
    window.addEventListener('resize', update)
    return () => {
      window.removeEventListener('scroll', update, true)
      window.removeEventListener('resize', update)
    }
  }, [targetElement])

  if (!rect) return null

  const padding = 8

  return (
    <div className="fixed inset-0 z-50" onClick={onClick}>
      <svg className="absolute inset-0 w-full h-full">
        <defs>
          <mask id="onboarding-mask">
            <rect x="0" y="0" width="100%" height="100%" fill="white" />
            <rect
              x={rect.left - padding}
              y={rect.top - padding}
              width={rect.width + padding * 2}
              height={rect.height + padding * 2}
              rx="8"
              fill="black"
            />
          </mask>
        </defs>
        <rect
          x="0" y="0" width="100%" height="100%"
          fill="rgba(0,0,0,0.3)"
          mask="url(#onboarding-mask)"
        />
      </svg>
      {/* Transparent clickable area over the cutout so clicks pass through to the element */}
      <div
        className="absolute"
        style={{
          left: rect.left - padding,
          top: rect.top - padding,
          width: rect.width + padding * 2,
          height: rect.height + padding * 2,
        }}
        onClick={(e) => e.stopPropagation()}
      />
    </div>
  )
}
