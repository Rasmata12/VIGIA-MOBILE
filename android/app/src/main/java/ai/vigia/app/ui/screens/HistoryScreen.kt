package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.ui.components.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.HistoryViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit = {}, recentOnly: Boolean = false) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val detail by viewModel.detail.collectAsStateWithLifecycle()

    var filterLevel by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showConfirmClear by remember { mutableStateOf(false) }

    val visibleItems = if (recentOnly) items.take(5) else items
    val filteredItems = remember(visibleItems, filterLevel, searchQuery) {
        visibleItems.filter { item ->
            val matchLevel = filterLevel == null || item.level.equals(filterLevel, ignoreCase = true)
            val matchQuery = searchQuery.isBlank() || item.preview.contains(searchQuery, ignoreCase = true)
            matchLevel && matchQuery
        }
    }

    Box(Modifier.fillMaxSize().background(BackgroundGradient)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SubtleBackButton(onBack = onBack, label = "Retour")
                    Spacer(Modifier.weight(1f))
                    if (!recentOnly && items.isNotEmpty()) {
                        TextButton(onClick = { showConfirmClear = true }) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = RiskDanger, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Effacer", color = RiskDanger, fontFamily = PoppinsFontFamily, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item {
                Column {
                    Text(
                        if (recentOnly) "Vérifications récentes" else "Historique",
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        color = VigiaTextPrimary,
                        fontSize = 24.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (recentOnly) "Voici vos cinq dernières vérifications." else "Retrouvez ici vos vérifications précédentes.",
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Barre de recherche et filtres
            if (visibleItems.isNotEmpty()) {
                item {
                    GlassCard(
                        backgroundBrush = luxuryCardGradient(VigiaPrimary),
                        borderBrush = luxuryBorderGradient(VigiaPrimary),
                        cornerRadius = 20.dp
                    ) {
                        VigiaField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = "Filtrer par URL, contact ou mot-clé...",
                            singleLine = true
                        )

                        Spacer(Modifier.height(12.dp))

                        // Filtres par niveau de menace
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            FilterPill(
                                label = "Tous (${visibleItems.size})",
                                selected = filterLevel == null,
                                color = VigiaPrimary
                            ) { filterLevel = null }

                            FilterPill(
                                label = "Dangereux",
                                selected = filterLevel == "dangerous",
                                color = RiskDanger
                            ) { filterLevel = if (filterLevel == "dangerous") null else "dangerous" }

                            FilterPill(
                                label = "Suspects",
                                selected = filterLevel == "suspicious",
                                color = RiskSuspicious
                            ) { filterLevel = if (filterLevel == "suspicious") null else "suspicious" }

                            FilterPill(
                                label = "Sûrs",
                                selected = filterLevel == "safe",
                                color = RiskSafe
                            ) { filterLevel = if (filterLevel == "safe") null else "safe" }
                        }
                    }
                }
            }

            error?.let { item { ErrorBanner(it) } }

            if (visibleItems.isEmpty()) {
                item {
                    GlassCard(cornerRadius = 24.dp) {
                        EmptyState(
                            title = "Historique vierge",
                            message = "Les résultats de tes vérifications apparaîtront ici."
                        )
                    }
                }
            } else if (filteredItems.isEmpty()) {
                item {
                    GlassCard(cornerRadius = 24.dp) {
                        EmptyState(
                            title = "Aucune analyse trouvée",
                            message = "Aucun rapport ne correspond à vos critères de recherche actuels."
                        )
                    }
                }
            } else {
                items(filteredItems, key = { it.id }) { item ->
                    AnalysisRow(item, modifier = Modifier.animateItemPlacement()) { viewModel.open(item.id) }
                }
            }
        }
    }

    // Modal de confirmation d'effacement
    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            title = {
                Text(
                    "Purger l'historique complet ?",
                    fontWeight = FontWeight.Bold,
                    fontFamily = PoppinsFontFamily,
                    color = VigiaTextPrimary,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    "Cette action supprimera définitivement les résultats enregistrés sur cet appareil et sur ton compte.",
                    fontFamily = PoppinsFontFamily,
                    color = VigiaTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAll()
                        showConfirmClear = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RiskDanger),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Supprimer définitivement", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) {
                    Text("Annuler", fontFamily = PoppinsFontFamily, color = VigiaTextSecondary, fontSize = 12.5.sp)
                }
            },
            containerColor = VigiaWhite,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Fiche d'audit détaillée (BottomSheet)
    detail?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeDetail() },
            containerColor = VigiaWhite,
            dragHandle = {
                Box(
                    Modifier
                        .padding(vertical = 12.dp)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFCBD5E1))
                )
            }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp)
            ) {
                SectionHeader("Détail de la vérification")
                Spacer(Modifier.height(10.dp))
                ResultSection(item, offline = !item.syncedWithServer)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.delete(item.id)
                        viewModel.closeDetail()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RiskDanger.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = RiskDanger, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Supprimer ce résultat", color = RiskDanger, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) color.copy(alpha = 0.12f) else VigiaWhite)
            .border(
                1.dp,
                if (selected) color else VigiaBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (selected) color else VigiaBorder)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontFamily = PoppinsFontFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) color else VigiaTextSecondary
            )
        }
    }
}
