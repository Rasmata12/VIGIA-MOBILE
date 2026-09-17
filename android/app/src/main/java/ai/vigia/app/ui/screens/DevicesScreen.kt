package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
            Text("Appareils Connectés", style = MaterialTheme.typography.headlineMedium, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Gérez les téléphones et terminaux autorisés à accéder à votre compte VIGIA AI.",
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 13.sp
            )
        }

        state.error?.let { ErrorBanner(it) }

        if (state.loading && state.devices.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VigiaPrimary)
            }
        } else if (state.devices.isEmpty()) {
            GlassCard {
                EmptyState(
                    title = "Aucun appareil enregistré",
                    message = "Vos appareils synchronisés apparaîtront ici."
                )
            }
        } else {
            SectionHeader("Terminaux Actifs (${state.devices.size})")

            state.devices.forEachIndexed { index, dev ->
                GlassCard(
                    borderColor = if (!dev.revoked) Color(0xFFDBEAFE) else VigiaBorder,
                    entranceDelayMillis = index * 40L
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Smartphone,
                                contentDescription = null,
                                tint = VigiaPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (dev.label.isNotBlank()) dev.label else "Appareil Android",
                                fontWeight = FontWeight.Bold,
                                fontFamily = PoppinsFontFamily,
                                color = VigiaTextPrimary,
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "Dernière connexion: ${dev.lastSeenAt.take(16).replace("T", " ")}",
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
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { viewModel.revoke(dev.id) }) {
                                Text("Révoquer l'accès", color = RiskDanger, fontFamily = PoppinsFontFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        if (state.sessions.isNotEmpty()) {
            SectionHeader("Sessions de Connexion (${state.sessions.size})")
            state.sessions.forEachIndexed { index, session ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (!session.revoked) Color(0xFFDBEAFE) else VigiaBorder,
                    entranceDelayMillis = index * 40L
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VpnKey,
                                contentDescription = null,
                                tint = VigiaTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = session.deviceLabel.ifBlank { "Session inconnue" },
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = PoppinsFontFamily,
                                color = VigiaTextPrimary,
                                fontSize = 13.5.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Ouverte le ${session.createdAt.take(16).replace("T", " ")} · expire le ${session.expiresAt.take(10)}",
                                fontSize = 11.sp,
                                fontFamily = PoppinsFontFamily,
                                color = VigiaTextMuted
                            )
                        }
                        InfoChip(if (session.revoked) "Révoquée" else "Active", if (session.revoked) RiskDanger else RiskSafe)
                    }
                }
            }
        }
    }
}
