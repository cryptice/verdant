// Fältet — Bed Plants screen. Plant-inventory ledger for a single bed.

const BED_PLANTS = [
  { sp: 'Dahlia',                 lat: 'Dahlia',               var: "'Café au Lait'",      n: 12, status: 'Blommar',   stage: 'HARVEST',  sown: '2026-02-14', planted: '2026-04-02', tone: 'blush',   hue: 'berry'   },
  { sp: 'Dahlia',                 lat: 'Dahlia',               var: "'Labyrinth'",          n: 8,  status: 'Knoppar',   stage: 'PLANT',    sown: '2026-02-14', planted: '2026-04-02', tone: 'blush',   hue: 'berry'   },
  { sp: 'Zinnia',                 lat: 'Zinnia elegans',       var: "'Queen Lime'",         n: 18, status: 'Växer',     stage: 'PLANT',    sown: '2026-03-01', planted: '2026-04-10', tone: 'sage',    hue: 'sage'    },
  { sp: 'Snapdragon',             lat: 'Antirrhinum majus',    var: "'Chantilly Bronze'",   n: 24, status: 'Skördeklar', stage: 'HARVEST', sown: '2026-01-20', planted: '2026-03-15', tone: 'peach',   hue: 'clay'    },
  { sp: 'Cosmos',                 lat: 'Cosmos bipinnatus',    var: "'Xanthos'",            n: 16, status: 'Växer',     stage: 'PLANT',    sown: '2026-03-05', planted: '2026-04-12', tone: 'butter',  hue: 'mustard' },
  { sp: 'Svartkummin',            lat: 'Nigella damascena',    var: "'Miss Jekyll'",         n: 22, status: 'Knoppar',   stage: 'PLANT',    sown: '2026-02-28', planted: '2026-04-08', tone: 'ink',     hue: 'sky'     },
  { sp: 'Strandtrift',            lat: 'Armeria maritima',     var: null,                    n: 6,  status: 'Vilar',     stage: 'RECOVER',  sown: '2025-08-10', planted: '2025-09-15', tone: 'rose',    hue: 'berry'   },
];

function FaltetBedDesktop() {
  return (
    <div style={{
      width: '100%', height: '100%',
      background: FAL.cream,
      fontFamily: 'Inter, -apple-system, sans-serif',
      color: FAL.ink,
      display: 'grid',
      gridTemplateColumns: '220px 1fr',
      overflow: 'hidden',
    }}>
      {/* Sidebar */}
      <aside style={{
        borderRight: `1px solid ${FAL.ink}`,
        padding: '28px 22px',
        display: 'flex', flexDirection: 'column',
      }}>
        <div style={{
          fontFamily: '"Fraunces", Georgia, serif',
          fontSize: 26, fontWeight: 400, letterSpacing: -0.3,
          fontStyle: 'italic', color: FAL.ink,
        }}>
          Verdant<span style={{ color: FAL.clay }}>.</span>
        </div>
        <div style={{
          fontFamily: 'ui-monospace, Menlo, monospace',
          fontSize: 9, letterSpacing: 1.8, textTransform: 'uppercase',
          color: FAL.forest, marginTop: 2, opacity: 0.7,
        }}>
          Est. 2026 — Småland
        </div>
        <div style={{ height: 28 }} />
        <div style={{
          fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 2,
          textTransform: 'uppercase', color: FAL.forest, opacity: 0.6, marginBottom: 12,
        }}>§ Navigera</div>
        {[
          ['Översikt', false],
          ['Trädgårdar', true],
          ['Säsonger', false],
          ['Arter', false],
          ['Plantor', false],
          ['Uppgifter', false],
          ['Frölager', false],
          ['Skörd', false],
        ].map(([l, active]) => (
          <div key={l} style={{
            padding: '7px 0',
            fontSize: 14,
            fontFamily: active ? '"Fraunces", serif' : 'Inter, sans-serif',
            fontStyle: active ? 'italic' : 'normal',
            fontWeight: active ? 500 : 400,
            color: active ? FAL.clay : FAL.ink,
            borderBottom: `1px solid ${FAL.ink}20`,
            display: 'flex', justifyContent: 'space-between',
          }}>
            <span>{l}</span>
            {active && <span style={{ color: FAL.clay }}>●</span>}
          </div>
        ))}
        <div style={{ flex: 1 }} />
        <div style={{
          fontFamily: 'ui-monospace, monospace', fontSize: 9,
          color: FAL.forest, opacity: 0.5, letterSpacing: 1.2,
          paddingTop: 12, borderTop: `1px solid ${FAL.ink}20`,
        }}>
          erik@l2c.se · sv / en
        </div>
      </aside>

      {/* Main */}
      <main style={{ overflowY: 'auto' }}>
        {/* Masthead with breadcrumbs */}
        <div style={{
          padding: '20px 40px',
          borderBottom: `1px solid ${FAL.ink}`,
          display: 'flex', justifyContent: 'space-between', alignItems: 'baseline',
          fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1.8,
          textTransform: 'uppercase', color: FAL.forest,
        }}>
          <span>
            Trädgårdar <span style={{ color: FAL.ink, opacity: 0.4 }}>/</span> Norra fältet <span style={{ color: FAL.ink, opacity: 0.4 }}>/</span> <span style={{ color: FAL.clay }}>Bädd 02</span>
          </span>
          <span style={{ fontStyle: 'italic', fontFamily: '"Fraunces", serif', textTransform: 'none', letterSpacing: 0, fontSize: 13 }}>— Bäddliggaren —</span>
          <span>86 plantor · 7 arter</span>
        </div>

        <div style={{ padding: '40px 40px' }}>
          {/* Hero */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: 48, alignItems: 'start', marginBottom: 36 }}>
            <div>
              <div style={{ marginBottom: 14, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                <span style={{
                  fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase',
                  color: FAL.mustard, padding: '4px 8px', border: `1px solid ${FAL.mustard}`, borderRadius: 999,
                }}>Bädd № 02</span>
                <span style={{
                  fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase',
                  color: FAL.sage, padding: '4px 8px', border: `1px solid ${FAL.sage}`, borderRadius: 999,
                }}>Full sol</span>
                <span style={{
                  fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase',
                  color: FAL.sky, padding: '4px 8px', border: `1px solid ${FAL.sky}`, borderRadius: 999,
                }}>Droppbevattning</span>
                <span style={{
                  fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase',
                  color: FAL.berry, padding: '4px 8px', border: `1px solid ${FAL.berry}`, borderRadius: 999,
                }}>Upphöjd</span>
              </div>
              <h1 style={{
                margin: 0,
                fontFamily: '"Fraunces", Georgia, serif',
                fontSize: 76, lineHeight: 1.0, letterSpacing: -0.4, fontWeight: 300,
                fontVariationSettings: '"SOFT" 100, "opsz" 144',
              }}>
                Bädd<span style={{ color: FAL.mustard }}>.02</span> —<br/>
                <span style={{ fontStyle: 'italic', color: FAL.clay }}>Dahliornas plats.</span>
              </h1>
              <p style={{
                marginTop: 16, maxWidth: 520, fontSize: 15, lineHeight: 1.5,
                color: FAL.forest, fontFamily: '"Fraunces", Georgia, serif',
              }}>
                Den sandiga jorden på sydöstra sluttningen. <span style={{ fontStyle: 'italic', color: FAL.berry }}>pH 6.8</span>, lera-mull efter tre års kompost. Brickan toppas igen under maj.
              </p>
            </div>
            <div>
              <PhotoPlaceholder label="Norra fältet · B2" tone="sage" style={{ width: '100%', height: 200 }} />
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 0, marginTop: 14, border: `1px solid ${FAL.ink}` }}>
                {[
                  ['Längd', '8.0 m'],
                  ['Bredd', '1.2 m'],
                  ['Yta', '9.6 m²'],
                  ['pH', '6.8'],
                ].map(([l, v], i) => (
                  <div key={i} style={{
                    padding: '10px 12px',
                    borderRight: i % 2 === 0 ? `1px solid ${FAL.ink}` : 'none',
                    borderTop: i > 1 ? `1px solid ${FAL.ink}` : 'none',
                  }}>
                    <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.6, textTransform: 'uppercase', color: FAL.forest, opacity: 0.7 }}>{l}</div>
                    <div style={{ fontFamily: '"Fraunces", serif', fontSize: 22, fontWeight: 400, letterSpacing: -0.4, marginTop: 2 }}>{v}</div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Stats band */}
          <div style={{
            display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)',
            gap: 0, marginBottom: 40, paddingBottom: 28,
            borderTop: `1px solid ${FAL.ink}`, borderBottom: `1px solid ${FAL.ink}`, paddingTop: 20,
          }}>
            {[
              { v: '86', l: 'Plantor', hue: 'sage' },
              { v: '7',  l: 'Arter', hue: 'mustard' },
              { v: '24', l: 'Skördeklara', hue: 'clay' },
              { v: '38', l: 'Blommar', hue: 'berry' },
              { v: '18', l: 'Växer', hue: 'sky' },
            ].map((s, i) => (
              <div key={i} style={{
                paddingLeft: i === 0 ? 0 : 24,
                borderLeft: i === 0 ? 'none' : `1px solid ${FAL.ink}30`,
              }}>
                <div style={{
                  fontFamily: '"Fraunces", serif', fontSize: 56, lineHeight: 0.95,
                  fontWeight: 300, letterSpacing: -0.4, color: FAL.ink,
                }}>{s.v}</div>
                <div style={{
                  fontFamily: 'ui-monospace, monospace', fontSize: 10,
                  letterSpacing: 1.6, textTransform: 'uppercase', color: FAL.forest,
                  marginTop: 8, display: 'flex', alignItems: 'center', gap: 6,
                }}>
                  <span style={{ width: 6, height: 6, borderRadius: 999, background: FAL[s.hue] }} />
                  {s.l}
                </div>
              </div>
            ))}
          </div>

          {/* Plants table header */}
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 14, marginBottom: 12 }}>
            <h2 style={{
              margin: 0, fontFamily: '"Fraunces", serif', fontSize: 30,
              fontStyle: 'italic', fontWeight: 400, letterSpacing: -0.3,
            }}>Plantor<span style={{ color: FAL.clay }}>.</span></h2>
            <span style={{ fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1.6, textTransform: 'uppercase', color: FAL.forest, opacity: 0.6 }}>
              7 arter · grupperat
            </span>
            <div style={{ flex: 1, height: 1, background: FAL.ink }} />
            <span style={{ fontFamily: '"Fraunces", serif', fontStyle: 'italic', fontSize: 14, color: FAL.forest }}>+ Så fröer</span>
            <span style={{ fontFamily: '"Fraunces", serif', fontStyle: 'italic', fontSize: 14, color: FAL.forest }}>+ Plantera</span>
          </div>

          {/* Column headers */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: '50px 1.5fr 1fr 70px 1fr 100px',
            gap: 18, padding: '8px 0',
            fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.6,
            textTransform: 'uppercase', color: FAL.forest, opacity: 0.7,
            borderBottom: `1px solid ${FAL.ink}`,
          }}>
            <span>№</span>
            <span>Art</span>
            <span>Sort</span>
            <span style={{ textAlign: 'right' }}>Antal</span>
            <span>Status</span>
            <span style={{ textAlign: 'right' }}>Sådd → Utpl.</span>
          </div>

          {BED_PLANTS.map((p, i) => (
            <div key={i} style={{
              display: 'grid',
              gridTemplateColumns: '50px 1.5fr 1fr 70px 1fr 100px',
              gap: 18, padding: '16px 0', alignItems: 'center',
              borderBottom: `1px solid ${FAL.ink}25`,
            }}>
              <div style={{
                fontFamily: '"Fraunces", serif', fontStyle: 'italic',
                fontSize: 26, fontWeight: 300, color: FAL[p.hue], lineHeight: 1,
              }}>№{String(i + 1).padStart(2, '0')}</div>

              <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                <PhotoPlaceholder label={p.sp.slice(0, 3)} tone={p.tone} style={{ width: 44, height: 44 }} />
                <div>
                  <div style={{ fontFamily: '"Fraunces", serif', fontSize: 20, fontWeight: 400, letterSpacing: -0.3 }}>
                    {p.sp}
                  </div>
                  <div style={{
                    fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.4,
                    textTransform: 'uppercase', color: FAL.forest, opacity: 0.7, marginTop: 2,
                    fontStyle: 'italic',
                  }}>{p.lat}</div>
                </div>
              </div>

              <div style={{
                fontFamily: '"Fraunces", serif', fontStyle: 'italic',
                fontSize: 15, color: FAL[p.hue],
              }}>{p.var || '—'}</div>

              <div style={{
                textAlign: 'right',
                fontFamily: '"Fraunces", serif', fontSize: 28, fontWeight: 300, letterSpacing: -0.3,
              }}>{p.n}</div>

              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ width: 7, height: 7, borderRadius: 999, background: FAL[p.hue] }} />
                <span style={{ fontFamily: '"Fraunces", serif', fontStyle: 'italic', fontSize: 15 }}>
                  {p.status}
                </span>
                <span style={{
                  fontFamily: 'ui-monospace, monospace', fontSize: 8, letterSpacing: 1.2,
                  textTransform: 'uppercase', color: FAL[p.hue],
                  border: `1px solid ${FAL[p.hue]}`, padding: '1px 5px', borderRadius: 999,
                }}>{p.stage}</span>
              </div>

              <div style={{
                textAlign: 'right',
                fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 0.8,
                color: FAL.forest,
              }}>
                <div>{p.sown}</div>
                <div style={{ opacity: 0.6, marginTop: 2 }}>→ {p.planted}</div>
              </div>
            </div>
          ))}

          {/* Footer: danger + timeline hint */}
          <div style={{ marginTop: 40, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 32 }}>
            <div style={{
              background: FAL.ink, color: FAL.cream, padding: 24,
              position: 'relative', overflow: 'hidden',
            }}>
              <div style={{
                position: 'absolute', top: -20, right: -20, width: 100, height: 100,
                borderRadius: 999, background: FAL.butter, opacity: 0.2,
              }} />
              <div style={{
                fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 2,
                textTransform: 'uppercase', color: FAL.butter, marginBottom: 10,
              }}>§ Historik</div>
              <div style={{
                fontFamily: '"Fraunces", serif', fontSize: 26, lineHeight: 1.1,
                fontStyle: 'italic', fontWeight: 300, letterSpacing: -0.5,
              }}>
                142 stjälkar skördade från denna bädd <span style={{ color: FAL.blush }}>säsong 2025</span>.
              </div>
              <div style={{ marginTop: 14, display: 'flex', justifyContent: 'space-between', fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.6, textTransform: 'uppercase', opacity: 0.8 }}>
                <span style={{ color: FAL.sage }}>bästa vecka: v.32</span>
                <span style={{ color: FAL.blush }}>+24% vs 2024 ▲</span>
              </div>
            </div>
            <div style={{ border: `1px solid ${FAL.clay}40`, padding: '14px 18px' }}>
              <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 2, textTransform: 'uppercase', color: FAL.clay, marginBottom: 6 }}>
                Farozon
              </div>
              <div style={{ fontFamily: '"Fraunces", serif', fontStyle: 'italic', fontSize: 15, color: FAL.forest }}>
                Radera bädd — alla plantor ovan flyttas till återvinning.
              </div>
              <div style={{
                marginTop: 12,
                fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1.4,
                textTransform: 'uppercase', color: FAL.clay,
              }}>→ Radera bädd.02</div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

// ─────────── MOBILE ───────────
function FaltetBedMobile() {
  return (
    <div style={{
      width: '100%', height: '100%',
      background: FAL.cream, color: FAL.ink,
      fontFamily: 'Inter, sans-serif',
      overflowY: 'auto',
    }}>
      <div style={{
        padding: '14px 18px', borderBottom: `1px solid ${FAL.ink}`,
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
      }}>
        <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: FAL.forest }}>
          ← Norra fältet
        </div>
        <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: FAL.forest }}>
          ≡
        </div>
      </div>

      <div style={{ padding: '22px 18px 80px' }}>
        <div style={{ display: 'flex', gap: 6, marginBottom: 10, flexWrap: 'wrap' }}>
          <span style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: FAL.mustard, padding: '3px 7px', border: `1px solid ${FAL.mustard}`, borderRadius: 999 }}>Bädd № 02</span>
          <span style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: FAL.sage, padding: '3px 7px', border: `1px solid ${FAL.sage}`, borderRadius: 999 }}>Full sol</span>
          <span style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: FAL.sky, padding: '3px 7px', border: `1px solid ${FAL.sky}`, borderRadius: 999 }}>Dropp</span>
        </div>
        <h1 style={{
          margin: 0, fontFamily: '"Fraunces", serif',
          fontSize: 44, lineHeight: 1.0, letterSpacing: -0.7, fontWeight: 300,
        }}>
          Bädd<span style={{ color: FAL.mustard }}>.02</span> —<br/>
          <span style={{ fontStyle: 'italic', color: FAL.clay }}>Dahliornas plats.</span>
        </h1>

        <div style={{ marginTop: 18 }}>
          <PhotoPlaceholder label="Norra fältet · B2" tone="sage" style={{ width: '100%', height: 140 }} />
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8, marginTop: 16, marginBottom: 22 }}>
          {[
            { v: '86', l: 'Plant.', hue: 'sage' },
            { v: '7',  l: 'Arter', hue: 'mustard' },
            { v: '24', l: 'Skörd', hue: 'clay' },
            { v: '38', l: 'Blom', hue: 'berry' },
          ].map((s, i) => (
            <div key={i} style={{ borderTop: `1px solid ${FAL.ink}` }}>
              <div style={{ fontFamily: '"Fraunces", serif', fontSize: 28, lineHeight: 1.0, fontWeight: 300, letterSpacing: -0.4, marginTop: 8 }}>{s.v}</div>
              <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 8, letterSpacing: 1.4, textTransform: 'uppercase', color: FAL.forest, marginTop: 4, display: 'flex', alignItems: 'center', gap: 4 }}>
                <span style={{ width: 4, height: 4, borderRadius: 999, background: FAL[s.hue] }} />
                {s.l}
              </div>
            </div>
          ))}
        </div>

        <div style={{ display: 'flex', alignItems: 'baseline', gap: 10, marginBottom: 10 }}>
          <h2 style={{ margin: 0, fontFamily: '"Fraunces", serif', fontSize: 22, fontStyle: 'italic', fontWeight: 400 }}>Plantor<span style={{ color: FAL.clay }}>.</span></h2>
          <div style={{ flex: 1, height: 1, background: FAL.ink }} />
        </div>

        {BED_PLANTS.map((p, i) => (
          <div key={i} style={{
            padding: '12px 0', borderBottom: `1px solid ${FAL.ink}25`,
            display: 'grid', gridTemplateColumns: '40px 1fr auto', gap: 12, alignItems: 'center',
          }}>
            <PhotoPlaceholder label={p.sp.slice(0, 3)} tone={p.tone} style={{ width: 40, height: 40 }} />
            <div>
              <div style={{ fontFamily: '"Fraunces", serif', fontSize: 17, letterSpacing: -0.2, lineHeight: 1.1 }}>
                {p.sp}
                {p.var && <span style={{ fontStyle: 'italic', color: FAL[p.hue] }}> {p.var}</span>}
              </div>
              <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.2, textTransform: 'uppercase', color: FAL.forest, opacity: 0.7, marginTop: 3, display: 'flex', alignItems: 'center', gap: 6 }}>
                <span style={{ width: 5, height: 5, borderRadius: 999, background: FAL[p.hue] }} />
                {p.status} · {p.stage}
              </div>
            </div>
            <div style={{
              fontFamily: '"Fraunces", serif', fontSize: 26, fontWeight: 300, letterSpacing: -0.3,
              color: FAL[p.hue],
            }}>{p.n}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

window.FaltetTasksDesktop = window.FaltetTasksDesktop; // keep
window.FaltetBedDesktop = FaltetBedDesktop;
window.FaltetBedMobile = FaltetBedMobile;
