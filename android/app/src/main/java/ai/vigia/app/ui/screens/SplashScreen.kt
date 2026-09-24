package ai.vigia.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.R
import ai.vigia.app.ui.components.GlowOrb
import ai.vigia.app.ui.theme.*
import kotlinx.coroutines.delay

private const val SPLASH_MS = 1600

/** Écran de lancement ultra-premium : fond blanc pur et lumineux,
 * logo épuré sans fond noir, ondes cyber concentriques animées et signature VIGIA AI. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        progress = 1f
        delay(SPLASH_MS.toLong())
        onFinished()
    }

    val progressAnim by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(SPLASH_MS, easing = FastOutSlowInEasing),
        label = "splashProgress"
    )

    val infinite = rememberInfiniteTransition(label = "splashWaves")
    val pulse by infinite.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "logoPulse"
    )

    val ringScale by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "ringScale"
    )
    val ringAlpha by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "ringAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Halos de lumière diffuse en arrière-plan
        GlowOrb(
            color = VigiaCyan,
            intensity = 0.22f,
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.Center)
        )
        GlowOrb(
            color = VigiaPrimary,
            intensity = 0.14f,
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.Center)
        )

        // Ondes concentriques animées (ondes de bouclier radar)
        Canvas(modifier = Modifier.size(260.dp)) {
            val r = (size.width / 2) * ringScale
            drawCircle(
                color = VigiaPrimaryBright.copy(alpha = ringAlpha),
                radius = r,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = VigiaCyan.copy(alpha = (ringAlpha * 0.7f).coerceIn(0f, 1f)),
                radius = (r * 0.75f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 28.dp)
        ) {
            // Médaillon de logo central blanc pur
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(pulse)
                    .size(132.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = VigiaPrimary.copy(alpha = 0.20f),
                        spotColor = VigiaPrimary.copy(alpha = 0.30f)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
                    .border(
                        width = 1.5.dp,
                        brush = SolidColor(VigiaBorder),
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                Image(
                    painter = painterResource(R.drawable.vigia_logo),
                    contentDescription = "VIGIA AI Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
            }

            Spacer(Modifier.height(28.dp))

            // Marque VIGIA AI avec badge en dégradé
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "VIGIA",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = VigiaTextPrimary,
                    fontSize = 28.sp,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SolidColor(VigiaPrimary))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "AI",
                        fontSize = 13.sp,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "BOUCLIER CYBER & ANTI-FRAUDE PERSONNEL",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                color = VigiaPrimary,
                fontSize = 10.5.sp,
                letterSpacing = 1.6.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Protection intelligente en temps réel contre le phishing, les faux messages et les arnaques de paiement.",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(34.dp))

            // Barre de chargement ultra-fine haute précision
            Box(
                Modifier
                    .width(140.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressAnim)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SolidColor(VigiaPrimary))
                )
            }

            Spacer(Modifier.height(28.dp))

            TextButton(onClick = onFinished) {
                Text(
                    text = "Accéder directement →",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = VigiaPrimary
                )
            }
        }
    }
}
