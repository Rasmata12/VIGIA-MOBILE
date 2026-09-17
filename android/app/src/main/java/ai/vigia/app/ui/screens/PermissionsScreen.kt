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
            .padding(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Retour", tint = VigiaPrimary)
            }
            Text("Retour", fontFamily = PoppinsFontFamily, color = VigiaPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { viewModel.refresh(context) }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Actualiser", tint = VigiaPrimary)
            }
        }

        Column {
            Text("Centre des Permissions", style = MaterialTheme.typography.headlineMedium, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Audit en temps réel des autorisations système accordées à VIGIA AI.",
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 13.sp
            )
        }

        GlassCard(
            borderColor = Color(0xFFDBEAFE),
            backgroundColor = Color(0xFFEFF6FF)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = VigiaPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Principe du moindre privilège", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaPrimary, fontSize = 13.5.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "VIGIA ne demande aucune permission superflue (aucun accès contacts, aucun accès SMS en arrière-plan, aucune localisation).",
                        fontSize = 12.sp,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextSecondary
                    )
                }
            }
        }

        SectionHeader("Permissions Système")

        state.items.forEach { item ->
            val isGranted = item.state == PermissionCenter.State.GRANTED
            val isActionRequired = item.state == PermissionCenter.State.ACTION_REQUIRED

            GlassCard(
                borderColor = if (isGranted) RiskSafeBorder
                else if (isActionRequired) RiskSuspiciousBorder
                else VigiaBorder
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isGranted) RiskSafeBg
                                else if (isActionRequired) RiskSuspiciousBg
                                else Color(0xFFF1F5F9)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isGranted) Icons.Rounded.Check else Icons.Rounded.PriorityHigh,
                            contentDescription = null,
                            tint = if (isGranted) RiskSafe else RiskSuspicious,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.name, fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = when (item.state) {
                                PermissionCenter.State.GRANTED -> "Accordée"
                                PermissionCenter.State.ACTION_REQUIRED -> "Action requise"
                                PermissionCenter.State.DENIED -> "Refusée"
                                PermissionCenter.State.NOT_REQUIRED -> "Non requise"
                            },
                            fontSize = 11.5.sp,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isGranted) RiskSafe else if (isActionRequired) RiskSuspicious else VigiaTextMuted
                        )
                    }
                    InfoChip(
                        text = if (isGranted) "Actif" else "Inactif",
                        color = if (isGranted) RiskSafe else RiskSuspicious
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(item.why, style = MaterialTheme.typography.bodyMedium, fontFamily = PoppinsFontFamily, color = VigiaTextSecondary, fontSize = 12.5.sp)

                if (item.actionLabel != null && item.intent != null) {
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { context.startActivity(item.intent) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActionRequired) RiskSuspicious else VigiaPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(item.actionLabel, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
