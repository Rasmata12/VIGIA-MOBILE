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
    "emploi" to "Faux Recrutement",
    "annonce" to "Arnaque Annonce",
    "paiement" to "Fraude Mobile Money",
    "phishing" to "Lien Piégé",
    "autre" to "Autre Menace"
)

private fun communityRiskLabel(code: String): String = when (code) {
    "tres_signale" -> "Signalements nombreux"
    "suspect" -> "Menace Suspecte Signalée"
    "a_surveiller" -> "Signalement Récent à Surveiller"
    else -> "Aucun Signalement Connu à ce Jour"
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

    val emeraldColor = Color(0xFF059669)

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
            InfoChip("Réseau Citoyen", emeraldColor)
        }

        // Titre & Sous-titre
        Column {
            Text(
                "Protection Collective",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = VigiaTextPrimary,
                fontSize = 24.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Signalez les numéros frauduleux et protégez l'ensemble de la communauté.",
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 13.sp
            )
        }

        // HÉROS — Impact de la vigilance partagée
        HeroSurface(orbColors = listOf(emeraldColor, VigiaPrimary), cornerRadius = 20.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(icon = Icons.Rounded.Groups, tint = emeraldColor, size = 40.dp, iconSize = 20.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Bouclier d'immunité collective",
                        fontWeight = FontWeight.Bold,
                        fontFamily = PoppinsFontFamily,
                        color = emeraldColor,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Un signalement est agrégé avec ceux d'autres utilisateurs et peut devenir un signal supplémentaire dans les analyses VIGIA.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Onglets Luxe Vérifier / Signaler
        GlassCard(
            cornerRadius = 18.dp,
            contentPadding = PaddingValues(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CommunityTabButton(
                    label = "Interroger la base",
                    icon = Icons.Rounded.Search,
                    selected = tab == 0,
                    modifier = Modifier.weight(1f)
                ) {
                    tab = 0
                    viewModel.reset()
                }
                CommunityTabButton(
                    label = "Signaler une menace",
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
            // TAB 0 : VÉRIFIER
            GlassCard(
                backgroundColor = Color.White,
                borderColor = VigiaBorder,
                cornerRadius = 24.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon = Icons.Rounded.TravelExplore, tint = VigiaPrimary, size = 36.dp, iconSize = 18.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Vérifier un contact ou lien",
                            fontWeight = FontWeight.Bold,
                            fontFamily = PoppinsFontFamily,
                            color = VigiaTextPrimary,
                            fontSize = 15.sp
                        )
                    }
                    TextButton(onClick = {
                        checkTarget = "+225 07 88 99 00"
                    }) {
                        Text(
                            "Exemple",
                            fontFamily = PoppinsFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = VigiaPrimary
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Saisissez un numéro de téléphone, un domaine ou une URL pour consulter les signalements communautaires connus.",
                    fontFamily = PoppinsFontFamily,
                    color = VigiaTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(Modifier.height(14.dp))

                VigiaField(
                    value = checkTarget,
                    onValueChange = { checkTarget = it },
                    label = "Numéro, domaine ou URL suspecte",
                    supporting = "Ex: +225 07..., 05..., ou https://..."
                )

                Spacer(Modifier.height(18.dp))

                GradientButton(
                    text = "Interroger le registre communautaire",
                    onClick = { viewModel.check(checkTarget) },
                    loading = state.loading,
                    enabled = checkTarget.isNotBlank(),
                    icon = Icons.Rounded.Shield,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            state.checkResult?.let { res ->
                val riskColor = communityRiskColor(res.riskFromReports)
                GlassCard(
                    backgroundColor = Color.White,
                    borderColor = riskColor.copy(alpha = 0.35f),
                    cornerRadius = 24.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            icon = if (res.reporters > 0) Icons.Rounded.ReportProblem else Icons.Rounded.CheckCircle,
                            tint = riskColor,
                            size = 44.dp,
                            iconSize = 22.dp
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                communityRiskLabel(res.riskFromReports),
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = PoppinsFontFamily,
                                color = riskColor,
                                fontSize = 14.5.sp
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                if (res.reporters > 0)
                                    "${res.reporters} citoyen(s) ont formellement signalé cette cible"
                                else
                                    "Aucune plainte enregistrée à ce jour dans la base",
                                fontFamily = PoppinsFontFamily,
                                color = VigiaTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (res.byCategory.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Répartition des signalements :",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = VigiaTextPrimary,
                            fontSize = 12.5.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            res.byCategory.forEach { (cat, count) ->
                                InfoChip("$cat : $count", riskColor)
                            }
                        }
                    }
                }
            }
        } else {
            // TAB 1 : SIGNALER
            GlassCard(
                backgroundColor = Color.White,
                borderColor = RiskDanger.copy(alpha = 0.35f),
                cornerRadius = 24.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon = Icons.Rounded.NotificationImportant, tint = RiskDanger, size = 36.dp, iconSize = 18.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Dénoncer une tentative",
                            fontWeight = FontWeight.Bold,
                            fontFamily = PoppinsFontFamily,
                            color = VigiaTextPrimary,
                            fontSize = 15.sp
                        )
                    }
                    TextButton(onClick = {
                        reportTarget = "+225 05 12 34 56"
                        reportCategory = "paiement"
                        reportDescription = "M'a appelé en se faisant passer pour un agent Wave demandant l'annulation d'un transfert frauduleux pour obtenir mon code secret."
                    }) {
                        Text(
                            "Exemple type",
                            fontFamily = PoppinsFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = RiskDanger
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Votre signalement est confidentiel. Une même cible ne peut être comptée qu’une fois par utilisateur.",
                    fontFamily = PoppinsFontFamily,
                    color = VigiaTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(Modifier.height(14.dp))

                VigiaField(
                    value = reportTarget,
                    onValueChange = { reportTarget = it },
                    label = "Numéro de téléphone, domaine ou URL à signaler",
                    supporting = "Ex: +225 07... ou https://login-faux..."
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    "Nature de l'escroquerie",
                    fontFamily = PoppinsFontFamily,
                    fontSize = 12.5.sp,
                    color = VigiaTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        REPORT_CATEGORIES.take(2).forEach { (key, label) ->
                            val selected = reportCategory == key
                            FilterChip(
                                selected = selected,
                                onClick = { reportCategory = key },
                                label = {
                                    Text(
                                        label,
                                        fontFamily = PoppinsFontFamily,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RiskDanger,
                                    selectedLabelColor = Color.White,
                                    containerColor = VigiaSurfaceHigh,
                                    labelColor = VigiaTextSecondary
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        REPORT_CATEGORIES.drop(2).forEach { (key, label) ->
                            val selected = reportCategory == key
                            FilterChip(
                                selected = selected,
                                onClick = { reportCategory = key },
                                label = {
                                    Text(
                                        label,
                                        fontFamily = PoppinsFontFamily,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RiskDanger,
                                    selectedLabelColor = Color.White,
                                    containerColor = VigiaSurfaceHigh,
                                    labelColor = VigiaTextSecondary
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                VigiaField(
                    value = reportDescription,
                    onValueChange = { reportDescription = it },
                    label = "Détails de l'approche (optionnel)",
                    minLines = 3,
                    singleLine = false,
                    supporting = "Décrivez le mode opératoire (ne partagez aucune donnée personnelle)"
                )

                Spacer(Modifier.height(18.dp))

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
                val res = state.reportResult!!
                GlassCard(
                    backgroundColor = Color.White,
                    borderColor = RiskSafe.copy(alpha = 0.35f),
                    cornerRadius = 24.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon = Icons.Rounded.VerifiedUser, tint = RiskSafe, size = 44.dp, iconSize = 22.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                "Signalement Enregistré avec Succès",
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = PoppinsFontFamily,
                                color = RiskSafe,
                                fontSize = 14.5.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Cette cible compte maintenant ${res.communityReporters} signalement(s) provenant d’utilisateurs distincts. Ce signal est pris en compte dans les analyses VIGIA.",
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

        state.trending?.let { trend ->
            if (trend.items.isNotEmpty()) {
                SectionHeader("Cibles récemment signalées", "Cibles avec au moins deux signalants distincts sur les 30 derniers jours — ce n'est pas une preuve à elle seule.")
                trend.items.forEach { item ->
                    GlassCard(backgroundColor = Color.White, borderColor = VigiaBorder, contentPadding = PaddingValues(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Rounded.ReportProblem, RiskDanger, size = 36.dp, iconSize = 18.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.targetKey, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = VigiaTextPrimary, softWrap = true)
                                Text("${item.reporters} signalants • ${item.topCategory}", fontFamily = PoppinsFontFamily, fontSize = 11.sp, color = VigiaTextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // Charte citoyenne
        GlassCard(
            backgroundColor = Color.White,
            borderColor = VigiaSecondary.copy(alpha = 0.35f),
            cornerRadius = 24.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = Icons.Rounded.SecurityUpdateGood, tint = VigiaSecondary, size = 36.dp, iconSize = 18.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Charte de Signalement Responsable",
                    fontWeight = FontWeight.Bold,
                    fontFamily = PoppinsFontFamily,
                    color = VigiaSecondary,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Un signalement communautaire est un indice, pas une preuve absolue. Signalez uniquement une tentative réellement observée et ne publiez jamais de données bancaires ou personnelles.",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                color = fg,
                fontFamily = PoppinsFontFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.5.sp
            )
        }
    }
}
