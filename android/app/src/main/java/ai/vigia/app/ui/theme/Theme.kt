package ai.vigia.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// =========================================================================
// PALETTE PREMIUM LUMINEUSE VIGIA AI (LIGHT THEME PUR)
// Esthétique haut de gamme inspirée de Pinterest, Revolut et 1Password
// =========================================================================

val VigiaWhite = Color(0xFFFFFFFF)
val VigiaCanvas = Color(0xFFF8FAFC)
val VigiaCanvasSubtle = Color(0xFFF1F5F9)
val VigiaSurface = Color(0xFFFFFFFF)
val VigiaSurfaceSubtle = Color(0xFFF8FAFC)
val VigiaSurfaceHigh = Color(0xFFF1F5F9)

val VigiaBorder = Color(0xFFE2E8F0)
val VigiaBorderSubtle = Color(0xFFEEF2F6)

// Accents technologiques & cyber-sécurité
val VigiaPrimary = Color(0xFF2563EB)        // Bleu Royal intense
val VigiaSecondary = Color(0xFF4F46E5)      // Indigo profond
val VigiaCyan = Color(0xFF0284C7)           // Cyan ciel lumineux
val VigiaViolet = Color(0xFF7C3AED)         // Violet électrique
val VigiaBlue = Color(0xFF3B82F6)
val VigiaPrimarySoft = Color(0xFFEFF6FF)

// Typographie de précision
val VigiaTextPrimary = Color(0xFF0F172A)     // Noir ardoise profond
val VigiaTextSecondary = Color(0xFF475569)   // Gris ardoise équilibré
val VigiaTextMuted = Color(0xFF94A3B8)       // Gris neutre doux

// Statuts de menace
val RiskSafe = Color(0xFF059669)            // Vert Émeraude vif
val RiskSafeBg = Color(0xFFECFDF5)          // Fond vert menthe très doux
val RiskSafeBorder = Color(0xFFA7F3D0)

val RiskSuspicious = Color(0xFFD97706)      // Ambre chaud
val RiskSuspiciousBg = Color(0xFFFFFBEB)    // Fond crème doux
val RiskSuspiciousBorder = Color(0xFFFDE68A)

val RiskDanger = Color(0xFFDC2626)          // Rouge carmin d'alerte
val RiskDangerBg = Color(0xFFFEF2F2)        // Fond rose d'alerte subtil
val RiskDangerBorder = Color(0xFFFECACA)

// Dégradés haute fidélité
val BrandGradient = Brush.horizontalGradient(
    listOf(VigiaPrimary, VigiaSecondary, VigiaCyan)
)

val BackgroundGradient = Brush.verticalGradient(
    listOf(
        Color(0xFFFFFFFF),
        Color(0xFFF8FAFC),
        Color(0xFFF1F5F9)
    )
)

val CardGlassGradient = Brush.verticalGradient(
    listOf(
        Color(0xFFFFFFFF),
        Color(0xFFFAFCFF)
    )
)

val CardBorderGradient = Brush.linearGradient(
    listOf(
        Color(0xFFE2E8F0),
        Color(0xFFCBD5E1)
    )
)

val SafeBorderGradient = Brush.linearGradient(
    listOf(RiskSafeBorder, RiskSafe)
)

val DangerBorderGradient = Brush.linearGradient(
    listOf(RiskDangerBorder, RiskDanger)
)

fun riskColor(level: String): Color = when (level.lowercase()) {
    "dangerous" -> RiskDanger
    "suspicious" -> RiskSuspicious
    "safe" -> RiskSafe
    else -> VigiaPrimary
}

fun riskLabel(level: String): String = when (level.lowercase()) {
    "dangerous" -> "Dangereux"
    "suspicious" -> "Suspect"
    "safe" -> "Sûr"
    else -> "Inconnu"
}

fun riskBackground(level: String): Color = when (level.lowercase()) {
    "dangerous" -> RiskDangerBg
    "suspicious" -> RiskSuspiciousBg
    "safe" -> RiskSafeBg
    else -> Color(0xFFEFF6FF)
}

// Police Poppins standard pour toute l'application
val PoppinsFontFamily = FontFamily.SansSerif

private val VigiaLightColors = lightColorScheme(
    primary = VigiaPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEFF6FF),
    onPrimaryContainer = VigiaPrimary,
    secondary = VigiaSecondary,
    onSecondary = Color.White,
    tertiary = VigiaViolet,
    background = VigiaCanvas,
    onBackground = VigiaTextPrimary,
    surface = VigiaSurface,
    onSurface = VigiaTextPrimary,
    surfaceVariant = VigiaSurfaceSubtle,
    onSurfaceVariant = VigiaTextSecondary,
    error = RiskDanger,
    onError = Color.White,
    outline = VigiaBorder
)

val VigiaTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        letterSpacing = (-0.5).sp,
        color = VigiaTextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = (-0.3).sp,
        color = VigiaTextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = VigiaTextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        color = VigiaTextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp,
        lineHeight = 22.sp,
        color = VigiaTextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = VigiaTextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.5.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun VigiaTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VigiaLightColors,
        typography = VigiaTypography,
        content = content
    )
}
