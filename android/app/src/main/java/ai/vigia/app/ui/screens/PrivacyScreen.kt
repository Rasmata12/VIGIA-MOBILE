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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PrivacyScreen(
    viewModel: PrivacyViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showConfirmPurge by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(state.exportedFileUri) {
        val uri = state.exportedFileUri ?: return@LaunchedEffect
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            android.content.Intent.createChooser(intent, "Exporter mes données VIGIA")
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        viewModel.consumeExportedFile()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // En-tête de navigation
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubtleBackButton(onBack = onBack, label = "Retour")
            Spacer(Modifier.weight(1f))
            InfoChip("Zero-Knowledge & RGPD", VigiaPrimary)
        }

        // Titre & Sous-titre
        Column {
            Text(
                "Centre de Confidentialité",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = VigiaTextPrimary,
                fontSize = 24.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Transparence totale et souveraineté absolue sur vos données personnelles et vos audits.",
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            )
        }

        // HÉROS — Souveraineté
        HeroSurface(orbColors = listOf(VigiaPrimary, VigiaEmerald), cornerRadius = 24.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                IconBadge(icon = Icons.Rounded.EnhancedEncryption, tint = VigiaPrimary, size = 44.dp, iconSize = 22.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Architecture Zero-Knowledge",
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaPrimary,
                        fontSize = 14.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Vos analyses sont traitées de manière chiffrée. Vos identifiants bancaires ou numéros confidentiels ne sont jamais stockés en clair sur nos serveurs.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        state.message?.let {
            GlassCard(
                backgroundBrush = luxuryCardGradient(RiskSafe),
                borderBrush = luxuryBorderGradient(RiskSafe),
                cornerRadius = 20.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon = Icons.Rounded.CheckCircle, tint = RiskSafe, size = 32.dp, iconSize = 16.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(it, color = RiskSafe, fontWeight = FontWeight.SemiBold, fontFamily = PoppinsFontFamily, fontSize = 13.sp)
                }
            }
        }

        state.error?.let { ErrorBanner(it) }

        val summary = state.summary
        if (summary != null) {
            SectionHeader("Données Liées à Votre Profil")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile("Analyses", summary.analysesStored.toString(), VigiaPrimary, Modifier.weight(1f))
                StatTile("Alertes", summary.alertsStored.toString(), RiskSuspicious, Modifier.weight(1f))
                StatTile("Appareils", summary.devices.toString(), VigiaViolet, Modifier.weight(1f))
            }

            // Bloc : Ce qui est conservé
            GlassCard(
                backgroundBrush = luxuryCardGradient(VigiaPrimary),
                borderBrush = luxuryBorderGradient(VigiaPrimary),
                cornerRadius = 22.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon = Icons.Rounded.Assignment, tint = VigiaPrimary, size = 36.dp, iconSize = 18.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Ce qui est conservé pour votre protection", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaPrimary, fontSize = 14.5.sp)
                }
                Spacer(Modifier.height(12.dp))
                summary.whatIsStored.forEach { item ->
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                        Text("•", color = VigiaPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(item, style = MaterialTheme.typography.bodyMedium, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 12.5.sp)
                    }
                }
            }

            // Bloc : Ce qui n'est JAMAIS conservé
            GlassCard(
                backgroundBrush = luxuryCardGradient(RiskSafe),
                borderBrush = luxuryBorderGradient(RiskSafe),
                cornerRadius = 22.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon = Icons.Rounded.VerifiedUser, tint = RiskSafe, size = 36.dp, iconSize = 18.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Ce qui n'est JAMAIS enregistré", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = RiskSafe, fontSize = 14.5.sp)
                }
                Spacer(Modifier.height(12.dp))
                summary.whatIsNeverStored.forEach { item ->
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                        Text("✓", color = RiskSafe, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(item, style = MaterialTheme.typography.bodyMedium, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 12.5.sp)
                    }
                }
            }

            // Politique de rétention
            SectionHeader("Rétention & Cycle de Vie")
            GlassCard(
                backgroundBrush = luxuryCardGradient(VigiaSecondary),
                borderBrush = luxuryBorderGradient(VigiaSecondary),
                cornerRadius = 22.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Délai de purge automatique", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 14.sp)
                        Spacer(Modifier.height(3.dp))
                        Text("Conservation maximale : ${summary.retentionDays} jours glissants", fontSize = 12.sp, fontFamily = PoppinsFontFamily, color = VigiaTextMuted)
                    }
                    OutlinedButton(
                        onClick = { viewModel.applyRetention() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VigiaPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Purger", color = VigiaPrimary, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Actions de souveraineté
            SectionHeader("Droits et Souveraineté")

            GradientButton(
                text = if (state.exporting) "Exportation en cours…" else "Exporter mes données (JSON RGPD)",
                onClick = { viewModel.exportData(context) },
                enabled = !state.exporting,
                loading = state.exporting,
                icon = Icons.Rounded.FileDownload,
                modifier = Modifier.fillMaxWidth()
            )

            GlassCard(
                backgroundBrush = luxuryCardGradient(RiskDanger),
                borderBrush = luxuryBorderGradient(RiskDanger),
                cornerRadius = 22.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon = Icons.Rounded.DeleteForever, tint = RiskDanger, size = 36.dp, iconSize = 18.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Suppression intégrale des données", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = RiskDanger, fontSize = 14.5.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Efface immédiatement et irréversiblement tout l'historique d'analyses, les alertes et les métadonnées sans supprimer votre compte.",
                    fontSize = 12.sp,
                    fontFamily = PoppinsFontFamily,
                    color = VigiaTextSecondary,
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(14.dp))

                if (!showConfirmPurge) {
                    OutlinedButton(
                        onClick = { showConfirmPurge = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RiskDanger),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Purger toutes mes données maintenant", color = RiskDanger, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                viewModel.purgeAllData()
                                showConfirmPurge = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RiskDanger),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirmer la purge", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { showConfirmPurge = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Annuler", fontFamily = PoppinsFontFamily, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
