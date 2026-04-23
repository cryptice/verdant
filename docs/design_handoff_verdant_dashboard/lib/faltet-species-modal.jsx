// Fältet — Edit Species modal, shown over the Bed screen.

function FaltetSpeciesModal() {
  return (
    <div style={{
      width: '100%', height: '100%', position: 'relative',
      fontFamily: 'Inter, -apple-system, sans-serif', color: FAL.ink,
      overflow: 'hidden',
    }}>
      {/* Faded background (bed screen) */}
      <div style={{ position: 'absolute', inset: 0, filter: 'blur(1.5px)', opacity: 0.35, pointerEvents: 'none' }}>
        {window.FaltetBedDesktop ? <window.FaltetBedDesktop /> : null}
      </div>

      {/* Scrim */}
      <div style={{
        position: 'absolute', inset: 0,
        background: 'rgba(30,36,29,0.55)',
      }} />

      {/* Modal */}
      <div style={{
        position: 'absolute', top: '50%', left: '50%',
        transform: 'translate(-50%, -50%)',
        width: 760, maxHeight: '86%',
        background: FAL.paper,
        border: `1px solid ${FAL.ink}`,
        boxShadow: '24px 24px 0 rgba(30,36,29,0.15)',
        display: 'flex', flexDirection: 'column',
        overflow: 'hidden',
      }}>
        {/* Masthead */}
        <div style={{
          padding: '14px 28px',
          borderBottom: `1px solid ${FAL.ink}`,
          display: 'flex', justifyContent: 'space-between', alignItems: 'baseline',
          fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1.8,
          textTransform: 'uppercase', color: FAL.forest,
          background: FAL.cream,
        }}>
          <span>Art · Redigera</span>
          <span style={{ fontStyle: 'italic', fontFamily: '"Fraunces", serif', textTransform: 'none', letterSpacing: 0, fontSize: 13 }}>— Artkortet —</span>
          <span style={{ color: FAL.clay, cursor: 'pointer' }}>✕ Stäng</span>
        </div>

        {/* Body */}
        <div style={{ overflowY: 'auto', padding: '28px 32px 24px' }}>
          {/* Hero */}
          <div style={{ display: 'grid', gridTemplateColumns: '96px 1fr auto', gap: 18, alignItems: 'flex-start', marginBottom: 22 }}>
            <PhotoPlaceholder label="Dahlia" tone="blush" style={{ width: 96, height: 96 }} />
            <div>
              <div style={{ display: 'flex', gap: 6, marginBottom: 8 }}>
                <span style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: FAL.berry, padding: '3px 7px', border: `1px solid ${FAL.berry}`, borderRadius: 999 }}>Fleråriga</span>
                <span style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: FAL.mustard, padding: '3px 7px', border: `1px solid ${FAL.mustard}`, borderRadius: 999 }}>Knölväxt</span>
                <span style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: FAL.sage, padding: '3px 7px', border: `1px solid ${FAL.sage}`, borderRadius: 999 }}>Full sol</span>
              </div>
              <h1 style={{ margin: 0, fontFamily: '"Fraunces", serif', fontSize: 44, fontWeight: 300, lineHeight: 1, letterSpacing: -0.6 }}>
                Dahlia <span style={{ fontStyle: 'italic', color: FAL.clay }}>'Café au Lait'</span>
              </h1>
              <div style={{ fontFamily: '"Fraunces", serif', fontStyle: 'italic', fontSize: 14, color: FAL.forest, opacity: 0.75, marginTop: 4 }}>
                Dahlia pinnata · krämrosa, decorative · 15–20 cm
              </div>
            </div>
            <div style={{
              fontFamily: '"Fraunces", serif', fontStyle: 'italic',
              fontSize: 22, color: FAL.mustard, letterSpacing: -0.3,
            }}>№ 047</div>
          </div>

          <FalRule />

          {/* Form grid */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px 28px', padding: '22px 0' }}>
            <Field label="Namn · Svenska" value="Dahlia" />
            <Field label="Namn · English" value="Dahlia" />
            <Field label="Sort · Svenska" value="Café au Lait" accent="clay" />
            <Field label="Sort · English" value="Café au Lait" accent="clay" />
            <Field label="Latinskt namn" value="Dahlia pinnata" italic />
            <Field label="Familj" value="Asteraceae" />

            <div style={{ gridColumn: '1 / -1', paddingTop: 6 }}>
              <div style={{
                fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.8,
                textTransform: 'uppercase', color: FAL.forest, opacity: 0.6, marginBottom: 12,
              }}>§ Odling</div>
            </div>

            <Field label="Sådd" value="feb — mar" accent="mustard" />
            <Field label="Omplantering" value="apr" accent="sage" />
            <Field label="Plantering ut" value="maj — efter frost" accent="sky" />
            <Field label="Skörd" value="jul — okt" accent="clay" />
            <Field label="Groningstid" value="10–14 dagar" />
            <Field label="Dagar till skörd" value="90–120" />

            {/* Textarea-like */}
            <div style={{ gridColumn: '1 / -1' }}>
              <div style={{
                fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.8,
                textTransform: 'uppercase', color: FAL.forest, opacity: 0.7, marginBottom: 8,
              }}>Odlingsanteckningar</div>
              <div style={{
                border: `1px solid ${FAL.ink}`,
                padding: '14px 16px',
                minHeight: 82,
                fontFamily: '"Fraunces", serif', fontStyle: 'italic', fontSize: 16,
                color: FAL.ink, lineHeight: 1.5,
                background: FAL.cream,
              }}>
                Starta inne i februari, kallstratifiering ej nödvändig. Knölarna sätts ut efter sista frost — i Småland runt 15 maj. Stöd behövs från 40 cm.<span style={{ color: FAL.forest, opacity: 0.4 }}> |</span>
              </div>
            </div>
          </div>

          <FalRule />

          {/* Seed providers */}
          <div style={{ padding: '20px 0 8px' }}>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 10, marginBottom: 10 }}>
              <h3 style={{ margin: 0, fontFamily: '"Fraunces", serif', fontSize: 20, fontStyle: 'italic', fontWeight: 400, letterSpacing: -0.3 }}>
                Fröleverantörer<span style={{ color: FAL.clay }}>.</span>
              </h3>
              <span style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: FAL.forest, opacity: 0.6 }}>
                3 länkade
              </span>
              <div style={{ flex: 1, height: 1, background: `${FAL.ink}30` }} />
              <span style={{ fontFamily: '"Fraunces", serif', fontStyle: 'italic', fontSize: 13, color: FAL.forest }}>+ Lägg till</span>
            </div>
            {[
              ['Impecta', 'DAH-128', '85 kr', 'mustard'],
              ['Florea',  'FL-9921',  '120 kr', 'sage'],
              ['Wexthuset', 'WX-0462', '95 kr', 'sky'],
            ].map(([name, code, price, tone], i) => (
              <div key={i} style={{
                display: 'grid', gridTemplateColumns: '1fr 1fr 80px 28px',
                gap: 14, padding: '10px 0', alignItems: 'center',
                borderBottom: `1px solid ${FAL.ink}20`,
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span style={{ width: 6, height: 6, borderRadius: 999, background: FAL[tone] }} />
                  <span style={{ fontFamily: '"Fraunces", serif', fontSize: 15 }}>{name}</span>
                </div>
                <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1, color: FAL.forest, opacity: 0.75 }}>{code}</div>
                <div style={{ fontFamily: '"Fraunces", serif', fontStyle: 'italic', fontSize: 14, color: FAL[tone], textAlign: 'right' }}>{price}</div>
                <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 11, color: FAL.forest, opacity: 0.5, textAlign: 'right' }}>↗</div>
              </div>
            ))}
          </div>
        </div>

        {/* Footer actions */}
        <div style={{
          padding: '14px 28px',
          borderTop: `1px solid ${FAL.ink}`,
          display: 'flex', justifyContent: 'space-between', alignItems: 'center',
          background: FAL.cream,
        }}>
          <div style={{
            fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1.6,
            textTransform: 'uppercase', color: FAL.clay,
          }}>↵ radera art</div>
          <div style={{ display: 'flex', gap: 10 }}>
            <button style={{
              fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1.8,
              textTransform: 'uppercase', color: FAL.forest,
              padding: '10px 18px', border: `1px solid ${FAL.forest}`, background: 'transparent',
              borderRadius: 0, cursor: 'pointer',
            }}>Avbryt</button>
            <button style={{
              fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1.8,
              textTransform: 'uppercase', color: FAL.cream,
              padding: '10px 22px', background: FAL.ink, border: `1px solid ${FAL.ink}`,
              borderRadius: 0, cursor: 'pointer',
            }}>Spara ändringar →</button>
          </div>
        </div>
      </div>
    </div>
  );
}

function Field({ label, value, accent, italic }) {
  const color = accent ? FAL[accent] : FAL.ink;
  return (
    <div>
      <div style={{
        fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.8,
        textTransform: 'uppercase', color: FAL.forest, opacity: 0.7, marginBottom: 6,
      }}>{label}</div>
      <div style={{
        borderBottom: `1px solid ${FAL.ink}`,
        padding: '4px 0 8px',
        fontFamily: '"Fraunces", serif',
        fontSize: 20, fontWeight: 400, letterSpacing: -0.3,
        fontStyle: italic ? 'italic' : 'normal',
        color,
      }}>{value}</div>
    </div>
  );
}

// Mobile: bottom-sheet version
function FaltetSpeciesModalMobile() {
  return (
    <div style={{
      width: '100%', height: '100%', position: 'relative',
      fontFamily: 'Inter, sans-serif', color: FAL.ink, overflow: 'hidden',
    }}>
      <div style={{ position: 'absolute', inset: 0, filter: 'blur(1px)', opacity: 0.3 }}>
        {window.FaltetBedMobile ? <window.FaltetBedMobile /> : null}
      </div>
      <div style={{ position: 'absolute', inset: 0, background: 'rgba(30,36,29,0.55)' }} />

      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0,
        background: FAL.paper,
        borderTop: `1px solid ${FAL.ink}`,
        maxHeight: '92%',
        display: 'flex', flexDirection: 'column',
      }}>
        <div style={{ padding: '8px 0 0', display: 'flex', justifyContent: 'center' }}>
          <div style={{ width: 44, height: 3, background: FAL.ink, opacity: 0.3 }} />
        </div>
        <div style={{
          padding: '12px 18px', borderBottom: `1px solid ${FAL.ink}`,
          display: 'flex', justifyContent: 'space-between', alignItems: 'baseline',
          fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.6,
          textTransform: 'uppercase', color: FAL.forest,
        }}>
          <span>Art · Redigera</span>
          <span style={{ color: FAL.clay }}>✕</span>
        </div>

        <div style={{ overflowY: 'auto', padding: '18px 18px 14px' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '64px 1fr', gap: 14, marginBottom: 16 }}>
            <PhotoPlaceholder label="Dahlia" tone="blush" style={{ width: 64, height: 64 }} />
            <div>
              <div style={{ display: 'flex', gap: 4, marginBottom: 6, flexWrap: 'wrap' }}>
                <span style={{ fontFamily: 'ui-monospace, monospace', fontSize: 8, letterSpacing: 1.2, textTransform: 'uppercase', color: FAL.berry, padding: '2px 6px', border: `1px solid ${FAL.berry}`, borderRadius: 999 }}>Flerårig</span>
                <span style={{ fontFamily: 'ui-monospace, monospace', fontSize: 8, letterSpacing: 1.2, textTransform: 'uppercase', color: FAL.mustard, padding: '2px 6px', border: `1px solid ${FAL.mustard}`, borderRadius: 999 }}>Knöl</span>
              </div>
              <div style={{ fontFamily: '"Fraunces", serif', fontSize: 26, fontWeight: 300, lineHeight: 1, letterSpacing: -0.4 }}>
                Dahlia<br/><span style={{ fontStyle: 'italic', color: FAL.clay, fontSize: 20 }}>'Café au Lait'</span>
              </div>
            </div>
          </div>

          <div style={{ display: 'grid', gap: 14 }}>
            <Field label="Namn · SV" value="Dahlia" />
            <Field label="Sort · SV" value="Café au Lait" accent="clay" />
            <Field label="Latinskt namn" value="Dahlia pinnata" italic />
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
              <Field label="Sådd" value="feb — mar" accent="mustard" />
              <Field label="Skörd" value="jul — okt" accent="clay" />
            </div>

            <div>
              <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.8, textTransform: 'uppercase', color: FAL.forest, opacity: 0.7, marginBottom: 6 }}>Anteckningar</div>
              <div style={{
                border: `1px solid ${FAL.ink}`, padding: '12px 14px', minHeight: 70,
                fontFamily: '"Fraunces", serif', fontStyle: 'italic', fontSize: 14,
                lineHeight: 1.5, background: FAL.cream,
              }}>
                Knölar sätts ut efter sista frost. Stöd från 40 cm.<span style={{ opacity: 0.4 }}> |</span>
              </div>
            </div>
          </div>
        </div>

        <div style={{
          padding: '12px 18px', borderTop: `1px solid ${FAL.ink}`,
          display: 'flex', gap: 8, background: FAL.cream,
        }}>
          <button style={{
            flex: 1, fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.6,
            textTransform: 'uppercase', color: FAL.forest,
            padding: '10px 14px', border: `1px solid ${FAL.forest}`, background: 'transparent',
            borderRadius: 0, cursor: 'pointer',
          }}>Avbryt</button>
          <button style={{
            flex: 2, fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.6,
            textTransform: 'uppercase', color: FAL.cream,
            padding: '10px 14px', background: FAL.ink, border: `1px solid ${FAL.ink}`,
            borderRadius: 0, cursor: 'pointer',
          }}>Spara ändringar →</button>
        </div>
      </div>
    </div>
  );
}

window.FaltetSpeciesModal = FaltetSpeciesModal;
window.FaltetSpeciesModalMobile = FaltetSpeciesModalMobile;
