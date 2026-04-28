# Botanical plates

The `BotanicalIllustration` composable expects 17th-century botanical
engravings dropped into this directory. Until you place real assets, every
plate falls back to `botanical_placeholder.xml` — a small line-art sprig.

## Aesthetic target

Hand-coloured copperplate engraving from the early 1600s. Single specimen
centered on the page, fine cross-hatching for shading, naturalistic but
slightly stylized. Watercolour washes applied over the engraved lines —
muted, slightly powdery palette: sage and olive greens, ochre yellows,
brick reds, dusty rose pinks. The engraved lines remain visible through
the colour. The composable passes colour through unchanged, so source
images should already be in colour.

## Recommended source

**Basilius Besler — _Hortus Eystettensis_ (1613).** A florilegium of the
Bishop of Eichstätt's garden; 367 plates, all public domain. Wikimedia
Commons has high-resolution scans:

  https://commons.wikimedia.org/wiki/Category:Hortus_Eystettensis

Other period sources that match the style: John Gerard's _Herball_ (1597),
Pierre Vallet's _Le Jardin du Roy_ (1608), Crispijn de Passe's _Hortus
Floridus_ (1614), Pietro Andrea Mattioli's herbals.

## Plates the app expects

Replace each entry below with a copy of a Besler (or equivalent) scan.
1024×1024 PNG, transparent or white background, both work. The composable
crops with `ContentScale.Fit` so any aspect ratio survives.

| File                        | Used at                      | Suggested specimen        |
|-----------------------------|------------------------------|---------------------------|
| `botanical_empty_garden.png`| Empty-state: no plants       | Tulip in bloom            |
| `botanical_frontispiece.png`| Splash / drawer header       | Peony or large rose plate |
| `botanical_trellis.png`     | Empty-state: no tasks        | Vine / espaliered specimen|
| `botanical_harvest.png`     | Empty-state: no harvest      | Fruiting branch (apple, fig) |

Once a file lands, update `BotanicalPlate` in `BotanicalIllustration.kt` to
point at it (e.g. `R.drawable.botanical_empty_garden`). The placeholder
vector can stay; new code can default to it.
