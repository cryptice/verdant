// Direction 3 — "Växthuset" (Greenhouse)
// Organic, warm, edgy-friendly. Big rounded asymmetric tiles,
// hand-written Caveat accents, sage/peach/butter color fields, blob shapes,
// oversized emoji, soft gradients.

const VX = {
  bg:     '#F7F3EA',
  paper:  '#FDFAF2',
  ink:    '#1F2A1D',
  deep:   '#2B3B28',
  sage:   '#7BAF7D',
  sageDark: '#4E7D52',
  sageLight: '#E4EFDE',
  peach:  '#F5B991',
  peachLight: '#FBE6D3',
  butter: '#F0D36B',
  butterLight: '#F9EBB8',
  rose:   '#E59393',
  roseLight: '#F5D4D0',
  clay:   '#D35A3A',
};

function Blob({ color = VX.sageLight, size = 140, style = {}, variant = 0 }) {
  const paths = [
    'M50,10 C75,5 95,25 95,50 C95,78 70,95 50,92 C22,90 5,72 10,45 C13,22 28,14 50,10 Z',
    'M50,8 C82,12 92,30 88,58 C85,82 62,94 38,90 C12,86 6,62 12,38 C18,18 32,6 50,8 Z',
    'M48,12 C72,10 90,22 92,52 C94,80 68,92 44,90 C18,88 8,68 12,42 C14,22 30,14 48,12 Z',
  ];
  return (
    <svg viewBox="0 0 100 100" style={{ width: size, height: size, ...style }}>
      <path d={paths[variant % paths.length]} fill={color} />
    </svg>
  );
}

function VXDesktop() {
  return (
    <div style={{
      width: '100%', height: '100%',
      background: VX.bg, color: VX.ink,
      fontFamily: '"Inter", -apple-system, sans-serif',
      display: 'grid', gridTemplateColumns: '76px 1fr',
      overflow: 'hidden',
    }}>
      {/* Emoji nav rail */}
      <aside style={{
        padding: '20px 0', display: 'flex', flexDirection: 'column',
        alignItems: 'center', gap: 6,
      }}>
        <div style={{
          width: 44, height: 44, borderRadius: 14,
          background: VX.deep, color: VX.butter,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontFamily: '"Caveat", cursive', fontSize: 28, fontWeight: 700,
          transform: 'rotate(-6deg)', marginBottom: 10,
        }}>V</div>
        {[
          { e: '🏠', a: true, bg: VX.sageLight },
          { e: '🏡', a: false },
          { e: '🌿', a: false },
          { e: '🌱', a: false },
          { e: '📋', a: false },
          { e: '🫘', a: false },
          { e: '📈', a: false },
          { e: '👥', a: false },
        ].map((n, i) => (
          <div key={i} style={{
            width: 52, height: 52, borderRadius: 16,
            background: n.a ? n.bg : 'transparent',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 22,
            boxShadow: n.a ? `inset 0 0 0 1.5px ${VX.sage}40` : 'none',
          }}>{n.e}</div>
        ))}
      </aside>

      <main style={{ overflowY: 'auto', padding: '28px 32px 40px 8px' }}>
        {/* Top header with greeting */}
        <div style={{
          display: 'grid', gridTemplateColumns: '1fr auto',
          alignItems: 'center', marginBottom: 22,
        }}>
          <div>
            <div style={{
              fontFamily: '"Caveat", cursive', fontSize: 26,
              color: VX.sageDark, transform: 'rotate(-2deg)',
              display: 'inline-block', marginBottom: 4,
            }}>God morgon ✿</div>
            <h1 style={{
              margin: 0, fontSize: 44, fontWeight: 700,
              letterSpacing: -1.2, color: VX.ink,
              fontFamily: '"Instrument Serif", Georgia, serif',
            }}>
              Erik, det är en fin{' '}
              <span style={{
                background: VX.butterLight, padding: '0 10px',
                borderRadius: 20, fontStyle: 'italic',
              }}>fredag</span> för trädgården.
            </h1>
          </div>
          <div style={{ display: 'flex', gap: 10 }}>
            <button style={{
              padding: '10px 14px', background: VX.paper,
              border: `1.5px solid ${VX.ink}15`, borderRadius: 14,
              fontSize: 13, fontWeight: 500, color: VX.ink,
              cursor: 'pointer', fontFamily: 'inherit',
            }}>☀ 14°C</button>
            <button style={{
              padding: '10px 18px', background: VX.ink, color: VX.paper,
              border: 'none', borderRadius: 14,
              fontSize: 13, fontWeight: 600,
              cursor: 'pointer', fontFamily: 'inherit',
              display: 'flex', alignItems: 'center', gap: 6,
            }}>+ <span>Ny sådd</span></button>
          </div>
        </div>

        {/* Hero mosaic — bento */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(4, 1fr)',
          gridAutoRows: 140,
          gap: 14, marginBottom: 18,
        }}>
          {/* Big featured harvest */}
          <div style={{
            gridColumn: 'span 2', gridRow: 'span 2',
            background: VX.deep, color: VX.paper,
            borderRadius: 28, padding: 26,
            position: 'relative', overflow: 'hidden',
          }}>
            <Blob color={`${VX.sage}50`} size={220} variant={1}
              style={{ position: 'absolute', bottom: -60, right: -50 }} />
            <div style={{ position: 'relative', zIndex: 1 }}>
              <div style={{
                fontFamily: '"Caveat", cursive', fontSize: 22,
                color: VX.butter, transform: 'rotate(-1deg)',
                display: 'inline-block',
              }}>veckans stjärna ✦</div>
              <div style={{
                marginTop: 6, fontSize: 32, fontWeight: 700,
                letterSpacing: -0.6, lineHeight: 1.1,
                fontFamily: '"Instrument Serif", serif',
              }}>Dahlia "Café au&nbsp;Lait"</div>
              <div style={{ display: 'flex', gap: 24, marginTop: 18 }}>
                <div>
                  <div style={{ fontSize: 36, fontWeight: 700, letterSpacing: -1 }}>2,1<span style={{ fontSize: 16, opacity: 0.7, marginLeft: 2 }}>kg</span></div>
                  <div style={{ fontSize: 11, opacity: 0.7, letterSpacing: 0.4, textTransform: 'uppercase' }}>Denna vecka</div>
                </div>
                <div>
                  <div style={{ fontSize: 36, fontWeight: 700, letterSpacing: -1, color: VX.butter }}>+18%</div>
                  <div style={{ fontSize: 11, opacity: 0.7, letterSpacing: 0.4, textTransform: 'uppercase' }}>Mot förra</div>
                </div>
                <div>
                  <div style={{ fontSize: 36, fontWeight: 700, letterSpacing: -1 }}>47</div>
                  <div style={{ fontSize: 11, opacity: 0.7, letterSpacing: 0.4, textTransform: 'uppercase' }}>Stjälkar</div>
                </div>
              </div>
              <div style={{
                marginTop: 22, padding: '8px 12px',
                background: 'rgba(255,255,255,0.08)',
                borderRadius: 999, display: 'inline-flex',
                alignItems: 'center', gap: 8, fontSize: 12,
              }}>
                <span>▲</span> Se skördetrend
              </div>
            </div>
          </div>

          {/* Frost countdown */}
          <div style={{
            background: VX.peachLight, borderRadius: 24,
            padding: 20, position: 'relative', overflow: 'hidden',
          }}>
            <div style={{
              fontFamily: '"Caveat", cursive', fontSize: 18,
              color: VX.clay, transform: 'rotate(-2deg)', display: 'inline-block',
            }}>sista frosten ❄︎</div>
            <div style={{ fontSize: 54, fontWeight: 700, letterSpacing: -2, color: VX.deep, marginTop: 4, lineHeight: 1 }}>
              ~11<span style={{ fontSize: 18, marginLeft: 4, color: VX.clay }}>dagar</span>
            </div>
            <div style={{ fontSize: 11, color: VX.deep, opacity: 0.7, marginTop: 6 }}>2 maj (prognos)</div>
          </div>

          {/* In tray */}
          <div style={{
            background: VX.butterLight, borderRadius: 24,
            padding: 20, position: 'relative',
          }}>
            <div style={{ fontSize: 28, marginBottom: 6 }}>🌱</div>
            <div style={{ fontSize: 42, fontWeight: 700, letterSpacing: -1.4, color: VX.deep, lineHeight: 1 }}>47</div>
            <div style={{ fontSize: 12, color: VX.deep, opacity: 0.7, marginTop: 4 }}>plantor i brickan</div>
            <div style={{
              position: 'absolute', top: 16, right: 16,
              padding: '3px 10px', borderRadius: 999,
              background: VX.sage, color: VX.paper,
              fontSize: 10, fontWeight: 600, letterSpacing: 0.4,
            }}>6 REDO</div>
          </div>

          {/* Tasks tile */}
          <div style={{
            background: VX.paper, border: `1.5px solid ${VX.ink}10`,
            borderRadius: 24, padding: 20,
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 10 }}>
              <div style={{
                fontFamily: '"Caveat", cursive', fontSize: 20,
                color: VX.sageDark, transform: 'rotate(-1deg)',
              }}>idag ✓</div>
              <div style={{ fontSize: 11, color: VX.ink, opacity: 0.5 }}>3/5</div>
            </div>
            <div style={{
              height: 8, background: VX.sageLight, borderRadius: 4, overflow: 'hidden',
            }}>
              <div style={{ width: '60%', height: '100%', background: VX.sage, borderRadius: 4 }} />
            </div>
            <div style={{ fontSize: 13, marginTop: 10, color: VX.ink, fontWeight: 500 }}>
              Skörda Snapdragon
            </div>
            <div style={{ fontSize: 11, color: VX.ink, opacity: 0.6 }}>nästa i kön · 12 st</div>
          </div>

          {/* Active plants */}
          <div style={{
            background: VX.sageLight, borderRadius: 24,
            padding: 20, position: 'relative', overflow: 'hidden',
          }}>
            <Blob color={`${VX.sage}30`} size={100} variant={2}
              style={{ position: 'absolute', bottom: -20, right: -20 }} />
            <div style={{ position: 'relative' }}>
              <div style={{
                fontFamily: '"Caveat", cursive', fontSize: 18,
                color: VX.sageDark, transform: 'rotate(-1deg)',
              }}>aktiva plantor</div>
              <div style={{ fontSize: 48, fontWeight: 700, letterSpacing: -1.6, color: VX.deep, marginTop: 4, lineHeight: 1 }}>312</div>
              <div style={{ fontSize: 11, color: VX.sageDark, marginTop: 4, fontWeight: 600 }}>+24 denna vecka</div>
            </div>
          </div>
        </div>

        {/* Gardens strip */}
        <div style={{
          display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 14,
        }}>
          <div>
            <div style={{
              fontFamily: '"Caveat", cursive', fontSize: 22,
              color: VX.clay, transform: 'rotate(-1deg)', display: 'inline-block',
            }}>dina hörn</div>
            <h2 style={{
              margin: 0, fontSize: 28, fontWeight: 700, letterSpacing: -0.8,
              fontFamily: '"Instrument Serif", serif',
            }}>Trädgårdar</h2>
          </div>
          <div style={{ fontSize: 13, color: VX.sageDark, fontWeight: 500 }}>Se alla 3 →</div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 14, marginBottom: 18 }}>
          {[
            { n: 'Norra fältet',    bg: VX.sageLight,   blob: VX.sage,   emoji: '🌾', b: 6, p: 184, mood: 'blommar snart' },
            { n: 'Södra växthuset', bg: VX.peachLight,  blob: VX.peach,  emoji: '🌷', b: 3, p: 82,  mood: 'groningsfas' },
            { n: 'Köksträdgården',  bg: VX.butterLight, blob: VX.butter, emoji: '🥬', b: 4, p: 46,  mood: 'vilar nu' },
          ].map((g, i) => (
            <div key={i} style={{
              background: g.bg, borderRadius: 24, padding: 22,
              position: 'relative', overflow: 'hidden', minHeight: 150,
            }}>
              <Blob color={`${g.blob}60`} size={120} variant={i}
                style={{ position: 'absolute', bottom: -40, right: -30 }} />
              <div style={{ position: 'relative' }}>
                <div style={{ fontSize: 32, marginBottom: 10 }}>{g.emoji}</div>
                <div style={{
                  fontSize: 22, fontWeight: 700, letterSpacing: -0.4,
                  color: VX.deep, fontFamily: '"Instrument Serif", serif',
                }}>{g.n}</div>
                <div style={{
                  fontFamily: '"Caveat", cursive', fontSize: 18,
                  color: VX.deep, opacity: 0.7, marginTop: 2,
                  transform: 'rotate(-1deg)', display: 'inline-block',
                }}>{g.mood}</div>
                <div style={{ display: 'flex', gap: 14, marginTop: 12 }}>
                  <div>
                    <div style={{ fontSize: 20, fontWeight: 700, color: VX.deep }}>{g.b}</div>
                    <div style={{ fontSize: 10, color: VX.deep, opacity: 0.6, letterSpacing: 0.3, textTransform: 'uppercase' }}>bäddar</div>
                  </div>
                  <div>
                    <div style={{ fontSize: 20, fontWeight: 700, color: VX.deep }}>{g.p}</div>
                    <div style={{ fontSize: 10, color: VX.deep, opacity: 0.6, letterSpacing: 0.3, textTransform: 'uppercase' }}>plantor</div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Bottom row: tray plants + tasks list */}
        <div style={{ display: 'grid', gridTemplateColumns: '1.3fr 1fr', gap: 14 }}>
          {/* Tray plants */}
          <div style={{
            background: VX.paper, borderRadius: 24,
            padding: 22, border: `1.5px solid ${VX.ink}10`,
          }}>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 10, marginBottom: 14 }}>
              <h3 style={{
                margin: 0, fontSize: 22, fontWeight: 700, letterSpacing: -0.4,
                fontFamily: '"Instrument Serif", serif',
              }}>I brickan</h3>
              <span style={{
                fontFamily: '"Caveat", cursive', fontSize: 20,
                color: VX.sageDark, transform: 'rotate(-1deg)',
                display: 'inline-block',
              }}>grodda små ✿</span>
            </div>
            {[
              { n: 'Zinnia elegans', sv: 'Zinnia',     c: 18, s: 'hjärtblad', d: '9d', tone: VX.sageLight, ring: VX.sage },
              { n: 'Cosmos bipinnatus', sv: 'Rosenskära', c: 12, s: 'hjärtblad', d: '7d', tone: VX.peachLight, ring: VX.peach },
              { n: 'Antirrhinum majus', sv: 'Lejongap',   c: 9, s: 'groddar',  d: '5d', tone: VX.butterLight, ring: VX.butter },
              { n: 'Nigella damascena', sv: 'Jungfrun',   c: 8, s: 'groddar',  d: '4d', tone: VX.roseLight, ring: VX.rose },
            ].map((p, i) => (
              <div key={i} style={{
                padding: '10px 0',
                borderTop: i > 0 ? `1px solid ${VX.ink}08` : 'none',
                display: 'grid', gridTemplateColumns: '40px 1fr auto auto',
                gap: 14, alignItems: 'center',
              }}>
                <div style={{
                  width: 40, height: 40, borderRadius: 12,
                  background: p.tone, display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: 20,
                }}>🌱</div>
                <div>
                  <div style={{ fontWeight: 600, fontSize: 14 }}>{p.sv}</div>
                  <div style={{ fontSize: 11, color: VX.ink, opacity: 0.55, fontStyle: 'italic' }}>{p.n}</div>
                </div>
                <div style={{
                  padding: '3px 9px', borderRadius: 999,
                  background: p.tone, color: VX.deep,
                  fontSize: 11, fontWeight: 500,
                }}>{p.s} · {p.d}</div>
                <div style={{ fontSize: 22, fontWeight: 700, color: VX.deep, letterSpacing: -0.6, minWidth: 30, textAlign: 'right' }}>{p.c}</div>
              </div>
            ))}
          </div>

          {/* Tasks list */}
          <div style={{
            background: VX.deep, borderRadius: 24, padding: 22, color: VX.paper,
            position: 'relative', overflow: 'hidden',
          }}>
            <Blob color={`${VX.sage}30`} size={140} variant={0}
              style={{ position: 'absolute', top: -40, right: -40 }} />
            <div style={{ position: 'relative' }}>
              <div style={{
                fontFamily: '"Caveat", cursive', fontSize: 22,
                color: VX.butter, transform: 'rotate(-2deg)', display: 'inline-block',
              }}>att göra ✓</div>
              <h3 style={{
                margin: '4px 0 14px', fontSize: 22, fontWeight: 700, letterSpacing: -0.4,
                fontFamily: '"Instrument Serif", serif',
              }}>Idag, fredag</h3>
              {[
                { t: 'Omplantera Zinnia', done: true },
                { t: 'Vattna Norra bädd 3–6', done: true },
                { t: 'Så Bupleurum', done: true },
                { t: 'Skörda Snapdragon · 12 st', done: false, hl: true },
                { t: 'Foto varietetsförsök', done: false },
              ].map((task, i) => (
                <div key={i} style={{
                  padding: '8px 0',
                  display: 'flex', alignItems: 'center', gap: 10,
                  opacity: task.done ? 0.45 : 1,
                }}>
                  <div style={{
                    width: 18, height: 18, borderRadius: 999,
                    border: `1.5px solid ${task.done ? VX.sage : VX.paper}60`,
                    background: task.done ? VX.sage : 'transparent',
                    color: VX.deep, fontSize: 11, fontWeight: 800,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}>{task.done ? '✓' : ''}</div>
                  <div style={{
                    fontSize: 14, flex: 1,
                    textDecoration: task.done ? 'line-through' : 'none',
                    fontWeight: task.hl ? 600 : 400,
                  }}>{task.t}</div>
                  {task.hl && <span style={{
                    fontFamily: '"Caveat", cursive', fontSize: 16, color: VX.butter,
                    transform: 'rotate(-3deg)', display: 'inline-block',
                  }}>next!</span>}
                </div>
              ))}
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

function VXMobile() {
  return (
    <div style={{
      width: '100%', height: '100%', overflowY: 'auto',
      background: VX.bg, color: VX.ink,
      fontFamily: '"Inter", sans-serif',
    }}>
      <div style={{ padding: '18px 16px 100px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18 }}>
          <div style={{
            width: 36, height: 36, borderRadius: 12,
            background: VX.deep, color: VX.butter,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontFamily: '"Caveat", cursive', fontSize: 22, fontWeight: 700,
            transform: 'rotate(-6deg)',
          }}>V</div>
          <div style={{
            padding: '6px 12px', background: VX.paper, borderRadius: 999,
            fontSize: 11, color: VX.ink, border: `1px solid ${VX.ink}12`,
          }}>☀ 14°C</div>
        </div>

        <div style={{
          fontFamily: '"Caveat", cursive', fontSize: 22,
          color: VX.sageDark, transform: 'rotate(-2deg)',
          display: 'inline-block',
        }}>God morgon ✿</div>
        <h1 style={{
          margin: '2px 0 18px', fontSize: 30, fontWeight: 700, letterSpacing: -0.8,
          fontFamily: '"Instrument Serif", serif', lineHeight: 1.1,
        }}>
          Erik, fin{' '}
          <span style={{ background: VX.butterLight, padding: '0 8px', borderRadius: 14, fontStyle: 'italic' }}>fredag</span>.
        </h1>

        {/* Hero */}
        <div style={{
          background: VX.deep, color: VX.paper,
          borderRadius: 22, padding: 20, marginBottom: 10,
          position: 'relative', overflow: 'hidden',
        }}>
          <Blob color={`${VX.sage}50`} size={160} variant={1}
            style={{ position: 'absolute', bottom: -40, right: -40 }} />
          <div style={{ position: 'relative' }}>
            <div style={{
              fontFamily: '"Caveat", cursive', fontSize: 18, color: VX.butter,
              transform: 'rotate(-1deg)', display: 'inline-block',
            }}>veckans stjärna ✦</div>
            <div style={{ marginTop: 4, fontSize: 22, fontWeight: 700, letterSpacing: -0.5, fontFamily: '"Instrument Serif", serif' }}>
              Dahlia "Café au Lait"
            </div>
            <div style={{ display: 'flex', gap: 16, marginTop: 12 }}>
              <div>
                <div style={{ fontSize: 26, fontWeight: 700, letterSpacing: -0.8 }}>2,1<span style={{ fontSize: 12, opacity: 0.7 }}>kg</span></div>
                <div style={{ fontSize: 10, opacity: 0.7, textTransform: 'uppercase', letterSpacing: 0.4 }}>denna v.</div>
              </div>
              <div>
                <div style={{ fontSize: 26, fontWeight: 700, letterSpacing: -0.8, color: VX.butter }}>+18%</div>
                <div style={{ fontSize: 10, opacity: 0.7, textTransform: 'uppercase', letterSpacing: 0.4 }}>trend</div>
              </div>
            </div>
          </div>
        </div>

        {/* 2×2 mini tiles */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 18 }}>
          <div style={{ background: VX.peachLight, borderRadius: 18, padding: 14 }}>
            <div style={{ fontFamily: '"Caveat", cursive', fontSize: 16, color: VX.clay, transform: 'rotate(-2deg)', display: 'inline-block' }}>sista frosten ❄︎</div>
            <div style={{ fontSize: 32, fontWeight: 700, letterSpacing: -1, color: VX.deep, lineHeight: 1 }}>~11<span style={{ fontSize: 12, color: VX.clay, marginLeft: 2 }}>d</span></div>
          </div>
          <div style={{ background: VX.butterLight, borderRadius: 18, padding: 14 }}>
            <div style={{ fontSize: 22, marginBottom: 2 }}>🌱</div>
            <div style={{ fontSize: 28, fontWeight: 700, letterSpacing: -1, color: VX.deep, lineHeight: 1 }}>47</div>
            <div style={{ fontSize: 10, color: VX.deep, opacity: 0.65, marginTop: 2 }}>i brickan · 6 redo</div>
          </div>
          <div style={{ background: VX.sageLight, borderRadius: 18, padding: 14 }}>
            <div style={{ fontFamily: '"Caveat", cursive', fontSize: 16, color: VX.sageDark, transform: 'rotate(-1deg)', display: 'inline-block' }}>plantor</div>
            <div style={{ fontSize: 32, fontWeight: 700, letterSpacing: -1, color: VX.deep, lineHeight: 1 }}>312</div>
            <div style={{ fontSize: 10, color: VX.sageDark, fontWeight: 600, marginTop: 2 }}>+24 v.17</div>
          </div>
          <div style={{ background: VX.paper, border: `1.5px solid ${VX.ink}10`, borderRadius: 18, padding: 14 }}>
            <div style={{ fontFamily: '"Caveat", cursive', fontSize: 16, color: VX.sageDark, transform: 'rotate(-1deg)', display: 'inline-block' }}>idag ✓</div>
            <div style={{ fontSize: 22, fontWeight: 700, letterSpacing: -0.6, color: VX.deep, lineHeight: 1, marginTop: 2 }}>3<span style={{ fontSize: 14, opacity: 0.5 }}>/5</span></div>
            <div style={{ height: 6, background: VX.sageLight, borderRadius: 3, marginTop: 8, overflow: 'hidden' }}>
              <div style={{ width: '60%', height: '100%', background: VX.sage }} />
            </div>
          </div>
        </div>

        {/* Gardens */}
        <div style={{
          fontFamily: '"Caveat", cursive', fontSize: 20,
          color: VX.clay, transform: 'rotate(-1deg)', display: 'inline-block',
        }}>dina hörn</div>
        <h2 style={{ margin: '0 0 12px', fontSize: 22, fontWeight: 700, letterSpacing: -0.6, fontFamily: '"Instrument Serif", serif' }}>Trädgårdar</h2>
        <div style={{ display: 'grid', gap: 10 }}>
          {[
            { n: 'Norra fältet',    bg: VX.sageLight,   blob: VX.sage,   emoji: '🌾', b: 6, p: 184 },
            { n: 'Södra växthuset', bg: VX.peachLight,  blob: VX.peach,  emoji: '🌷', b: 3, p: 82 },
            { n: 'Köksträdgården',  bg: VX.butterLight, blob: VX.butter, emoji: '🥬', b: 4, p: 46 },
          ].map((g, i) => (
            <div key={i} style={{
              background: g.bg, borderRadius: 18, padding: 14,
              display: 'grid', gridTemplateColumns: '48px 1fr auto',
              gap: 12, alignItems: 'center', position: 'relative', overflow: 'hidden',
            }}>
              <Blob color={`${g.blob}50`} size={80} variant={i}
                style={{ position: 'absolute', bottom: -30, right: -20 }} />
              <div style={{ fontSize: 28, position: 'relative' }}>{g.emoji}</div>
              <div style={{ position: 'relative' }}>
                <div style={{ fontWeight: 700, fontSize: 15, color: VX.deep, fontFamily: '"Instrument Serif", serif' }}>{g.n}</div>
                <div style={{ fontSize: 11, color: VX.deep, opacity: 0.65 }}>{g.b} bäddar · {g.p} plantor</div>
              </div>
              <div style={{ position: 'relative', fontSize: 20, color: VX.deep, opacity: 0.5 }}>›</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

window.VXDesktop = VXDesktop;
window.VXMobile = VXMobile;
