package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.R
import ai.vigia.app.guard.VigiaNotificationListener
import ai.vigia.app.ui.components.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.GuardViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private data class GuardedApp(
    val name: String,
    val desc: String,
    val icon: ImageVector? = null,
    val drawableRes: Int? = null,
    val color: Color
)

@Composable
fun GuardScreen(
    viewModel: GuardViewModel,
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val active = state.localEnabled && state.listenerEnabled

    LaunchedEffect(Unit) {
        viewModel.load(context)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ------------------------------------------------------ Navigation
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubtleBackButton(onBack = onBack, label = "Retour")
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Actualiser", tint = VigiaPrimary)
            }
        }

        Column {
            Text(
                "VIGIA Guard 24/7",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = VigiaTextPrimary,
                fontSize = 24.sp
            )
        }

        // ------------------------------------------------------ HÉROS — Radar de Veille
        HeroSurface(
            orbColors = if (active) listOf(RiskSafe, VigiaCyan) else listOf(VigiaTextMuted, VigiaBorder),
            cornerRadius = 26.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CyberRadarView(active = active, radarSize = 128.dp)

                Spacer(Modifier.height(18.dp))

                Text(
                    text = if (active) "Bouclier Temps Réel Actif" else "Bouclier en Veille",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = PoppinsFontFamily,
                    color = if (active) RiskSafe else VigiaTextSecondary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = if (active)
                        "VIGIA surveille discrètement les notifications de vos applications de messagerie pour neutraliser les menaces avant tout clic."
                    else
                        "Activez le commutateur ci-dessous pour démarrer la protection continue de votre appareil.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = PoppinsFontFamily,
                    color = VigiaTextSecondary,
                    textAlign = TextAlign.Center,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(1.dp, if (state.localEnabled) RiskSafe.copy(alpha = 0.35f) else VigiaBorder, RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBadge(
                        icon = Icons.Rounded.PowerSettingsNew,
                        tint = if (state.localEnabled) RiskSafe else VigiaTextMuted,
                        size = 40.dp,
                        iconSize = 19.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Protection en continu",
                            fontWeight = FontWeight.Bold,
                            fontFamily = PoppinsFontFamily,
                            color = VigiaTextPrimary,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Analyse des liens entrants",
                            fontFamily = PoppinsFontFamily,
                            fontSize = 11.5.sp,
                            color = VigiaTextSecondary
                        )
                    }
                    Switch(
                        checked = state.localEnabled,
                        onCheckedChange = { enabled -> viewModel.toggleGuard(context, enabled) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RiskSafe
                        )
                    )
                }
            }
        }

        // ------------------------------------------------------ Autorisation Système Requise
        if (!state.listenerEnabled) {
            GlassCard(
                borderColor = RiskSuspiciousBorder,
                backgroundColor = RiskSuspiciousBg,
                cornerRadius = 20.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon = Icons.Rounded.Warning, tint = RiskSuspicious, size = 42.dp, iconSize = 21.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Accès aux notifications requis",
                            fontWeight = FontWeight.Bold,
                            fontFamily = PoppinsFontFamily,
                            color = RiskSuspicious,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "Pour intercepter les liens malveillants, VIGIA a besoin de l'autorisation d'écoute des notifications Android.",
                            fontSize = 12.sp,
                            fontFamily = PoppinsFontFamily,
                            color = VigiaTextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                GradientButton(
                    text = "Ouvrir les autorisations Android",
                    onClick = {
                        val intent = VigiaNotificationListener.settingsIntent()
                        context.startActivity(intent)
                    },
                    icon = Icons.Rounded.Settings,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ------------------------------------------------------ Statistiques 24h
        Column {
            SectionHeader("Activité de Surveillance (24h)")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile(
                    label = "Scans Réalisés",
                    value = (state.serverStatus?.eventsLast24h ?: 0).toString(),
                    color = VigiaPrimary,
                    icon = Icons.Rounded.VerifiedUser,
                    badgeText = "24h",
                    modifier = Modifier.weight(1f).height(208.dp),
                    subtitle = "Notifications vérifiées"
                )
                StatTile(
                    label = "Menaces Neutralisées",
                    value = (state.serverStatus?.alertsLast24h ?: 0).toString(),
                    color = if ((state.serverStatus?.alertsLast24h ?: 0) > 0) RiskDanger else RiskSafe,
                    icon = Icons.Rounded.Shield,
                    badgeText = "Bloquées",
                    modifier = Modifier.weight(1f).height(208.dp),
                    subtitle = "Alertes émises"
                )
            }
        }

        // ------------------------------------------------------ Applications Couvertes
        Column {
            SectionHeader("Messageries Protégées par Guard")
            Spacer(Modifier.height(10.dp))
            val apps = listOf(
                GuardedApp("WhatsApp & Business", "Messages, liens et fichiers financiers entrants", drawableRes = R.drawable.ic_whatsapp, color = Color(0xFF25D366)),
                GuardedApp("Google Messages (SMS)", "Tentatives de smishing et faux avis de livraison", drawableRes = R.drawable.ic_sms, color = VigiaPrimary),
                GuardedApp("Telegram Messenger", "Canaux publics et discussions directes", drawableRes = R.drawable.ic_telegram, color = Color(0xFF229ED9)),
                GuardedApp("Gmail & Messageries", "Emails de phishing et pièces jointes piégées", drawableRes = R.drawable.ic_gmail, color = Color(0xFFEA4335)),
                GuardedApp("Facebook & Instagram", "Liens de phishing envoyés en message privé", icon = Icons.Rounded.Forum, color = VigiaViolet)
            )

            GlassCard(
                backgroundBrush = luxuryCardGradient(VigiaPrimary),
                borderBrush = luxuryBorderGradient(VigiaPrimary),
                cornerRadius = 22.dp
            ) {
                apps.forEachIndexed { index, app ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (app.drawableRes != null) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .shadow(4.dp, RoundedCornerShape(12.dp), ambientColor = Color(0x0A000000))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(1.dp, VigiaBorderSubtle, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(app.drawableRes),
                                    contentDescription = app.name,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else if (app.icon != null) {
                            IconBadge(icon = app.icon, tint = app.color, size = 40.dp, iconSize = 19.dp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = app.name,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = PoppinsFontFamily,
                                color = VigiaTextPrimary,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                    if (index < apps.size - 1) {
                        HorizontalDivider(color = VigiaBorderSubtle, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }

        // ------------------------------------------------------ Souveraineté & Zéro Espionnage
        GlassCard(
            borderBrush = luxuryBorderGradient(VigiaViolet),
            backgroundBrush = luxuryCardGradient(VigiaViolet),
            cornerRadius = 20.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = Icons.Rounded.Shield, tint = VigiaViolet, size = 40.dp, iconSize = 19.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Garantie Zéro Espionnage",
                        fontWeight = FontWeight.Bold,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaViolet,
                        fontSize = 14.5.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "VIGIA Guard ne stocke et ne transmet JAMAIS vos conversations privées. Seules les empreintes cryptographiques SHA-256 des URL sont vérifiées.",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}
