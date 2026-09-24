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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.vigia.app.local.AnalysisEntity
import ai.vigia.app.net.DayBucketDto
import ai.vigia.app.ui.components.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.DashboardViewModel

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
    val stats = state.stats

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BackgroundGradient),
        contentPadding = PaddingValues(18.dp, 14.dp, 18.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(BrandGradient), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Shield, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Bonjour 👋", fontFamily = PoppinsFontFamily, fontSize = 12.sp, color = VigiaTextSecondary)
                    Text("Votre sécurité, simplement.", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = VigiaTextPrimary)
                }
                InfoChip(if (state.offline) "Local" else "Protégé", if (state.offline) RiskSuspicious else RiskSafe)
                IconButton(onClick = { viewModel.refresh() }) { Icon(Icons.Rounded.Refresh, "Actualiser", tint = VigiaTextSecondary) }
            }
        }

        if (state.offline) item {
            GlassCard(borderColor = RiskSuspiciousBorder, backgroundColor = RiskSuspiciousBg, cornerRadius = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.SignalWifiOff, null, tint = RiskSuspicious, modifier = Modifier.size(21.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Mode local", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = RiskSuspicious)
                        Text("Le moteur local reste disponible.", fontFamily = PoppinsFontFamily, fontSize = 11.sp, color = VigiaTextSecondary)
                    }
                }
            }
        }

        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(BrandGradient)) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Security, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("BOUCLIER VIGIA", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White.copy(alpha = .75f), letterSpacing = 1.1.sp)
                            Text("Protection intelligente", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color.White)
                        }
                        if (stats != null) {
                            Text("${stats.protectionScore ?: 0}", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Color.White)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (stats == null || stats.totalAnalyses == 0) "Prêt à vérifier votre premier contenu."
                        else "${stats.totalAnalyses} contenus vérifiés • ${stats.dangerous} menace(s) détectée(s)",
                        fontFamily = PoppinsFontFamily, fontSize = 12.sp, color = Color.White.copy(alpha = .82f)
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { onAnalyze("url") }, modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = VigiaPrimary)
                    ) {
                        Icon(Icons.Rounded.Search, null, modifier = Modifier.size(19.dp)); Spacer(Modifier.width(8.dp))
                        Text("Vérifier quelque chose", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }
        }

        item { SectionHeader("Vérifier maintenant", "Une action à la fois, sans surcharge") }
        item {
            SimpleActionCard("Analyser un lien", "Phishing, domaine suspect, fausse page", Icons.Rounded.Link, VigiaPrimary, Modifier.fillMaxWidth()) { onAnalyze("url") }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SimpleActionCard("Message", "SMS / chat", Icons.Rounded.ChatBubble, VigiaSecondary, Modifier.weight(1f)) { onAnalyze("text") }
                SimpleActionCard("QR code", "Lien caché", Icons.Rounded.QrCodeScanner, VigiaPrimary, Modifier.weight(1f)) { onAnalyze("qr") }
            }
        }
        item { SimpleActionCard("Photo ou vidéo", "Analyse visuelle par IA", Icons.Rounded.PermMedia, VigiaViolet, Modifier.fillMaxWidth()) { onAnalyze("media") } }

        item { SectionHeader("Protection active", "Les outils essentiels de VIGIA") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SimpleActionCard("VIGIA Guard", "Protection en temps réel", Icons.Rounded.Security, RiskSafe, Modifier.weight(1f)) { onNavigateToModule("guard") }
                SimpleActionCard("Avant de payer", "Contrôler avant transfert", Icons.Rounded.AccountBalanceWallet, RiskDanger, Modifier.weight(1f)) { onNavigateToModule("before_pay") }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth().clickable { onNavigateToModule("services") }, backgroundColor = Color.White, borderColor = VigiaBorder, cornerRadius = 18.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(VigiaPrimarySoft), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Apps, null, tint = VigiaPrimary) }
                    Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) {
                        Text("Tous les services", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = VigiaTextPrimary)
                        Text("Emploi, annonces, communauté, radar et plus", fontFamily = PoppinsFontFamily, fontSize = 11.5.sp, color = VigiaTextSecondary)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = VigiaTextMuted)
                }
            }
        }

        if (stats != null && stats.totalAnalyses > 0) {
            item { SectionHeader("Votre activité", actionLabel = "Historique", onAction = onOpenHistory) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile("Total", stats.totalAnalyses.toString(), VigiaPrimary, Modifier.weight(1f))
                    StatTile("Sûrs", stats.safe.toString(), RiskSafe, Modifier.weight(1f))
                    StatTile("Suspects", stats.suspicious.toString(), RiskSuspicious, Modifier.weight(1f))
                    StatTile("Dangereux", stats.dangerous.toString(), RiskDanger, Modifier.weight(1f))
                }
            }
        }

        item { SectionHeader("Dernières analyses", actionLabel = "Tout voir", onAction = onOpenHistory) }
        if (history.isEmpty()) item {
            GlassCard(backgroundColor = Color.White, borderColor = VigiaBorder) { EmptyState("Votre historique est vide", "Vos vérifications apparaîtront ici avec leur niveau de risque.") }
        } else items(history.take(3), key = { it.id }) { item -> AnalysisRow(item) { onOpenItem(item.id) } }
    }
}

@Composable
private fun SimpleActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier.clickable(onClick = onClick), backgroundColor = Color.White, borderColor = color.copy(alpha = .20f), cornerRadius = 18.dp, contentPadding = PaddingValues(13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(color.copy(alpha = .10f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) {
                Text(title, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp, color = VigiaTextPrimary)
                Text(subtitle, fontFamily = PoppinsFontFamily, fontSize = 10.5.sp, color = VigiaTextSecondary, maxLines = 2)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = color, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun WeeklyChart(days: List<DayBucketDto>) {
    val max = (days.maxOfOrNull { it.total } ?: 0).coerceAtLeast(1)
    GlassCard(backgroundColor = Color.White, borderColor = VigiaBorder, cornerRadius = 18.dp) {
        Row(
            Modifier.fillMaxWidth().height(100.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEachIndexed { i, day ->
                val barColor = when {
                    day.dangerous > 0 -> RiskDanger
                    day.suspicious > 0 -> RiskSuspicious
                    day.total > 0 -> RiskSafe
                    else -> VigiaBorder
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.fillMaxWidth().height((72 * day.total.toFloat() / max).dp.coerceAtLeast(5.dp))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(barColor)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(day.date.takeLast(2), fontSize = 9.sp, fontFamily = PoppinsFontFamily, fontWeight = if (i == days.lastIndex) FontWeight.Bold else FontWeight.Medium, color = VigiaTextSecondary)
                }
            }
        }
    }
}

@Composable
fun AnalysisRow(item: AnalysisEntity, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val color = riskColor(item.level)
    GlassCard(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        borderColor = color.copy(alpha = .22f),
        backgroundColor = Color.White,
        cornerRadius = 17.dp,
        contentPadding = PaddingValues(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = when (item.kind) {
                    "url" -> Icons.Rounded.Link
                    "qr" -> Icons.Rounded.QrCodeScanner
                    "media" -> Icons.Rounded.PermMedia
                    else -> Icons.Rounded.ChatBubble
                },
                tint = color,
                size = 40.dp,
                iconSize = 19.dp
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when (item.kind) {
                        "url" -> "Lien"
                        "qr" -> "QR code"
                        "media" -> "Photo / vidéo"
                        else -> "Message"
                    },
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = VigiaTextPrimary
                )
                Spacer(Modifier.height(3.dp))
                Text(item.preview.take(70), fontFamily = PoppinsFontFamily, fontSize = 10.5.sp, color = VigiaTextSecondary, maxLines = 2)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                LevelBadge(item.level)
                Spacer(Modifier.height(3.dp))
                Text("${item.score}/100", fontFamily = PoppinsFontFamily, fontSize = 9.5.sp, color = VigiaTextMuted, fontWeight = FontWeight.Bold)
            }
        }
    }
}
