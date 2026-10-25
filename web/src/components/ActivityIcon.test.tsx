import { describe, expect, it } from 'vitest'
import { render } from '@testing-library/react'
import { ActivityIcon, BedIcon } from './ActivityIcon'

describe('ActivityIcon', () => {
  it('draws a distinct icon for each activity it knows', () => {
    const types = ['SOW', 'SOAK', 'POT_UP', 'PLANT', 'HARVEST', 'RECOVER', 'DISCARD', 'WATER', 'WEED', 'FERTILIZE', 'MOW', 'RAKE', 'PRUNE', 'EDGE', 'SWEEP', 'TOP_UP', 'CLEAN', 'INSPECT', 'TODO', 'PURCHASE', 'SUPPORT', 'PINCH', 'DISBUD']
    const drawings = types.map(type => render(<ActivityIcon activityType={type} />).container.querySelector('svg')!.innerHTML)
    expect(new Set(drawings).size).toBe(types.length)
  })

  it('falls back to a generic mark for an unknown activity and stays decorative', () => {
    const svg = render(<ActivityIcon activityType="SOMETHING_NEW" />).container.querySelector('svg')!
    expect(svg).toHaveAttribute('aria-hidden', 'true')
    expect(svg.innerHTML).not.toBe('')
  })

  it('draws a bed', () => {
    expect(render(<BedIcon />).container.querySelector('svg')).not.toBeNull()
  })
})
