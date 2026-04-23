// Fältet — Task list screen. Editorial ledger layout.

function FalTaskChip({ children, tone = 'forest' }) {
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 6,
      fontFamily: 'ui-monospace, "SF Mono", Menlo, monospace',
      fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase',
      color: FAL[tone],
      padding: '4px 8px',
      border: `1px solid ${FAL[tone]}`,
      borderRadius: 999,
    }}>{children}</span>
  );
}

function FalRule2({ color = '#1E241D', style }) {
  return <div style={{ height: 1, background: color, ...style }} />;
}

const TASKS = [
  { n: 1,  act: 'Skörda',       icon: '✂', sp: 'Narcissus poeticus',    var: "'Actaea'",          group: 'Vårlökar',       bed: 'Norra fältet · B2', due: 'Idag',       when: 'overdue', remain: '12/40',  tone: 'clay'    },
  { n: 2,  act: 'Omplantera',   icon: '🪴', sp: 'Zinnia elegans',        var: "'Queen Lime'",      group: null,             bed: 'Bricka · 72',       due: 'Idag',       when: 'today',   remain: '18/18',  tone: 'sage'    },
  { n: 3,  act: 'Så',           icon: '🌰', sp: 'Bupleurum rotundifolium', var: null,            group: 'Fyllnadsblommor', bed: 'Norra fältet · B4', due: 'Idag',       when: 'today',   remain: '0/200',  tone: 'mustard' },
  { n: 4,  act: 'Vattna',       icon: '💧', sp: null,                    var: null,              group: null,             bed: 'Norra fältet · B3–6', due: 'Imorgon',   when: 'soon',    remain: null,     tone: 'sky'     },
  { n: 5,  act: 'Plantera ut',  icon: '🌳', sp: 'Cosmos bipinnatus',     var: "'Xanthos'",         group: null,             bed: 'Södra fältet · B1', due: '24 apr',     when: 'soon',    remain: '0/48',   tone: 'berry'   },
  { n: 6,  act: 'Skörda',       icon: '✂', sp: 'Tulipa',                 var: "'La Belle Époque'", group: 'Tulpaner',       bed: 'Södra växthuset',  due: '25 apr',     when: 'soon',    remain: '0/60',   tone: 'clay'    },
  { n: 7,  act: 'Så',           icon: '🌰', sp: 'Antirrhinum majus',     var: "'Chantilly Mix'",   group: null,             bed: 'Bricka · 128',      due: '26 apr',     when: 'soon',    remain: '0/128',  tone: 'mustard' },
  { n: 8,  act: 'Gödsla',       icon: '🧪', sp: 'Dahlia',                 var: 'alla sorter',      group: 'Dahlior',        bed: 'Norra fältet',      due: '28 apr',     when: 'soon',    remain: null,     tone: 'berry'   },
];

function FaltetTasksDesktop() {
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
          ['Trädgårdar', false],
          ['Säsonger', false],
          ['Arter', false],
          ['Plantor', false],
          ['Uppgifter', true],
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
        {/* Masthead */}
        <div style={{
          padding: '20px 40px',
          borderBottom: `1px solid ${FAL.ink}`,
          display: 'flex', justifyContent: 'space-between', alignItems: 'baseline',
          fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1.8,
          textTransform: 'uppercase', color: FAL.forest,
        }}>
          <span>V.17 · Fredag 21 April 2026</span>
          <span style={{ fontStyle: 'italic', fontFamily: '"Fraunces", serif', textTransform: 'none', letterSpacing: 0, fontSize: 13 }}>— Uppgiftsliggaren —</span>
          <span>8 öppna · 3 idag</span>
        </div>

        <div style={{ padding: '40px 40px' }}>
          {/* Hero header */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr auto', gap: 40, alignItems: 'end', marginBottom: 28 }}>
            <div>
              <div style={{ marginBottom: 14, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                <FalTaskChip>Uppgifter</FalTaskChip>
                <FalTaskChip tone="clay">1 försenad</FalTaskChip>
                <FalTaskChip tone="mustard">3 idag</FalTaskChip>
                <FalTaskChip tone="sky">4 kommande</FalTaskChip>
              </div>
              <h1 style={{
                margin: 0,
                fontFamily: '"Fraunces", Georgia, serif',
                fontSize: 84, lineHeight: 1.0, letterSpacing: -1.2, fontWeight: 300,
                fontVariationSettings: '"SOFT" 100, "opsz" 144',
              }}>
                Veckans<br/>
                <span style={{ fontStyle: 'italic', color: FAL.clay }}>göromål.</span>
              </h1>
            </div>
            <div style={{ textAlign: 'right', display: 'flex', gap: 18 }}>
              {[
                { v: '3', l: 'Idag', hue: 'clay' },
                { v: '8', l: 'Öppna', hue: 'mustard' },
                { v: '37', l: 'V.17 klara', hue: 'sage' },
              ].map((s, i) => (
                <div key={i} style={{ textAlign: 'left' }}>
                  <div style={{
                    fontFamily: '"Fraunces", serif', fontSize: 56, lineHeight: 0.95,
                    fontWeight: 300, letterSpacing: -0.4, color: FAL[s.hue],
                  }}>{s.v}</div>
                  <div style={{
                    fontFamily: 'ui-monospace, monospace', fontSize: 10,
                    letterSpacing: 1.4, textTransform: 'uppercase', color: FAL.forest, marginTop: 4,
                  }}>{s.l}</div>
                </div>
              ))}
            </div>
          </div>

          {/* Filter row */}
          <div style={{
            display: 'flex', gap: 8, marginBottom: 22, alignItems: 'center',
            paddingBottom: 16, borderBottom: `1px solid ${FAL.ink}`,
          }}>
            <span style={{ fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1.6, textTransform: 'uppercase', color: FAL.forest, opacity: 0.6, marginRight: 6 }}>Filtrera</span>
            {[
              ['Alla', 'forest', true],
              ['Försenade', 'clay'],
              ['Idag', 'mustard'],
              ['Veckan', 'sky'],
              ['Sådd', 'mustard'],
              ['Plantering', 'sage'],
              ['Skörd', 'clay'],
              ['Vård', 'sky'],
            ].map(([label, tone, active], i) => (
              <span key={i} style={{
                fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1.4,
                textTransform: 'uppercase',
                padding: '5px 10px',
                borderRadius: 999,
                border: `1px solid ${FAL[tone]}`,
                background: active ? FAL[tone] : 'transparent',
                color: active ? FAL.cream : FAL[tone],
              }}>{label}</span>
            ))}
            <div style={{ flex: 1 }} />
            <span style={{ fontFamily: '"Fraunces", serif', fontStyle: 'italic', fontSize: 14, color: FAL.forest }}>+ Ny uppgift</span>
          </div>

          {/* Today section */}
          <div style={{ marginBottom: 36 }}>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 14, marginBottom: 12 }}>
              <h2 style={{
                margin: 0, fontFamily: '"Fraunces", serif', fontSize: 30,
                fontStyle: 'italic', fontWeight: 400, letterSpacing: -0.3,
              }}>Idag<span style={{ color: FAL.clay }}>.</span></h2>
              <span style={{ fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1.6, textTransform: 'uppercase', color: FAL.forest, opacity: 0.6 }}>
                21 april · 3 st
              </span>
              <div style={{ flex: 1, height: 1, background: FAL.ink }} />
            </div>

            {TASKS.filter(t => t.when === 'overdue' || t.when === 'today').map((task, i) => (
              <TaskRow key={i} task={task} />
            ))}
          </div>

          {/* Upcoming */}
          <div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 14, marginBottom: 12 }}>
              <h2 style={{
                margin: 0, fontFamily: '"Fraunces", serif', fontSize: 30,
                fontStyle: 'italic', fontWeight: 400, letterSpacing: -0.3,
              }}>Kommande<span style={{ color: FAL.sky }}>.</span></h2>
              <span style={{ fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1.6, textTransform: 'uppercase', color: FAL.forest, opacity: 0.6 }}>
                22–30 april · 5 st
              </span>
              <div style={{ flex: 1, height: 1, background: FAL.ink }} />
            </div>

            {TASKS.filter(t => t.when === 'soon').map((task, i) => (
              <TaskRow key={i} task={task} />
            ))}
          </div>
        </div>
      </main>
    </div>
  );
}

function TaskRow({ task }) {
  const colour = FAL[task.tone];
  return (
    <div style={{
      display: 'grid',
      gridTemplateColumns: '48px 1fr 260px 150px 40px',
      gap: 20, alignItems: 'center',
      padding: '18px 0',
      borderBottom: `1px solid ${FAL.ink}25`,
    }}>
      {/* Number */}
      <div style={{
        fontFamily: '"Fraunces", serif', fontStyle: 'italic',
        fontSize: 26, fontWeight: 300, color: colour,
        lineHeight: 1, letterSpacing: -0.4,
      }}>№{String(task.n).padStart(2, '0')}</div>

      {/* Activity + species */}
      <div>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 10, marginBottom: 3 }}>
          <span style={{
            fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1.6,
            textTransform: 'uppercase', color: colour,
          }}>{task.act}</span>
          {task.group && (
            <span style={{
              fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.2,
              textTransform: 'uppercase', color: FAL.forest, opacity: 0.7,
              padding: '2px 7px', borderRadius: 999,
              border: `1px solid ${FAL.forest}40`,
            }}>grupp · {task.group}</span>
          )}
        </div>
        <div style={{
          fontFamily: '"Fraunces", serif', fontSize: 22, fontWeight: 400,
          letterSpacing: -0.3, lineHeight: 1.1,
        }}>
          {task.sp ? task.sp : task.bed.split(' · ')[0]}
          {task.var && <span style={{ fontStyle: 'italic', color: colour }}> {task.var}</span>}
        </div>
      </div>

      {/* Location */}
      <div style={{
        fontFamily: '"Fraunces", serif', fontSize: 14, fontStyle: 'italic',
        color: FAL.forest, display: 'flex', alignItems: 'center', gap: 8,
      }}>
        <span style={{ width: 6, height: 6, borderRadius: 999, background: colour }} />
        {task.bed}
        {task.remain && (
          <span style={{
            fontFamily: 'ui-monospace, monospace', fontSize: 10, letterSpacing: 1.2,
            textTransform: 'uppercase', color: FAL.forest, opacity: 0.6, marginLeft: 4,
            fontStyle: 'normal',
          }}>{task.remain}</span>
        )}
      </div>

      {/* Due */}
      <div style={{ textAlign: 'right' }}>
        <div style={{
          fontFamily: '"Fraunces", serif', fontSize: 20, fontStyle: 'italic',
          color: task.when === 'overdue' ? FAL.clay : task.when === 'today' ? FAL.ink : FAL.forest,
          fontWeight: 400, letterSpacing: -0.2,
        }}>{task.due}</div>
        <div style={{
          fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.6,
          textTransform: 'uppercase',
          color: task.when === 'overdue' ? FAL.clay : FAL.forest,
          opacity: task.when === 'overdue' ? 1 : 0.6, marginTop: 2,
        }}>
          {task.when === 'overdue' ? '▲ försenad' : task.when === 'today' ? 'deadline idag' : 'planerad'}
        </div>
      </div>

      {/* Check */}
      <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
        <div style={{
          width: 22, height: 22, border: `1.5px solid ${FAL.ink}`, borderRadius: 0,
          background: 'transparent',
        }} />
      </div>
    </div>
  );
}

// ─────────── MOBILE ───────────
function FaltetTasksMobile() {
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
        <div style={{ fontFamily: '"Fraunces", serif', fontStyle: 'italic', fontSize: 22, letterSpacing: -0.4 }}>
          Verdant<span style={{ color: FAL.clay }}>.</span>
        </div>
        <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: FAL.forest }}>
          V.17 · ≡
        </div>
      </div>

      <div style={{ padding: '24px 18px 80px' }}>
        <div style={{ display: 'flex', gap: 6, marginBottom: 12, flexWrap: 'wrap' }}>
          <FalTaskChip>Uppgifter</FalTaskChip>
          <FalTaskChip tone="clay">1 försenad</FalTaskChip>
          <FalTaskChip tone="mustard">3 idag</FalTaskChip>
        </div>
        <h1 style={{
          margin: 0, fontFamily: '"Fraunces", serif',
          fontSize: 48, lineHeight: 1.0, letterSpacing: -0.4, fontWeight: 300,
        }}>
          Veckans<br/>
          <span style={{ fontStyle: 'italic', color: FAL.clay }}>göromål.</span>
        </h1>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12, marginTop: 22, marginBottom: 22 }}>
          {[
            { v: '3', l: 'Idag', hue: 'clay' },
            { v: '8', l: 'Öppna', hue: 'mustard' },
            { v: '37', l: 'Klara', hue: 'sage' },
          ].map((s, i) => (
            <div key={i}>
              <div style={{ fontFamily: '"Fraunces", serif', fontSize: 34, lineHeight: 1.0, fontWeight: 300, letterSpacing: -0.4, color: FAL[s.hue] }}>{s.v}</div>
              <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: FAL.forest, marginTop: 3 }}>{s.l}</div>
            </div>
          ))}
        </div>

        <div style={{ display: 'flex', gap: 6, overflowX: 'auto', marginBottom: 18, paddingBottom: 4 }}>
          {[
            ['Alla', 'forest', true],
            ['Försenade', 'clay'],
            ['Idag', 'mustard'],
            ['Sådd', 'mustard'],
            ['Skörd', 'clay'],
            ['Vård', 'sky'],
          ].map(([label, tone, active], i) => (
            <span key={i} style={{
              fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.2,
              textTransform: 'uppercase',
              padding: '4px 9px', borderRadius: 999,
              border: `1px solid ${FAL[tone]}`,
              background: active ? FAL[tone] : 'transparent',
              color: active ? FAL.cream : FAL[tone],
              whiteSpace: 'nowrap',
            }}>{label}</span>
          ))}
        </div>

        {/* Today */}
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 10, marginBottom: 8 }}>
          <h2 style={{ margin: 0, fontFamily: '"Fraunces", serif', fontSize: 22, fontStyle: 'italic', fontWeight: 400 }}>Idag<span style={{ color: FAL.clay }}>.</span></h2>
          <div style={{ flex: 1, height: 1, background: FAL.ink }} />
        </div>

        {TASKS.filter(t => t.when !== 'soon').map((task, i) => (
          <MobileTaskRow key={i} task={task} />
        ))}

        <div style={{ display: 'flex', alignItems: 'baseline', gap: 10, marginTop: 20, marginBottom: 8 }}>
          <h2 style={{ margin: 0, fontFamily: '"Fraunces", serif', fontSize: 22, fontStyle: 'italic', fontWeight: 400 }}>Kommande<span style={{ color: FAL.sky }}>.</span></h2>
          <div style={{ flex: 1, height: 1, background: FAL.ink }} />
        </div>

        {TASKS.filter(t => t.when === 'soon').slice(0, 4).map((task, i) => (
          <MobileTaskRow key={i} task={task} />
        ))}
      </div>
    </div>
  );
}

function MobileTaskRow({ task }) {
  const colour = FAL[task.tone];
  return (
    <div style={{
      padding: '12px 0', borderBottom: `1px solid ${FAL.ink}25`,
      display: 'grid', gridTemplateColumns: '36px 1fr auto', gap: 10, alignItems: 'start',
    }}>
      <div style={{
        fontFamily: '"Fraunces", serif', fontStyle: 'italic',
        fontSize: 18, fontWeight: 300, color: colour, lineHeight: 1, letterSpacing: -0.3,
      }}>№{String(task.n).padStart(2, '0')}</div>
      <div>
        <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: colour, marginBottom: 2 }}>
          {task.act}
        </div>
        <div style={{ fontFamily: '"Fraunces", serif', fontSize: 16, letterSpacing: -0.2, lineHeight: 1.15 }}>
          {task.sp || task.bed.split(' · ')[0]}
          {task.var && <span style={{ fontStyle: 'italic', color: colour }}> {task.var}</span>}
        </div>
        <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: 1.2, textTransform: 'uppercase', color: FAL.forest, opacity: 0.7, marginTop: 3, display: 'flex', alignItems: 'center', gap: 6 }}>
          <span style={{ width: 5, height: 5, borderRadius: 999, background: colour }} />
          {task.bed}{task.remain && ` · ${task.remain}`}
        </div>
      </div>
      <div style={{ textAlign: 'right' }}>
        <div style={{
          fontFamily: '"Fraunces", serif', fontSize: 14, fontStyle: 'italic',
          color: task.when === 'overdue' ? FAL.clay : FAL.forest,
        }}>{task.due}</div>
        <div style={{ width: 18, height: 18, border: `1.5px solid ${FAL.ink}`, marginTop: 6, marginLeft: 'auto' }} />
      </div>
    </div>
  );
}

window.FaltetTasksDesktop = FaltetTasksDesktop;
window.FaltetTasksMobile = FaltetTasksMobile;
