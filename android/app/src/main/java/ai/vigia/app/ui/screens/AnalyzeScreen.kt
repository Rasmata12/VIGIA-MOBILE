package ai.vigia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vigia.app.local.AnalysisEntity
import ai.vigia.app.ui.components.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.AnalyzeViewModel
import ai.vigia.app.ui.vm.parseSignals
import ai.vigia.app.ui.vm.parseSources
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AnalyzeScreen(
    viewModel: AnalyzeViewModel,
    initialKind: String,
    prefilled: String = "",
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var kind by remember { mutableStateOf(initialKind) }
    var content by remember { mutableStateOf(prefilled) }

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
            InfoChip("Moteur Hybride L1-L6", VigiaPrimary)
        }

        Column {
            Text("Vérification Forensique", style = MaterialTheme.typography.headlineMedium, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Analyse heuristique, réputationnelle et IA pour détecter hameçonnage, escroqueries et malwares.",
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 13.sp
            )
        }

        TabRow(
            selectedTabIndex = when (kind) { "url" -> 0; "text" -> 1; else -> 2 },
            modifier = Modifier.clip(RoundedCornerShape(14.dp)),
            containerColor = VigiaWhite,
            contentColor = VigiaPrimary
        ) {
            Tab(
                selected = kind == "url",
                onClick = { kind = "url"; viewModel.reset() },
                text = { Text("Lien / URL", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = kind == "text",
                onClick = { kind = "text"; viewModel.reset() },
                text = { Text("Message / SMS", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = kind == "qr",
                onClick = { kind = "qr"; viewModel.reset() },
                text = { Text("Scanner QR", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold) }
            )
        }

        if (kind == "qr") {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = Color(0xFFDBEAFE)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFEFF6FF))
                            .border(2.dp, VigiaPrimary, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = "Scanner QR",
                            tint = VigiaPrimary,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Scanner un QR code suspect", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, fontSize = 15.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Pointez votre appareil vers un QR code suspect pour décoder l'URL et exécuter l'audit complet.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = PoppinsFontFamily,
                        color = VigiaTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    VigiaField(
                        value = content,
                        onValueChange = { content = it },
                        label = "Ou collez le contenu du QR code décodé",
                        singleLine = true
                    )
                }
            }
        } else {
            GlassCard {
                VigiaField(
                    value = content,
                    onValueChange = { content = it },
                    label = if (kind == "url") "Collez le lien complet à analyser" else "Collez le message (SMS, WhatsApp, email)",
                    singleLine = kind == "url",
                    minLines = if (kind == "url") 1 else 5,
                    supporting = if (kind == "url")
                        "Exemple : https://securite-connexion-orange.xyz/login"
                    else
                        "Confidentialité garantie : minimisation des données conforme RGPD."
                )

                Spacer(Modifier.height(14.dp))

                GradientButton(
                    text = "Démarrer l'analyse de sécurité",
                    onClick = { viewModel.analyse(if (kind == "qr") "url" else kind, content) },
                    loading = state.loading,
                    enabled = content.isNotBlank(),
                    icon = Icons.Rounded.Search,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (kind == "qr" && content.isNotBlank()) {
            GradientButton(
                text = "Analyser le QR décodé",
                onClick = { viewModel.analyse("url", content) },
                loading = state.loading,
                icon = Icons.Rounded.Search,
                modifier = Modifier.fillMaxWidth()
            )
        }

        state.error?.let {
            ErrorBanner(it, onRetry = { viewModel.analyse(if (kind == "qr") "url" else kind, content) })
        }

        state.result?.let { result ->
            SectionHeader("Rapport Forensique Complet")
            ResultSection(result, offline = state.offline)
        }
    }
}

@Composable
fun ResultSection(result: AnalysisEntity, offline: Boolean) {
    val signals = remember(result.id) { parseSignals(result.signalsJson) }
    val sources = remember(result.id) { parseSources(result.sourcesJson) }
    val indicators = signals.filter { it.weight > 0 && it.code != "ai_action" }.sortedByDescending { it.weight }
    val actions = signals.filter { it.code == "ai_action" }
    val reassuring = signals.filter { it.weight < 0 }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Carte verdict avec jauge centrale
        GlassCard(
            Modifier.fillMaxWidth(),
            borderBrush = when (result.level) {
                "dangerous" -> DangerBorderGradient
                "safe" -> SafeBorderGradient
                else -> CardBorderGradient
            },
            backgroundColor = riskBackground(result.level)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                RiskGauge(result.score, result.level, size = 180.dp)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = result.summary,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = VigiaTextPrimary
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(
                    text = if (result.aiUsed) "IA Activée" else "Moteur Heuristique",
                    color = if (result.aiUsed) VigiaViolet else VigiaPrimary
                )
                if (offline || !result.syncedWithServer) {
                    InfoChip("Analyse Locale", RiskSuspicious)
                }
            }
        }

        // Signaux et indicateurs de risque
        if (indicators.isNotEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Signaux de Fraude Identifiés (${indicators.size})", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
                Spacer(Modifier.height(10.dp))
                indicators.forEach { signal ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            Modifier
                                .padding(top = 5.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (signal.weight >= 25) RiskDanger
                                    else if (signal.weight >= 12) RiskSuspicious
                                    else VigiaPrimary
                                )
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = signal.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = VigiaTextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                InfoChip("+${signal.weight}", if (signal.weight >= 25) RiskDanger else RiskSuspicious)
                            }
                            if (signal.evidence.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Preuve: ${signal.evidence}",
                                    fontSize = 11.5.sp,
                                    fontFamily = PoppinsFontFamily,
                                    color = VigiaTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recommandations tactiques
        if (actions.isNotEmpty()) {
            GlassCard(
                Modifier.fillMaxWidth(),
                borderColor = Color(0xFFE9D5FF),
                backgroundColor = Color(0xFFFAF5FF)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = VigiaViolet,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Mesures de Sécurité Immédiates", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaViolet, fontSize = 14.5.sp)
                }
                Spacer(Modifier.height(8.dp))
                actions.forEach {
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                        Text("•", color = VigiaViolet, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text(it.label, style = MaterialTheme.typography.bodyMedium, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
                    }
                }
            }
        }

        // Éléments rassurants
        if (reassuring.isNotEmpty()) {
            GlassCard(
                Modifier.fillMaxWidth(),
                borderColor = RiskSafeBorder,
                backgroundColor = RiskSafeBg
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = RiskSafe,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Points de Confiance Détectés", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = RiskSafe, fontSize = 14.sp)
                }
                Spacer(Modifier.height(8.dp))
                reassuring.forEach {
                    Text("• ${it.label}", style = MaterialTheme.typography.bodyMedium, fontFamily = PoppinsFontFamily, color = VigiaTextSecondary)
                }
            }
        }

        // Sondes réseau et sources d'intelligence
        if (sources.isNotEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Sondes Réseau & Moteurs Consultés", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary)
                Spacer(Modifier.height(10.dp))
                sources.forEach { source ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(source.name, style = MaterialTheme.typography.bodyMedium, fontFamily = PoppinsFontFamily, color = VigiaTextPrimary, modifier = Modifier.weight(1f))
                        InfoChip(
                            text = when (source.status) {
                                "ok" -> "Vérifié"
                                "flagged" -> "Menace Détectée"
                                "disabled" -> "Non Configuré"
                                "error" -> "Indisponible"
                                else -> source.status
                            },
                            color = when (source.status) {
                                "flagged" -> RiskDanger
                                "disabled", "error" -> VigiaTextMuted
                                else -> RiskSafe
                            }
                        )
                    }
                    if (source.detail.isNotBlank()) {
                        Text(source.detail, fontSize = 11.sp, fontFamily = PoppinsFontFamily, color = VigiaTextMuted)
                    }
                }
            }
        }
    }
}
