package ai.vigia.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.R
import ai.vigia.app.ui.components.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.AuthViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AuthScreen(viewModel: AuthViewModel, onAuthenticated: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isRegister by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    LaunchedEffect(state.loggedIn) { if (state.loggedIn) onAuthenticated() }

    Box(Modifier.fillMaxSize().background(BackgroundGradient)) {
        GlowOrb(
            color = VigiaPrimary,
            intensity = 0.16f,
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-90).dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Médaillon de logo sur fond blanc pur
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(136.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = Color(0x12000000),
                        spotColor = Color(0x1C0B1930)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
                    .border(1.dp, VigiaBorderSubtle, RoundedCornerShape(32.dp))
            ) {
                Image(
                    painter = painterResource(R.drawable.vigia_logo),
                    contentDescription = "VIGIA AI Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(108.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "VIGIA",
                    style = MaterialTheme.typography.headlineMedium,
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
                    Text("AI", fontSize = 12.sp, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.ExtraBold, color = VigiaPrimary)
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "Bouclier cyber pour vos paiements et vos messages.",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(24.dp))

            // Carte d'authentification de luxe
            GlassCard(
                backgroundBrush = luxuryCardGradient(VigiaPrimary),
                borderBrush = luxuryBorderGradient(VigiaPrimary),
                cornerRadius = 24.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Onglets personnalisés
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AuthTab(
                        label = "Connexion",
                        selected = !isRegister,
                        modifier = Modifier.weight(1f)
                    ) {
                        isRegister = false
                        viewModel.clearError()
                    }
                    AuthTab(
                        label = "Créer un Compte",
                        selected = isRegister,
                        modifier = Modifier.weight(1f)
                    ) {
                        isRegister = true
                        viewModel.clearError()
                    }
                }

                Spacer(Modifier.height(20.dp))

                AnimatedVisibility(
                    visible = isRegister,
                    enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(140)) + shrinkVertically(tween(140))
                ) {
                    Column {
                        VigiaField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Nom complet (optionnel)"
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }

                VigiaField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Adresse email professionnelle ou perso",
                    keyboardType = KeyboardType.Email
                )

                Spacer(Modifier.height(12.dp))

                VigiaField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Mot de passe",
                    isPassword = true
                )

                AnimatedVisibility(
                    visible = isRegister,
                    enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(140)) + shrinkVertically(tween(140))
                ) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        VigiaField(
                            value = confirm,
                            onValueChange = { confirm = it },
                            label = "Confirmer le mot de passe",
                            isPassword = true
                        )
                    }
                }

                state.error?.let {
                    Spacer(Modifier.height(14.dp))
                    ErrorBanner(it)
                }

                Spacer(Modifier.height(22.dp))

                GradientButton(
                    text = if (isRegister) "Créer mon Compte Sécurisé" else "Se Connecter à VIGIA",
                    onClick = {
                        if (isRegister) viewModel.register(email, password, confirm, name)
                        else viewModel.login(email, password)
                    },
                    enabled = !state.loading,
                    loading = state.loading,
                    icon = if (isRegister) Icons.Rounded.PersonAdd else Icons.Rounded.LockOpen,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(22.dp))

            // Raccourci découverte invité
            TextButton(
                onClick = onAuthenticated,
                colors = ButtonDefaults.textButtonColors(contentColor = VigiaPrimary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Explorer sans compte (Mode Essai)",
                        fontFamily = PoppinsFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VigiaPrimary
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(15.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = VigiaTextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Souveraineté des données • Chiffrement SHA-256 de bout en bout",
                    fontSize = 11.sp,
                    fontFamily = PoppinsFontFamily,
                    color = VigiaTextMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AuthTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) VigiaWhite else Color.Transparent
    val fg = if (selected) VigiaPrimary else VigiaTextSecondary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontFamily = PoppinsFontFamily,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.5.sp,
            color = fg
        )
    }
}
