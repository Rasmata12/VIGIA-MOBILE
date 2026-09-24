package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.guard.PermissionCenter
import ai.vigia.app.ui.components.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.PermissionsViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PermissionsScreen(
    viewModel: PermissionsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refresh(context)
    }

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
            IconButton(onClick = { viewModel.refresh(context) }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Actualiser", tint = VigiaPrimary)
            }
            InfoChip("Transparence Système", VigiaPrimary)
        }

        // Titre & Sous-titre
        Column {
            Text(
                "Centre des Permissions",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = VigiaTextPrimary,
                fontSize = 24.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Audit en temps réel des autorisations système requises et accordées à l'application VIGIA AI.",
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            )
        }

        // HÉROS — Philosophie du moindre privilège
        HeroSurface(orbColors = listOf(VigiaPrimary, VigiaSecondary), cornerRadius = 24.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                IconBadge(icon = Icons.Rounded.PrivacyTip, tint = VigiaPrimary, size = 44.dp, iconSize = 22.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Principe du Moindre Privilège",
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaPrimary,
                        fontSize = 14.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "VIGIA n'exige aucun accès intrusif : zéro lecture de votre carnet d'adresses, zéro localisation GPS et aucun scan de messages en arrière-plan à votre insu.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        SectionHeader("État des Autorisations Système")

        state.items.forEach { item ->
            val isGranted = item.state == PermissionCenter.State.GRANTED
            val isActionRequired = item.state == PermissionCenter.State.ACTION_REQUIRED
            val statusColor = if (isGranted) RiskSafe else if (isActionRequired) RiskSuspicious else VigiaTextMuted

            GlassCard(
                backgroundBrush = luxuryCardGradient(statusColor),
                borderBrush = luxuryBorderGradient(statusColor),
                cornerRadius = 22.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        icon = if (isGranted) Icons.Rounded.CheckCircle else Icons.Rounded.ReportProblem,
                        tint = statusColor,
                        size = 42.dp,
                        iconSize = 20.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.name,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PoppinsFontFamily,
                            color = VigiaTextPrimary,
                            fontSize = 14.5.sp
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = when (item.state) {
                                PermissionCenter.State.GRANTED -> "Autorisation active et sécurisée"
                                PermissionCenter.State.ACTION_REQUIRED -> "Configuration requise"
                                PermissionCenter.State.DENIED -> "Autorisation refusée"
                                PermissionCenter.State.NOT_REQUIRED -> "Non requise sur ce système"
                            },
                            fontSize = 11.5.sp,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }
                    InfoChip(
                        text = if (isGranted) "Opérationnel" else "À régler",
                        color = statusColor
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    item.why,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = PoppinsFontFamily,
                    color = VigiaTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                if (item.actionLabel != null && item.intent != null) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { context.startActivity(item.intent) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActionRequired) RiskSuspicious else VigiaPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(item.actionLabel, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}
