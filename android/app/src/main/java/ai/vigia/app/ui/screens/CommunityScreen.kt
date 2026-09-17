package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
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
import ai.vigia.app.ui.vm.CommunityViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val REPORT_CATEGORIES = listOf(
    "emploi" to "Emploi",
    "annonce" to "Annonce",
    "paiement" to "Paiement",
    "phishing" to "Phishing",
    "autre" to "Autre"
)

private fun communityRiskLabel(code: String): String = when (code) {
    "tres_signale" -> "Très signalé par la communauté"
    "suspect" -> "Signalé comme suspect"
    "a_surveiller" -> "À surveiller"
    else -> "Aucun signalement connu"
}

private fun communityRiskColor(code: String): Color = when (code) {
    "tres_signale" -> RiskDanger
    "suspect" -> RiskDanger
    "a_surveiller" -> RiskSuspicious
    else -> RiskSafe
}

@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(0) } // 0 = verifier, 1 = signaler

    var checkTarget by remember { mutableStateOf("") }
    var reportTarget by remember { mutableStateOf("") }
    var reportCategory by remember { mutableStateOf("autre") }
    var reportDescription by remember { mutableStateOf("") }

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
            InfoChip("Espace communautaire", RiskSafe)
        }

        Column {
            Text("Protection collective", style = MaterialTheme.typography.headlineMedium, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Chaque signalement croisé protège automatiquement les autres utilisateurs, même s'ils ne consultent jamais les signalements.",
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 13.sp
            )
        }

        // Onglets Vérifier / Signaler
        GlassCard(cornerRadius = 16.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CommunityTabButton("Vérifier", Icons.Rounded.Search, tab == 0, Modifier.weight(1f)) { tab = 0; viewModel.reset() }
                CommunityTabButton("Signaler", Icons.Rounded.Flag, tab == 1, Modifier.weight(1f)) { tab = 1; viewModel.reset() }
            }
        }

        if (tab == 0) {
            GlassCard {
                Text("Vérifier avant d'agir", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Colle un lien, un domaine ou un numéro de téléphone pour voir s'il a déjà été signalé.",
                    fontFamily = PoppinsFontFamily, color = VigiaTextSecondary, fontSize = 12.5.sp
                )
                Spacer(Modifier.height(12.dp))
                VigiaField(
                    value = checkTarget,
                    onValueChange = { checkTarget = it },
                    label = "Lien, domaine ou numéro"
                )
                Spacer(Modifier.height(16.dp))
                GradientButton(
                    text = "Vérifier dans la communauté",
                    onClick = { viewModel.check(checkTarget) },
                    loading = state.loading,
                    enabled = checkTarget.isNotBlank(),
                    icon = Icons.Rounded.TravelExplore,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            state.checkResult?.let { res ->
                GlassCard(
                    borderColor = communityRiskColor(res.riskFromReports).copy(alpha = 0.4f),
                    backgroundColor = communityRiskColor(res.riskFromReports).copy(alpha = 0.06f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (res.reporters > 0) Icons.Rounded.ReportProblem else Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = communityRiskColor(res.riskFromReports),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                communityRiskLabel(res.riskFromReports),
                                fontWeight = FontWeight.Bold,
                                fontFamily = PoppinsFontFamily,
                                color = communityRiskColor(res.riskFromReports),
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "${res.reporters} signalement(s) par des utilisateurs distincts",
                                fontFamily = PoppinsFontFamily,
                                color = VigiaTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    if (res.byCategory.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            res.byCategory.forEach { (cat, count) ->
                                InfoChip("$cat: $count", VigiaPrimary)
                            }
                        }
                    }
                }
            }
        } else {
            GlassCard {
                Text("Signaler une cible dangereuse", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Ton signalement est comptabilisé une seule fois, même si tu le refais plusieurs fois.",
                    fontFamily = PoppinsFontFamily, color = VigiaTextSecondary, fontSize = 12.5.sp
                )
                Spacer(Modifier.height(12.dp))

                VigiaField(
                    value = reportTarget,
                    onValueChange = { reportTarget = it },
                    label = "Lien, domaine ou numéro à signaler"
                )

                Spacer(Modifier.height(12.dp))
                Text("Catégorie", fontFamily = PoppinsFontFamily, fontSize = 13.sp, color = VigiaTextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    REPORT_CATEGORIES.forEach { (key, label) ->
                        val selected = reportCategory == key
                        FilterChip(
                            selected = selected,
                            onClick = { reportCategory = key },
                            label = { Text(label, fontFamily = PoppinsFontFamily, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VigiaPrimary, selectedLabelColor = Color.White)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                VigiaField(
                    value = reportDescription,
                    onValueChange = { reportDescription = it },
                    label = "Description (optionnel)",
                    minLines = 2,
                    singleLine = false,
                    supporting = "Ce qui s'est passé, sans donnée personnelle"
                )

                Spacer(Modifier.height(16.dp))

                GradientButton(
                    text = "Envoyer le signalement",
                    onClick = { viewModel.report(reportTarget, reportCategory, reportDescription) },
                    loading = state.loading,
                    enabled = reportTarget.isNotBlank(),
                    icon = Icons.Rounded.Flag,
                    gradient = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(RiskSuspicious, RiskDanger)),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.reportSent && state.reportResult != null) {
                val res = state.reportResult!!
                GlassCard(borderColor = RiskSafeBorder, backgroundColor = RiskSafeBg) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = RiskSafe, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Signalement enregistré", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = RiskSafe, fontSize = 14.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${res.communityReporters} personne(s) ont signalé cette cible au total. Merci, tu protèges la communauté.",
                                fontFamily = PoppinsFontFamily, color = VigiaTextSecondary, fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        state.error?.let { ErrorBanner(it) }
    }
}

@Composable
private fun CommunityTabButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) VigiaPrimary else Color.Transparent
    val fg = if (selected) Color.White else VigiaTextSecondary
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = fg, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}
