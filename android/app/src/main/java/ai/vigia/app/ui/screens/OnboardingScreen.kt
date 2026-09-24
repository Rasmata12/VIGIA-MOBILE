package ai.vigia.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
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
import ai.vigia.app.ui.components.SubtleBackButton
import ai.vigia.app.ui.theme.*

private data class OnboardingSlide(
    val image: Int,
    val badge: String,
    val badgeColor: Color,
    val heading: String,
    val description: String
)

private val SLIDES = listOf(
    OnboardingSlide(
        image = R.drawable.onboard_phishing,
        badge = "DÉTECTION PHISHING L1-L6",
        badgeColor = VigiaPrimary,
        heading = "Neutralisez les Liens & SMS Pièges",
        description = "Inspection forensique approfondie des liens suspects reçus par SMS, WhatsApp et messages financiers avant tout clic imprudent."
    ),
    OnboardingSlide(
        image = R.drawable.onboard_payment,
        badge = "BEFORE PAY™ ANTI-FRAUDE",
        badgeColor = RiskDanger,
        heading = "Sécurisez vos Transferts d'Argent",
        description = "Auditez instantanément le destinataire, le montant et le prétexte avant d'envoyer de l'argent par Wave, Orange Money ou virement."
    ),
    OnboardingSlide(
        image = R.drawable.onboard_device,
        badge = "PROTECTION 24/7 EN ARRIÈRE-PLAN",
        badgeColor = VigiaCyan,
        heading = "Votre Appareil Sous Haute Garde",
        description = "Un bouclier discret qui tourne en tâche de fond, sans jamais ralentir votre téléphone ni accéder à vos données personnelles."
    ),
    OnboardingSlide(
        image = R.drawable.onboard_shield,
        badge = "SCANNER CAMÉRA & GUARD ACTIF",
        badgeColor = RiskSafe,
        heading = "Flashing & Détection d'Erreurs",
        description = "Scannez les QR codes et factures douteuses avec le scanner Google intégré pour détecter les redirections malveillantes en 1 seconde."
    )
)

@Composable
fun OnboardingScreen(
    onBackToWelcome: () -> Unit,
    onFinished: () -> Unit
) {
    var index by remember { mutableStateOf(0) }
    var slideDirection by remember { mutableStateOf(1) } // 1 for next, -1 for back
    val isLast = index == SLIDES.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Halos d'ambiance doux en haut et bas
        GlowOrb(
            color = SLIDES[index].badgeColor,
            intensity = 0.14f,
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-80).dp)
        )
        GlowOrb(
            color = VigiaPrimary,
            intensity = 0.08f,
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 80.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Barre supérieure : Flèche de retour subtile + Passer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 18.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flèche de retour subtile pour voir les écrans précédents
                if (index > 0) {
                    SubtleBackButton(
                        onBack = {
                            slideDirection = -1
                            index--
                        },
                        label = "Précédent"
                    )
                } else {
                    Box(Modifier.size(40.dp)) // Espace réservé pour équilibre visuel
                }

                // Stepper pill élégant : "1 sur 4"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, VigiaBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Étape ${index + 1} sur ${SLIDES.size}",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = VigiaTextPrimary
                    )
                }

                TextButton(onClick = onFinished) {
                    Text(
                        "Passer",
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Corps central défilant
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = index,
                    transitionSpec = {
                        if (slideDirection > 0) {
                            (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(260))) togetherWith
                                (slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(200)))
                        } else {
                            (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it } + fadeIn(tween(260))) togetherWith
                                (slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200)))
                        }
                    },
                    label = "onboardingSlideAnimation"
                ) { i ->
                    val slide = SLIDES[i]
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Carte visuelle principale avec cadre spéculaire
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.15f)
                                .shadow(
                                    elevation = 18.dp,
                                    shape = RoundedCornerShape(32.dp),
                                    ambientColor = slide.badgeColor.copy(alpha = 0.20f),
                                    spotColor = slide.badgeColor.copy(alpha = 0.15f)
                                )
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(
                                    width = 1.2.dp,
                                    brush = SolidColor(slide.badgeColor.copy(alpha = 0.35f)),
                                    shape = RoundedCornerShape(32.dp)
                                )
                        ) {
                            Image(
                                painter = painterResource(id = slide.image),
                                contentDescription = slide.heading,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Badge flottant sur l'image
                            Box(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .align(Alignment.TopStart)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.92f))
                                    .border(1.dp, slide.badgeColor.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(slide.badgeColor)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = slide.badge,
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.5.sp,
                                        color = slide.badgeColor,
                                        letterSpacing = 0.7.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(28.dp))

                        Text(
                            text = slide.heading,
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            color = VigiaTextPrimary,
                            fontSize = 21.sp,
                            lineHeight = 28.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = slide.description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = PoppinsFontFamily,
                            color = VigiaTextSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Barres de progression réactives (indicateur étendu)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SLIDES.indices.forEach { i ->
                        val isCurrent = i == index
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .width(if (isCurrent) 28.dp else 8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isCurrent)
                                        SolidColor(VigiaPrimary)
                                    else
                                        SolidColor(Color(0xFFCBD5E1))
                                )
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            // Bouton principal en bas
            GradientButton(
                text = if (isLast) "Démarrer la Protection →" else "Continuer",
                onClick = {
                    if (isLast) {
                        onFinished()
                    } else {
                        slideDirection = 1
                        index++
                    }
                },
                icon = if (isLast) null else Icons.Rounded.ArrowForward,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            )
        }
    }
}
