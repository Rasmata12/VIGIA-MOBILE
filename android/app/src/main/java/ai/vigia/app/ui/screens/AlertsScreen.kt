package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.vigia.app.ui.components.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.AlertsViewModel

@Composable
fun AlertsScreen(viewModel: AlertsViewModel, onBack: () -> Unit) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    LazyColumn(
        Modifier.fillMaxSize().background(BackgroundGradient),
        contentPadding = PaddingValues(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubtleBackButton(onBack = onBack, label = "Retour")
                Spacer(Modifier.weight(1f))
                IconButton(onClick = viewModel::load) { Icon(Icons.Rounded.Refresh, "Actualiser", tint = VigiaPrimary) }
            }
        }
        item {
            Text("Notifications", fontWeight = FontWeight.ExtraBold, fontSize = 25.sp, color = VigiaTextPrimary)
            Spacer(Modifier.height(3.dp))
            Text("Alertes liées aux vérifications de sécurité.", fontSize = 12.sp, color = VigiaTextSecondary)
        }
        state.error?.let { message -> item { ErrorBanner(message, onRetry = viewModel::load) } }
        if (state.loading) item {
            GlassCard(backgroundColor = Color.White, borderColor = VigiaBorder) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = VigiaPrimary)
                    Spacer(Modifier.width(10.dp))
                    Text("Chargement des notifications…", color = VigiaTextSecondary, fontSize = 12.sp)
                }
            }
        }
        else if (state.alerts.isEmpty()) item {
            GlassCard(backgroundColor = Color.White, borderColor = VigiaBorder) {
                EmptyState("Aucune notification", "Une alerte apparaîtra ici si une vérification détecte un risque.")
            }
        } else items(state.alerts, key = { it.id }) { alert ->
            val tint = riskColor(alert.level)
            GlassCard(
                modifier = Modifier.fillMaxWidth().clickable { if (!alert.read) viewModel.markRead(alert.id) },
                backgroundColor = Color.White,
                borderColor = if (alert.read) VigiaBorder else tint.copy(alpha = .42f),
                cornerRadius = 17.dp
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(tint.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                        Icon(if (alert.read) Icons.Rounded.Notifications else Icons.Rounded.Warning, null, tint = tint, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = VigiaTextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(alert.body, fontSize = 12.sp, lineHeight = 17.sp, color = VigiaTextSecondary)
                        Spacer(Modifier.height(6.dp))
                        Text(if (alert.read) "Lue" else "Touchez pour marquer comme lue", fontSize = 10.5.sp, color = tint)
                    }
                }
            }
        }
    }
}
