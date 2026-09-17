package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.ui.components.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.MomentShieldViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MomentShieldScreen(
    viewModel: MomentShieldViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Moment, 1 = Shield

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
            IconButton(onClick = { viewModel.load() }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Actualiser", tint = VigiaPrimary)
            }
        }

        Column {
            Text("Intelligence & Radar", style = MaterialTheme.typography.headlineMedium, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Analyse des scénarios d'attaque combinés et tendances des cyber-menaces.",
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 13.sp
            )
        }

        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.clip(RoundedCornerShape(14.dp)),
            containerColor = VigiaWhite,
            contentColor = VigiaPrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Moment (Corrélations)", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Shield (Radar Menaces)", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold) }
            )
        }

        if (state.loading && state.moment == null && state.shield == null) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VigiaPrimary)
            }
        } else if (selectedTab == 0) {
            // ==================== ONGLET MOMENT ====================
            val moment = state.moment
            if (moment != null) {
                GlassCard(
                    borderColor = if (moment.elevatedRisk) RiskDangerBorder else RiskSafeBorder,
                    backgroundColor = if (moment.elevatedRisk) RiskDangerBg else RiskSafeBg
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (moment.elevatedRisk) RiskDanger.copy(alpha = 0.15f) else RiskSafe.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (moment.elevatedRisk) Icons.Rounded.Warning else Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = if (moment.elevatedRisk) RiskDanger else RiskSafe,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (moment.elevatedRisk) "RISQUE CORRÉLÉ ÉLEVÉ" else "AUCUN SCÉNARIO COMBINÉ",
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = PoppinsFontFamily,
                                color = if (moment.elevatedRisk) RiskDanger else RiskSafe,
                                fontSize = 13.5.sp
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = if (moment.elevatedRisk)
                                    "Plusieurs signaux suspects reçus sur une courte période pointent vers une attaque coordonnée."
                                else
                                    "Aucune corrélation d'arnaque détectée sur la fenêtre glissante de ${moment.windowHours}h.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = PoppinsFontFamily,
                                color = VigiaTextSecondary,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }

                if (moment.correlations.isNotEmpty()) {
                    SectionHeader("Corrélations d'Attaque Détectées")
                    moment.correlations.forEach { corrMap ->
                        val explanation = corrMap["explanation"]?.toString()?.trim('"') ?: "Scénario détecté"
                        val weight = corrMap["weight"]?.toString()?.toIntOrNull() ?: 0

                        GlassCard(borderColor = RiskDangerBorder, backgroundColor = RiskDangerBg) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(explanation, fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 14.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Poids du scénario: +$weight", fontFamily = PoppinsFontFamily, color = RiskDanger, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                LevelBadge("dangerous")
                            }
                        }
                    }
                }

                SectionHeader(
                    title = "Événements analysés (${moment.events.size})",
                    subtitle = "Fenêtre d'observation de ${moment.windowHours} heures",
                    actionLabel = if (moment.events.isNotEmpty()) "Réinitialiser" else null,
                    onAction = { viewModel.clearMoment() }
                )

                if (moment.events.isEmpty()) {
                    GlassCard {
                        EmptyState(
                            title = "Aucun événement récent",
                            message = "Les analyses effectuées et les messages scannés dans les dernières ${moment.windowHours} heures apparaîtront ici pour détecter les attaques en chaîne."
                        )
                    }
                } else {
                    moment.events.forEach { event ->
                        GlassCard(borderColor = riskColor(event.riskLevel).copy(alpha = 0.3f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(riskColor(event.riskLevel).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (event.type == "url") Icons.Rounded.Link else Icons.Rounded.ChatBubble,
                                        contentDescription = null,
                                        tint = riskColor(event.riskLevel),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = "${event.source.uppercase()} • ${event.type.uppercase()}",
                                        fontSize = 11.sp,
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = VigiaTextMuted
                                    )
                                    if (event.packageName.isNotBlank()) {
                                        Text(event.packageName, fontSize = 12.sp, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontWeight = FontWeight.Medium)
                                    }
                                    Text(event.createdAt.take(16).replace("T", " "), fontSize = 11.sp, fontFamily = PoppinsFontFamily, color = VigiaTextSecondary)
                                }
                                LevelBadge(event.riskLevel)
                            }
                        }
                    }
                }
            }
        } else {
            // ==================== ONGLET SHIELD ====================
            val shield = state.shield
            if (shield != null) {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = VigiaPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Radar VIGIA Shield", style = MaterialTheme.typography.titleMedium, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text(shield.message, style = MaterialTheme.typography.bodyMedium, fontFamily = PoppinsFontFamily, color = VigiaTextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                SectionHeader("Tes Statistiques Personnelles (30 jours)")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    val total = shield.personal["total_analyses"]?.toString() ?: "0"
                    val flagged = shield.personal["flagged"]?.toString() ?: "0"
                    StatTile("Analyses", total, VigiaPrimary, Modifier.weight(1f))
                    StatTile("Menaces", flagged, RiskDanger, Modifier.weight(1f))
                }

                SectionHeader("Tendances Communautaires")
                GlassCard {
                    val commAvailable = shield.community["available"]?.toString()?.toBoolean() == true
                    if (commAvailable) {
                        val cTotal = shield.community["analyses_last_30_days"]?.toString() ?: "0"
                        val cFlagged = shield.community["flagged_last_30_days"]?.toString() ?: "0"
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Analyses globales (30j)", fontFamily = PoppinsFontFamily, color = VigiaTextSecondary)
                            Text(cTotal, fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Menaces neutralisées", fontFamily = PoppinsFontFamily, color = VigiaTextSecondary)
                            Text(cFlagged, fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = RiskDanger)
                        }
                    } else {
                        Text(
                            text = "Statistiques communautaires en attente de données suffisantes. " +
                                "VIGIA ne simule aucun chiffre artificiel pour garantir une intégrité totale.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = PoppinsFontFamily,
                            color = VigiaTextSecondary
                        )
                    }
                }

                if (shield.topCategories.isNotEmpty()) {
                    SectionHeader("Typologies de Menaces Fréquentes")
                    shield.topCategories.forEach { catMap ->
                        val catName = catMap["category"]?.toString()?.trim('"') ?: "Général"
                        val count = catMap["occurrences"]?.toString() ?: "0"

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFFBEB)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.TrackChanges,
                                        contentDescription = null,
                                        tint = RiskSuspicious,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(catName, fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 14.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text("$count détections réelles", fontSize = 12.sp, fontFamily = PoppinsFontFamily, color = VigiaTextMuted)
                                }
                                InfoChip("Menace active", RiskSuspicious)
                            }
                        }
                    }
                }
            }
        }
    }
}
