# Android Fältet Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish the Fältet aesthetic foundation on the Verdant Android app — palette, Material color mapping, zero-radius shapes, Fraunces + Inter typography via Downloadable Fonts, shared primitives (Chip, Rule, Stat, Field, PhotoPlaceholder, Masthead), drawer + bottom-bar restyle, shared scaffold + FAB presets.

**Architecture:** Replace `Color.kt` + `Theme.kt` with Fältet tokens and Material `colorScheme` mapping; override `Shapes` to zero radius globally; load Fraunces + Inter via `androidx.compose.ui:ui-text-google-fonts`; build shared primitives under `ui/faltet/` that mirror web's `components/faltet/`; restyle the existing `ModalNavigationDrawer` + `NavigationBar` in `ui/navigation/NavGraph.kt`. Every existing screen inherits Fältet colors + typography automatically via Material token mapping. Design spec: `docs/plans/2026-04-22-android-faltet-foundation-design.md`.

**Tech Stack:** Kotlin + Jetpack Compose (Material 3) + Hilt + Retrofit. Compose BOM `2024.12.01` (per existing `build.gradle.kts`).

**Important notes:**
- Solo-dev, commits to `main`.
- **Surprise from reality-check:** only `Color.kt` + `Theme.kt` reference the old color constants (`GreenPrimary`, `Cream`, `TextPrimary`, etc.). Spec §6 anticipated find-replace across ~44 screens; actually the screens all go through `MaterialTheme.colorScheme.*`, so there's nothing to find-replace. `PhotoPicker.kt` references `Color.White` (Compose stdlib), not `app.verdant.android.ui.theme.White`. Plan Task 2 becomes a sanity-check pass rather than a mechanical migration.
- Every task ends with `cd android && ./gradlew compileDebugKotlin --no-daemon -q` green + its own commit.

---

## Phasing overview

| Task | Name |
|---|---|
| 1 | Replace `Color.kt` + `Theme.kt` |
| 2 | Sanity-check old-constant references (scope smaller than spec assumed) |
| 3 | Typography + Google Fonts |
| 4 | Primitives — Chip, Rule, Stat, Field, PhotoPlaceholder, Masthead |
| 5 | Drawer restyle |
| 6 | Bottom bar restyle |
| 7 | `FaltetScreenScaffold` + `FaltetFab` |
| 8 | Verify + milestone |

---

## Task 1 — Replace `Color.kt` + `Theme.kt`

**Goal:** Swap Verdant's old green/cream palette for the Fältet 11-color palette. Remap Material `colorScheme` so every Compose component picks up Fältet automatically. Override `Shapes` to zero radius globally.

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/theme/Color.kt`
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/theme/Theme.kt`

### Step 1: Overwrite `Color.kt`

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/theme/Color.kt
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

// Hairline alpha variants
val FaltetInkLine20 = FaltetInk.copy(alpha = 0.20f)
val FaltetInkLine40 = FaltetInk.copy(alpha = 0.40f)
val FaltetInkFill04 = FaltetInk.copy(alpha = 0.04f)
```

### Step 2: Overwrite `Theme.kt`

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/theme/Theme.kt
package app.verdant.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FaltetColorScheme = lightColorScheme(
    primary            = FaltetInk,
    onPrimary          = FaltetCream,
    primaryContainer   = FaltetPaper,
    onPrimaryContainer = FaltetInk,
    secondary          = FaltetClay,
    onSecondary        = FaltetCream,
    tertiary           = FaltetSage,
    onTertiary         = FaltetCream,
    background         = FaltetCream,
    onBackground       = FaltetInk,
    surface            = FaltetPaper,
    onSurface          = FaltetInk,
    surfaceVariant     = FaltetCream,
    onSurfaceVariant   = FaltetForest,
    outline            = FaltetInk,
    outlineVariant     = FaltetInkLine20,
    error              = FaltetClay,
    onError            = FaltetCream,
)

// Zero-radius shape family — matches web's Fältet brutalist aesthetic.
// Components needing pill radius (Chip, FAB) apply CircleShape locally.
private val FaltetShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small      = RoundedCornerShape(0.dp),
    medium     = RoundedCornerShape(0.dp),
    large      = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun verdantTopAppBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor             = FaltetCream,
    titleContentColor          = FaltetInk,
    navigationIconContentColor = FaltetInk,
    actionIconContentColor     = FaltetInk,
)

@Composable
fun VerdantTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FaltetColorScheme,
        typography  = FaltetTypography,   // defined in Task 3
        shapes      = FaltetShapes,
        content     = content,
    )
}
```

The `typography = FaltetTypography` reference will fail until Task 3 lands. To keep Task 1 self-contained and compilable, temporarily pass `Typography()` (the default) here and change to `FaltetTypography` at the end of Task 3. Or: allow Task 1's commit to be compile-broken for one commit and fix in Task 3. **Recommendation:** use `Typography()` as a placeholder in this commit and replace in Task 3 — keeps each commit green.

Final Task 1 version of the `VerdantTheme` body:

```kotlin
    MaterialTheme(
        colorScheme = FaltetColorScheme,
        typography  = androidx.compose.material3.Typography(),  // replaced in Task 3
        shapes      = FaltetShapes,
        content     = content,
    )
```

### Step 3: Verify

```bash
cd /Users/erik/development/verdant/android && ./gradlew compileDebugKotlin --no-daemon -q
```

Expected: exit 0.

### Step 4: Commit

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/theme/Color.kt \
        android/app/src/main/kotlin/app/verdant/android/ui/theme/Theme.kt
git commit -m "feat: Fältet palette + MaterialTheme color mapping + zero-radius Shapes"
```

---

## Task 2 — Sanity-check old-constant references

**Goal:** Verify no remaining references to deleted color constants exist outside the theme files. Fix any stragglers.

**Files:** Audit only — no predicted edits.

### Step 1: Search for deleted names

```bash
cd /Users/erik/development/verdant
grep -rn 'GreenPrimary\|GreenDark\|GreenLight\|ErrorRed' android/app/src/main/kotlin --include="*.kt"
grep -rn '\bCream\b\|CreamDark\|\bTextPrimary\b\|\bTextSecondary\b' android/app/src/main/kotlin --include="*.kt" | grep -v 'theme/Color.kt\|theme/Theme.kt'
```

Expected: **no matches.** The reality-check confirmed Task 1 already removed them; this step confirms nothing was missed.

### Step 2: Handle `White` references (stdlib vs deleted theme constant)

```bash
grep -rn 'import.*app\.verdant\.android\.ui\.theme\.White\|import.*app\.verdant\.android\.ui\.theme\.Cream\|import.*app\.verdant\.android\.ui\.theme\.TextPrimary' android/app/src/main/kotlin --include="*.kt"
```

Any matches indicate a file imports a now-deleted constant. Expected: **no matches** (the reality check showed only `PhotoPicker.kt` uses `Color.White`, which is `androidx.compose.ui.graphics.Color.White` — a stdlib symbol, not a theme constant).

### Step 3: If any matches show up

For each file that references a deleted constant, replace per this table:

| Deleted theme constant | Replacement |
|---|---|
| `app.verdant.android.ui.theme.GreenPrimary` | `FaltetClay` |
| `app.verdant.android.ui.theme.GreenDark` | `FaltetInk` |
| `app.verdant.android.ui.theme.GreenLight` | `FaltetSage` |
| `app.verdant.android.ui.theme.Cream` | `FaltetCream` |
| `app.verdant.android.ui.theme.CreamDark` | `FaltetPaper` |
| `app.verdant.android.ui.theme.White` | `FaltetCream` |
| `app.verdant.android.ui.theme.TextPrimary` | `FaltetInk` |
| `app.verdant.android.ui.theme.TextSecondary` | `FaltetForest` |
| `app.verdant.android.ui.theme.ErrorRed` | `FaltetClay` |

Rewrite the import and replace the usage in a single edit per file. Update imports to `app.verdant.android.ui.theme.Faltet*`.

### Step 4: Verify

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

Green.

### Step 5: Commit only if changes were needed

If Step 3 touched any files, commit:

```bash
git add android/app/src/main/kotlin/...
git commit -m "refactor: migrate stray references to Fältet color constants"
```

Otherwise skip the commit — no file was modified, and the audit is effectively part of Task 1's validation.

---

## Task 3 — Typography + Google Fonts

**Goal:** Load Fraunces + Inter via Downloadable Fonts API. Build `FaltetTypography` mapping to Material typography slots. Wire it into `VerdantTheme`.

**Files:**
- Modify: `android/app/build.gradle.kts`
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/theme/Typography.kt`
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/theme/Theme.kt` (use `FaltetTypography`)

### Step 1: Add Gradle dependency

Open `android/app/build.gradle.kts`. Inside the `dependencies { }` block, next to the other `androidx.compose.ui` lines, add:

```kotlin
    implementation("androidx.compose.ui:ui-text-google-fonts")
```

Version comes from the Compose BOM (no explicit version needed; the BOM resolves it).

### Step 2: Sync + verify the dependency resolves

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

Green. If the resolve fails, pin an explicit version by appending `:1.7.5` (matching Compose 1.7.x). Confirm the installed Compose version first: `./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep 'androidx.compose.ui:ui:' | head -1`.

### Step 3: Create `Typography.kt`

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/theme/Typography.kt
package app.verdant.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import app.verdant.android.R

val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs,
)

private val FrauncesFont = GoogleFont("Fraunces")
private val InterFont    = GoogleFont("Inter")

val FaltetDisplay = FontFamily(
    Font(googleFont = FrauncesFont, fontProvider = GoogleFontsProvider, weight = FontWeight.W300, style = FontStyle.Normal),
    Font(googleFont = FrauncesFont, fontProvider = GoogleFontsProvider, weight = FontWeight.W300, style = FontStyle.Italic),
    Font(googleFont = FrauncesFont, fontProvider = GoogleFontsProvider, weight = FontWeight.W400, style = FontStyle.Normal),
    Font(googleFont = FrauncesFont, fontProvider = GoogleFontsProvider, weight = FontWeight.W400, style = FontStyle.Italic),
    Font(googleFont = FrauncesFont, fontProvider = GoogleFontsProvider, weight = FontWeight.W500, style = FontStyle.Normal),
)

val FaltetBody = FontFamily(
    Font(googleFont = InterFont, fontProvider = GoogleFontsProvider, weight = FontWeight.W400),
    Font(googleFont = InterFont, fontProvider = GoogleFontsProvider, weight = FontWeight.W500),
    Font(googleFont = InterFont, fontProvider = GoogleFontsProvider, weight = FontWeight.W600),
)

val FaltetMono: FontFamily = FontFamily.Monospace

val FaltetTypography = Typography(
    // Display — Fraunces, weight 300, used for headlines, hero titles, stat values.
    displayLarge   = TextStyle(fontFamily = FaltetDisplay, fontWeight = FontWeight.W300, fontSize = 80.sp, lineHeight = 84.sp, letterSpacing = (-1.5).sp),
    displayMedium  = TextStyle(fontFamily = FaltetDisplay, fontWeight = FontWeight.W300, fontSize = 56.sp, lineHeight = 60.sp, letterSpacing = (-1.0).sp),
    displaySmall   = TextStyle(fontFamily = FaltetDisplay, fontWeight = FontWeight.W300, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.8).sp),
    headlineLarge  = TextStyle(fontFamily = FaltetDisplay, fontWeight = FontWeight.W300, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = FaltetDisplay, fontWeight = FontWeight.W300, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall  = TextStyle(fontFamily = FaltetDisplay, fontWeight = FontWeight.W300, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge     = TextStyle(fontFamily = FaltetDisplay, fontWeight = FontWeight.W400, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium    = TextStyle(fontFamily = FaltetDisplay, fontWeight = FontWeight.W400, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall     = TextStyle(fontFamily = FaltetDisplay, fontWeight = FontWeight.W400, fontSize = 14.sp, lineHeight = 18.sp),
    // Body — Inter, weight 400/500.
    bodyLarge      = TextStyle(fontFamily = FaltetBody, fontWeight = FontWeight.W400, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium     = TextStyle(fontFamily = FaltetBody, fontWeight = FontWeight.W400, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall      = TextStyle(fontFamily = FaltetBody, fontWeight = FontWeight.W400, fontSize = 12.sp, lineHeight = 16.sp),
    // Labels — mono uppercase. Callers must uppercase the text themselves via String.uppercase().
    labelLarge     = TextStyle(fontFamily = FaltetMono, fontWeight = FontWeight.W400, fontSize = 11.sp, letterSpacing = 1.8.sp),
    labelMedium    = TextStyle(fontFamily = FaltetMono, fontWeight = FontWeight.W400, fontSize = 10.sp, letterSpacing = 1.4.sp),
    labelSmall     = TextStyle(fontFamily = FaltetMono, fontWeight = FontWeight.W400, fontSize =  9.sp, letterSpacing = 1.4.sp),
)
```

### Step 4: Wire `FaltetTypography` into `VerdantTheme`

In `Theme.kt`, change the `MaterialTheme` call's `typography` parameter:

```kotlin
    MaterialTheme(
        colorScheme = FaltetColorScheme,
        typography  = FaltetTypography,
        shapes      = FaltetShapes,
        content     = content,
    )
```

Remove the placeholder `androidx.compose.material3.Typography()` from Task 1 step 2.

### Step 5: Verify certs reference resolves

The `R.array.com_google_android_gms_fonts_certs` reference comes from the `ui-text-google-fonts` library — it ships the resource. Compile:

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

If the reference fails to resolve, create `android/app/src/main/res/values/font_certs.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <array name="com_google_android_gms_fonts_certs">
        <item>@array/com_google_android_gms_fonts_certs_dev</item>
        <item>@array/com_google_android_gms_fonts_certs_prod</item>
    </array>
</resources>
```

Then re-verify.

### Step 6: Commit

```bash
git add android/app/build.gradle.kts \
        android/app/src/main/kotlin/app/verdant/android/ui/theme/Typography.kt \
        android/app/src/main/kotlin/app/verdant/android/ui/theme/Theme.kt
# If font_certs.xml was created, add it too:
# git add android/app/src/main/res/values/font_certs.xml
git commit -m "feat: Fältet typography — Fraunces + Inter via Downloadable Fonts"
```

---

## Task 4 — Primitives

**Goal:** Ship shared Fältet primitives under `ui/faltet/`.

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetTone.kt`
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/Chip.kt`
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/Rule.kt`
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/Stat.kt`
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/Field.kt`
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/PhotoPlaceholder.kt`
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/Masthead.kt`

### Step 1: `FaltetTone.kt` (shared enum + color mapping)

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetTone.kt
package app.verdant.android.ui.faltet

import androidx.compose.ui.graphics.Color
import app.verdant.android.ui.theme.FaltetBerry
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetMustard
import app.verdant.android.ui.theme.FaltetSage
import app.verdant.android.ui.theme.FaltetSky

enum class FaltetTone { Clay, Mustard, Berry, Sky, Sage, Forest }

fun FaltetTone.color(): Color = when (this) {
    FaltetTone.Clay    -> FaltetClay
    FaltetTone.Mustard -> FaltetMustard
    FaltetTone.Berry   -> FaltetBerry
    FaltetTone.Sky     -> FaltetSky
    FaltetTone.Sage    -> FaltetSage
    FaltetTone.Forest  -> FaltetForest
}
```

### Step 2: `Chip.kt`

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/Chip.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetCream

@Composable
fun Chip(
    text: String,
    tone: FaltetTone = FaltetTone.Forest,
    filled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val toneColor = tone.color()
    Box(
        modifier = modifier
            .then(if (filled) Modifier.background(toneColor, CircleShape) else Modifier)
            .border(1.dp, toneColor, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.W400,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = if (filled) FaltetCream else toneColor,
            ),
        )
    }
}
```

### Step 3: `Rule.kt`

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/Rule.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20

enum class RuleVariant { Ink, Soft }

@Composable
fun Rule(variant: RuleVariant = RuleVariant.Ink, modifier: Modifier = Modifier) {
    val color = when (variant) {
        RuleVariant.Ink  -> FaltetInk
        RuleVariant.Soft -> FaltetInkLine20
    }
    Box(modifier = modifier.fillMaxWidth().height(1.dp).background(color))
}
```

### Step 4: `Stat.kt`

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/Stat.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk

enum class StatSize { Large, Medium, Small }

@Composable
fun Stat(
    value: String,
    label: String,
    unit: String? = null,
    delta: String? = null,
    hue: FaltetTone = FaltetTone.Sage,
    size: StatSize = StatSize.Large,
    modifier: Modifier = Modifier,
) {
    val valueSize = when (size) { StatSize.Large -> 88.sp; StatSize.Medium -> 56.sp; StatSize.Small -> 32.sp }
    val unitSize  = when (size) { StatSize.Large -> 28.sp; StatSize.Medium -> 18.sp; StatSize.Small -> 14.sp }
    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontFamily = FaltetDisplay,
                fontWeight = FontWeight.W300,
                fontSize = valueSize,
                letterSpacing = (-1.2).sp,
                color = FaltetInk,
            )
            unit?.let {
                Text(
                    text = " $it",
                    fontFamily = FaltetDisplay,
                    fontStyle = FontStyle.Italic,
                    fontSize = unitSize,
                    color = hue.color(),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(hue.color(), CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(
                text = label.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 1.8.sp,
                color = FaltetForest,
            )
            delta?.let {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "▲ $it",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 1.8.sp,
                    color = FaltetClay,
                )
            }
        }
    }
}
```

### Step 5: `Field.kt`

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/Field.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk

@Composable
fun Field(
    label: String,
    value: String,
    onValueChange: ((String) -> Unit)? = null,
    accent: FaltetTone? = null,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
) {
    val valueColor = accent?.color() ?: FaltetInk
    Column(modifier) {
        Text(
            text = label.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = FaltetForest.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(4.dp))
        if (onValueChange != null) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontFamily = FaltetDisplay,
                    fontWeight = FontWeight.W300,
                    fontSize = 20.sp,
                    color = valueColor,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = FaltetInk,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    .padding(vertical = 4.dp),
                decorationBox = { inner ->
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            fontFamily = FaltetDisplay,
                            fontSize = 20.sp,
                            color = FaltetForest.copy(alpha = 0.4f),
                        )
                    }
                    inner()
                },
            )
        } else {
            Text(
                text = value,
                fontFamily = FaltetDisplay,
                fontWeight = FontWeight.W300,
                fontSize = 20.sp,
                color = valueColor,
            )
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(FaltetInk),
            )
        }
    }
}
```

### Step 6: `PhotoPlaceholder.kt`

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/PhotoPlaceholder.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetBlush
import app.verdant.android.ui.theme.FaltetButter
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetSage

enum class PhotoTone { Sage, Blush, Butter }

@Composable
fun PhotoPlaceholder(
    label: String,
    tone: PhotoTone = PhotoTone.Sage,
    aspectRatio: Float = 16f / 9f,
    modifier: Modifier = Modifier,
) {
    val toneColor = when (tone) {
        PhotoTone.Sage   -> FaltetSage
        PhotoTone.Blush  -> FaltetBlush
        PhotoTone.Butter -> FaltetButter
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        toneColor.copy(alpha = 0.35f),
                        toneColor.copy(alpha = 0.12f),
                        FaltetCream,
                    ),
                ),
            )
            .border(1.dp, FaltetInk),
    ) {
        Text(
            text = label.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = FaltetForest.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
        )
    }
}
```

### Step 7: `Masthead.kt`

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/Masthead.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk

@Composable
fun Masthead(
    left: String,
    center: String,
    right: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(FaltetCream)
            .drawBehind {
                drawLine(
                    color = FaltetInk,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 22.dp, vertical = 14.dp),
    ) {
        Text(
            text = left.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 1.8.sp,
            color = FaltetForest,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = center,
            fontFamily = FaltetDisplay,
            fontStyle = FontStyle.Italic,
            fontSize = 14.sp,
            color = FaltetForest,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(2f),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            right?.invoke()
        }
    }
}
```

### Step 8: Verify

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

Green.

### Step 9: Commit

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/
git commit -m "feat: Fältet primitives — Chip, Rule, Stat, Field, PhotoPlaceholder, Masthead"
```

---

## Task 5 — Drawer restyle

**Goal:** Replace the drawer sheet content in `NavGraph.kt` with a Fältet-styled column — wordmark header, grouped nav sections under `§ SECTION` mono small-caps headers, italic Fraunces labels with clay active bullet.

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/navigation/NavGraph.kt`

### Step 1: Read the current drawer content

```bash
grep -n 'drawerContent\|ModalNavigationDrawer\|NavigationDrawerItem' /Users/erik/development/verdant/android/app/src/main/kotlin/app/verdant/android/ui/navigation/NavGraph.kt | head -20
```

Identify the section under `drawerContent = { ... }`. Typical structure is a `ModalDrawerSheet { Column { /* header */; /* items */ } }`. Note the destinations the current drawer exposes.

### Step 2: Replace the drawer content

Replace the body of `drawerContent = { ... }` with a Fältet column. Full replacement pattern:

```kotlin
drawerContent = {
    ModalDrawerSheet(
        drawerContainerColor = FaltetCream,
        drawerContentColor = FaltetInk,
    ) {
        Column(modifier = Modifier.fillMaxHeight().padding(top = 24.dp)) {
            // Wordmark header
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = FaltetInk)) { append("Verdant") }
                        withStyle(SpanStyle(color = FaltetClay)) { append(".") }
                    },
                    fontFamily = FaltetDisplay,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.W300,
                    fontSize = 26.sp,
                )
            }
            Text(
                text = "Est. 2026".uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.8.sp,
                color = FaltetForest,
                modifier = Modifier.padding(start = 18.dp, top = 4.dp),
            )
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(FaltetInk))

            // Scrollable nav body
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                // Section 1 — § ODLING
                DrawerSection("§ Odling")
                DrawerItem("Översikt", Screen.MyWorld.route, currentRoute, navController, scope, drawerState)
                DrawerItem("Plantor", Screen.PlantedSpeciesList.route, currentRoute, navController, scope, drawerState)

                // Section 2 — § UPPGIFTER
                DrawerSection("§ Uppgifter")
                DrawerItem("Uppgifter", Screen.TaskList.route, currentRoute, navController, scope, drawerState)
                DrawerItem("Frölager", Screen.SeedInventory.route, currentRoute, navController, scope, drawerState)
                DrawerItem("Förbrukning", Screen.Supplies.route, currentRoute, navController, scope, drawerState)
                DrawerItem("Successioner", Screen.SuccessionSchedules.route, currentRoute, navController, scope, drawerState)
                DrawerItem("Mål", Screen.ProductionTargets.route, currentRoute, navController, scope, drawerState)

                // Section 3 — § SKÖRD & FÖRSÄLJNING
                DrawerSection("§ Skörd & Försäljning")
                DrawerItem("Skörd", Screen.HarvestStats.route, currentRoute, navController, scope, drawerState)
                DrawerItem("Kunder", Screen.CustomerList.route, currentRoute, navController, scope, drawerState)
                DrawerItem("Buketter", Screen.BouquetRecipes.route, currentRoute, navController, scope, drawerState)

                // Section 4 — § ANALYS
                DrawerSection("§ Analys")
                DrawerItem("Försök", Screen.VarietyTrials.route, currentRoute, navController, scope, drawerState)
                DrawerItem("Skadedjur", Screen.PestDiseaseLog.route, currentRoute, navController, scope, drawerState)
                DrawerItem("Analys", Screen.Analytics.route, currentRoute, navController, scope, drawerState)
            }

            // Section 5 — § KONTO (bottom-docked)
            Box(Modifier.fillMaxWidth().height(1.dp).background(FaltetInkLine20))
            DrawerSection("§ Konto")
            DrawerItem("Säsonger", Screen.SeasonSelector.route, currentRoute, navController, scope, drawerState)
            DrawerItem("Konto", Screen.Account.route, currentRoute, navController, scope, drawerState)
            Spacer(Modifier.height(16.dp))
        }
    }
}
```

Introduce two private helpers at the bottom of `NavGraph.kt` (or in a new file `ui/navigation/DrawerItems.kt`):

```kotlin
@Composable
private fun DrawerSection(title: String) {
    Text(
        text = title.uppercase(),
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
        letterSpacing = 1.4.sp,
        color = FaltetForest.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 18.dp, top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun DrawerItem(
    label: String,
    route: String,
    currentRoute: String?,
    navController: NavController,
    scope: CoroutineScope,
    drawerState: DrawerState,
) {
    val active = currentRoute == route
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable {
                scope.launch { drawerState.close() }
                navController.navigate(route) {
                    popUpTo(Screen.MyWorld.route)
                }
            }
            .drawBehind {
                drawLine(
                    color = FaltetInkLine20,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            fontFamily = FaltetDisplay,
            fontStyle = FontStyle.Italic,
            fontSize = 18.sp,
            color = if (active) FaltetClay else FaltetInk,
            modifier = Modifier.weight(1f),
        )
        if (active) {
            Text(
                text = "●",
                fontFamily = FaltetDisplay,
                fontSize = 16.sp,
                color = FaltetClay,
            )
        }
    }
}
```

### Step 3: Route-existence check

Cross-reference `Screen.*` routes against the existing `sealed class Screen` declarations in `NavGraph.kt`. If any of `SeedInventory`, `Supplies`, `SuccessionSchedules`, `ProductionTargets`, `HarvestStats`, `CustomerList`, `BouquetRecipes`, `VarietyTrials`, `PestDiseaseLog`, `Analytics`, `SeasonSelector`, `Account` are missing from the sealed class, drop the corresponding `DrawerItem(...)` line rather than inventing a route.

Verify using:

```bash
grep -E "data object (MyWorld|PlantedSpeciesList|TaskList|SeedInventory|Supplies|SuccessionSchedules|ProductionTargets|HarvestStats|CustomerList|BouquetRecipes|VarietyTrials|PestDiseaseLog|Analytics|SeasonSelector|Account)" /Users/erik/development/verdant/android/app/src/main/kotlin/app/verdant/android/ui/navigation/NavGraph.kt
```

Delete drawer rows whose route isn't found.

### Step 4: Imports

Ensure the following are imported at the top of `NavGraph.kt` (merge with existing imports):

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
```

### Step 5: Verify

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

Green.

### Step 6: Commit

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/navigation/NavGraph.kt
git commit -m "feat: Fältet drawer — wordmark header, grouped sections, clay active bullet"
```

---

## Task 6 — Bottom bar restyle

**Goal:** Bottom `NavigationBar` becomes Fältet: cream bg, 1dp ink top border, mono uppercase labels, clay active icon/label, no pill indicator.

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/navigation/NavGraph.kt`

### Step 1: Locate the current bottom bar

```bash
grep -n 'bottomBar\|NavigationBar\b\|NavigationBarItem' /Users/erik/development/verdant/android/app/src/main/kotlin/app/verdant/android/ui/navigation/NavGraph.kt
```

Find the `NavigationBar(...) { ... NavigationBarItem(...) ... }` block.

### Step 2: Replace the `NavigationBar` container

Replace the existing `NavigationBar { ... }` invocation with:

```kotlin
NavigationBar(
    containerColor = FaltetCream,
    contentColor = FaltetInk,
    modifier = Modifier
        .drawBehind {
            drawLine(
                color = FaltetInk,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 1.dp.toPx(),
            )
        },
) {
    // items…
}
```

### Step 3: Restyle each `NavigationBarItem`

For each existing item, add `colors = NavigationBarItemDefaults.colors(...)` and change the label text:

```kotlin
NavigationBarItem(
    selected = currentRoute == Screen.MyWorld.route,
    onClick = { navController.navigate(Screen.MyWorld.route) { popUpTo(Screen.MyWorld.route) { inclusive = true } } },
    icon = { Icon(Icons.Default.Home, contentDescription = null) },
    label = {
        Text(
            text = stringResource(R.string.my_world).uppercase(),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
        )
    },
    colors = NavigationBarItemDefaults.colors(
        selectedIconColor = FaltetClay,
        selectedTextColor = FaltetClay,
        unselectedIconColor = FaltetForest,
        unselectedTextColor = FaltetForest,
        indicatorColor = Color.Transparent,
    ),
)
```

Apply the same `colors` + `label` text-style change to every existing `NavigationBarItem` in the bar. Don't modify the `icon` slot or the `selected` / `onClick` logic.

### Step 4: Imports

Add to the top of `NavGraph.kt` if missing:

```kotlin
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.graphics.Color
```

### Step 5: Verify

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

Green.

### Step 6: Commit

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/navigation/NavGraph.kt
git commit -m "feat: Fältet bottom nav — cream bg, mono labels, clay active, no pill"
```

---

## Task 7 — `FaltetScreenScaffold` + `FaltetFab`

**Goal:** Ship the shared scaffold and FAB preset. Not yet adopted by any screen — that's later sub-specs.

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetScreenScaffold.kt`
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetFab.kt`

### Step 1: `FaltetScreenScaffold.kt`

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetScreenScaffold.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import app.verdant.android.ui.theme.FaltetCream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaltetScreenScaffold(
    mastheadLeft: String,
    mastheadCenter: String,
    mastheadRight: @Composable (() -> Unit)? = null,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    fab: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = FaltetCream,
        topBar = {
            Column {
                topBar()
                Masthead(left = mastheadLeft, center = mastheadCenter, right = mastheadRight)
            }
        },
        bottomBar = bottomBar,
        floatingActionButton = { fab?.invoke() },
        content = content,
    )
}
```

### Step 2: `FaltetFab.kt`

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetFab.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetInk

@Composable
fun FaltetFab(
    onClick: () -> Unit,
    contentDescription: String?,
    icon: ImageVector = Icons.Default.Add,
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = FaltetInk,
        contentColor = FaltetClay,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
        ),
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}
```

### Step 3: Verify

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

Green.

### Step 4: Commit

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetScreenScaffold.kt \
        android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetFab.kt
git commit -m "feat: FaltetScreenScaffold + FaltetFab primitives"
```

---

## Task 8 — Verify + milestone

### Step 1: Full Android build

```bash
cd android && ./gradlew assembleDebug --no-daemon -q
```

Expected: BUILD SUCCESSFUL. APK lands at `android/app/build/outputs/apk/debug/app-debug.apk`.

### Step 2: Install + manual smoke

Install the debug APK on a real device or emulator, sign in, and open:
- MyWorld (bottom nav)
- PlantedSpeciesList (bottom nav)
- TaskList (bottom nav)
- Open the drawer; tap through each drawer entry.

Look for:
- Cream backgrounds everywhere.
- Ink primary text, forest secondary text.
- Clay accents on active drawer entry + active bottom-nav icon.
- Fraunces italic in headings (after a brief system-serif flash on cold font cache first run).
- Zero corner radius on cards, buttons, dialogs.
- No obvious layout breaks or crashes.

### Step 3: Milestone commit

```bash
git commit --allow-empty -m "milestone: Android Fältet foundation complete (Spec A)"
```

---

## Verification summary

After task 8:

- `android/app/src/main/kotlin/app/verdant/android/ui/theme/` contains Fältet palette + Material color mapping + zero-radius shapes + Fraunces/Inter typography via Google Fonts.
- `android/app/src/main/kotlin/app/verdant/android/ui/faltet/` contains Chip, Rule, Stat, Field, PhotoPlaceholder, Masthead, FaltetScreenScaffold, FaltetFab, FaltetTone.
- `NavGraph.kt` has Fältet-styled drawer + bottom bar.
- `./gradlew assembleDebug` green.
- Existing screens visually picking up Fältet colors + Fraunces typography via `MaterialTheme`.

**Follow-up — Android sub-specs B / C / D** port individual screens to editorial Fältet layouts (ledger hero, masthead-center editorial strip, Stat displays, Field form patterns). Each sub-spec brainstormed independently when ready.

---

## Self-review notes

- **Spec §1 scope:** covered by Tasks 1–7.
- **Spec §2 Color/Theme:** Tasks 1–2.
- **Spec §3 Typography:** Task 3.
- **Spec §4 Primitives:** Task 4.
- **Spec §5 Scaffold/drawer/bottom bar/FAB:** Tasks 5–7.
- **Spec §6 Migration strategy:** Task 2 (verified scope is near-zero thanks to MaterialTheme indirection).
- **Spec §7 Testing:** Task 8.
- **Spec §8 Phasing:** matches plan 1-to-1.
