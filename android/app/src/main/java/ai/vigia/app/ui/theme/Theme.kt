package ai.vigia.app.ui.theme

import ai.vigia.app.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
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

// Accents technologiques & cyber-sécurité — calibrés sur le logo officiel VIGIA AI
val VigiaNavy = Color(0xFF001A5E)           // Bleu marine du bouclier (logo)
val VigiaNavyDeep = Color(0xFF00123F)       // Marine très profond (fonds héros)
val VigiaPrimary = Color(0xFF0B57D0)        // Bleu royal intense (logo)
val VigiaPrimaryBright = Color(0xFF0072E9)  // Bleu électrique exact du logo
val VigiaSecondary = Color(0xFF4F46E5)      // Indigo profond
val VigiaCyan = Color(0xFF22D3EE)           // Cyan lumineux (reflet de l'œil du logo)
val VigiaCyanDeep = Color(0xFF0284C7)       // Cyan ciel plus soutenu
val VigiaViolet = Color(0xFF7C3AED)         // Violet électrique
val VigiaBlue = Color(0xFF3B82F6)
val VigiaEmerald = Color(0xFF10B981)      // Vert émeraude
val VigiaAmber = Color(0xFFF59E0B)        // Ambre chaud
val VigiaRose = Color(0xFFF43F5E)         // Rose vif
val VigiaPrimarySoft = Color(0xFFEFF6FF)

// Couleurs de repère par module — même famille bleu/cyan du logo, nuancées pour que
// chaque écran ait une identité propre tout en restant clairement "VIGIA AI".
val ModuleDashboard = VigiaPrimary
val ModuleAnalyze = VigiaPrimaryBright
val ModuleGuard = VigiaCyanDeep
val ModuleMomentShield = Color(0xFF0D9488)   // Teal
val ModuleHistory = Color(0xFF6366F1)        // Indigo clair
val ModuleCommunity = VigiaViolet
val ModuleDevices = Color(0xFF334155)        // Ardoise
val ModuleListing = Color(0xFF0EA5E9)        // Bleu ciel
val ModuleJobOffer = Color(0xFFD97706)       // Ambre (vigilance emploi)
val ModuleBeforePay = Color(0xFFDC2626)      // Rouge alerte (argent)
val ModulePrivacy = Color(0xFF0F766E)        // Vert sarcelle
val ModuleSettings = Color(0xFF475569)
val ModuleAuth = VigiaNavy

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

// Dégradés haute fidélité — reproduisent le dégradé exact du logo (marine → bleu → cyan)
val BrandGradient: Brush = Brush.linearGradient(
    colors = listOf(VigiaNavy, VigiaPrimary, VigiaPrimaryBright)
)
val BrandGradientVivid: Brush = Brush.linearGradient(
    colors = listOf(VigiaPrimary, VigiaPrimaryBright, VigiaCyan)
)
val HeroGradientDark: Brush = Brush.linearGradient(
    colors = listOf(VigiaNavyDeep, VigiaNavy, VigiaPrimary)
)
fun moduleGradient(accent: Color): Brush = Brush.linearGradient(
    colors = listOf(accent.copy(alpha = 0.92f), accent)
)
val BackgroundGradient: Brush = Brush.verticalGradient(
    colors = listOf(Color(0xFFFBFDFF), VigiaCanvas, Color(0xFFF3F7FC))
)
val CardGlassGradient: Brush = Brush.verticalGradient(
    colors = listOf(Color.White, Color(0xFFFBFDFF))
)
val CardTintBlue: Brush = SolidColor(Color(0xFFF3F8FF))
val CardTintEmerald: Brush = SolidColor(Color(0xFFF0FDF4))
val CardTintAmber: Brush = SolidColor(Color(0xFFFFFBEB))
val CardTintRose: Brush = SolidColor(Color(0xFFFEF2F2))
val CardTintViolet: Brush = SolidColor(Color(0xFFFAF5FF))
val CardTintCyan: Brush = SolidColor(Color(0xFFECFEFF))
fun luxuryCardGradient(accent: Color = VigiaPrimary): Brush = SolidColor(accent.copy(alpha = 0.035f))
fun luxuryBorderGradient(accent: Color = VigiaPrimary): Brush = SolidColor(accent.copy(alpha = 0.28f))
val SpecularBorderGradient: Brush = SolidColor(VigiaBorder)
val CardBorderGradient: Brush = SolidColor(VigiaBorder)
val SafeBorderGradient: Brush = SolidColor(RiskSafe)
val DangerBorderGradient: Brush = SolidColor(RiskDanger)

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

// Police Poppins réelle (fichiers .ttf officiels, licence OFL) - remplace l'ancien
// alias vers la police système par défaut, qui n'etait pas vraiment Poppins.
val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_extrabold, FontWeight.ExtraBold)
)

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
