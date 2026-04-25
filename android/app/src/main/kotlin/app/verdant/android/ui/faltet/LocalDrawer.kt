// android/app/src/main/kotlin/app/verdant/android/ui/faltet/LocalDrawer.kt
package app.verdant.android.ui.faltet

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Hoists the side drawer open-callback so any Masthead can render the burger
 * button without each screen having to pass a callback down through layers.
 * The NavGraph provides this; if absent (e.g. preview, splash) the burger is
 * simply not rendered.
 */
val LocalDrawerOpen = staticCompositionLocalOf<(() -> Unit)?> { null }
