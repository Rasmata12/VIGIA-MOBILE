package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.ui.components.GradientButton
import ai.vigia.app.ui.theme.*

@Composable
fun WelcomeScreen(
    onDiscover: () -> Unit,
    onHaveAccount: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(BackgroundGradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp)
                .padding(top = 20.dp, bottom = 36.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onHaveAccount) {
                    Text("Passer", fontFamily = PoppinsFontFamily, color = VigiaTextMuted, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(VigiaPrimarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = VigiaPrimary,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Bienvenue sur",
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = VigiaTextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "VIGIA AI",
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = VigiaPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Votre bouclier intelligent contre le phishing, le piratage WhatsApp et les arnaques financières.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = PoppinsFontFamily,
                    color = VigiaTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DotStep(active = true)
                    DotStep(active = false)
                    DotStep(active = false)
                }
            }

            Spacer(Modifier.weight(1f))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GradientButton(
                    text = "Découvrir VIGIA AI",
                    onClick = onDiscover,
                    icon = Icons.Rounded.ArrowForward,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = onHaveAccount,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("J'ai déjà un compte", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun DotStep(active: Boolean) {
    Box(
        Modifier
            .height(6.dp)
            .width(if (active) 20.dp else 6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (active) VigiaPrimary else VigiaBorder)
    )
}
