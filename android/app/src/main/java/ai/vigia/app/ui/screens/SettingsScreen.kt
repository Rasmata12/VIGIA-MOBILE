package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.ui.components.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.SettingsViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onLogout: () -> Unit,
    onDeleted: () -> Unit,
    onOpenPrivacy: () -> Unit = {},
    onOpenPermissions: () -> Unit = {},
    onOpenDevices: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }

    Column(
        Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(top = 24.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text("Paramètres & Sécurité", style = MaterialTheme.typography.headlineMedium, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Configuration des moteurs de protection et gestion du compte.", fontFamily = PoppinsFontFamily, color = VigiaTextSecondary, fontSize = 12.5.sp)
        }

        // Fiche Compte Utilisateur
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = VigiaPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Compte Actif", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 14.5.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(state.email.ifBlank { "Utilisateur VIGIA" }, fontFamily = PoppinsFontFamily, color = VigiaTextSecondary, fontSize = 12.5.sp)
                }
                InfoChip("Sécurisé", RiskSafe)
            }
        }

        // Hub des centres de sécurité avancés
        SectionHeader("Centres de Contrôle & Conformité")

        SettingsNavTile(
            title = "Centre de Confidentialité & RGPD",
            subtitle = "Transparence des données, audit zéro-stockage, purge",
            icon = Icons.Rounded.Shield,
            onClick = onOpenPrivacy
        )

        SettingsNavTile(
            title = "Centre des Permissions Android",
            subtitle = "État des accès Internet, Notifications, Caméra, Guard",
            icon = Icons.Rounded.VpnKey,
            onClick = onOpenPermissions
        )

        SettingsNavTile(
            title = "Gestion des Appareils",
            subtitle = "Terminaux autorisés et révocation de session",
            icon = Icons.Rounded.Smartphone,
            onClick = onOpenDevices
        )

        // État des services et moteurs
        SectionHeader("État des Moteurs de Sécurité")
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Serveur d'analyse VIGIA", fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
                InfoChip(
                    text = when (state.serverReachable) { true -> "En Ligne"; false -> "Injoignable"; null -> "Vérification…" },
                    color = if (state.serverReachable == true) RiskSafe else RiskDanger
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Moteur IA (Claude 3.5 Sonnet)", fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
                InfoChip(
                    text = when (state.aiConfigured) { true -> "Configuré"; false -> "Non configuré"; null -> "Inconnu" },
                    color = if (state.aiConfigured == true) VigiaViolet else VigiaTextMuted
                )
            }
            if (state.aiConfigured == false) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Aucune clé IA n'est définie sur le serveur : le moteur d'analyse utilise les règles heuristiques réelles sans simulation artificielle.",
                    fontSize = 11.5.sp,
                    fontFamily = PoppinsFontFamily,
                    color = VigiaTextMuted
                )
            }
        }

        // Préférences d'analyse et d'alertes
        SectionHeader("Préférences")
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Notifications de menaces", fontWeight = FontWeight.SemiBold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Émises uniquement lorsqu'un risque avéré est détecté.", fontFamily = PoppinsFontFamily, fontSize = 11.5.sp, color = VigiaTextSecondary)
                }
                Switch(
                    checked = state.notifications,
                    onCheckedChange = viewModel::setNotifications,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = VigiaPrimary)
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Analyses IA approfondies", fontWeight = FontWeight.SemiBold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Génère des explications contextuelles via le modèle de langage.", fontFamily = PoppinsFontFamily, fontSize = 11.5.sp, color = VigiaTextSecondary)
                }
                Switch(
                    checked = state.aiEnabled,
                    onCheckedChange = viewModel::setAi,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = VigiaPrimary)
                )
            }
        }

        state.message?.let { ErrorBanner(it) }

        // Déconnexion
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = VigiaPrimary)
        ) {
            Icon(Icons.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Se Déconnecter de la Session", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
        }

        // Zone de suppression de compte
        GlassCard(
            Modifier.fillMaxWidth(),
            borderColor = RiskDangerBorder,
            backgroundColor = RiskDangerBg
        ) {
            Text("Suppression définitive du compte", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = RiskDanger, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Supprime irréversiblement votre compte, vos clés de session et l'intégralité de vos historiques sur le serveur.",
                fontSize = 12.sp,
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary
            )
            Spacer(Modifier.height(12.dp))

            if (!confirmDelete) {
                TextButton(onClick = { confirmDelete = true }) {
                    Text("Supprimer mon compte...", color = RiskDanger, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold)
                }
            } else {
                VigiaField(password, { password = it }, "Confirmez votre mot de passe", isPassword = true)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.deleteAccount(password) },
                    enabled = password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = RiskDanger),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Confirmer la suppression irréversible", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingsNavTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        borderColor = VigiaBorder
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VigiaPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontSize = 11.5.sp, fontFamily = PoppinsFontFamily, color = VigiaTextSecondary)
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = VigiaPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
