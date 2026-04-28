package app.verdant.android.ui.theme

import androidx.compose.ui.graphics.Color

// Fältet palette — matches web tokens in web/src/index.css
val FaltetCream   = Color(0xFFF5EFE2)
val FaltetPaper   = Color(0xFFFBF7EC)
val FaltetInk     = Color(0xFF1E241D)
val FaltetForest  = Color(0xFF2F3D2E)
val FaltetSage    = Color(0xFF6B8F6A)
val FaltetClay    = Color(0xFFB6553C)
val FaltetMustard = Color(0xFFC89A2B)
val FaltetBerry   = Color(0xFF7A2E44)
val FaltetSky     = Color(0xFF4A7A8C)
val FaltetButter  = Color(0xFFF2D27A)
val FaltetBlush   = Color(0xFFE9B8A8)
// FaltetBloom — soft rose used for non-action accents (selected tab tint,
// gentle highlights, divider washes). Keeps Mustard reserved for primary
// action affordances so the hierarchy is unambiguous.
val FaltetBloom   = Color(0xFFD18A88)

// Hairline alpha variants
val FaltetInkLine20 = FaltetInk.copy(alpha = 0.20f)
val FaltetInkLine40 = FaltetInk.copy(alpha = 0.40f)
val FaltetInkFill04 = FaltetInk.copy(alpha = 0.04f)

// Semantic aliases
// FaltetAccent — primary interactive accent (active states, highlights, primary affordances)
// FaltetClay   — destructive semantics only (delete, confirm-destructive, error state)
val FaltetAccent = FaltetMustard
