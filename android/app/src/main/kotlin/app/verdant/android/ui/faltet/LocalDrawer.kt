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

/**
 * Hoists a navigate-to-Account callback so the Masthead always renders an
 * account icon at the top-right. NavGraph provides this; if absent the icon
 * is omitted.
 */
val LocalAccountOpen = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * Hoists a navigate-to-Dashboard callback so the Masthead can render a
 * dashboard icon next to the burger. NavGraph provides this; if absent the
 * icon is omitted.
 */
val LocalDashboardOpen = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * Hoists `navController.popBackStack()` so the Masthead can render a back
 * arrow on detail screens. NavGraph sets this on every non-root destination;
 * when present the Masthead swaps the burger for a back arrow.
 */
val LocalNavigateBack = staticCompositionLocalOf<(() -> Unit)?> { null }
