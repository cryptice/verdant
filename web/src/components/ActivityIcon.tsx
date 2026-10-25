import type { ReactNode } from 'react'

// Line icons on a 24-unit grid, drawn in currentColor so a row's tone colors them.
const PATHS: Record<string, ReactNode> = {
  SOW: <>
    <path d="M6 21h12" />
    <path d="M12 21v-8" />
    <path d="M12 15c0-3 2-5 5-5 0 3-2 5-5 5z" />
    <path d="M12 13c0-2.5-1.8-4-4.5-4 0 2.5 1.8 4 4.5 4z" />
  </>,
  SOAK: <>
    <path d="M4 13h16a8 8 0 0 1-16 0z" />
    <path d="M12 3c-2 2.5-3 4-3 5.2a3 3 0 0 0 6 0C15 7 14 5.5 12 3z" />
  </>,
  POT_UP: <>
    <path d="M6 11h12v2H6z" />
    <path d="M7 13h10l-1.2 7.2a1 1 0 0 1-1 .8H9.2a1 1 0 0 1-1-.8z" />
    <path d="M12 11V6" />
    <path d="M12 8c0-2 1.5-3.5 3.5-3.5 0 2-1.5 3.5-3.5 3.5z" />
  </>,
  PLANT: <>
    <path d="M3 20h18" />
    <path d="M12 20v-6" />
    <path d="M12 16c-3 0-5-2-5-5 3 0 5 2 5 5z" />
    <path d="M12 14c0-3 2-5 5-5 0 3-2 5-5 5z" />
    <path d="M12 3v3m-2-1.5 2 1.5 2-1.5" />
  </>,
  HARVEST: <>
    <path d="M4 10h16l-2 9.2a1 1 0 0 1-1 .8H7a1 1 0 0 1-1-.8z" />
    <path d="M8 10a4 4 0 0 1 8 0" />
    <path d="M9 14v3m3-3v3m3-3v3" />
  </>,
  RECOVER: <>
    <path d="M4 12a8 8 0 1 0 2.3-5.7" />
    <path d="M4 4v4h4" />
  </>,
  DISCARD: <>
    <path d="M4 7h16" />
    <path d="M9 7V4h6v3" />
    <path d="M6 7l1 13h10l1-13" />
  </>,
  WATER: <path d="M12 3c-3 4-5 6.7-5 9.5a5 5 0 0 0 10 0C17 9.7 15 7 12 3z" />,
  WEED: <>
    <path d="M5 19c0-8 6-13 14-14-1 8-6 14-14 14z" />
    <path d="M5 19l8-8" />
  </>,
  FERTILIZE: <>
    <path d="M7 8h10l2 12H5z" />
    <path d="M9 8 8 4h8l-1 4" />
    <path d="M12 12v4m-2-2h4" />
  </>,
  MOW: <>
    <path d="M3 20h18" />
    <path d="M6 20c0-4-1-7-3-9" />
    <path d="M10 20c0-5 1-9 3-12" />
    <path d="M15 20c0-4 1-7 4-9" />
  </>,
  RAKE: <>
    <path d="M12 3v12" />
    <path d="M6 15h12" />
    <path d="M6 15v4m3-4v4m3-4v4m3-4v4m3-4v4" />
  </>,
  PRUNE: <>
    <circle cx="6" cy="18" r="2.5" />
    <circle cx="18" cy="18" r="2.5" />
    <path d="M7.8 16.2 18 4" />
    <path d="M16.2 16.2 6 4" />
  </>,
  EDGE: <>
    <path d="M12 3v9" />
    <path d="M10 3h4" />
    <path d="M8 12h8v4a4 4 0 0 1-8 0z" />
  </>,
  SWEEP: <>
    <path d="M15 3l-4 9" />
    <path d="M7 12h8l1 9H6z" />
    <path d="M9.5 16v5m3-5v5" />
  </>,
  TOP_UP: <>
    <path d="M12 3v10" />
    <path d="M8 9l4 4 4-4" />
    <path d="M4 15v4a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-4" />
  </>,
  CLEAN: <>
    <path d="M11 3l1.8 4.2L17 9l-4.2 1.8L11 15l-1.8-4.2L5 9l4.2-1.8z" />
    <path d="M18 14l.9 2.1L21 17l-2.1.9L18 20l-.9-2.1L15 17l2.1-.9z" />
  </>,
  INSPECT: <>
    <circle cx="11" cy="11" r="6" />
    <path d="M15.5 15.5 20 20" />
  </>,
  TODO: <>
    <rect x="4" y="4" width="16" height="16" rx="2" />
    <path d="M8 12l3 3 5-6" />
  </>,
  PURCHASE: <>
    <path d="M6 8h12l-1 12H7z" />
    <path d="M9 8a3 3 0 0 1 6 0" />
  </>,
  SUPPORT: <>
    <path d="M12 3v18" />
    <path d="M9 21h6" />
    <path d="M12 8c-2.5 0-4 1.5-4 3" />
    <path d="M12 13c2.5 0 4 1.5 4 3" />
  </>,
}

const FALLBACK = <circle cx="12" cy="12" r="3" />

function Icon({ size, children, ...data }: { size: number; children: ReactNode; 'data-activity'?: string }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      style={{ display: 'block', flexShrink: 0 }}
      {...data}
    >
      {children}
    </svg>
  )
}

/** Marks a row by what is done in it (sow, pot up …); decorative, the row names the activity. */
export function ActivityIcon({ activityType, size = 24 }: { activityType: string; size?: number }) {
  return <Icon size={size} data-activity={activityType}>{PATHS[activityType] ?? FALLBACK}</Icon>
}

export function BedIcon({ size = 24 }: { size?: number }) {
  return (
    <Icon size={size}>
      <rect x="3" y="12" width="18" height="7" rx="1" />
      <path d="M8 12V9m4 3V8m4 4V9" />
    </Icon>
  )
}
