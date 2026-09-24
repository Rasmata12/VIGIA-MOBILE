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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.ServiceLocator
import ai.vigia.app.ui.components.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.SettingsViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProfileScreen(viewModel: SettingsViewModel, onOpenSettings: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tokenStore = ServiceLocator.tokens
    var hfToken by remember { mutableStateOf(tokenStore.hfToken.orEmpty()) }
    var showToken by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var expandedTip by rememberSaveable { mutableStateOf<Int?>(null) }

    Column(
        Modifier.fillMaxSize().background(VigiaCanvas).verticalScroll(rememberScrollState())
            .padding(20.dp).padding(top = 18.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Profil", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp, color = VigiaTextPrimary)
        Text("Tous les paramètres utiles sont visibles ici.", fontFamily = PoppinsFontFamily, fontSize = 12.5.sp, color = VigiaTextSecondary)

        GlassCard(backgroundColor = Color.White, borderColor = VigiaBorder, cornerRadius = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(VigiaPrimary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.email.ifBlank { "Utilisateur VIGIA" }, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = VigiaTextPrimary)
                    Text("Compte VIGIA", fontFamily = PoppinsFontFamily, fontSize = 11.5.sp, color = VigiaTextSecondary)
                }
            }
        }

        SectionHeader("Moteurs", "État réel du serveur et de l'intelligence artificielle")
        GlassCard(backgroundColor = Color.White, borderColor = VigiaBorder) {
            EngineRow("Serveur VIGIA", when (state.serverReachable) { true -> "En ligne"; false -> "Hors ligne"; null -> "Vérification…" }, state.serverReachable == true)
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = VigiaBorderSubtle)
            Spacer(Modifier.height(10.dp))
            EngineRow("IA VIGIA", when (state.aiConfigured) { true -> "Opérationnelle"; false -> "Non configurée"; null -> "Inconnue" }, state.aiConfigured == true)
        }

        SectionHeader("Clé Hugging Face", "Facultative — stockée chiffrée sur ce téléphone et envoyée uniquement pendant une analyse")
        GlassCard(backgroundColor = Color.White, borderColor = VigiaBorder) {
            OutlinedTextField(
                value = hfToken, onValueChange = { hfToken = it; saved = false },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("hf_…") }, singleLine = true,
                visualTransformation = if (showToken) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                trailingIcon = { IconButton({ showToken = !showToken }) { Icon(if (showToken) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null) } },
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text("Utilisée pour l'analyse IA de textes et pour l'analyse visuelle photo/vidéo. Une clé valide avec accès Inference Providers est nécessaire.", fontFamily = PoppinsFontFamily, fontSize = 11.5.sp, color = VigiaTextSecondary, lineHeight = 16.sp)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { tokenStore.hfToken = hfToken.trim().ifBlank { null }; saved = true },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VigiaPrimary)
            ) { Icon(Icons.Rounded.Save, null); Spacer(Modifier.width(8.dp)); Text(if (saved) "Clé enregistrée" else "Enregistrer la clé", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold) }
        }

        SectionHeader("Préférences", "Contrôles visibles et modifiables sans ouvrir un autre écran")
        GlassCard(backgroundColor = Color.White, borderColor = VigiaBorder) {
            SettingSwitch("Notifications de menace", "Recevoir les alertes VIGIA", state.notifications, viewModel::setNotifications)
            Spacer(Modifier.height(10.dp)); HorizontalDivider(color = VigiaBorderSubtle); Spacer(Modifier.height(10.dp))
            SettingSwitch("Analyses IA approfondies", "Utiliser la couche IA lorsqu'elle est disponible", state.aiEnabled, viewModel::setAi)
        }

        SectionHeader("Conseils de sécurité", "Touchez un conseil pour dérouler les détails")
        listOf(
            "Ne partage jamais un code PIN ou OTP.",
            "Vérifie toujours le domaine avant de payer ou de te connecter.",
            "En cas d'urgence, ouvre toi-même l'application officielle.",
            "Pour une offre d'emploi ou une annonce, vérifie l'entreprise et les frais.",
            "Avant un transfert, confirme le bénéficiaire par un canal connu."
        ).forEachIndexed { index, title ->
            val expanded = expandedTip == index
            GlassCard(
                modifier = Modifier.fillMaxWidth().clickable { expandedTip = if (expanded) null else index },
                backgroundColor = Color.White, borderColor = VigiaBorder, contentPadding = PaddingValues(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.clip(CircleShape).background(VigiaPrimary).padding(horizontal = 8.dp, vertical = 4.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(title, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = VigiaTextPrimary, modifier = Modifier.weight(1f))
                    Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = if (expanded) "Réduire" else "Dérouler", tint = VigiaPrimary)
                }
                AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
                    val details = when (index) {
                        0 -> "VIGIA ne te demandera jamais ton code OTP. Un support légitime n'a pas besoin de connaître ce secret."
                        1 -> "Un cadenas HTTPS ne suffit pas : lis le nom de domaine complet et évite les liens raccourcis ou inattendus."
                        2 -> "Si un message crée une urgence, ferme-le puis ouvre toi-même l'application ou le site officiel depuis tes favoris."
                        3 -> "Une demande de frais avant l'embauche, de dépôt ou de matériel doit être vérifiée indépendamment avant tout paiement."
                        else -> "Avant un transfert Mobile Money ou bancaire, vérifie le nom du bénéficiaire et confirme sur un autre canal."
                    }
                    Text(details, fontFamily = PoppinsFontFamily, fontSize = 12.sp, color = VigiaTextSecondary, lineHeight = 17.sp, modifier = Modifier.padding(start = 42.dp, top = 9.dp))
                }
            }
        }

        state.message?.let { ErrorBanner(it) }
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
