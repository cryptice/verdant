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

// EB Garamond — a revival of Claude Garamond's 16th-century roman cuts. Has
// open counters (more readable than Fraunces at small sizes) and a restrained
// italic that sits closer to the period botanical journal aesthetic we're
// after without feeling cramped.
private val EbGaramondFont = GoogleFont("EB Garamond")
private val InterFont      = GoogleFont("Inter")

val FaltetDisplay = FontFamily(
    Font(googleFont = EbGaramondFont, fontProvider = GoogleFontsProvider, weight = FontWeight.W400, style = FontStyle.Normal),
    Font(googleFont = EbGaramondFont, fontProvider = GoogleFontsProvider, weight = FontWeight.W400, style = FontStyle.Italic),
    Font(googleFont = EbGaramondFont, fontProvider = GoogleFontsProvider, weight = FontWeight.W500, style = FontStyle.Normal),
    Font(googleFont = EbGaramondFont, fontProvider = GoogleFontsProvider, weight = FontWeight.W500, style = FontStyle.Italic),
    Font(googleFont = EbGaramondFont, fontProvider = GoogleFontsProvider, weight = FontWeight.W600, style = FontStyle.Normal),
)

val FaltetBody = FontFamily(
    Font(googleFont = InterFont, fontProvider = GoogleFontsProvider, weight = FontWeight.W400),
    Font(googleFont = InterFont, fontProvider = GoogleFontsProvider, weight = FontWeight.W500),
    Font(googleFont = InterFont, fontProvider = GoogleFontsProvider, weight = FontWeight.W600),
)

/**
 * Section / caption face — EB Garamond italic at small sizes. Used by section
 * headers, masthead corner labels, and bottom-bar tab labels in place of the
 * earlier mono-uppercase treatment. Pairs with a small floral glyph (✻ / ❀)
 * for the warm, journal-page feel.
 */
val FaltetCaption = FaltetDisplay

val FaltetMono: FontFamily = FontFamily.Monospace

val FaltetTypography = Typography(
    // Display — EB Garamond, weight 400. Slightly heavier than Fraunces W300
    // but with broader counters; renders better at large sizes on phone DPI.
    displayLarge   = TextStyle(fontFamily = FaltetDisplay, fontWeight = FontWeight.W400, fontSize = 72.sp, lineHeight = 78.sp, letterSpacing = (-1.0).sp),
    displayMedium  = TextStyle(fontFamily = FaltetDisplay, fontWeight = FontWeight.W400, fontSize = 52.sp, lineHeight = 58.sp, letterSpacing = (-0.6).sp),
    displaySmall   = TextStyle(fontFamily = FaltetDisplay, fontWeight = FontWeight.W400, fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.4).sp),
    headlineLarge  = TextStyle(fontFamily = FaltetDisplay, fontWeight = FontWeight.W400, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = FaltetDisplay, fontWeight = FontWeight.W400, fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall  = TextStyle(fontFamily = FaltetDisplay, fontWeight = FontWeight.W400, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge     = TextStyle(fontFamily = FaltetDisplay, fontWeight = FontWeight.W500, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium    = TextStyle(fontFamily = FaltetDisplay, fontWeight = FontWeight.W500, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall     = TextStyle(fontFamily = FaltetDisplay, fontWeight = FontWeight.W500, fontSize = 14.sp, lineHeight = 18.sp),
    // Body — Inter, weight 400/500.
    bodyLarge      = TextStyle(fontFamily = FaltetBody, fontWeight = FontWeight.W400, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium     = TextStyle(fontFamily = FaltetBody, fontWeight = FontWeight.W400, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall      = TextStyle(fontFamily = FaltetBody, fontWeight = FontWeight.W400, fontSize = 12.sp, lineHeight = 16.sp),
    // Labels — EB Garamond italic, sentence case. Replaces the earlier
    // mono-uppercase treatment.
    labelLarge     = TextStyle(fontFamily = FaltetCaption, fontWeight = FontWeight.W500, fontStyle = FontStyle.Italic, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium    = TextStyle(fontFamily = FaltetCaption, fontWeight = FontWeight.W500, fontStyle = FontStyle.Italic, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall     = TextStyle(fontFamily = FaltetCaption, fontWeight = FontWeight.W500, fontStyle = FontStyle.Italic, fontSize = 11.sp, lineHeight = 14.sp),
)
