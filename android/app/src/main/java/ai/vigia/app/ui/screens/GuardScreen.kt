package ai.vigia.app.ui.screens

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.guard.VigiaNotificationListener
import ai.vigia.app.ui.components.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.GuardViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun GuardScreen(
    viewModel: GuardViewModel,
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load(context)
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
            InfoChip(
                text = if (state.localEnabled && state.listenerEnabled) "Bouclier Actif" else "En pause",
                color = if (state.localEnabled && state.listenerEnabled) RiskSafe else RiskSuspicious
            )
        }

        Column {
            Text("VIGIA Guard", style = MaterialTheme.typography.headlineMedium, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Protection proactive en temps réel contre les liens et messages frauduleux reçus par notification.",
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 13.sp
            )
        }

        // Vue centrale avec radar animé
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderBrush = if (state.localEnabled && state.listenerEnabled) CardBorderGradient else null
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CyberRadarView(
                    active = state.localEnabled && state.listenerEnabled,
                    size = 140.dp
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = if (state.localEnabled && state.listenerEnabled)
                        "Bouclier de protection actif"
                    else
                        "Bouclier en pause ou en attente",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = PoppinsFontFamily,
                    color = if (state.localEnabled && state.listenerEnabled) RiskSafe else VigiaTextSecondary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = if (state.localEnabled && state.listenerEnabled)
                        "VIGIA surveille discrètement vos notifications de messagerie et vous alerte en cas de menace avérée."
                    else
                        "Activez le commutateur ci-dessous pour démarrer la protection proactive de vos applications.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = PoppinsFontFamily,
                    color = VigiaTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Activer la protection en direct", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Détection automatique des liens piégés", fontFamily = PoppinsFontFamily, fontSize = 11.5.sp, color = VigiaTextMuted)
                    }
                    Switch(
                        checked = state.localEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleGuard(context, enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = VigiaPrimary
                        )
                    )
                }
            }
        }

        // Statut d'autorisation système Android
        if (!state.listenerEnabled) {
            GlassCard(
                borderColor = RiskSuspiciousBorder,
                backgroundColor = RiskSuspiciousBg
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = RiskSuspicious,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Autorisation Android requise", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = RiskSuspicious, fontSize = 14.sp)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "Pour analyser les notifications entrantes, VIGIA a besoin de l'accès aux notifications.",
                            fontSize = 12.sp,
                            fontFamily = PoppinsFontFamily,
                            color = VigiaTextSecondary
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                GradientButton(
                    text = "Ouvrir les réglages Android",
                    onClick = {
                        val intent = VigiaNotificationListener.settingsIntent()
                        context.startActivity(intent)
                    },
                    icon = Icons.Rounded.Settings,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Statistiques 24 heures réelles
        SectionHeader("Activité des dernières 24h")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile(
                label = "Scans effectués",
                value = (state.serverStatus?.eventsLast24h ?: 0).toString(),
                color = VigiaPrimary,
                modifier = Modifier.weight(1f),
                subtitle = "Notifications vérifiées"
            )
            StatTile(
                label = "Menaces bloquées",
                value = (state.serverStatus?.alertsLast24h ?: 0).toString(),
                color = if ((state.serverStatus?.alertsLast24h ?: 0) > 0) RiskDanger else RiskSafe,
                modifier = Modifier.weight(1f),
                subtitle = "Alertes émises"
            )
        }

        // Applications couvertes
        SectionHeader("Applications sécurisées par Guard")
        GlassCard {
            val apps = listOf(
                Pair("WhatsApp & WA Business", "Messages, liens et transferts entrants"),
                Pair("Google Messages & SMS", "Smishing et faux avis de livraison"),
                Pair("Telegram Messenger", "Canaux publics et discussions directes"),
                Pair("Gmail & Outlook", "Emails d'hameçonnage et pièces jointes"),
                Pair("Messenger & Instagram Direct", "Liens suspects reçus en privé")
            )

            apps.forEachIndexed { index, (name, desc) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChatBubble,
                            contentDescription = null,
                            tint = VigiaPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, fontWeight = FontWeight.SemiBold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(desc, fontFamily = PoppinsFontFamily, color = VigiaTextSecondary, fontSize = 11.5.sp)
                    }
                    InfoChip("Protégé", RiskSafe)
                }
                if (index < apps.size - 1) {
                    HorizontalDivider(color = VigiaBorderSubtle, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }

        // Carte Souveraineté & Confidentialité
        GlassCard(
            borderColor = Color(0xFFE9D5FF),
            backgroundColor = Color(0xFFFAF5FF)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = VigiaViolet,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text("Minimisation stricte des données", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaViolet, fontSize = 14.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "VIGIA Guard ne stocke JAMAIS le contenu complet de vos conversations. " +
                    "Seules une empreinte cryptographique SHA-256 et les règles de risque détectées sont traitées.",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
