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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
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
            .padding(top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column {
            Text(
                "Paramètres & Sécurité",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = VigiaTextPrimary,
                fontSize = 24.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Supervision des moteurs de protection, centres de conformité et gestion du profil.",
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            )
        }

        // ------------------------------------------------------ Carte compte
        HeroSurface(orbColors = listOf(VigiaPrimary, VigiaSecondary), cornerRadius = 26.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SolidColor(VigiaPrimary)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Rounded.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Compte VIGIA Protégé",
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextPrimary,
                        fontSize = 14.5.sp
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        state.email.ifBlank { "Utilisateur Souverain" },
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextSecondary,
                        fontSize = 12.sp
                    )
                }
                InfoChip("Sécurisé", RiskSafe)
            }
        }

        // ------------------------------------------------------ Centres de controle
        Column {
            SectionHeader("Centres de Contrôle & Souveraineté")
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsNavTile(
                    title = "Centre de Confidentialité & RGPD",
                    subtitle = "Transparence des données, audit zéro-stockage, purge",
                    icon = Icons.Rounded.Shield,
                    color = VigiaViolet,
                    onClick = onOpenPrivacy
                )
                SettingsNavTile(
                    title = "Centre des Permissions Android",
                    subtitle = "Moindre privilège, caméra scanner, Guard",
                    icon = Icons.Rounded.VpnKey,
                    color = VigiaCyan,
                    onClick = onOpenPermissions
                )
                SettingsNavTile(
                    title = "Gestion des Appareils Synchronisés",
                    subtitle = "Terminaux autorisés et révocation de session",
                    icon = Icons.Rounded.Smartphone,
                    color = VigiaSecondary,
                    onClick = onOpenDevices
                )
            }
        }

        // ------------------------------------------------------ Etat des moteurs
        Column {
            SectionHeader("Santé des Moteurs Forensiques")
            Spacer(Modifier.height(10.dp))
            GlassCard(
                backgroundBrush = luxuryCardGradient(VigiaPrimary),
                borderBrush = luxuryBorderGradient(VigiaPrimary),
                cornerRadius = 22.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                EngineStatusRow(
                    label = "Serveur d'analyse VIGIA",
                    status = when (state.serverReachable) { true -> "En Ligne"; false -> "Injoignable"; null -> "Vérification…" },
                    ok = state.serverReachable == true
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = VigiaBorderSubtle)
                Spacer(Modifier.height(8.dp))
                EngineStatusRow(
                    label = "Moteur IA Neural Forensique",
                    status = when (state.aiConfigured) { true -> "Opérationnel"; false -> "Moteur Heuristique"; null -> "Inconnu" },
                    ok = state.aiConfigured == true
                )
                if (state.aiConfigured == false) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Le moteur d'analyse utilise les règles de détection heuristiques en local pour une confidentialité maximale.",
                        fontSize = 11.5.sp,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextMuted,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // ------------------------------------------------------ Préférences
        Column {
            SectionHeader("Préférences de Détection")
            Spacer(Modifier.height(10.dp))
            GlassCard(
                backgroundBrush = luxuryCardGradient(VigiaSecondary),
                borderBrush = luxuryBorderGradient(VigiaSecondary),
                cornerRadius = 22.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Notifications de menaces", fontWeight = FontWeight.SemiBold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 14.sp)
                        Spacer(Modifier.height(3.dp))
                        Text("Alertes instantanées uniquement lorsqu'un risque avéré est intercepté.", fontFamily = PoppinsFontFamily, fontSize = 11.5.sp, color = VigiaTextSecondary)
                    }
                    Switch(
                        checked = state.notifications,
                        onCheckedChange = viewModel::setNotifications,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = VigiaPrimary)
                    )
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = VigiaBorderSubtle)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Analyses IA approfondies", fontWeight = FontWeight.SemiBold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 14.sp)
                        Spacer(Modifier.height(3.dp))
                        Text("Génère des explications contextuelles et décortique le Scam DNA.", fontFamily = PoppinsFontFamily, fontSize = 11.5.sp, color = VigiaTextSecondary)
                    }
                    Switch(
                        checked = state.aiEnabled,
                        onCheckedChange = viewModel::setAi,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = VigiaPrimary)
                    )
                }
            }
        }

        state.message?.let { ErrorBanner(it) }

        // ------------------------------------------------------ Déconnexion
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = VigiaPrimary),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, VigiaPrimary.copy(alpha = 0.4f))
        ) {
            Icon(Icons.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Se Déconnecter de la Session", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        // ------------------------------------------------------ Suppression de compte
        GlassCard(
            backgroundBrush = luxuryCardGradient(RiskDanger),
            borderBrush = luxuryBorderGradient(RiskDanger),
            cornerRadius = 24.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = Icons.Rounded.DeleteForever, tint = RiskDanger, size = 38.dp, iconSize = 18.dp)
                Spacer(Modifier.width(10.dp))
                Text("Suppression définitive du compte", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = RiskDanger, fontSize = 14.5.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Supprime irréversiblement votre compte, vos clés cryptographiques et l'intégralité de vos historiques sur le cloud.",
                fontSize = 12.sp,
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(14.dp))

            if (!confirmDelete) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RiskDanger),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Supprimer définitivement mon compte...", color = RiskDanger, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
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
private fun EngineStatusRow(label: String, status: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (ok) RiskSafe else RiskDanger)
        )
        Spacer(Modifier.width(10.dp))
        Text(label, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
        InfoChip(text = status, color = if (ok) RiskSafe else RiskDanger)
    }
}

@Composable
private fun SettingsNavTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        backgroundBrush = luxuryCardGradient(color),
        borderBrush = luxuryBorderGradient(color),
        cornerRadius = 20.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = icon, tint = color, size = 42.dp, iconSize = 20.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 14.sp)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, fontSize = 11.5.sp, fontFamily = PoppinsFontFamily, color = VigiaTextSecondary)
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
