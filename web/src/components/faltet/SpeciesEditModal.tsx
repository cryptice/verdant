import { motion, AnimatePresence } from 'framer-motion'
import type { PanInfo } from 'framer-motion'
import { useTranslation } from 'react-i18next'
import { useEffect, useState } from 'react'
import { SpeciesEditForm } from './SpeciesEditForm'
import { Masthead } from './Masthead'

export function SpeciesEditModal({
  speciesId,
  onClose,
}: {
  speciesId: number | null
  onClose: () => void
}) {
  const { t } = useTranslation()
  const [isMobile, setIsMobile] = useState(window.innerWidth < 768)

  useEffect(() => {
    const onResize = () => setIsMobile(window.innerWidth < 768)
    window.addEventListener('resize', onResize)
    return () => window.removeEventListener('resize', onResize)
  }, [])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  const handleDragEnd = (_: unknown, info: PanInfo) => {
    if (info.offset.y > 80) onClose()
  }

  return (
    <AnimatePresence>
      {speciesId !== null && (
        <>
          <motion.div
            key="scrim"
            onClick={onClose}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.18, ease: 'easeOut' }}
            style={{
              position: 'fixed',
              inset: 0,
              background: 'rgba(30,36,29,0.55)',
              backdropFilter: 'blur(1.5px)',
              zIndex: 100,
            }}
          />
          <motion.div
            key="modal"
            initial={isMobile ? { y: '100%' } : { scale: 0.97, y: 6, opacity: 0 }}
            animate={isMobile ? { y: '0%' } : { scale: 1, y: 0, opacity: 1 }}
            exit={isMobile ? { y: '100%' } : { scale: 0.97, y: 6, opacity: 0 }}
            transition={{
              duration: isMobile ? 0.26 : 0.22,
              ease: [0.22, 1, 0.36, 1],
            }}
            drag={isMobile ? 'y' : false}
            dragConstraints={{ top: 0, bottom: 0 }}
            onDragEnd={handleDragEnd}
            style={{
              position: 'fixed',
              zIndex: 110,
              background: 'var(--color-paper)',
              border: '1px solid var(--color-ink)',
              boxShadow: '24px 24px 0 rgba(30,36,29,0.15)',
              ...(isMobile
                ? { left: 0, right: 0, bottom: 0, maxHeight: '86vh', overflowY: 'auto' }
                : {
                    left: '50%',
                    top: '50%',
                    transform: 'translate(-50%, -50%)',
                    width: 760,
                    maxHeight: '86vh',
                    overflowY: 'auto',
                  }),
            }}
          >
            {isMobile && (
              <div style={{ display: 'flex', justifyContent: 'center', padding: '10px 0 0' }}>
                <div
                  style={{
                    width: 44,
                    height: 3,
                    borderRadius: 999,
                    background: 'var(--color-ink)',
                    opacity: 0.4,
                  }}
                />
              </div>
            )}
            <Masthead
              left={t('species.masthead.left')}
              center={t('species.masthead.center')}
              right={
                <button
                  onClick={onClose}
                  style={{
                    background: 'transparent',
                    border: 'none',
                    fontFamily: 'var(--font-mono)',
                    fontSize: 10,
                    letterSpacing: 1.4,
                    textTransform: 'uppercase',
                    color: 'var(--color-clay)',
                    cursor: 'pointer',
                  }}
                >
                  ✕ {t('common.close')}
                </button>
              }
            />
            <SpeciesEditForm speciesId={speciesId} onSaved={onClose} />
          </motion.div>
        </>
      )}
    </AnimatePresence>
  )
}
