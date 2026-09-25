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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.vigia.app.local.AnalysisEntity
import ai.vigia.app.ui.components.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAnalyze: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenItem: (String) -> Unit,
    onNavigateToModule: (String) -> Unit = {},
    onOpenNotifications: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    BoxWithConstraints(Modifier.fillMaxSize().background(BackgroundGradient)) {
        val compact = maxWidth < 360.dp
        val horizontalPadding = if (compact) 14.dp else 20.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontalPadding, 12.dp, horizontalPadding, 104.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(BrandGradient), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Shield, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("VIGIA", Modifier.weight(1f), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = VigiaTextPrimary)
                    InfoChip(if (state.offline) "Hors ligne" else "Protégé", if (state.offline) RiskSuspicious else RiskSafe)
                    IconButton(onClick = onOpenNotifications) {
                        BadgedBox(badge = {
                            val unread = state.stats?.unreadAlerts ?: 0
                            if (unread > 0) Badge { Text(if (unread > 9) "9+" else unread.toString()) }
                        }) { Icon(Icons.Rounded.Notifications, "Notifications", tint = VigiaTextSecondary) }
                    }
                }
            }

            if (state.offline) item {
                GlassCard(borderColor = RiskSuspiciousBorder, backgroundColor = RiskSuspiciousBg, cornerRadius = 14.dp, contentPadding = PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SignalWifiOff, null, tint = RiskSuspicious, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(9.dp))
                        Text("Connexion indisponible. Vérifiez votre réseau.", fontSize = 12.sp, color = VigiaTextSecondary)
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), backgroundBrush = BrandGradient, borderColor = Color.Transparent, cornerRadius = 22.dp, contentPadding = PaddingValues(if (compact) 16.dp else 20.dp)) {
                    Text("Restez à l’abri des arnaques", fontWeight = FontWeight.ExtraBold, fontSize = if (compact) 18.sp else 21.sp, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    val count = state.stats?.totalAnalyses ?: 0
                    Text(if (count == 0) "Vérifiez un lien ou un message avant d’agir." else "$count vérification${if (count > 1) "s" else ""} effectuée${if (count > 1) "s" else ""}", fontSize = 12.sp, color = Color.White.copy(alpha = .86f))
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { onAnalyze("url") }, modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
                        shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = VigiaPrimary)
                    ) {
                        Icon(Icons.Rounded.Search, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(7.dp))
                        Text("Vérifier maintenant", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item { Text("Choisir un contenu", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = VigiaTextPrimary) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                        QuickAction("Lien", Icons.Rounded.Link, VigiaPrimary, Modifier.weight(1f)) { onAnalyze("url") }
                        QuickAction("Message", Icons.Rounded.ChatBubble, VigiaSecondary, Modifier.weight(1f)) { onAnalyze("text") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                        QuickAction("QR code", Icons.Rounded.QrCodeScanner, VigiaPrimary, Modifier.weight(1f)) { onAnalyze("qr") }
                        QuickAction("Photo / vidéo", Icons.Rounded.PermMedia, VigiaViolet, Modifier.weight(1f)) { onAnalyze("media") }
                    }
                }
            }

            item { Text("Protection", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = VigiaTextPrimary) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    CompactLink("VIGIA Guard", "Protection en temps réel", Icons.Rounded.Security, RiskSafe) { onNavigateToModule("guard") }
                    CompactLink("Avant de payer", "Vérifier un transfert", Icons.Rounded.AccountBalanceWallet, RiskDanger) { onNavigateToModule("before_pay") }
                    CompactLink("Tous les services", "Communauté, emploi et leçons", Icons.Rounded.Apps, VigiaPrimary) { onNavigateToModule("services") }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Récent", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = VigiaTextPrimary)
                    TextButton(onClick = onOpenHistory) { Text("Historique") }
                }
            }
            if (history.isEmpty()) item {
                GlassCard(backgroundColor = Color.White, borderColor = VigiaBorder, cornerRadius = 16.dp, contentPadding = PaddingValues(14.dp)) {
                    Text("Aucune vérification pour le moment.", fontSize = 12.sp, color = VigiaTextSecondary)
                }
            } else items(history.take(2), key = { it.id }) { entry -> AnalysisRow(entry) { onOpenItem(entry.id) } }
        }
    }
}

@Composable
fun AnalysisRow(item: AnalysisEntity, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val color = riskColor(item.level)
    GlassCard(modifier.fillMaxWidth().clickable(onClick = onClick), backgroundColor = Color.White, borderColor = VigiaBorder, cornerRadius = 15.dp, contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.History, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.preview, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = VigiaTextPrimary)
                Text("Score ${item.score}/100", fontSize = 10.sp, color = VigiaTextSecondary)
            }
            LevelBadge(item.level)
        }
    }
}

@Composable
private fun QuickAction(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier = modifier.clickable(onClick = onClick), backgroundColor = Color.White, borderColor = VigiaBorder, cornerRadius = 15.dp, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = VigiaTextPrimary)
        }
    }
}

@Composable
private fun CompactLink(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().clickable(onClick = onClick), backgroundColor = Color.White, borderColor = VigiaBorder, cornerRadius = 15.dp, contentPadding = PaddingValues(13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = .10f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = VigiaTextPrimary)
                Text(subtitle, fontSize = 11.sp, color = VigiaTextSecondary)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = VigiaTextMuted)
        }
    }
}
