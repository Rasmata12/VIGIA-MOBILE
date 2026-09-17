package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.ui.components.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.PrivacyViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PrivacyScreen(
    viewModel: PrivacyViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showConfirmPurge by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Retour", tint = VigiaPrimary)
            }
            Text("Retour", fontFamily = PoppinsFontFamily, color = VigiaPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            InfoChip("Zero-Knowledge & RGPD", VigiaPrimary)
        }

        Column {
            Text("Centre de Confidentialité", style = MaterialTheme.typography.headlineMedium, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Transparence totale et souveraineté absolue sur vos données personnelles.",
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 13.sp
            )
        }

        state.message?.let {
            GlassCard(borderColor = RiskSafeBorder, backgroundColor = RiskSafeBg) {
                Text(it, color = RiskSafe, fontWeight = FontWeight.SemiBold, fontFamily = PoppinsFontFamily, style = MaterialTheme.typography.bodyMedium)
            }
        }

        state.error?.let { ErrorBanner(it) }

        val summary = state.summary
        if (summary != null) {
            // Métriques des données stockées
            SectionHeader("Données Liées à Votre Compte")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile("Analyses", summary.analysesStored.toString(), VigiaPrimary, Modifier.weight(1f))
                StatTile("Alertes", summary.alertsStored.toString(), RiskSuspicious, Modifier.weight(1f))
                StatTile("Appareils", summary.devices.toString(), VigiaViolet, Modifier.weight(1f))
            }

            // Bloc : Ce qui est conservé
            GlassCard(borderColor = Color(0xFFDBEAFE)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Assignment,
                        contentDescription = null,
                        tint = VigiaPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Ce qui est conservé", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaPrimary, fontSize = 14.sp)
                }
                Spacer(Modifier.height(10.dp))
                summary.whatIsStored.forEach { item ->
                    Text("• $item", style = MaterialTheme.typography.bodyMedium, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, modifier = Modifier.padding(vertical = 3.dp))
                }
            }

            // Bloc : Ce qui n'est JAMAIS conservé
            GlassCard(borderColor = RiskSafeBorder, backgroundColor = RiskSafeBg) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = RiskSafe,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Ce qui n'est JAMAIS conservé", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = RiskSafe, fontSize = 14.sp)
                }
                Spacer(Modifier.height(10.dp))
                summary.whatIsNeverStored.forEach { item ->
                    Text("• $item", style = MaterialTheme.typography.bodyMedium, fontFamily = PoppinsFontFamily, color = VigiaTextSecondary, modifier = Modifier.padding(vertical = 3.dp))
                }
            }

            // Politique de rétention
            SectionHeader("Politique de Rétention")
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Délai de purge automatique", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("Actuellement: ${summary.retentionDays} jours", fontSize = 12.sp, fontFamily = PoppinsFontFamily, color = VigiaTextMuted)
                    }
                    TextButton(onClick = { viewModel.applyRetention() }) {
                        Text("Purger maintenant", color = VigiaPrimary, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Actions de souveraineté
            SectionHeader("Actions de Souveraineté")

            OutlinedButton(
                onClick = { /* Export json */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VigiaPrimary)
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Exporter mes données (Format JSON RGPD)", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
            }

            GlassCard(borderColor = RiskDangerBorder, backgroundColor = RiskDangerBg) {
                Text("Suppression intégrale des données", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = RiskDanger, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Efface immédiatement tout l'historique d'analyses, les événements et les alertes sans supprimer votre compte.",
                    fontSize = 12.sp,
                    fontFamily = PoppinsFontFamily,
                    color = VigiaTextSecondary
                )
                Spacer(Modifier.height(12.dp))

                if (!showConfirmPurge) {
                    TextButton(onClick = { showConfirmPurge = true }) {
                        Text("Purger toutes mes données", color = RiskDanger, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                viewModel.purgeAllData()
                                showConfirmPurge = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RiskDanger),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirmer la purge", fontFamily = PoppinsFontFamily)
                        }
                        OutlinedButton(
                            onClick = { showConfirmPurge = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Annuler", fontFamily = PoppinsFontFamily)
                        }
                    }
                }
            }
        }
    }
}
