package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.ui.components.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.SettingsViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProfileScreen(viewModel: SettingsViewModel, onOpenSettings: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    LaunchedEffect(state.email, state.fullName) {
        email = state.email
        fullName = state.fullName
    }
    LaunchedEffect(state.message, state.profileSaving) {
        if (!state.profileSaving && state.message == "Ton compte a été mis à jour.") editing = false
    }
    Column(
        Modifier.fillMaxSize().background(VigiaCanvas).verticalScroll(rememberScrollState())
            .padding(20.dp).padding(top = 18.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Profil", Modifier.weight(1f), fontFamily = PoppinsFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp, color = VigiaTextPrimary)
            TextButton(onClick = {
                if (editing) {
                    email = state.email
                    fullName = state.fullName
                }
                editing = !editing
            }) {
                Icon(if (editing) Icons.Rounded.Close else Icons.Rounded.Edit, null)
                Spacer(Modifier.width(5.dp))
                Text(if (editing) "Annuler" else "Modifier")
            }
        }

        GlassCard(backgroundColor = Color.White, borderColor = VigiaBorder, cornerRadius = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(VigiaPrimary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    if (editing) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            VigiaField(fullName, { fullName = it }, "Nom")
                            VigiaField(email, { email = it }, "Adresse e-mail", keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)
                        }
                    } else {
                        Text(state.fullName.ifBlank { "Utilisateur VIGIA" }, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = VigiaTextPrimary)
                        Text(state.email, fontFamily = PoppinsFontFamily, fontSize = 11.5.sp, color = VigiaTextSecondary)
                    }
                }
            }
            if (editing) {
                Spacer(Modifier.height(12.dp))
                GradientButton(
                    text = "Enregistrer les changements",
                    onClick = { viewModel.updateProfile(email, fullName) },
                    enabled = !state.profileSaving,
                    loading = state.profileSaving,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        SectionHeader("État des services")
        GlassCard(backgroundColor = Color.White, borderColor = VigiaBorder) {
            EngineRow("Serveur VIGIA", when (state.serverReachable) { true -> "En ligne"; false -> "Hors ligne"; null -> "Vérification…" }, state.serverReachable == true)
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = VigiaBorderSubtle)
            Spacer(Modifier.height(10.dp))
            EngineRow("IA VIGIA", when (state.aiConfigured) { true -> "Opérationnelle"; false -> "Non configurée"; null -> "Inconnue" }, state.aiConfigured == true)
        }

        SectionHeader("Préférences")
        GlassCard(backgroundColor = Color.White, borderColor = VigiaBorder) {
            SettingSwitch("Notifications de menace", "Recevoir les alertes VIGIA", state.notifications, viewModel::setNotifications)
            Spacer(Modifier.height(10.dp)); HorizontalDivider(color = VigiaBorderSubtle); Spacer(Modifier.height(10.dp))
            SettingSwitch("Analyses IA approfondies", "Utiliser la couche IA lorsqu'elle est disponible", state.aiEnabled, viewModel::setAi)
        }

        state.message?.let {
            if (it == "Ton compte a été mis à jour.") {
                Text(it, fontFamily = PoppinsFontFamily, fontSize = 12.sp, color = RiskSafe, modifier = Modifier.padding(horizontal = 4.dp))
            } else ErrorBanner(it)
        }
        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Security, null); Spacer(Modifier.width(8.dp)); Text("Confidentialité, appareils et suppression du compte") }
    }
}

@Composable private fun EngineRow(label: String, value: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (ok) RiskSafe else RiskDanger)); Spacer(Modifier.width(10.dp)); Text(label, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold, color = VigiaTextPrimary, modifier = Modifier.weight(1f)); InfoChip(value, if (ok) RiskSafe else RiskDanger)
    }
}

@Composable private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = VigiaTextPrimary); Text(subtitle, fontFamily = PoppinsFontFamily, fontSize = 11.sp, color = VigiaTextSecondary) }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = VigiaPrimary))
    }
}
