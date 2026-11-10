# Botanical illustrations

These are web-optimized copies of the artwork already bundled with the Android app.
The peony is the exact illustration used by `ui/splash/SplashScreen.kt`.

| Web asset | Android source in `android/app/src/main/res/drawable/` |
| --- | --- |
| `peony*.webp` | `botanical_frontispiece.png` |
| `tulip*.webp` | `botanical_empty_garden.png` |
| `trellis*.webp` | `botanical_trellis.png` |
| `harvest*.webp` | `botanical_harvest.png` |

Each source was converted with `cwebp -q 82 -resize SIZE SIZE`, at 768px and 320px.
Keep the Android originals as the source of truth. The shared React component
uses responsive sources, empty alt text for decorative use, and CSS multiply
blending to let the light image backgrounds sit naturally on the paper surfaces.

See `android/docs/BOTANICAL_PLATES.md` for the existing art direction and sourcing notes.
