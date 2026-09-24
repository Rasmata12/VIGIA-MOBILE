package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

    val shieldColor = VigiaViolet

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
            IconButton(onClick = { viewModel.load() }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Actualiser", tint = VigiaPrimary)
            }
            InfoChip("Radar & Intelligence", shieldColor)
        }

        // Titre & Sous-titre
        Column {
            Text(
                "Intelligence & Radar",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = VigiaTextPrimary,
                fontSize = 24.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Détection des attaques coordonnées multi-canaux (SMS, WhatsApp, appels).",
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 13.sp
            )
        }

        // Sélecteur d'onglets de luxe
        GlassCard(
            cornerRadius = 18.dp,
            contentPadding = PaddingValues(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RadarTabButton(
                    label = "Moment (Attaques en chaîne)",
                    icon = Icons.Rounded.Timeline,
                    selected = selectedTab == 0,
                    modifier = Modifier.weight(1f)
                ) {
                    selectedTab = 0
                }
                RadarTabButton(
                    label = "Radar (Menaces Actives)",
                    icon = Icons.Rounded.Radar,
                    selected = selectedTab == 1,
                    modifier = Modifier.weight(1f)
                ) {
                    selectedTab = 1
                }
            }
        }

        if (state.loading && state.moment == null && state.shield == null) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VigiaPrimary)
            }
        } else if (selectedTab == 0) {
            // ==================== ONGLET MOMENT ====================
            val moment = state.moment
            if (moment != null) {
                val isElevated = moment.elevatedRisk
                val statusColor = if (isElevated) RiskDanger else RiskSafe

                HeroSurface(
                    orbColors = listOf(statusColor, if (isElevated) RiskSuspicious else VigiaPrimary),
                    cornerRadius = 24.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        IconBadge(
                            icon = if (isElevated) Icons.Rounded.Warning else Icons.Rounded.VerifiedUser,
                            tint = statusColor,
                            size = 44.dp,
                            iconSize = 22.dp
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = if (isElevated) "ATTAQUE COORDONNÉE SUSPECTÉE" else "FLUX MULTICANAUX SÉCURISÉS",
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = PoppinsFontFamily,
                                color = statusColor,
                                fontSize = 13.5.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (isElevated)
                                    "Plusieurs signaux suspects reçus sur une courte période pointent vers une attaque coordonnée ciblant vos comptes."
                                else
                                    "Aucune corrélation d'arnaque n'est détectée sur votre fenêtre glissante d'activité de ${moment.windowHours} heures.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = PoppinsFontFamily,
                                color = VigiaTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                if (moment.correlations.isNotEmpty()) {
                    SectionHeader("Scénarios d'Attaque Corrélés")
                    moment.correlations.forEach { corrMap ->
                        val explanation = corrMap["explanation"]?.toString()?.trim('"') ?: "Scénario suspect détecté"
                        val weight = corrMap["weight"]?.toString()?.toIntOrNull() ?: 0

                        GlassCard(
                            backgroundBrush = luxuryCardGradient(RiskDanger),
                            borderBrush = luxuryBorderGradient(RiskDanger),
                            cornerRadius = 24.dp
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        explanation,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = PoppinsFontFamily,
                                        color = VigiaTextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Impact de corrélation : +$weight",
                                        fontFamily = PoppinsFontFamily,
                                        color = RiskDanger,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LevelBadge("dangerous")
                            }
                        }
                    }
                }

                SectionHeader(
                    title = "Événements Analysés (${moment.events.size})",
                    subtitle = "Fenêtre glissante de ${moment.windowHours} heures",
                    actionLabel = if (moment.events.isNotEmpty()) "Vider l'historique" else null,
                    onAction = { viewModel.clearMoment() }
                )

                if (moment.events.isEmpty()) {
                    GlassCard(cornerRadius = 24.dp) {
                        EmptyState(
                            title = "Aucun événement récent",
                            message = "Les messages scannés et les liens vérifiés dans les dernières ${moment.windowHours} heures apparaîtront ici pour identifier les attaques coordonnées."
                        )
                    }
                } else {
                    moment.events.forEach { event ->
                        val evtColor = riskColor(event.riskLevel)
                        GlassCard(
                            backgroundBrush = luxuryCardGradient(evtColor),
                            borderBrush = luxuryBorderGradient(evtColor),
                            cornerRadius = 20.dp
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconBadge(
                                    icon = if (event.type == "url") Icons.Rounded.Link else Icons.Rounded.ChatBubble,
                                    tint = evtColor,
                                    size = 38.dp,
                                    iconSize = 18.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = "${event.source.uppercase()} • ${event.type.uppercase()}",
                                        fontSize = 11.sp,
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = VigiaTextMuted
                                    )
                                    if (event.packageName.isNotBlank()) {
                                        Text(
                                            event.packageName,
                                            fontSize = 12.5.sp,
                                            fontFamily = PoppinsFontFamily,
                                            color = VigiaTextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        event.createdAt.take(16).replace("T", " à "),
                                        fontSize = 11.sp,
                                        fontFamily = PoppinsFontFamily,
                                        color = VigiaTextSecondary
                                    )
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
                HeroSurface(orbColors = listOf(VigiaPrimary, VigiaViolet), cornerRadius = 24.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconBadge(icon = Icons.Rounded.Radar, tint = VigiaPrimary, size = 48.dp, iconSize = 24.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                "Radar VIGIA Shield Actif",
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = PoppinsFontFamily,
                                color = VigiaPrimary,
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                shield.message,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = PoppinsFontFamily,
                                color = VigiaTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                SectionHeader("Statistiques de Défense (30 jours)")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    val total = shield.personal["total_analyses"]?.toString() ?: "0"
                    val flagged = shield.personal["flagged"]?.toString() ?: "0"
                    StatTile("Analyses effectuées", total, VigiaPrimary, Modifier.weight(1f))
                    StatTile("Menaces contrées", flagged, RiskDanger, Modifier.weight(1f))
                }

                SectionHeader("Tendances Communautaires")
                GlassCard(
                    backgroundBrush = luxuryCardGradient(VigiaPrimary),
                    borderBrush = luxuryBorderGradient(VigiaPrimary),
                    cornerRadius = 24.dp
                ) {
                    val commAvailable = shield.community["available"]?.toString()?.toBoolean() == true
                    if (commAvailable) {
                        val cTotal = shield.community["analyses_last_30_days"]?.toString() ?: "0"
                        val cFlagged = shield.community["flagged_last_30_days"]?.toString() ?: "0"
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Analyses globales réseau (30j)", fontFamily = PoppinsFontFamily, color = VigiaTextSecondary, fontSize = 13.sp)
                            Text(cTotal, fontWeight = FontWeight.ExtraBold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Tentatives neutralisées", fontFamily = PoppinsFontFamily, color = VigiaTextSecondary, fontSize = 13.sp)
                            Text(cFlagged, fontWeight = FontWeight.ExtraBold, fontFamily = PoppinsFontFamily, color = RiskDanger, fontSize = 14.sp)
                        }
                    } else {
                        Text(
                            text = "Statistiques communautaires en attente d'échantillonnage suffisant. VIGIA ne simule aucun chiffre artificiel pour garantir une intégrité totale de sa métrologie.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = PoppinsFontFamily,
                            color = VigiaTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                if (shield.topCategories.isNotEmpty()) {
                    SectionHeader("Typologies de Menaces Détectées")
                    shield.topCategories.forEach { catMap ->
                        val catName = catMap["category"]?.toString()?.trim('"') ?: "Général"
                        val count = catMap["occurrences"]?.toString() ?: "0"

                        GlassCard(
                            backgroundBrush = luxuryCardGradient(RiskSuspicious),
                            borderBrush = luxuryBorderGradient(RiskSuspicious),
                            cornerRadius = 20.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconBadge(icon = Icons.Rounded.TrackChanges, tint = RiskSuspicious, size = 38.dp, iconSize = 18.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        catName,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = PoppinsFontFamily,
                                        color = VigiaTextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        "$count détections réelles enregistrées",
                                        fontSize = 12.sp,
                                        fontFamily = PoppinsFontFamily,
                                        color = VigiaTextMuted
                                    )
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

@Composable
private fun RadarTabButton(
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
                fontSize = 12.sp
            )
        }
    }
}
