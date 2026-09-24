package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.vigia.app.ui.components.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.CommunityViewModel

private val REPORT_CATEGORIES = listOf(
    "emploi" to "Faux recrutement",
    "annonce" to "Petite annonce",
    "paiement" to "Paiement mobile",
    "phishing" to "Lien piégé",
    "autre" to "Autre menace"
)

private fun communityRiskLabel(code: String): String = when (code) {
    "tres_signale" -> "Plusieurs signalements"
    "suspect" -> "Cible signalée comme suspecte"
    "a_surveiller" -> "Signalement récent"
    else -> "Aucun signalement connu"
}

private fun communityRiskColor(code: String): Color = when (code) {
    "tres_signale", "suspect" -> RiskDanger
    "a_surveiller" -> RiskSuspicious
    else -> RiskSafe
}

@Composable
fun CommunityScreen(viewModel: CommunityViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var checkTarget by rememberSaveable { mutableStateOf("") }
    var reportTarget by rememberSaveable { mutableStateOf("") }
    var reportCategory by rememberSaveable { mutableStateOf("autre") }
    var reportDescription by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SubtleBackButton(onBack = onBack, label = "Retour")

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Communauté",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 25.sp,
                color = VigiaTextPrimary
            )
            Text(
                "Vérifiez une cible ou partagez un signalement utile.",
                fontFamily = PoppinsFontFamily,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = VigiaTextSecondary
            )
        }

        GlassCard(cornerRadius = 16.dp, contentPadding = PaddingValues(5.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                CommunityTabButton(
                    label = "Vérifier",
                    icon = Icons.Rounded.Search,
                    selected = tab == 0,
                    modifier = Modifier.weight(1f)
                ) {
                    tab = 0
                    viewModel.reset()
                }
                CommunityTabButton(
                    label = "Signaler",
                    icon = Icons.Rounded.Flag,
                    selected = tab == 1,
                    modifier = Modifier.weight(1f)
                ) {
                    tab = 1
                    viewModel.reset()
                }
            }
        }

        if (tab == 0) {
            GlassCard(backgroundColor = Color.White, borderColor = VigiaBorder, cornerRadius = 20.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.TravelExplore, VigiaPrimary, size = 38.dp, iconSize = 20.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Vérifier une cible",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = VigiaTextPrimary,
                        fontSize = 16.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Recherchez un numéro, un domaine ou un lien dans les signalements reçus.",
                    fontFamily = PoppinsFontFamily,
                    color = VigiaTextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(14.dp))
                VigiaField(
                    value = checkTarget,
                    onValueChange = { checkTarget = it },
                    label = "Numéro, domaine ou URL",
                    supporting = "Exemple : +225 07… ou https://site-exemple.com"
                )
                Spacer(Modifier.height(14.dp))
                GradientButton(
                    text = "Vérifier cette cible",
                    onClick = { viewModel.check(checkTarget) },
                    loading = state.loading,
                    enabled = checkTarget.isNotBlank(),
                    icon = Icons.Rounded.Shield,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            state.checkResult?.let { result ->
                val risk = communityRiskColor(result.riskFromReports)
                GlassCard(backgroundColor = Color.White, borderColor = risk.copy(alpha = 0.35f), cornerRadius = 20.dp) {
                    Row(verticalAlignment = Alignment.Top) {
                        IconBadge(
                            if (result.reporters > 0) Icons.Rounded.ReportProblem else Icons.Rounded.CheckCircle,
                            risk,
                            size = 40.dp,
                            iconSize = 21.dp
                        )
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                communityRiskLabel(result.riskFromReports),
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                color = risk,
                                fontSize = 14.sp,
                                lineHeight = 19.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (result.reporters > 0) "${result.reporters} utilisateur(s) ont signalé cette cible."
                                else "Aucun signalement communautaire trouvé pour cette cible.",
                                fontFamily = PoppinsFontFamily,
                                color = VigiaTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                    if (result.byCategory.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Catégories signalées",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = VigiaTextPrimary,
                            fontSize = 12.5.sp
                        )
                        Spacer(Modifier.height(7.dp))
                        result.byCategory.entries.toList().chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { (category, count) ->
                                    Text(
                                        "$category · $count",
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(risk.copy(alpha = 0.08f))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        fontFamily = PoppinsFontFamily,
                                        color = risk,
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }

            state.trending?.items?.takeIf { it.isNotEmpty() }?.let { items ->
                SectionHeader(
                    title = "Signalements récents",
                    subtitle = "Plusieurs utilisateurs ont signalé ces cibles. Un signalement n’est pas une preuve."
                )
                items.forEach { item ->
                    GlassCard(backgroundColor = Color.White, borderColor = VigiaBorder, contentPadding = PaddingValues(12.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            IconBadge(Icons.Rounded.ReportProblem, RiskDanger, size = 34.dp, iconSize = 18.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.targetKey,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    color = VigiaTextPrimary,
                                    lineHeight = 17.sp
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    "${item.reporters} signalant(s) · ${item.topCategory}",
                                    fontFamily = PoppinsFontFamily,
                                    fontSize = 11.5.sp,
                                    color = VigiaTextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            GlassCard(backgroundColor = Color.White, borderColor = RiskDanger.copy(alpha = 0.25f), cornerRadius = 20.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.Flag, RiskDanger, size = 38.dp, iconSize = 20.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Signaler une tentative",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = VigiaTextPrimary,
                        fontSize = 16.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Indiquez une cible et choisissez le type de fraude observé.",
                    fontFamily = PoppinsFontFamily,
                    color = VigiaTextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
                TextButton(
                    onClick = {
                        reportTarget = "+225 05 12 34 56"
                        reportCategory = "paiement"
                        reportDescription = "Un appelant se faisait passer pour un agent Mobile Money et demandait mon code secret."
                    },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                ) {
                    Text("Remplir un exemple", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
                }
                VigiaField(
                    value = reportTarget,
                    onValueChange = { reportTarget = it },
                    label = "Numéro, domaine ou URL",
                    supporting = "Ne publiez pas d’information personnelle."
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Type de fraude",
                    fontFamily = PoppinsFontFamily,
                    fontSize = 12.5.sp,
                    color = VigiaTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(7.dp))
                REPORT_CATEGORIES.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        pair.forEach { (key, label) ->
                            val selected = reportCategory == key
                            FilterChip(
                                selected = selected,
                                onClick = { reportCategory = key },
                                modifier = Modifier.weight(1f),
                                label = {
                                    Text(
                                        label,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        fontFamily = PoppinsFontFamily,
                                        fontSize = 11.5.sp,
                                        lineHeight = 15.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VigiaPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = VigiaSurfaceHigh,
                                    labelColor = VigiaTextSecondary
                                )
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(10.dp))
                VigiaField(
                    value = reportDescription,
                    onValueChange = { reportDescription = it },
                    label = "Que s’est-il passé ? (facultatif)",
                    minLines = 3,
                    singleLine = false,
                    supporting = "Décrivez les faits sans ajouter de données bancaires ou privées."
                )
                Spacer(Modifier.height(14.dp))
                GradientButton(
                    text = "Envoyer le signalement",
                    onClick = { viewModel.report(reportTarget, reportCategory, reportDescription) },
                    loading = state.loading,
                    enabled = reportTarget.isNotBlank(),
                    icon = Icons.Rounded.Campaign,
                    gradient = androidx.compose.ui.graphics.SolidColor(RiskDanger),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.reportSent && state.reportResult != null) {
                val result = state.reportResult!!
                GlassCard(backgroundColor = Color.White, borderColor = RiskSafe.copy(alpha = 0.35f), cornerRadius = 20.dp) {
                    Row(verticalAlignment = Alignment.Top) {
                        IconBadge(Icons.Rounded.VerifiedUser, RiskSafe, size = 38.dp, iconSize = 20.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Signalement enregistré",
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = RiskSafe,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Cette cible compte ${result.communityReporters} signalement(s) d’utilisateurs distincts.",
                                fontFamily = PoppinsFontFamily,
                                color = VigiaTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }

        GlassCard(backgroundColor = Color.White, borderColor = VigiaBorder, contentPadding = PaddingValues(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = VigiaPrimary, modifier = Modifier.padding(top = 1.dp))
                Spacer(Modifier.width(9.dp))
                Text(
                    "Les signalements sont des indices, pas des preuves. Signalez uniquement des faits observés et ne publiez aucune information bancaire ou personnelle.",
                    fontFamily = PoppinsFontFamily,
                    color = VigiaTextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
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
    val foreground = if (selected) Color.White else VigiaTextSecondary
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) VigiaPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = foreground,
            fontFamily = PoppinsFontFamily,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}
