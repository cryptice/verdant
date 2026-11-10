type BotanicalPlate = 'peony' | 'tulip' | 'trellis' | 'harvest'

/** The same hand-colored plates used by Android, presented as decorative artwork. */
export function BotanicalIllustration({
  plate = 'peony',
  className = '',
  priority = false,
  sizes = '160px',
}: {
  plate?: BotanicalPlate
  className?: string
  priority?: boolean
  sizes?: string
}) {
  return (
    <img
      className={`botanical-plate ${className}`}
      src={`/illustrations/${plate}.webp`}
      srcSet={`/illustrations/${plate}-small.webp 320w, /illustrations/${plate}.webp 768w`}
      sizes={sizes}
      width={768}
      height={768}
      alt=""
      aria-hidden="true"
      draggable={false}
      loading={priority ? 'eager' : 'lazy'}
      decoding="async"
    />
  )
}
