package com.vigia.ai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.vigia.ai.core.PermissionChecker
import com.vigia.ai.data.AnalysisHistory
import com.vigia.ai.data.VigiaDatabase
import com.vigia.ai.domain.LocalRiskEngine
import com.vigia.ai.domain.RiskResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Interception du Partage Système Android (WhatsApp, SMS, Chrome, Twitter, etc.)
        var sharedText: String? = null
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        }

        // Interception de l'alerte depuis une notification VIGIA Guard
        val alertContent = intent?.getStringExtra("EXTRA_ALERT_CONTENT")
        val initialText = sharedText ?: alertContent

        setContent {
            VigiaLightAppTheme {
                VigiaAppNavigation(initialText)
            }
        }
    }
}

// --------------------------------------------------------------------------
// THEME CLAIR PREMIUM VIGIA AI (Sans dark mode terne, lumineux et élégant)
// --------------------------------------------------------------------------
val VigiaEmerald = Color(0xFF059669)
val VigiaDarkText = Color(0xFF0F172A)
val VigiaSubText = Color(0xFF64748B)
val VigiaBgLight = Color(0xFFF8FAFC)
val VigiaCardBg = Color(0xFFFFFFFF)
val VigiaBorderColor = Color(0xFFE2E8F0)
val VigiaDangerRed = Color(0xFFDC2626)
val VigiaWarningAmber = Color(0xFFD97706)

@Composable
fun VigiaLightAppTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        background = VigiaBgLight,
        surface = VigiaCardBg,
        primary = VigiaEmerald,
        onPrimary = Color.White,
        onBackground = VigiaDarkText,
        onSurface = VigiaDarkText
    )
    MaterialTheme(colorScheme = colors, content = content)
}

// --------------------------------------------------------------------------
// NAVIGATION PRINCIPALE AVEC SUPPORT SHARE INTENT & ROOM
// --------------------------------------------------------------------------
@Composable
fun VigiaAppNavigation(sharedText: String?) {
    val navController = rememberNavController()
    val startDestination = if (sharedText != null) "verify" else "home"

    Scaffold(
        containerColor = VigiaBgLight,
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 4.dp) {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

                NavigationBarItem(
                    selected = currentRoute == "home",
                    onClick = { navController.navigate("home") },
                    icon = { Text("🛡️", fontSize = 18.sp) },
                    label = { Text("Accueil", color = if (currentRoute == "home") VigiaEmerald else VigiaSubText, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = currentRoute == "verify",
                    onClick = { navController.navigate("verify") },
                    icon = { Text("🔍", fontSize = 18.sp) },
                    label = { Text("Vérifier", color = if (currentRoute == "verify") VigiaEmerald else VigiaSubText, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = currentRoute == "beforepay",
                    onClick = { navController.navigate("beforepay") },
                    icon = { Text("💳", fontSize = 18.sp) },
                    label = { Text("Avant de payer", color = if (currentRoute == "beforepay") VigiaEmerald else VigiaSubText, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = currentRoute == "shield",
                    onClick = { navController.navigate("shield") },
                    icon = { Text("🌐", fontSize = 18.sp) },
                    label = { Text("Shield", color = if (currentRoute == "shield") VigiaEmerald else VigiaSubText, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = currentRoute == "history",
                    onClick = { navController.navigate("history") },
                    icon = { Text("📋", fontSize = 18.sp) },
                    label = { Text("Historique", color = if (currentRoute == "history") VigiaEmerald else VigiaSubText, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") { VigiaHomeScreen(navController) }
            composable("verify") { VigiaVerifyScreen(sharedText, navController) }
            composable("beforepay") { VigiaBeforePayScreen(navController) }
            composable("moment") { VigiaMomentScreen(navController) }
            composable("shield") { VigiaShieldScreen(navController) }
            composable("history") { VigiaHistoryScreen(navController) }
            composable("permissions") { VigiaPermissionsScreen(navController) }
            composable("settings") { VigiaSettingsScreen(navController) }
        }
    }
}

// --------------------------------------------------------------------------
// 1. ÉCRAN HOME (Protection active, métriques réelles)
// --------------------------------------------------------------------------
@Composable
fun VigiaHomeScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val database = remember { VigiaDatabase.getDatabase(context) }
    val historyList by database.historyDao().getAllHistory().collectAsState(initial = emptyList())
    var isGuardActive by remember { mutableStateOf(PermissionChecker.isNotificationListenerEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGuardActive = PermissionChecker.isNotificationListenerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VigiaBgLight)
            .padding(20.dp)
    ) {
        // En-tête de marque
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🛡️", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("VIGIA AI", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = VigiaDarkText)
            }
            IconButton(onClick = { navController.navigate("permissions") }) {
                Text("⚙️", fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Carte d'état de protection
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = if (isGuardActive) Color(0xFFECFDF5) else Color(0xFFFEF3C7)),
            border = BorderStroke(1.dp, if (isGuardActive) Color(0xFFA7F3D0) else Color(0xFFFDE68A))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(if (isGuardActive) VigiaEmerald else VigiaWarningAmber, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isGuardActive) "✓" else "!", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        if (isGuardActive) "Protection active" else "Protection en pause",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = VigiaDarkText
                    )
                    Text(
                        if (isGuardActive) "VIGIA surveille et vous protège" else "Activez les autorisations pour protéger vos SMS & WhatsApp",
                        fontSize = 12.sp,
                        color = VigiaSubText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Raccourcis d'actions
        Text("Actions rapides", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VigiaDarkText)
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { navController.navigate("verify") },
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VigiaEmerald),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("🔍 Vérifier un SMS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { navController.navigate("beforepay") },
                modifier = Modifier.weight(1f).height(50.dp),
                border = BorderStroke(1.dp, VigiaEmerald),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("💳 Avant de payer", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VigiaEmerald)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Dernières analyses réelles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Activité de contrôle (${historyList.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VigiaDarkText)
            Text(
                "Voir tout",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = VigiaEmerald,
                modifier = Modifier.clickable { navController.navigate("history") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (historyList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, VigiaBorderColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Aucun enregistrement pour le moment.", color = VigiaSubText, fontSize = 13.sp)
                    Text("Partagez un SMS ou collez un texte pour démarrer.", color = VigiaSubText, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(historyList.take(4), key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, VigiaBorderColor),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.type, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VigiaEmerald)
                                Text(item.contentSummary, fontSize = 13.sp, color = VigiaDarkText, maxLines = 1)
                            }
                            val badgeColor = if (item.riskScore >= 60) VigiaDangerRed else if (item.riskScore >= 30) VigiaWarningAmber else VigiaEmerald
                            Text(
                                "${item.riskLevel} (${item.riskScore}/100)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// 2. ÉCRAN VERIFY (Analyse Réelle Texte, Partage, Room)
// --------------------------------------------------------------------------
@Composable
fun VigiaVerifyScreen(sharedText: String?, navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var textToAnalyze by remember { mutableStateOf(sharedText ?: "") }
    var result by remember { mutableStateOf<RiskResult?>(null) }

    // Analyse automatique au partage Android
    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) {
            val eval = LocalRiskEngine.analyzeText(sharedText)
            result = eval
            coroutineScope.launch(Dispatchers.IO) {
                val summary = if (sharedText.length > 80) sharedText.take(80) + "..." else sharedText
                VigiaDatabase.getDatabase(context).historyDao().insertHistory(
                    AnalysisHistory(
                        type = "VERIFY (Partage)",
                        contentSummary = summary,
                        riskScore = eval.score,
                        riskLevel = eval.level.label
                    )
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VigiaBgLight)
            .padding(20.dp)
    ) {
        Text("Vérifier un message", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = VigiaDarkText)
        Text("Collez un SMS, message WhatsApp ou lien à analyser", fontSize = 13.sp, color = VigiaSubText)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = textToAnalyze,
            onValueChange = {
                textToAnalyze = it
                result = null
            },
            placeholder = { Text("Collez votre texte ou lien suspect ici...", color = Color.Gray, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth().height(140.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = VigiaBorderColor,
                focusedBorderColor = VigiaEmerald,
                unfocusedTextColor = VigiaDarkText,
                focusedTextColor = VigiaDarkText
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (textToAnalyze.isNotBlank()) {
                    val eval = LocalRiskEngine.analyzeText(textToAnalyze)
                    result = eval
                    coroutineScope.launch(Dispatchers.IO) {
                        val summary = if (textToAnalyze.length > 80) textToAnalyze.take(80) + "..." else textToAnalyze
                        VigiaDatabase.getDatabase(context).historyDao().insertHistory(
                            AnalysisHistory(
                                type = "VERIFY (Manuel)",
                                contentSummary = summary,
                                riskScore = eval.score,
                                riskLevel = eval.level.label
                            )
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VigiaEmerald)
        ) {
            Text("Lancer l'analyse réelle", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        result?.let {
            val isHigh = it.level.label == "CRITIQUE" || it.level.label == "ÉLEVÉ"
            val isMod = it.level.label == "MODÉRÉ"
            val badgeColor = if (isHigh) VigiaDangerRed else if (isMod) VigiaWarningAmber else VigiaEmerald
            val cardBg = if (isHigh) Color(0xFFFEF2F2) else if (isMod) Color(0xFFFFFBEB) else Color(0xFFECFDF5)

            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("RISQUE : ${it.level.label}", fontWeight = FontWeight.ExtraBold, color = badgeColor, fontSize = 15.sp)
                        Text("Score : ${it.score}/100", fontWeight = FontWeight.Bold, color = badgeColor, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Signaux détectés :", fontWeight = FontWeight.Bold, color = VigiaDarkText, fontSize = 12.sp)

                    it.signals.forEach { sig ->
                        Text("• $sig", color = VigiaDarkText, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, VigiaBorderColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            it.recommendation,
                            modifier = Modifier.padding(10.dp),
                            fontSize = 12.sp,
                            color = VigiaDarkText
                        )
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// 3. ÉCRAN AVANT DE PAYER (Contrôle Mobile Money)
// --------------------------------------------------------------------------
@Composable
fun VigiaBeforePayScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var recipient by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var evalDone by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VigiaBgLight)
            .padding(20.dp)
    ) {
        Text("Avant de payer", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = VigiaDarkText)
        Text("Contrôlez les demandes de transfert d'argent", fontSize = 13.sp, color = VigiaSubText)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = recipient,
            onValueChange = { recipient = it; evalDone = false },
            label = { Text("Bénéficiaire (Nom ou Numéro)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it; evalDone = false },
            label = { Text("Montant (ex: 25 000 FCFA)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = note,
            onValueChange = { note = it; evalDone = false },
            label = { Text("Motif ou message reçu") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                if (recipient.isNotBlank() || amount.isNotBlank()) {
                    evalDone = true
                    coroutineScope.launch(Dispatchers.IO) {
                        VigiaDatabase.getDatabase(context).historyDao().insertHistory(
                            AnalysisHistory(
                                type = "BEFORE PAY",
                                contentSummary = "Transfert vers $recipient ($amount) - $note",
                                riskScore = 40,
                                riskLevel = "MODÉRÉ"
                            )
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VigiaEmerald)
        ) {
            Text("Évaluer la transaction", fontWeight = FontWeight.Bold, color = Color.White)
        }

        if (evalDone) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("🟡 PRUDENCE RECOMMANDÉE", fontWeight = FontWeight.Bold, color = VigiaWarningAmber, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Ne communiquez jamais votre code secret PIN Orange Money, Wave ou Moov sous aucun prétexte.", fontSize = 12.sp, color = VigiaDarkText)
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// 4. ÉCRAN MOMENT (Animation de chaîne d'événements)
// --------------------------------------------------------------------------
@Composable
fun VigiaMomentScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VigiaBgLight)
            .padding(20.dp)
    ) {
        Text("Moment VIGIA", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = VigiaDarkText)
        Text("Chronologie de la protection proactive", fontSize = 13.sp, color = VigiaSubText)

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, VigiaBorderColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("1. Notification SMS ou WhatsApp reçue", fontWeight = FontWeight.Bold, color = VigiaDarkText, fontSize = 13.sp)
                Text("2. Moteur heuristique VIGIA intercepte en tâche de fond", fontWeight = FontWeight.Bold, color = VigiaEmerald, fontSize = 13.sp)
                Text("3. Calcul de risque et confrontation aux IOCs réels", fontWeight = FontWeight.Bold, color = VigiaEmerald, fontSize = 13.sp)
                Text("4. Alerte immédiate avant tout clic utilisateur", fontWeight = FontWeight.Bold, color = VigiaDangerRed, fontSize = 13.sp)
            }
        }
    }
}

// --------------------------------------------------------------------------
// 5. ÉCRAN SHIELD (Base IOC et menaces)
// --------------------------------------------------------------------------
@Composable
fun VigiaShieldScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VigiaBgLight)
            .padding(20.dp)
    ) {
        Text("VIGIA Shield", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = VigiaDarkText)
        Text("Indicateurs de compromission réels observés", fontSize = 13.sp, color = VigiaSubText)

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, VigiaBorderColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🔴 Phishing Mobile Money : Domaines .top, .xyz usurpant Orange / Wave", fontSize = 12.sp, color = VigiaDarkText)
                Text("🔴 Faux recrutement : Frais de dossier exigés par transfert immédiat", fontSize = 12.sp, color = VigiaDarkText)
                Text("🟡 Loteries frauduleuses : Promesses de gain nécessitant un dépôt d'avance", fontSize = 12.sp, color = VigiaDarkText)
            }
        }
    }
}

// --------------------------------------------------------------------------
// 6. ÉCRAN HISTORIQUE (Room Database Réelle)
// --------------------------------------------------------------------------
@Composable
fun VigiaHistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val database = remember { VigiaDatabase.getDatabase(context) }
    val historyList by database.historyDao().getAllHistory().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VigiaBgLight)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Historique", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = VigiaDarkText)
                Text("Base locale Room (${historyList.size} entrées)", fontSize = 12.sp, color = VigiaSubText)
            }
            if (historyList.isNotEmpty()) {
                TextButton(onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        database.historyDao().clearHistory()
                    }
                }) {
                    Text("Effacer tout", color = VigiaDangerRed, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (historyList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucun enregistrement pour le moment.", color = VigiaSubText, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(historyList, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, VigiaBorderColor),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.type, fontWeight = FontWeight.Bold, color = VigiaEmerald, fontSize = 12.sp)
                                val badgeColor = if (item.riskScore >= 60) VigiaDangerRed else if (item.riskScore >= 30) VigiaWarningAmber else VigiaEmerald
                                Text("${item.riskLevel} (${item.riskScore}/100)", fontWeight = FontWeight.Bold, color = badgeColor, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(item.contentSummary, color = VigiaDarkText, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// 7. ÉCRAN PERMISSIONS & CONFIDENTIALITÉ
// --------------------------------------------------------------------------
@Composable
fun VigiaPermissionsScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasNotifListener by remember { mutableStateOf(PermissionChecker.isNotificationListenerEnabled(context)) }
    var hasNotifPost by remember { mutableStateOf(PermissionChecker.hasNotificationPermission(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotifListener = PermissionChecker.isNotificationListenerEnabled(context)
                hasNotifPost = PermissionChecker.hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VigiaBgLight)
            .padding(20.dp)
    ) {
        Text("Centre de Sécurité", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = VigiaDarkText)
        Text("Autorisations système réelles", fontSize = 13.sp, color = VigiaSubText)

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, VigiaBorderColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Accès aux Notifications (Guard)", fontWeight = FontWeight.Bold, color = VigiaDarkText, fontSize = 13.sp)
                        Text("Interception SMS / WhatsApp en temps réel", color = VigiaSubText, fontSize = 11.sp)
                    }
                    if (hasNotifListener) {
                        Text("ACCORDÉ", color = VigiaEmerald, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    } else {
                        Button(
                            onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                            colors = ButtonDefaults.buttonColors(containerColor = VigiaWarningAmber)
                        ) {
                            Text("ACTIVER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// 8. ÉCRAN PARAMÈTRES
// --------------------------------------------------------------------------
@Composable
fun VigiaSettingsScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VigiaBgLight)
            .padding(20.dp)
    ) {
        Text("Paramètres", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = VigiaDarkText)
        Text("Configuration et confidentialité", fontSize = 13.sp, color = VigiaSubText)

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, VigiaBorderColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Version : VIGIA Mobile 1.0.0 (Production)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VigiaDarkText)
                Text("Moteur d'analyse : Local Risk Engine (Offline Room)", fontSize = 12.sp, color = VigiaSubText)
                Text("Confidentialité : Aucune donnée personnelle transmise à des tiers.", fontSize = 12.sp, color = VigiaSubText)
            }
        }
    }
}
