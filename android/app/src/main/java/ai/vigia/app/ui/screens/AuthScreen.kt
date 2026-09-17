package ai.vigia.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.R
import ai.vigia.app.ui.components.ErrorBanner
import ai.vigia.app.ui.components.GlassCard
import ai.vigia.app.ui.components.GradientButton
import ai.vigia.app.ui.components.VigiaField
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 42.dp, bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Halo lumineux derrière le logo
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF))
                )
                Image(
                    painter = painterResource(R.drawable.vigia_logo),
                    contentDescription = "VIGIA AI Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(26.dp))
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
                        .background(Color(0xFFEFF6FF))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("AI", fontSize = 12.sp, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, color = VigiaPrimary)
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "Protection avancée contre le phishing et la fraude numérique.",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Carte contenant le formulaire
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = VigiaBorder
            ) {
                TabRow(
                    selectedTabIndex = if (isRegister) 1 else 0,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = VigiaPrimary
                ) {
                    Tab(
                        selected = !isRegister,
                        onClick = { isRegister = false; viewModel.clearError() },
                        text = { Text("Connexion", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = isRegister,
                        onClick = { isRegister = true; viewModel.clearError() },
                        text = { Text("Créer un Compte", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(Modifier.height(20.dp))

                if (isRegister) {
                    VigiaField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Nom complet (optionnel)"
                    )
                    Spacer(Modifier.height(12.dp))
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

                if (isRegister) {
                    Spacer(Modifier.height(12.dp))

                    VigiaField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = "Confirmer le mot de passe",
                        isPassword = true
                    )
                }

                state.error?.let {
                    Spacer(Modifier.height(14.dp))
                    ErrorBanner(it)
                }

                Spacer(Modifier.height(22.dp))

                GradientButton(
                    text = if (isRegister) "Créer mon Compte Sécurisé" else "Se Connecter",
                    onClick = {
                        if (isRegister) viewModel.register(email, password, confirm, name)
                        else viewModel.login(email, password)
                    },
                    enabled = !state.loading,
                    loading = state.loading,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = VigiaTextMuted,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Souveraineté des données • Chiffrement SHA-256",
                    fontSize = 11.5.sp,
                    fontFamily = PoppinsFontFamily,
                    color = VigiaTextMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

