package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.local.AnalysisEntity
import ai.vigia.app.net.DayBucketDto
import ai.vigia.app.ui.components.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.DashboardViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAnalyze: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenItem: (String) -> Unit,
    onNavigateToModule: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGradient),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // En-tête de marque avec statut du bouclier
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("VIGIA", style = MaterialTheme.typography.headlineMedium, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.ExtraBold, color = VigiaTextPrimary)
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
                    Text("Bouclier Numérique & Anti-Fraude", fontFamily = PoppinsFontFamily, color = VigiaTextSecondary, fontSize = 12.5.sp)
                }

                InfoChip(
                    text = if (state.offline) "Mode Local" else "Système Actif",
                    color = if (state.offline) RiskSuspicious else RiskSafe
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Actualiser",
                        tint = VigiaPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Bannière mode hors ligne
        if (state.offline) {
            item {
                GlassCard(
                    borderColor = RiskSuspiciousBorder,
                    backgroundColor = RiskSuspiciousBg
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.SignalWifiOff,
                            contentDescription = null,
                            tint = RiskSuspicious,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Mode hors ligne activé", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = RiskSuspicious, fontSize = 13.5.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Le serveur VIGIA est inaccessible. Le moteur d'analyse local continue de vous protéger.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = PoppinsFontFamily,
                                color = VigiaTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Carte principale de score de protection
        item {
            val stats = state.stats
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = VigiaBorder
            ) {
                if (state.loading && stats == null) {
                    Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = VigiaPrimary)
                    }
                } else if (stats != null && stats.totalAnalyses > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RiskGauge(
                            score = stats.protectionScore ?: 0,
                            level = when {
                                (stats.protectionScore ?: 0) >= 70 -> "safe"
                                (stats.protectionScore ?: 0) >= 40 -> "suspicious"
                                else -> "dangerous"
                            },
                            size = 155.dp
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Score de Protection", fontWeight = FontWeight.ExtraBold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 16.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Calculé en temps réel sur la base de vos ${stats.totalAnalyses} analyses réelles et menaces évitées.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = PoppinsFontFamily,
                                color = VigiaTextSecondary,
                                fontSize = 12.5.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            InfoChip(
                                text = "${stats.safe} contenus sûrs",
                                color = RiskSafe
                            )
                        }
                    }
                } else if (stats != null) {
                    EmptyState(
                        title = "Prêt à sécuriser vos échanges",
                        message = "Lancez votre première analyse : collez un lien ou un message pour calculer votre indice de protection."
                    )
                } else {
                    Text("Statistiques serveur indisponibles.", fontFamily = PoppinsFontFamily, color = VigiaTextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text("${state.localTotal} analyse(s) locale(s) enregistrée(s).", fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Métriques clés
        state.stats?.takeIf { it.totalAnalyses > 0 }?.let { stats ->
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile("Analysés", stats.totalAnalyses.toString(), VigiaPrimary, Modifier.weight(1f))
                    StatTile("Dangereux", stats.dangerous.toString(), RiskDanger, Modifier.weight(1f))
                    StatTile("Suspects", stats.suspicious.toString(), RiskSuspicious, Modifier.weight(1f))
                }
            }

            if (stats.last7Days.any { it.total > 0 }) {
                item {
                    SectionHeader("Activité sur 7 Jours")
                    WeeklyChart(stats.last7Days)
                }
            }
        }

        // Grille des actions rapides
        item {
            SectionHeader("Actions Rapides de Sécurité")
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ActionTile(
                    title = "Vérifier un Lien",
                    subtitle = "URL, phishing, domaines suspects",
                    color = VigiaPrimary,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Link
                ) { onAnalyze("url") }

                ActionTile(
                    title = "Analyser un Message",
                    subtitle = "SMS, WhatsApp, faux colis",
                    color = VigiaSecondary,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.ChatBubble
                ) { onAnalyze("text") }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ActionTile(
                    title = "Avant de Payer",
                    subtitle = "Anti-fraude Mobile Money & virements",
                    color = VigiaViolet,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.AccountBalanceWallet
                ) { onNavigateToModule("before_pay") }

                ActionTile(
                    title = "VIGIA Guard",
                    subtitle = "Bouclier en temps réel",
                    color = RiskSafe,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Security
                ) { onNavigateToModule("guard") }
            }
        }

        // Autres modules de protection
        item {
            SectionHeader("Autres Protections")
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ActionTile(
                    title = "Offre d'Emploi",
                    subtitle = "Faux recruteurs, frais avant embauche",
                    color = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.WorkspacePremium
                ) { onNavigateToModule("job_offer") }

                ActionTile(
                    title = "Petite Annonce",
                    subtitle = "Immobilier, véhicules, objets",
                    color = Color(0xFF7C3AED),
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Storefront
                ) { onNavigateToModule("listing") }
            }
        }

        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToModule("community") },
                borderColor = RiskSafeBorder,
                backgroundColor = RiskSafeBg
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(42.dp).clip(CircleShape).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Rounded.Diversity3, contentDescription = null, tint = RiskSafe, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Espace Communautaire", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 14.5.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Signaler une arnaque ou vérifier avant d'agir", fontFamily = PoppinsFontFamily, color = VigiaTextSecondary, fontSize = 12.sp)
                    }
                    Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = RiskSafe, modifier = Modifier.size(22.dp))
                }
            }
        }

        // Raccourci Radar / Shield
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToModule("moment_shield") },
                borderColor = Color(0xFFDBEAFE)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Radar,
                            contentDescription = null,
                            tint = VigiaPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Moment & Shield Intelligence", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 14.5.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Détection des attaques coordonnées sur 6h & tendances", fontFamily = PoppinsFontFamily, color = VigiaTextSecondary, fontSize = 12.sp)
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = VigiaPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Section Historique Récent
        item {
            SectionHeader(
                title = "Analyses Récentes",
                actionLabel = "Tout voir",
                onAction = onOpenHistory
            )
        }

        if (history.isEmpty()) {
            item {
                GlassCard {
                    EmptyState(
                        title = "Aucune analyse récente",
                        message = "Les liens et messages que vous analyserez apparaîtront ici."
                    )
                }
            }
        } else {
            items(history.take(4), key = { it.id }) { item ->
                AnalysisRow(item) { onOpenItem(item.id) }
            }
        }
    }
}

@Composable
fun ActionTile(
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Bolt,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier.clickable { onClick() },
        borderColor = color.copy(alpha = 0.25f),
        backgroundColor = color.copy(alpha = 0.04f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                color = VigiaTextPrimary,
                fontSize = 13.5.sp,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontFamily = PoppinsFontFamily,
            fontSize = 11.5.sp,
            color = VigiaTextSecondary,
            lineHeight = 15.sp,
            maxLines = 2
        )
    }
}

/** Graphique alimenté uniquement par les comptages réels renvoyés par le serveur */
@Composable
fun WeeklyChart(days: List<DayBucketDto>) {
    val max = (days.maxOfOrNull { it.total } ?: 0).coerceAtLeast(1)
    GlassCard {
        Row(
            Modifier
                .fillMaxWidth()
                .height(95.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEach { day ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    val fraction = day.total.toFloat() / max
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height((70 * fraction).dp.coerceAtLeast(4.dp))
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when {
                                    day.dangerous > 0 -> RiskDanger
                                    day.suspicious > 0 -> RiskSuspicious
                                    day.total > 0 -> RiskSafe
                                    else -> Color(0xFFE2E8F0)
                                }
                            )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(day.date.takeLast(2), fontSize = 10.sp, fontFamily = PoppinsFontFamily, color = VigiaTextSecondary)
                }
            }
        }
    }
}

@Composable
fun AnalysisRow(item: AnalysisEntity, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        borderColor = riskColor(item.level).copy(alpha = 0.35f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(riskColor(item.level).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.kind == "url") Icons.Rounded.Link else Icons.Rounded.ChatBubble,
                    contentDescription = null,
                    tint = riskColor(item.level),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (item.kind == "url") "Lien URL" else "Message texte",
                    fontSize = 11.5.sp,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = VigiaTextMuted
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.preview.take(85),
                    fontFamily = PoppinsFontFamily,
                    color = VigiaTextPrimary,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                LevelBadge(item.level)
                Spacer(Modifier.height(4.dp))
                Text("${item.score}/100", fontSize = 11.sp, fontFamily = PoppinsFontFamily, color = VigiaTextMuted, fontWeight = FontWeight.SemiBold)
            }
        }
        if (!item.syncedWithServer) {
            Spacer(Modifier.height(8.dp))
            InfoChip("Analyse locale (hors ligne)", RiskSuspicious)
        }
    }
}
