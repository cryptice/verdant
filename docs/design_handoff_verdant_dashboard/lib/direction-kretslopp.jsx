// Direction 2 — "Kretslopp" (Cycle)
// Data-ops. Near-black surfaces, saturated sage, Inter Tight, dense.
// Hero: seasonal arc timeline. Compact rows, numeric tables, monospace tags.

const KR = {
  bg:    '#12140F',
  panel: '#1A1D18',
  panel2:'#22261F',
  line:  '#2F3329',
  text:  '#E8EADD',
  dim:   '#8A9181',
  dimmer:'#5A6152',
  sage:  '#9BE07A',
  sage2: '#6FBF4C',
  clay:  '#FF8A5C',
  cream: '#F5EFE2',
};

function KrTag({ children, color = KR.sage }) {
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 5,
      fontFamily: 'ui-monospace, "JetBrains Mono", Menlo, monospace',
      fontSize: 9.5, letterSpacing: 0.5,
      color, padding: '2px 7px',
      border: `1px solid ${color}40`,
      background: `${color}12`,
      borderRadius: 3,
      textTransform: 'uppercase',
    }}>{children}</span>
  );
}

// Seasonal arc: an SVG showing the gardening year as a horizontal arc,
// with "today" marker, frost band, harvest curve, sowing marks.
function SeasonalArc({ h = 180 }) {
  const W = 720;
  const padX = 20;
  const arcY = h - 40;
  const ticks = ['Jan','Feb','Mar','Apr','Maj','Jun','Jul','Aug','Sep','Okt','Nov','Dec'];
  const todayX = padX + ((W - padX*2) * (3 + 20/30) / 12); // ~Apr 21
  const lastFrostX = padX + ((W - padX*2) * (4 + 2/30) / 12);

  // Harvest curve (approx bell around late summer)
  const pts = [];
  for (let i = 0; i <= 48; i++) {
    const m = i / 48 * 12;
    const x = padX + (W - padX*2) * m / 12;
    const y = arcY - Math.max(0, Math.exp(-Math.pow((m - 7.5) / 2.2, 2))) * (h - 80);
    pts.push(`${x},${y}`);
  }

  return (
    <svg viewBox={`0 0 ${W} ${h}`} style={{ width: '100%', height: h, display: 'block' }}>
      {/* Base line */}
      <line x1={padX} y1={arcY} x2={W - padX} y2={arcY} stroke={KR.line} strokeWidth="1" />
      {/* Month ticks */}
      {ticks.map((m, i) => {
        const x = padX + (W - padX*2) * i / 12;
        return (
          <g key={i}>
            <line x1={x} y1={arcY} x2={x} y2={arcY + 5} stroke={KR.dimmer} strokeWidth="0.5" />
            <text x={x} y={arcY + 16} fill={KR.dimmer} fontSize="9" fontFamily="ui-monospace, monospace" letterSpacing="0.5">{m}</text>
          </g>
        );
      })}
      {/* Frost band (before last frost) */}
      <rect x={padX} y={16} width={lastFrostX - padX} height={arcY - 16} fill="#4A78FF" fillOpacity="0.07" />
      <line x1={lastFrostX} y1={16} x2={lastFrostX} y2={arcY} stroke="#6FA0FF" strokeOpacity="0.4" strokeWidth="1" strokeDasharray="2 3" />
      <text x={lastFrostX + 6} y={24} fill="#6FA0FF" fontSize="9" fontFamily="ui-monospace, monospace" letterSpacing="0.5">LAST FROST</text>

      {/* Harvest curve area */}
      <polyline
        points={pts.join(' ')}
        fill="none"
        stroke={KR.sage}
        strokeWidth="1.5"
      />
      <polygon
        points={`${padX},${arcY} ${pts.join(' ')} ${W-padX},${arcY}`}
        fill={KR.sage}
        fillOpacity="0.12"
      />

      {/* Sowing ticks on baseline */}
      {[1.2, 2.5, 3.1, 3.7, 4.2].map((m, i) => {
        const x = padX + (W - padX*2) * m / 12;
        return <circle key={i} cx={x} cy={arcY} r="3" fill={KR.clay} />;
      })}

      {/* Today marker */}
      <line x1={todayX} y1={10} x2={todayX} y2={arcY + 8} stroke={KR.sage} strokeWidth="1.5" />
      <circle cx={todayX} cy={10} r="4" fill={KR.sage} />
      <text x={todayX + 10} y={14} fill={KR.sage} fontSize="10" fontFamily="ui-monospace, monospace" letterSpacing="1" fontWeight="600">IDAG · V.17</text>
    </svg>
  );
}

// Mini sparkline
function MiniSpark({ data, color = KR.sage, w = 100, h = 28 }) {
  const max = Math.max(...data);
  const min = Math.min(...data);
  const pts = data.map((v, i) => {
    const x = (i / (data.length - 1)) * w;
    const y = h - ((v - min) / (max - min || 1)) * (h - 2) - 1;
    return `${x},${y}`;
  }).join(' ');
  return (
    <svg width={w} height={h} style={{ display: 'block' }}>
      <polyline points={pts} fill="none" stroke={color} strokeWidth="1.3" />
    </svg>
  );
}

function KretsloppDesktop() {
  return (
    <div style={{
      width: '100%', height: '100%',
      background: KR.bg, color: KR.text,
      fontFamily: '"Inter", -apple-system, sans-serif',
      display: 'grid', gridTemplateColumns: '60px 1fr',
      overflow: 'hidden',
      fontFeatureSettings: '"cv11", "ss01", "tnum"',
    }}>
      {/* Thin icon rail */}
      <aside style={{
        borderRight: `1px solid ${KR.line}`, padding: '16px 0',
        display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4,
      }}>
        <div style={{
          width: 32, height: 32, background: KR.sage, color: KR.bg,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontFamily: 'ui-monospace, monospace', fontWeight: 700, fontSize: 14,
          borderRadius: 4, marginBottom: 12,
        }}>V</div>
        {['◉','▦','◰','✿','⎇','✓','⌬','∿','⚙'].map((s, i) => (
          <div key={i} style={{
            width: 36, height: 36,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: i === 0 ? KR.sage : KR.dim,
            background: i === 0 ? KR.panel2 : 'transparent',
            borderRadius: 4, fontSize: 16,
            borderLeft: i === 0 ? `2px solid ${KR.sage}` : '2px solid transparent',
          }}>{s}</div>
        ))}
      </aside>

      <main style={{ display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        {/* Top bar */}
        <div style={{
          height: 44, borderBottom: `1px solid ${KR.line}`,
          display: 'flex', alignItems: 'center', padding: '0 20px', gap: 16,
        }}>
          <div style={{
            fontFamily: 'ui-monospace, monospace', fontSize: 10.5,
            color: KR.dim, letterSpacing: 0.8,
          }}>
            <span style={{ color: KR.sage }}>l2c_farm</span>
            <span style={{ margin: '0 6px', opacity: 0.4 }}>/</span>
            <span>dashboard</span>
          </div>
          <div style={{ flex: 1 }} />
          <div style={{
            display: 'flex', alignItems: 'center', gap: 6,
            padding: '4px 10px', border: `1px solid ${KR.line}`,
            borderRadius: 4, fontFamily: 'ui-monospace, monospace',
            fontSize: 10.5, color: KR.dim,
          }}>
            <span style={{ color: KR.sage }}>●</span>
            SYNC · 2s
            <span style={{ opacity: 0.5, marginLeft: 8 }}>⌘K</span>
          </div>
          <div style={{
            width: 24, height: 24, borderRadius: 12,
            background: KR.panel2, color: KR.sage, fontSize: 10,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontFamily: 'ui-monospace, monospace',
          }}>EL</div>
        </div>

        <div style={{ flex: 1, overflowY: 'auto', padding: 20 }}>
          {/* Header */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 14 }}>
            <div>
              <div style={{
                fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1.4,
                color: KR.dim, textTransform: 'uppercase', marginBottom: 4,
              }}>21.04.2026 · V.17 · FRE</div>
              <h1 style={{
                margin: 0, fontSize: 26, fontWeight: 600, letterSpacing: -0.6,
                fontFamily: '"Inter", sans-serif',
              }}>
                Hej Erik. <span style={{ color: KR.dim, fontWeight: 400 }}>3 uppgifter väntar.</span>
              </h1>
            </div>
            <div style={{ display: 'flex', gap: 8 }}>
              <button style={{
                padding: '7px 12px', background: 'transparent',
                border: `1px solid ${KR.line}`, color: KR.dim,
                fontSize: 12, borderRadius: 4, fontFamily: 'inherit', cursor: 'pointer',
              }}>Exportera</button>
              <button style={{
                padding: '7px 14px', background: KR.sage, color: KR.bg,
                border: 'none', fontSize: 12, fontWeight: 600,
                borderRadius: 4, fontFamily: 'inherit', cursor: 'pointer',
              }}>+ Sådd</button>
            </div>
          </div>

          {/* Hero: seasonal arc */}
          <div style={{
            background: KR.panel, border: `1px solid ${KR.line}`,
            borderRadius: 6, padding: 18, marginBottom: 16,
          }}>
            <div style={{
              display: 'flex', justifyContent: 'space-between',
              alignItems: 'center', marginBottom: 8,
            }}>
              <div style={{ display: 'flex', gap: 10, alignItems: 'baseline' }}>
                <span style={{ fontSize: 13, fontWeight: 600 }}>Säsongsbåge 2026</span>
                <span style={{
                  fontFamily: 'ui-monospace, monospace', fontSize: 10,
                  color: KR.dim, letterSpacing: 0.8,
                }}>
                  DAG 111 / 365 · 30.4% · <span style={{ color: KR.sage }}>ACTIVE</span>
                </span>
              </div>
              <div style={{ display: 'flex', gap: 8, fontFamily: 'ui-monospace, monospace', fontSize: 10, color: KR.dim }}>
                <span><span style={{ color: KR.sage, marginRight: 4 }}>━</span>SKÖRD</span>
                <span><span style={{ color: KR.clay, marginRight: 4 }}>●</span>SÅDDER</span>
                <span><span style={{ color: '#6FA0FF', marginRight: 4 }}>▨</span>FROST</span>
              </div>
            </div>
            <SeasonalArc h={160} />
          </div>

          {/* Stat tiles */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 10, marginBottom: 16 }}>
            {[
              { l: 'PLANTOR AKTIVA', v: '312', d: '+24', spark: [2,3,3,4,5,6,7,8,10,12,14], c: KR.sage },
              { l: 'SKÖRD (APR)',   v: '8.4', u: 'kg', d: '+18%', spark: [1,2,2,3,4,5,6,8,8,9,10], c: KR.sage },
              { l: 'I BRICKA',      v: '47',  d: '−3 idag', spark: [8,7,7,6,6,6,5,5,4,4,3], c: KR.clay },
              { l: 'ARTER ODLADE',  v: '23',  d: '3 nya', spark: [1,1,2,2,2,3,3,4,5,5,5], c: KR.sage },
            ].map((s, i) => (
              <div key={i} style={{
                background: KR.panel, border: `1px solid ${KR.line}`,
                borderRadius: 6, padding: 14,
              }}>
                <div style={{
                  fontFamily: 'ui-monospace, monospace', fontSize: 9.5,
                  color: KR.dim, letterSpacing: 1.2, marginBottom: 8,
                }}>{s.l}</div>
                <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
                  <div style={{
                    fontSize: 32, fontWeight: 600, letterSpacing: -0.8,
                    fontFamily: '"Inter", sans-serif', color: KR.text,
                  }}>
                    {s.v}{s.u && <span style={{ fontSize: 14, color: KR.dim, marginLeft: 3 }}>{s.u}</span>}
                  </div>
                  <MiniSpark data={s.spark} color={s.c} w={60} h={24} />
                </div>
                <div style={{
                  fontFamily: 'ui-monospace, monospace', fontSize: 10,
                  color: s.c, letterSpacing: 0.5, marginTop: 4,
                }}>{s.d}</div>
              </div>
            ))}
          </div>

          {/* Two-column body */}
          <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr', gap: 16 }}>
            {/* Table: Trädgårdar */}
            <div style={{
              background: KR.panel, border: `1px solid ${KR.line}`,
              borderRadius: 6, overflow: 'hidden',
            }}>
              <div style={{
                padding: '12px 16px', display: 'flex',
                justifyContent: 'space-between', alignItems: 'center',
                borderBottom: `1px solid ${KR.line}`,
              }}>
                <div style={{ fontSize: 13, fontWeight: 600 }}>Trädgårdar</div>
                <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 10, color: KR.dim, letterSpacing: 0.8 }}>3 objekt · sortera ↓</div>
              </div>
              <table style={{
                width: '100%', borderCollapse: 'collapse',
                fontFamily: '"Inter", sans-serif', fontSize: 12,
              }}>
                <thead>
                  <tr style={{
                    fontFamily: 'ui-monospace, monospace', fontSize: 9.5,
                    color: KR.dim, letterSpacing: 1.2, textAlign: 'left',
                  }}>
                    <th style={{ padding: '8px 16px', fontWeight: 400 }}>NAMN</th>
                    <th style={{ padding: '8px 8px', fontWeight: 400 }}>BÄDDAR</th>
                    <th style={{ padding: '8px 8px', fontWeight: 400 }}>PLANTOR</th>
                    <th style={{ padding: '8px 8px', fontWeight: 400 }}>BELÄGG</th>
                    <th style={{ padding: '8px 16px', fontWeight: 400 }}>STATUS</th>
                  </tr>
                </thead>
                <tbody>
                  {[
                    { n: 'Norra fältet',       b: 6, p: 184, occ: 82, s: 'aktiv',    col: KR.sage },
                    { n: 'Södra växthuset',    b: 3, p: 82,  occ: 94, s: 'groning',  col: KR.clay },
                    { n: 'Köksträdgården',     b: 4, p: 46,  occ: 31, s: 'vilar',    col: KR.dim },
                  ].map((g, i) => (
                    <tr key={i} style={{ borderTop: `1px solid ${KR.line}` }}>
                      <td style={{ padding: '14px 16px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                          <div style={{
                            width: 6, height: 24, background: g.col, borderRadius: 1,
                          }} />
                          <div>
                            <div style={{ fontWeight: 500, color: KR.text }}>{g.n}</div>
                            <div style={{
                              fontFamily: 'ui-monospace, monospace', fontSize: 9.5,
                              color: KR.dimmer, letterSpacing: 0.5,
                            }}>GDN_{String(i+1).padStart(3,'0')}</div>
                          </div>
                        </div>
                      </td>
                      <td style={{ padding: '14px 8px', fontFamily: 'ui-monospace, monospace', fontVariantNumeric: 'tabular-nums', color: KR.text }}>{g.b}</td>
                      <td style={{ padding: '14px 8px', fontFamily: 'ui-monospace, monospace', fontVariantNumeric: 'tabular-nums', color: KR.text }}>{g.p}</td>
                      <td style={{ padding: '14px 8px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                          <div style={{
                            width: 40, height: 4, background: KR.line, borderRadius: 2, overflow: 'hidden',
                          }}>
                            <div style={{ width: `${g.occ}%`, height: '100%', background: g.col }} />
                          </div>
                          <span style={{ fontFamily: 'ui-monospace, monospace', fontSize: 10, color: KR.dim }}>{g.occ}%</span>
                        </div>
                      </td>
                      <td style={{ padding: '14px 16px' }}><KrTag color={g.col}>{g.s}</KrTag></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Tasks */}
            <div style={{
              background: KR.panel, border: `1px solid ${KR.line}`,
              borderRadius: 6, overflow: 'hidden',
            }}>
              <div style={{
                padding: '12px 16px', borderBottom: `1px solid ${KR.line}`,
                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
              }}>
                <div style={{ fontSize: 13, fontWeight: 600 }}>Idag · 3/5</div>
                <div style={{
                  fontFamily: 'ui-monospace, monospace', fontSize: 10,
                  color: KR.sage, letterSpacing: 0.8,
                }}>60% KLART</div>
              </div>
              {[
                { t: 'Omplantera Zinnia → 9cm', tag: 'PLANT', done: true },
                { t: 'Vattna Norra bädd 3–6', tag: 'CARE', done: true },
                { t: 'Så Bupleurum bädd 4', tag: 'SOW', done: true },
                { t: 'Skörda Snapdragon 12st', tag: 'HARV', done: false },
                { t: 'Foto varietetsförsök', tag: 'TEST', done: false },
              ].map((task, i) => (
                <div key={i} style={{
                  padding: '10px 16px',
                  borderTop: `1px solid ${KR.line}`,
                  display: 'flex', alignItems: 'center', gap: 10,
                  opacity: task.done ? 0.5 : 1,
                }}>
                  <div style={{
                    width: 14, height: 14, borderRadius: 3,
                    border: `1.5px solid ${task.done ? KR.sage : KR.line}`,
                    background: task.done ? KR.sage : 'transparent',
                    color: KR.bg, fontSize: 10, lineHeight: '11px',
                    display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800,
                  }}>{task.done ? '✓' : ''}</div>
                  <div style={{
                    flex: 1, fontSize: 12,
                    textDecoration: task.done ? 'line-through' : 'none',
                    color: task.done ? KR.dim : KR.text,
                  }}>{task.t}</div>
                  <KrTag color={task.tag === 'HARV' ? KR.clay : KR.sage}>{task.tag}</KrTag>
                </div>
              ))}
            </div>
          </div>

          {/* Bottom: Bricka rows */}
          <div style={{
            marginTop: 16,
            background: KR.panel, border: `1px solid ${KR.line}`,
            borderRadius: 6, overflow: 'hidden',
          }}>
            <div style={{
              padding: '12px 16px', borderBottom: `1px solid ${KR.line}`,
              display: 'flex', justifyContent: 'space-between',
            }}>
              <div style={{ fontSize: 13, fontWeight: 600 }}>Plantor i bricka · 47</div>
              <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 10, color: KR.dim, letterSpacing: 0.8 }}>
                ⇆ redo för omplantering: <span style={{ color: KR.sage }}>6</span>
              </div>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)' }}>
              {[
                { n: 'Zinnia elegans',        sv: 'Zinnia',       c: 18, s: 'HJBLAD', d: 9,  ready: true  },
                { n: 'Cosmos bipinnatus',     sv: 'Rosenskära',   c: 12, s: 'HJBLAD', d: 7,  ready: true  },
                { n: 'Antirrhinum majus',     sv: 'Lejongap',     c: 9,  s: 'GROD',   d: 5,  ready: false },
                { n: 'Nigella damascena',     sv: 'Jungfrun',     c: 8,  s: 'GROD',   d: 4,  ready: false },
              ].map((p, i) => (
                <div key={i} style={{
                  padding: 14,
                  borderLeft: i > 0 ? `1px solid ${KR.line}` : 'none',
                }}>
                  <div style={{
                    fontFamily: 'ui-monospace, monospace', fontSize: 9,
                    color: KR.dimmer, letterSpacing: 0.8, marginBottom: 4,
                  }}>{p.n.toUpperCase()}</div>
                  <div style={{ fontSize: 13, fontWeight: 500, marginBottom: 8 }}>{p.sv}</div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                    <div style={{ fontSize: 26, fontWeight: 600, letterSpacing: -0.6 }}>{p.c}</div>
                    <KrTag color={p.ready ? KR.sage : KR.dim}>{p.s} · {p.d}d</KrTag>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

function KretsloppMobile() {
  return (
    <div style={{
      width: '100%', height: '100%', overflowY: 'auto',
      background: KR.bg, color: KR.text,
      fontFamily: '"Inter", sans-serif',
    }}>
      <div style={{
        height: 48, borderBottom: `1px solid ${KR.line}`,
        display: 'flex', alignItems: 'center', padding: '0 16px', gap: 12,
      }}>
        <div style={{
          width: 28, height: 28, background: KR.sage, color: KR.bg,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontFamily: 'ui-monospace, monospace', fontWeight: 700, fontSize: 13,
          borderRadius: 4,
        }}>V</div>
        <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 10.5, color: KR.dim, letterSpacing: 0.8, flex: 1 }}>
          <span style={{ color: KR.sage }}>l2c_farm</span> <span style={{ opacity: 0.4 }}>/</span> dashboard
        </div>
        <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 10, color: KR.sage }}>●2s</div>
      </div>

      <div style={{ padding: 16 }}>
        <div style={{
          fontFamily: 'ui-monospace, monospace', fontSize: 9.5, letterSpacing: 1.2,
          color: KR.dim, marginBottom: 4,
        }}>21.04.2026 · V.17</div>
        <h1 style={{ margin: 0, fontSize: 20, fontWeight: 600, letterSpacing: -0.4, marginBottom: 14 }}>
          Hej Erik. <span style={{ color: KR.dim, fontWeight: 400 }}>3 väntar.</span>
        </h1>

        <div style={{
          background: KR.panel, border: `1px solid ${KR.line}`,
          borderRadius: 6, padding: 12, marginBottom: 12,
        }}>
          <div style={{
            fontFamily: 'ui-monospace, monospace', fontSize: 9.5,
            color: KR.dim, letterSpacing: 0.8, marginBottom: 4,
          }}>DAG 111 / 365 · 30.4%</div>
          <SeasonalArc h={120} />
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 12 }}>
          {[
            { l: 'PLANTOR',  v: '312', d: '+24', c: KR.sage, sp: [2,3,4,5,6,7,8,10,12,14] },
            { l: 'SKÖRD KG', v: '8.4', d: '+18%', c: KR.sage, sp: [1,2,3,4,5,6,8,8,9,10] },
            { l: 'BRICKA',   v: '47',  d: '−3',  c: KR.clay, sp: [8,7,6,6,5,5,4,4,3,3] },
            { l: 'ARTER',    v: '23',  d: '3 nya', c: KR.sage, sp: [1,2,2,3,4,5,5,5,5,5] },
          ].map((s, i) => (
            <div key={i} style={{ background: KR.panel, border: `1px solid ${KR.line}`, borderRadius: 6, padding: 12 }}>
              <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1, color: KR.dim, marginBottom: 6 }}>{s.l}</div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                <div style={{ fontSize: 22, fontWeight: 600, letterSpacing: -0.6 }}>{s.v}</div>
                <MiniSpark data={s.sp} color={s.c} w={40} h={18} />
              </div>
              <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9.5, color: s.c, marginTop: 2 }}>{s.d}</div>
            </div>
          ))}
        </div>

        <div style={{ background: KR.panel, border: `1px solid ${KR.line}`, borderRadius: 6, overflow: 'hidden', marginBottom: 12 }}>
          <div style={{ padding: '10px 14px', borderBottom: `1px solid ${KR.line}`, fontSize: 12, fontWeight: 600 }}>Idag · 3/5 · <span style={{ color: KR.sage, fontFamily: 'ui-monospace, monospace', fontSize: 10 }}>60%</span></div>
          {[
            { t: 'Omplantera Zinnia → 9cm', tag: 'PLANT', done: true },
            { t: 'Vattna Norra bädd 3–6', tag: 'CARE', done: true },
            { t: 'Skörda Snapdragon 12st', tag: 'HARV', done: false },
            { t: 'Foto varietetsförsök', tag: 'TEST', done: false },
          ].map((task, i) => (
            <div key={i} style={{
              padding: '10px 14px', borderTop: i > 0 ? `1px solid ${KR.line}` : 'none',
              display: 'flex', alignItems: 'center', gap: 8, opacity: task.done ? 0.5 : 1,
            }}>
              <div style={{
                width: 13, height: 13, borderRadius: 3,
                border: `1.5px solid ${task.done ? KR.sage : KR.line}`,
                background: task.done ? KR.sage : 'transparent',
                color: KR.bg, fontSize: 9, display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>{task.done ? '✓' : ''}</div>
              <div style={{ flex: 1, fontSize: 11.5, textDecoration: task.done ? 'line-through' : 'none' }}>{task.t}</div>
              <KrTag color={task.tag === 'HARV' ? KR.clay : KR.sage}>{task.tag}</KrTag>
            </div>
          ))}
        </div>

        <div style={{ background: KR.panel, border: `1px solid ${KR.line}`, borderRadius: 6, overflow: 'hidden' }}>
          <div style={{ padding: '10px 14px', borderBottom: `1px solid ${KR.line}`, fontSize: 12, fontWeight: 600 }}>Trädgårdar · 3</div>
          {[
            { n: 'Norra fältet', b: 6, p: 184, col: KR.sage },
            { n: 'Södra växthuset', b: 3, p: 82, col: KR.clay },
            { n: 'Köksträdgården', b: 4, p: 46, col: KR.dim },
          ].map((g, i) => (
            <div key={i} style={{
              padding: '12px 14px', borderTop: i > 0 ? `1px solid ${KR.line}` : 'none',
              display: 'flex', alignItems: 'center', gap: 10,
            }}>
              <div style={{ width: 4, height: 28, background: g.col, borderRadius: 1 }} />
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 12.5, fontWeight: 500 }}>{g.n}</div>
                <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9.5, color: KR.dim, letterSpacing: 0.6 }}>{g.b}B · {g.p}P</div>
              </div>
              <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 10, color: KR.dim }}>›</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

window.KretsloppDesktop = KretsloppDesktop;
window.KretsloppMobile = KretsloppMobile;
