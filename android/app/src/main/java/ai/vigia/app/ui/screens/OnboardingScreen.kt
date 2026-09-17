package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.ui.components.GradientButton
import ai.vigia.app.ui.theme.*

private data class OnboardingSlide(
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val heading: String,
    val description: String
)

private val SLIDES = listOf(
    OnboardingSlide(
        icon = Icons.Rounded.Search,
        iconBg = Color(0xFFEFF6FF),
        iconTint = VigiaPrimary,
        heading = "Détection Phishing L1-L6",
        description = "Inspection forensique approfondie des liens suspects reçus par SMS, WhatsApp et messages financiers."
    ),
    OnboardingSlide(
        icon = Icons.Rounded.AccountBalanceWallet,
        iconBg = Color(0xFFECFDF5),
        iconTint = RiskSafe,
        heading = "Before Pay™ Anti-Fraude",
        description = "Auditez instantanément le destinataire, le montant et le prétexte avant d'envoyer de l'argent par Wave, Orange Money ou virement."
    ),
    OnboardingSlide(
        icon = Icons.Rounded.Security,
        iconBg = Color(0xFFFAF5FF),
        iconTint = VigiaViolet,
        heading = "Guard 24/7 en Temps Réel",
        description = "Protection discrète en tâche de fond pour intercepter les menaces dès leur réception sans ralentir votre téléphone."
    )
)

@Composable
fun OnboardingScreen(
    onBackToWelcome: () -> Unit,
    onFinished: () -> Unit
) {
    var index by remember { mutableStateOf(0) }
    val slide = SLIDES[index]
    val isLast = index == SLIDES.lastIndex

    Box(Modifier.fillMaxSize().background(BackgroundGradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp)
                .padding(top = 20.dp, bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    if (index > 0) index-- else onBackToWelcome()
                }) {
                    Text("← Précédent", fontFamily = PoppinsFontFamily, color = VigiaPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onFinished) {
                    Text("Ignorer", fontFamily = PoppinsFontFamily, color = VigiaTextMuted, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(slide.iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = slide.icon,
                        contentDescription = null,
                        tint = slide.iconTint,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    text = slide.heading,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = VigiaTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = slide.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = PoppinsFontFamily,
                    color = VigiaTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(Modifier.height(22.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SLIDES.indices.forEach { i -> DotStep(active = i == index) }
                }
            }

            Spacer(Modifier.weight(1f))

            GradientButton(
                text = if (isLast) "Accéder à l'application →" else "Suivant",
                onClick = { if (isLast) onFinished() else index++ },
                icon = if (isLast) null else Icons.Rounded.ArrowForward,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
