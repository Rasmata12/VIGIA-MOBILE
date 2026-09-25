package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.ui.components.GlassCard
import ai.vigia.app.ui.theme.*

private data class ServiceLink(val name: String, val hint: String, val icon: ImageVector, val color: Color, val route: String)

@Composable
fun ServicesScreen(onNavigate: (String) -> Unit) {
    val groups = listOf(
        "Paiement" to listOf(ServiceLink("Avant de payer", "Vérifier un transfert", Icons.Rounded.AccountBalanceWallet, RiskDanger, "before_pay")),
        "Emploi" to listOf(ServiceLink("Offres d’emploi", "Vérifier une offre ou un recruteur", Icons.Rounded.WorkspacePremium, Color(0xFFD97706), "job_offer")),
        "Apprendre et signaler" to listOf(
            ServiceLink("Leçons", "Conseils pratiques", Icons.Rounded.School, VigiaPrimary, "lessons"),
            ServiceLink("Communauté", "Consulter ou envoyer un signalement", Icons.Rounded.Diversity3, RiskSafe, "community"),
            ServiceLink("Radar", "Menaces signalées récemment", Icons.Rounded.Radar, VigiaPrimary, "moment_shield")
        ),
        "Historique" to listOf(ServiceLink("Mes vérifications", "Retrouver les résultats précédents", Icons.Rounded.History, VigiaPrimary, "history"))
    )

    LazyColumn(
        Modifier.fillMaxSize().background(BackgroundGradient),
        contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Outils", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = VigiaTextPrimary) }
        groups.forEach { (title, links) ->
            item { Text(title, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = VigiaTextSecondary) }
            links.forEach { link -> item(key = link.route) { ServiceRow(link) { onNavigate(link.route) } } }
        }
    }
}

@Composable
private fun ServiceRow(link: ServiceLink, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        backgroundColor = Color.White,
        borderColor = VigiaBorder,
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(link.color.copy(alpha = .10f)), contentAlignment = Alignment.Center) {
                Icon(link.icon, null, tint = link.color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(link.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = VigiaTextPrimary)
                Text(link.hint, fontSize = 11.sp, color = VigiaTextSecondary)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = VigiaTextMuted)
        }
    }
}
