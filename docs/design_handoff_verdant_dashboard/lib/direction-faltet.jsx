// Direction 1 — "Fältet" (The Field)
// Editorial / magazine. Oversized Fraunces display serif, thin rules,
// monospace metadata, warm cream + deep forest. Big hero stat, asymmetric grid.

const FAL = {
  cream:   '#F5EFE2',
  paper:   '#FBF7EC',
  ink:     '#1E241D',
  forest:  '#2F3D2E',
  sage:    '#6B8F6A',
  clay:    '#B6553C',  // terracotta — primary accent
  mustard: '#C89A2B',  // saffron / mustard
  berry:   '#7A2E44',  // deep plum/berry
  sky:     '#4A7A8C',  // faded cornflower
  butter:  '#F2D27A',  // warm butter (fills)
  blush:   '#E9B8A8',  // dusty rose (fills)
  line:    '#1E241D',
};

function FalChip({ children, tone }) {
  // tone: undefined | 'clay' | 'mustard' | 'berry' | 'sky' | 'sage'
  const color = tone ? FAL[tone] : FAL.forest;
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 6,
      fontFamily: 'ui-monospace, "SF Mono", Menlo, monospace',
      fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase',
      color,
      padding: '4px 8px',
      border: `1px solid ${color}`,
      borderRadius: 999,
    }}>{children}</span>
  );
}

function FalStat({ value, unit, label, delta, hue = 'sage' }) {
  return (
    <div>
      <div style={{
        fontFamily: '"Fraunces", Georgia, serif',
        fontSize: 88, lineHeight: 0.95, fontWeight: 300,
        letterSpacing: -1.2, color: FAL.ink,
        fontVariationSettings: '"SOFT" 100, "opsz" 144',
      }}>
        {value}<span style={{ fontSize: 28, marginLeft: 4, color: FAL[hue], fontStyle: 'italic' }}>{unit}</span>
      </div>
      <div style={{
        fontFamily: 'ui-monospace, "SF Mono", Menlo, monospace',
        fontSize: 11, letterSpacing: 1.8, textTransform: 'uppercase',
        color: FAL.forest, marginTop: 10, display: 'flex', gap: 10, alignItems: 'center',
      }}>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          <span style={{ width: 6, height: 6, borderRadius: 999, background: FAL[hue] }} />
          {label}
        </span>
        {delta && <span style={{ color: FAL.clay }}>▲ {delta}</span>}
      </div>
    </div>
  );
}

function FalRule({ color = FAL.line, style }) {
  return <div style={{ height: 1, background: color, ...style }} />;
}

// ─────────── DESKTOP ───────────
function FaltetDesktop() {
  const bgLines = 'repeating-linear-gradient(to right, transparent 0, transparent calc((100% - 11px) / 12), rgba(30,36,29,0.04) calc((100% - 11px) / 12), rgba(30,36,29,0.04) calc((100% - 11px) / 12 + 1px))';

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
          Est. 2026
        </div>

        <div style={{ height: 28 }} />

        {/* Org switcher */}
        <div style={{
          border: `1px solid ${FAL.ink}`,
          padding: '10px 12px',
          display: 'flex', alignItems: 'center', gap: 10,
        }}>
          <div style={{
            width: 32, height: 32, background: FAL.forest, color: FAL.cream,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontFamily: '"Fraunces", serif', fontSize: 18, fontStyle: 'italic',
          }}>L</div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 13, fontWeight: 600, lineHeight: 1.1 }}>L2C Farm</div>
            <div style={{ fontSize: 10, color: FAL.forest, opacity: 0.7, fontFamily: 'ui-monospace, monospace' }}>↓ switch</div>
          </div>
        </div>

        <div style={{ height: 28 }} />

        <div style={{
          fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 2,
          textTransform: 'uppercase', color: FAL.forest, opacity: 0.6, marginBottom: 12,
        }}>§ Navigera</div>

        {[
          ['Översikt', true],
          ['Trädgårdar', false],
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
      <main style={{ overflowY: 'auto', background: bgLines }}>
        {/* Masthead */}
        <div style={{
          padding: '20px 40px',
          borderBottom: `1px solid ${FAL.ink}`,
          display: 'flex', justifyContent: 'space-between', alignItems: 'baseline',
          fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1.8,
          textTransform: 'uppercase', color: FAL.forest,
        }}>
          <span>V.17 · Fredag 21 April 2026</span>
          <span style={{ fontStyle: 'italic', fontFamily: '"Fraunces", serif', textTransform: 'none', letterSpacing: 0, fontSize: 13 }}>— Trädgårdsalmanackan —</span>
          <span>Växtzon III · ☀ 14°C</span>
        </div>

        <div style={{ padding: '48px 40px 40px' }}>
          {/* Hero header */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr auto', gap: 40, alignItems: 'end', marginBottom: 32 }}>
            <div>
              <div style={{ marginBottom: 18, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                <FalChip>Dashboard</FalChip>
                <FalChip tone="clay">3 uppgifter idag</FalChip>
                <FalChip tone="mustard">Sådd-säsong</FalChip>
                <FalChip tone="sky">Zon III</FalChip>
              </div>
              <h1 style={{
                margin: 0,
                fontFamily: '"Fraunces", Georgia, serif',
                fontSize: 92, lineHeight: 1.0, letterSpacing: -1.5,
                fontWeight: 300,
                fontVariationSettings: '"SOFT" 100, "opsz" 144',
              }}>
                God morgon,<br/>
                <span style={{ fontStyle: 'italic', color: FAL.clay }}>Erik.</span>
              </h1>
              <p style={{
                marginTop: 18, maxWidth: 540, fontSize: 15, lineHeight: 1.5,
                color: FAL.forest, fontFamily: '"Fraunces", Georgia, serif',
              }}>
                Tredje veckan i april. Sista frosten förväntas om <span style={{ color: FAL.clay, fontStyle: 'italic' }}>~11 dagar</span>.
                Sex arter väntar på omplantering, och din Dahlia-succession är redo.
              </p>
            </div>
            <div style={{ textAlign: 'right' }}>
              <PhotoPlaceholder label="Farm photo / hero" tone="sage" style={{ width: 220, height: 220 }} />
            </div>
          </div>

          <FalRule style={{ marginBottom: 40 }} />

          {/* Hero stats row */}
          <div style={{
            display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)',
            gap: 0, marginBottom: 56,
          }}>
            {[
              { v: '312', u: '', l: 'Plantor aktiva', d: null, hue: 'sage' },
              { v: '8.4', u: 'kg', l: 'Skörd i april', d: '18%', hue: 'clay' },
              { v: '47', u: '', l: 'I bricka', d: null, hue: 'mustard' },
              { v: '12', u: '', l: 'Arter sådda', d: '3 nya', hue: 'berry' },
            ].map((s, i) => (
              <div key={i} style={{
                paddingLeft: i === 0 ? 0 : 28,
                borderLeft: i === 0 ? 'none' : `1px solid ${FAL.ink}30`,
              }}>
                <FalStat {...s} />
              </div>
            ))}
          </div>

          {/* Asymmetric main grid */}
          <div style={{ display: 'grid', gridTemplateColumns: '7fr 5fr', gap: 40 }}>
            {/* Left column — Trädgårdar */}
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 18 }}>
                <h2 style={{
                  margin: 0, fontFamily: '"Fraunces", serif', fontSize: 32,
                  fontStyle: 'italic', fontWeight: 400, letterSpacing: -0.4,
                }}>
                  Trädgårdar<span style={{ color: FAL.clay }}>.</span>
                </h2>
                <span style={{ fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1.8, textTransform: 'uppercase', color: FAL.forest }}>
                  Se alla →
                </span>
              </div>

              <FalRule />

              {[
                { name: 'Norra fältet', beds: 6, plants: 184, emoji: 'N', tone: 'sage', mood: 'Blommar snart', hue: 'mustard' },
                { name: 'Södra växthuset', beds: 3, plants: 82, emoji: 'S', tone: 'cream', mood: 'Groningsfas', hue: 'sky' },
                { name: 'Köksträdgården', beds: 4, plants: 46, emoji: 'K', tone: 'peach', mood: 'Vilar', hue: 'berry' },
              ].map((g, i) => (
                <div key={i}>
                  <div style={{
                    padding: '20px 0', display: 'grid',
                    gridTemplateColumns: '72px 1fr auto', gap: 20, alignItems: 'center',
                  }}>
                    <PhotoPlaceholder label={g.name} tone={g.tone} style={{ width: 72, height: 72 }} />
                    <div>
                      <div style={{
                        fontFamily: '"Fraunces", serif', fontSize: 22,
                        fontWeight: 400, letterSpacing: -0.4, marginBottom: 4,
                        display: 'flex', alignItems: 'center', gap: 10,
                      }}>
                        <span>{g.name}</span>
                        <span style={{ width: 8, height: 8, borderRadius: 999, background: FAL[g.hue] }} />
                      </div>
                      <div style={{
                        fontFamily: 'ui-monospace, monospace', fontSize: 10,
                        letterSpacing: 1.4, textTransform: 'uppercase', color: FAL.forest,
                      }}>
                        {g.beds} bäddar — {g.plants} plantor — <span style={{ color: FAL[g.hue], fontStyle: 'italic', fontFamily: '"Fraunces", serif', textTransform: 'none', letterSpacing: 0, fontSize: 13 }}>{g.mood}</span>
                      </div>
                    </div>
                    <div style={{
                      fontFamily: '"Fraunces", serif', fontSize: 28,
                      fontStyle: 'italic', color: FAL[g.hue], opacity: 0.5,
                    }}>
                      №{String(i + 1).padStart(2, '0')}
                    </div>
                  </div>
                  <FalRule />
                </div>
              ))}
            </div>

            {/* Right column — Dagens skörd + Plantor i bricka */}
            <div>
              {/* Big pull-quote harvest */}
              <div style={{
                background: FAL.ink,
                color: FAL.cream,
                padding: 32,
                marginBottom: 32,
                position: 'relative',
                overflow: 'hidden',
              }}>
                <div style={{
                  position: 'absolute', top: -30, right: -30, width: 140, height: 140,
                  borderRadius: 999, background: FAL.butter, opacity: 0.18,
                }} />
                <div style={{
                  fontFamily: 'ui-monospace, monospace', fontSize: 10,
                  letterSpacing: 2, textTransform: 'uppercase',
                  color: FAL.butter, marginBottom: 14, position: 'relative',
                }}>§ Skördens vecka</div>
                <div style={{
                  fontFamily: '"Fraunces", serif', fontSize: 36,
                  lineHeight: 1.05, letterSpacing: -0.4,
                  fontStyle: 'italic', fontWeight: 300, position: 'relative',
                }}>
                  "Dahlia '<span style={{ color: FAL.butter }}>Café au Lait</span>'<br/>
                  gav <span style={{ color: FAL.blush }}>2.1 kg</span> — bästa<br/>
                  veckan hittills."
                </div>
                <FalRule color={FAL.sage} style={{ margin: '22px 0 14px', opacity: 0.4 }} />
                <div style={{
                  display: 'flex', justifyContent: 'space-between',
                  fontFamily: 'ui-monospace, monospace', fontSize: 10,
                  letterSpacing: 1.4, textTransform: 'uppercase', opacity: 0.8, position: 'relative',
                }}>
                  <span style={{ color: FAL.sage }}>v.16 · 12 skördar</span>
                  <span style={{ color: FAL.blush }}>+18% ▲</span>
                </div>
              </div>

              {/* Plantor i bricka */}
              <div>
                <h3 style={{
                  margin: 0, fontFamily: '"Fraunces", serif', fontSize: 24,
                  fontStyle: 'italic', fontWeight: 400, letterSpacing: -0.3, marginBottom: 14,
                }}>
                  I brickan<span style={{ color: FAL.clay }}>.</span>
                </h3>
                <FalRule />
                {[
                  ['Zinnia elegans', 18, 'Gronar', 'mustard'],
                  ['Cosmos bipinnatus', 12, 'Hjärtblad', 'sage'],
                  ['Antirrhinum majus', 9, 'Groddar', 'berry'],
                  ['Nigella damascena', 8, 'Hjärtblad', 'sky'],
                ].map(([name, count, stage, hue], i) => (
                  <div key={i}>
                    <div style={{
                      padding: '12px 0', display: 'flex',
                      justifyContent: 'space-between', alignItems: 'baseline',
                    }}>
                      <div style={{
                        fontFamily: '"Fraunces", serif', fontSize: 15,
                        fontStyle: 'italic', fontWeight: 400,
                        display: 'flex', alignItems: 'center', gap: 10,
                      }}>
                        <span style={{ width: 6, height: 6, borderRadius: 999, background: FAL[hue] }} />
                        {name}
                      </div>
                      <div style={{
                        display: 'flex', gap: 14, alignItems: 'baseline',
                        fontFamily: 'ui-monospace, monospace', fontSize: 10,
                        letterSpacing: 1.4, textTransform: 'uppercase', color: FAL[hue],
                      }}>
                        <span>{stage}</span>
                        <span style={{
                          fontFamily: '"Fraunces", serif', fontSize: 20,
                          letterSpacing: 0, textTransform: 'none', color: FAL.ink,
                        }}>{count}</span>
                      </div>
                    </div>
                    <FalRule style={{ opacity: 0.3 }} />
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Bottom: tasks band */}
          <div style={{ marginTop: 56 }}>
            <FalRule />
            <div style={{
              padding: '24px 0',
              display: 'grid', gridTemplateColumns: '200px 1fr',
              gap: 40,
            }}>
              <div>
                <h3 style={{
                  margin: 0, fontFamily: '"Fraunces", serif', fontSize: 28,
                  fontStyle: 'italic', fontWeight: 400, letterSpacing: -0.3,
                }}>Idag<span style={{ color: FAL.clay }}>.</span></h3>
                <div style={{
                  fontFamily: 'ui-monospace, monospace', fontSize: 10,
                  letterSpacing: 1.6, textTransform: 'uppercase',
                  color: FAL.forest, marginTop: 6,
                }}>3 av 5 klara</div>
              </div>
              <div style={{ display: 'grid', gap: 12 }}>
                {[
                  { t: 'Omplantera Zinnia till 9cm-kruka', tag: 'Plantering', done: true, tone: 'sage' },
                  { t: 'Vattna Norra fältet bädd 3–6', tag: 'Vård', done: true, tone: 'sky' },
                  { t: 'Så Bupleurum direkt i bädd 4', tag: 'Sådd', done: true, tone: 'mustard' },
                  { t: 'Skörda Snapdragon — 12 stjälkar', tag: 'Skörd', done: false, tone: 'clay' },
                  { t: 'Fotodokumentera varietetsförsök', tag: 'Försök', done: false, tone: 'berry' },
                ].map((task, i) => (
                  <div key={i} style={{
                    display: 'grid', gridTemplateColumns: '20px 1fr auto',
                    gap: 14, alignItems: 'center',
                    opacity: task.done ? 0.4 : 1,
                  }}>
                    <div style={{
                      width: 14, height: 14, border: `1.5px solid ${FAL.ink}`,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      background: task.done ? FAL.ink : 'transparent',
                      color: FAL.cream, fontSize: 10,
                    }}>{task.done ? '✓' : ''}</div>
                    <div style={{
                      fontFamily: '"Fraunces", serif', fontSize: 17,
                      textDecoration: task.done ? 'line-through' : 'none',
                    }}>{task.t}</div>
                    <FalChip tone={task.tone}>{task.tag}</FalChip>
                  </div>
                ))}
              </div>
            </div>
            <FalRule />
          </div>
        </div>
      </main>
    </div>
  );
}

// ─────────── MOBILE ───────────
function FaltetMobile() {
  return (
    <div style={{
      width: '100%', height: '100%',
      background: FAL.cream, color: FAL.ink,
      fontFamily: 'Inter, sans-serif',
      overflowY: 'auto',
    }}>
      {/* Masthead */}
      <div style={{
        padding: '14px 18px', borderBottom: `1px solid ${FAL.ink}`,
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
      }}>
        <div style={{
          fontFamily: '"Fraunces", serif', fontStyle: 'italic',
          fontSize: 22, letterSpacing: -0.4,
        }}>Verdant<span style={{ color: FAL.clay }}>.</span></div>
        <div style={{
          fontFamily: 'ui-monospace, monospace', fontSize: 9,
          letterSpacing: 1.4, textTransform: 'uppercase', color: FAL.forest,
        }}>V.17 · ≡</div>
      </div>

      <div style={{
        padding: '10px 18px', borderBottom: `1px solid ${FAL.ink}30`,
        fontFamily: 'ui-monospace, monospace', fontSize: 9,
        letterSpacing: 1.6, textTransform: 'uppercase', color: FAL.forest,
        display: 'flex', justifyContent: 'space-between',
      }}>
        <span>21 april · fredag</span>
        <span>zon III · ☀ 14°C</span>
      </div>

      <div style={{ padding: '28px 18px 100px' }}>
        <div style={{ display: 'flex', gap: 6, marginBottom: 14, flexWrap: 'wrap' }}>
          <FalChip>Översikt</FalChip>
          <FalChip tone="clay">3 idag</FalChip>
          <FalChip tone="mustard">Sådd</FalChip>
        </div>
        <h1 style={{
          margin: 0, fontFamily: '"Fraunces", serif',
          fontSize: 52, lineHeight: 1.0, letterSpacing: -0.4, fontWeight: 300,
        }}>
          God morgon,<br/>
          <span style={{ fontStyle: 'italic', color: FAL.clay }}>Erik.</span>
        </h1>
        <p style={{
          fontFamily: '"Fraunces", serif', fontSize: 14, lineHeight: 1.5,
          color: FAL.forest, marginTop: 14,
        }}>
          Sista frosten om <span style={{ color: FAL.clay, fontStyle: 'italic' }}>~11 dagar</span>. Sex arter väntar på omplantering.
        </p>

        <FalRule style={{ margin: '28px 0' }} />

        {/* Stat grid 2x2 */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
          {[
            { v: '312', u: '', l: 'Plantor', hue: 'sage' },
            { v: '8.4', u: 'kg', l: 'Skörd', d: '18%', hue: 'clay' },
            { v: '47', u: '', l: 'I bricka', hue: 'mustard' },
            { v: '12', u: '', l: 'Arter', hue: 'berry' },
          ].map((s, i) => (
            <div key={i} style={{
              paddingTop: i > 1 ? 20 : 0,
              borderTop: i > 1 ? `1px solid ${FAL.ink}30` : 'none',
            }}>
              <div style={{
                fontFamily: '"Fraunces", serif', fontSize: 52,
                lineHeight: 0.95, fontWeight: 300, letterSpacing: -0.4,
              }}>
                {s.v}<span style={{ fontSize: 18, color: FAL[s.hue], fontStyle: 'italic' }}>{s.u}</span>
              </div>
              <div style={{
                fontFamily: 'ui-monospace, monospace', fontSize: 9,
                letterSpacing: 1.6, textTransform: 'uppercase',
                color: FAL.forest, marginTop: 6,
                display: 'flex', alignItems: 'center', gap: 6,
              }}>
                <span style={{ width: 5, height: 5, borderRadius: 999, background: FAL[s.hue] }} />
                {s.l}{s.d && <span style={{ color: FAL.clay }}> · ▲{s.d}</span>}
              </div>
            </div>
          ))}
        </div>

        <FalRule style={{ margin: '32px 0 20px' }} />

        {/* Harvest quote */}
        <div style={{
          background: FAL.ink, color: FAL.cream, padding: 22, marginBottom: 28,
          position: 'relative', overflow: 'hidden',
        }}>
          <div style={{
            position: 'absolute', top: -20, right: -20, width: 90, height: 90,
            borderRadius: 999, background: FAL.butter, opacity: 0.2,
          }} />
          <div style={{
            fontFamily: 'ui-monospace, monospace', fontSize: 9,
            letterSpacing: 1.8, textTransform: 'uppercase',
            color: FAL.butter, marginBottom: 12, position: 'relative',
          }}>§ Skördens vecka</div>
          <div style={{
            fontFamily: '"Fraunces", serif', fontSize: 24, lineHeight: 1.1,
            fontStyle: 'italic', fontWeight: 300, letterSpacing: -0.4, position: 'relative',
          }}>
            "Dahlia '<span style={{ color: FAL.butter }}>Café au Lait</span>' gav <span style={{ color: FAL.blush }}>2.1 kg</span>."
          </div>
        </div>

        {/* Gardens */}
        <h2 style={{
          margin: '0 0 8px', fontFamily: '"Fraunces", serif',
          fontSize: 28, fontStyle: 'italic', fontWeight: 400, letterSpacing: -0.3,
        }}>Trädgårdar<span style={{ color: FAL.clay }}>.</span></h2>
        <FalRule />
        {[
          { name: 'Norra fältet', beds: 6, plants: 184, tone: 'sage', hue: 'mustard' },
          { name: 'Södra växthuset', beds: 3, plants: 82, tone: 'cream', hue: 'sky' },
          { name: 'Köksträdgården', beds: 4, plants: 46, tone: 'peach', hue: 'berry' },
        ].map((g, i) => (
          <div key={i}>
            <div style={{
              padding: '14px 0', display: 'grid',
              gridTemplateColumns: '52px 1fr auto', gap: 14, alignItems: 'center',
            }}>
              <PhotoPlaceholder label={g.name.slice(0, 3)} tone={g.tone} style={{ width: 52, height: 52 }} />
              <div>
                <div style={{ fontFamily: '"Fraunces", serif', fontSize: 17, fontWeight: 400, display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span>{g.name}</span>
                  <span style={{ width: 6, height: 6, borderRadius: 999, background: FAL[g.hue] }} />
                </div>
                <div style={{
                  fontFamily: 'ui-monospace, monospace', fontSize: 9,
                  letterSpacing: 1.4, textTransform: 'uppercase', color: FAL.forest, marginTop: 2,
                }}>{g.beds} b · {g.plants} p</div>
              </div>
              <div style={{ fontFamily: '"Fraunces", serif', fontStyle: 'italic', fontSize: 22, color: FAL[g.hue], opacity: 0.55 }}>
                №{String(i + 1).padStart(2, '0')}
              </div>
            </div>
            <FalRule style={{ opacity: 0.4 }} />
          </div>
        ))}
      </div>
    </div>
  );
}

window.FaltetDesktop = FaltetDesktop;
window.FaltetMobile = FaltetMobile;
