package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.ui.components.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.DevicesViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DevicesScreen(
    viewModel: DevicesViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
            InfoChip("Terminaux & Clés", VigiaPrimary)
        }

        // Titre & Sous-titre
        Column {
            Text(
                "Appareils Connectés",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = VigiaTextPrimary,
                fontSize = 24.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Supervisez et révoquez instantanément les accès des smartphones et tablettes reliés à votre compte.",
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            )
        }

        // HÉROS — Sécurité du périmètre
        HeroSurface(orbColors = listOf(VigiaPrimary, VigiaCyan), cornerRadius = 24.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                IconBadge(icon = Icons.Rounded.PhonelinkLock, tint = VigiaPrimary, size = 44.dp, iconSize = 22.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Périmètre de Confiance Zéro",
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaPrimary,
                        fontSize = 14.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "En cas de perte, vol ou doute sur un smartphone, révoquez immédiatement l'appareil. La clé cryptographique locale sera invalidée en temps réel.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        state.error?.let { ErrorBanner(it) }

        if (state.loading && state.devices.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VigiaPrimary)
            }
        } else if (state.devices.isEmpty()) {
            GlassCard(cornerRadius = 24.dp) {
                EmptyState(
                    title = "Aucun appareil enregistré",
                    message = "Vos appareils synchronisés apparaîtront ici dès leur première connexion cryptée."
                )
            }
        } else {
            SectionHeader("Terminaux Autorisés (${state.devices.size})")

            state.devices.forEachIndexed { index, dev ->
                val cardColor = if (!dev.revoked) VigiaPrimary else RiskDanger
                GlassCard(
                    backgroundBrush = luxuryCardGradient(cardColor),
                    borderBrush = luxuryBorderGradient(cardColor),
                    cornerRadius = 22.dp,
                    entranceDelayMillis = index * 40L
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            icon = Icons.Rounded.Smartphone,
                            tint = cardColor,
                            size = 42.dp,
                            iconSize = 20.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = dev.label.ifBlank { "Appareil Android Sécurisé" },
                                fontWeight = FontWeight.Bold,
                                fontFamily = PoppinsFontFamily,
                                color = VigiaTextPrimary,
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "Dernier signal : ${dev.lastSeenAt.take(16).replace("T", " à ")}",
                                fontSize = 11.5.sp,
                                fontFamily = PoppinsFontFamily,
                                color = VigiaTextSecondary
                            )
                        }
                        if (dev.revoked) {
                            InfoChip("Révoqué", RiskDanger)
                        } else {
                            InfoChip("Actif", RiskSafe)
                        }
                    }

                    if (!dev.revoked) {
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(
                                onClick = { viewModel.revoke(dev.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RiskDanger),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RiskDanger.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Révoquer l'appareil", fontFamily = PoppinsFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (state.sessions.isNotEmpty()) {
            SectionHeader("Jetons de Session Cryptographique (${state.sessions.size})")
            state.sessions.forEachIndexed { index, session ->
                val sessionColor = if (!session.revoked) VigiaSecondary else RiskDanger
                GlassCard(
                    backgroundBrush = luxuryCardGradient(sessionColor),
                    borderBrush = luxuryBorderGradient(sessionColor),
                    cornerRadius = 20.dp,
                    entranceDelayMillis = index * 40L,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon = Icons.Rounded.Key, tint = sessionColor, size = 36.dp, iconSize = 18.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = session.deviceLabel.ifBlank { "Session Web / Mobile" },
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = PoppinsFontFamily,
                                color = VigiaTextPrimary,
                                fontSize = 13.5.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Créée le ${session.createdAt.take(10)} • Expire le ${session.expiresAt.take(10)}",
                                fontSize = 11.sp,
                                fontFamily = PoppinsFontFamily,
                                color = VigiaTextMuted
                            )
                        }
                        InfoChip(if (session.revoked) "Expirée" else "Valide", if (session.revoked) RiskDanger else RiskSafe)
                    }
                }
            }
        }
    }
}
