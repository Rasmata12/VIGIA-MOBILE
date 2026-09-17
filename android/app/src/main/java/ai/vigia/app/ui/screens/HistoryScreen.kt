package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

@OptIn(ExperimentalMaterial3Api::class)
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val detail by viewModel.detail.collectAsStateWithLifecycle()

    var filterLevel by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showConfirmClear by remember { mutableStateOf(false) }

    val filteredItems = remember(items, filterLevel, searchQuery) {
        items.filter { item ->
            val matchLevel = filterLevel == null || item.level.equals(filterLevel, ignoreCase = true)
            val matchQuery = searchQuery.isBlank() || item.preview.contains(searchQuery, ignoreCase = true)
            matchLevel && matchQuery
        }
    }

    Box(Modifier.fillMaxSize().background(BackgroundGradient)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Historique Forensique", style = MaterialTheme.typography.headlineMedium, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("${items.size} analyse(s) enregistrée(s)", fontFamily = PoppinsFontFamily, color = VigiaTextSecondary, fontSize = 12.5.sp)
                    }
                    if (items.isNotEmpty()) {
                        TextButton(onClick = { showConfirmClear = true }) {
                            Text("Effacer tout", color = RiskDanger, fontFamily = PoppinsFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Barre de recherche
            if (items.isNotEmpty()) {
                item {
                    VigiaField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = "Rechercher une URL ou un extrait de message...",
                        singleLine = true
                    )
                }

                // Filtres par niveau de menace
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterPill(
                            label = "Tous (${items.size})",
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

            error?.let { item { ErrorBanner(it) } }

            if (items.isEmpty()) {
                item {
                    GlassCard {
                        EmptyState(
                            title = "Historique vierge",
                            message = "Chaque analyse de lien ou de message que vous lancez sera conservée ici avec son rapport technique complet."
                        )
                    }
                }
            } else if (filteredItems.isEmpty()) {
                item {
                    GlassCard {
                        EmptyState(
                            title = "Aucun résultat",
                            message = "Aucune analyse ne correspond aux critères de filtre sélectionnés."
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
            title = { Text("Effacer tout l'historique ?", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary) },
            text = {
                Text(
                    "Cette action supprimera définitivement toutes les analyses enregistrées sur cet appareil et sur le serveur.",
                    fontFamily = PoppinsFontFamily,
                    color = VigiaTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAll()
                        showConfirmClear = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RiskDanger)
                ) {
                    Text("Supprimer définitivement", fontFamily = PoppinsFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) {
                    Text("Annuler", fontFamily = PoppinsFontFamily, color = VigiaTextSecondary)
                }
            },
            containerColor = VigiaWhite
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
                        .padding(vertical = 10.dp)
                        .width(40.dp)
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
                SectionHeader("Fiche d'Audit Technique")
                Spacer(Modifier.height(10.dp))
                ResultSection(item, offline = !item.syncedWithServer)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            viewModel.delete(item.id)
                            viewModel.closeDetail()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RiskDanger.copy(alpha = 0.12f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Supprimer ce rapport", color = RiskDanger, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
                    }
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
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) color.copy(alpha = 0.12f) else VigiaWhite)
            .border(
                1.dp,
                if (selected) color else VigiaBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontFamily = PoppinsFontFamily,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) color else VigiaTextSecondary
        )
    }
}
