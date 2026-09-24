package ai.vigia.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.R
import ai.vigia.app.ui.components.GlowOrb
import ai.vigia.app.ui.components.GradientButton
import ai.vigia.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    onDiscover: () -> Unit,
    onHaveAccount: () -> Unit
) {
    var showLogo by remember { mutableStateOf(false) }
    var showText by remember { mutableStateOf(false) }
    var showButtons by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showLogo = true
        delay(180)
        showText = true
        delay(160)
        showButtons = true
    }

    Box(Modifier.fillMaxSize().background(BackgroundGradient)) {
        // Halos d'ambiance en fond de scène
        GlowOrb(
            color = VigiaPrimary,
            intensity = 0.16f,
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = 90.dp, y = (-60).dp)
        )
        GlowOrb(
            color = VigiaViolet,
            intensity = 0.12f,
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 60.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 26.dp)
                .padding(top = 16.dp, bottom = 32.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onHaveAccount) {
                    Text(
                        "Passer",
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedVisibility(
                    visible = showLogo,
                    enter = fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 4 }
                ) {
                    Box(
                        modifier = Modifier
                            .size(136.dp)
                            .shadow(28.dp, RoundedCornerShape(32.dp), ambientColor = VigiaPrimary.copy(alpha = 0.25f), spotColor = VigiaPrimary.copy(alpha = 0.25f))
                            .clip(RoundedCornerShape(32.dp))
                            .background(VigiaWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.vigia_logo),
                            contentDescription = "VIGIA AI Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(112.dp)
                                .clip(RoundedCornerShape(26.dp))
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                AnimatedVisibility(
                    visible = showText,
                    enter = fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 4 }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Bienvenue sur",
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = VigiaTextSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "VIGIA",
                                style = MaterialTheme.typography.headlineLarge,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                color = VigiaTextPrimary,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(VigiaPrimary.copy(alpha = 0.12f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text("AI", fontSize = 14.sp, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.ExtraBold, color = VigiaPrimary)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Le bouclier d'intelligence cyber pour neutraliser les arnaques financières, les faux recruteurs et les liens piégés.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = PoppinsFontFamily,
                            color = VigiaTextSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        Spacer(Modifier.height(24.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DotStep(active = true)
                            DotStep(active = false)
                            DotStep(active = false)
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(
                visible = showButtons,
                enter = fadeIn(tween(380)) + slideInVertically(tween(380)) { it / 5 }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GradientButton(
                        text = "Commencer l'expérience",
                        onClick = onDiscover,
                        icon = Icons.Rounded.ArrowForward,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = onHaveAccount,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, VigiaBorder)
                    ) {
                        Text(
                            "J'ai déjà un compte",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = VigiaTextPrimary,
                            fontSize = 13.5.sp
                        )
                    }
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
            .width(if (active) 22.dp else 7.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (active) VigiaPrimary else VigiaBorder)
    )
}
