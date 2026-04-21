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
