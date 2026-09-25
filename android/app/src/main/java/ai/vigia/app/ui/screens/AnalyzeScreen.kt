package ai.vigia.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MultipartBody
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.border
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
import ai.vigia.app.ServiceLocator
import ai.vigia.app.local.AnalysisEntity
import ai.vigia.app.ui.components.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.AnalyzeViewModel
import ai.vigia.app.ui.vm.parseSignals
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import org.json.JSONObject
import retrofit2.HttpException

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
            .padding(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubtleBackButton(onBack = onBack, label = "Retour")
            Spacer(Modifier.weight(1f))
        }

        Column {
            Text(
                "Vérification de sécurité",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = VigiaTextPrimary,
                fontSize = 24.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Détectez instantanément les faux liens, arnaques et messages piégés.",
                fontFamily = PoppinsFontFamily,
                color = VigiaTextSecondary,
                fontSize = 13.sp
            )
        }

        // Sélecteur de type d'analyse
        ScrollableTabRow(
            selectedTabIndex = when (kind) { "url" -> 0; "text" -> 1; "media" -> 2; else -> 3 },
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, VigiaBorder, RoundedCornerShape(16.dp)),
            containerColor = Color.White,
            contentColor = VigiaPrimary,
            edgePadding = 6.dp,
            divider = {}
        ) {
            Tab(
                selected = kind == "url",
                onClick = { kind = "url"; viewModel.reset() },
                modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (kind == "url") VigiaPrimary.copy(alpha = 0.12f) else Color.Transparent),
                text = {
                    Text(
                        "Lien / URL",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = if (kind == "url") FontWeight.Bold else FontWeight.Medium,
                        color = if (kind == "url") VigiaPrimary else VigiaTextSecondary,
                        fontSize = 12.5.sp
                    )
                }
            )
            Tab(
                selected = kind == "text",
                onClick = { kind = "text"; viewModel.reset() },
                modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (kind == "text") VigiaPrimary.copy(alpha = 0.12f) else Color.Transparent),
                text = {
                    Text(
                        "Message / SMS",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = if (kind == "text") FontWeight.Bold else FontWeight.Medium,
                        color = if (kind == "text") VigiaPrimary else VigiaTextSecondary,
                        fontSize = 12.5.sp
                    )
                }
            )
            Tab(
                selected = kind == "media",
                onClick = { kind = "media"; viewModel.reset() },
                modifier = Modifier.padding(4.dp).clip(RoundedCornerShape(12.dp)).background(if (kind == "media") VigiaPrimary.copy(alpha = 0.12f) else Color.Transparent),
                text = { Text("Photo / Vidéo", fontFamily = PoppinsFontFamily, fontWeight = if (kind == "media") FontWeight.Bold else FontWeight.Medium, color = if (kind == "media") VigiaPrimary else VigiaTextSecondary, fontSize = 11.5.sp) }
            )
            Tab(
                selected = kind == "qr",
                onClick = { kind = "qr"; viewModel.reset() },
                modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (kind == "qr") VigiaPrimary.copy(alpha = 0.12f) else Color.Transparent),
                text = {
                    Text(
                        "Scanner Caméra",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = if (kind == "qr") FontWeight.Bold else FontWeight.Medium,
                        color = if (kind == "qr") VigiaPrimary else VigiaTextSecondary,
                        fontSize = 12.5.sp
                    )
                }
            )
        }

        // ==================================================== ONGLET PHOTO / VIDÉO
        if (kind == "media") {
            MediaAnalyzePanel()
        } else if (kind == "qr") {
            val context = LocalContext.current
            var scanError by remember { mutableStateOf<String?>(null) }
            var lastScanned by remember { mutableStateOf(false) }

            fun launchScanner() {
                val activity = context as? Activity ?: return
                scanError = null
                val options = GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_AZTEC, Barcode.FORMAT_DATA_MATRIX)
                    .build()
                GmsBarcodeScanning.getClient(activity, options)
                    .startScan()
                    .addOnSuccessListener { barcode ->
                        val raw = barcode.rawValue ?: ""
                        content = raw
                        lastScanned = true
                        if (raw.isNotBlank()) {
                            // Analyse automatique immédiate dès que le scan réussit
                            viewModel.analyse("url", raw)
                        }
                    }
                    .addOnFailureListener {
                        scanError = "Le scanner Google n'a pas pu démarrer. Assurez-vous que les Services Google Play sont à jour sur votre téléphone."
                    }
                    .addOnCanceledListener {
                        // Annulé par l'utilisateur
                    }
            }

            // Viseur caméra holographique animé
            CameraScannerOverlay(
                isScanning = true,
                errorDetected = state.result?.level == "dangerous",
                statusText = if (lastScanned && content.isNotBlank())
                    "✓ Code scanné : ${content.take(30)}... Cliquez pour rescanner"
                else
                    "Viseur Google actif • Pointez vers un QR Code pour détection",
                onScanClick = { launchScanner() }
            )

            GradientButton(
                text = "Lancer le Scanner Caméra Google",
                onClick = { launchScanner() },
                icon = Icons.Rounded.QrCodeScanner,
                modifier = Modifier.fillMaxWidth()
            )

            scanError?.let {
                ErrorBanner(it)
            }

            GlassCard(
                backgroundBrush = luxuryCardGradient(VigiaPrimary),
                borderBrush = luxuryBorderGradient(VigiaPrimary)
            ) {
                Text(
                    "Ou saisissez le contenu décodé manuellement :",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = VigiaTextPrimary
                )
                Spacer(Modifier.height(8.dp))
                VigiaField(
                    value = content,
                    onValueChange = { content = it; lastScanned = false },
                    label = "URL ou données du QR code",
                    singleLine = true
                )
                if (content.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    GradientButton(
                        text = "Analyser ce contenu",
                        onClick = { viewModel.analyse("url", content) },
                        loading = state.loading,
                        icon = Icons.Rounded.Search,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            // ==================================================== ONGLETS URL & SMS
            GlassCard(
                backgroundBrush = luxuryCardGradient(if (kind == "url") VigiaPrimary else VigiaSecondary),
                borderBrush = luxuryBorderGradient(if (kind == "url") VigiaPrimary else VigiaSecondary)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (kind == "url") "Lien à inspecter :" else "Message à analyser :",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = VigiaTextPrimary
                    )
                }

                Spacer(Modifier.height(8.dp))

                VigiaField(
                    value = content,
                    onValueChange = { content = it },
                    label = if (kind == "url") "Saisissez ou collez le lien à vérifier" else "Collez le message reçu (SMS, WhatsApp, e-mail)",
                    singleLine = kind == "url",
                    minLines = if (kind == "url") 1 else 4,
                    supporting = if (kind == "url")
                        "Détection des faux domaines, homoglyphes et redirections masquées."
                    else
                        "Confidentialité absolue : aucune donnée personnelle n'est enregistrée sans accord."
                )

                Spacer(Modifier.height(14.dp))

                GradientButton(
                    text = "Démarrer l'audit de sécurité",
                    onClick = { viewModel.analyse(if (kind == "qr") "url" else kind, content) },
                    loading = state.loading,
                    enabled = content.isNotBlank(),
                    icon = Icons.Rounded.Search,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        state.error?.let {
            ErrorBanner(it, onRetry = { viewModel.analyse(if (kind == "qr") "url" else kind, content) })
        }

        state.result?.let { result ->
            SectionHeader("R\u00e9sultat")
            ResultSection(result, offline = state.offline)
        }
    }
}

@Composable
fun ResultSection(result: AnalysisEntity, offline: Boolean) {
    val firstAction = remember(result.id) {
        parseSignals(result.signalsJson).firstOrNull { it.code == "ai_action" }?.label
    }
    val color = riskColor(result.level)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = color.copy(alpha = 0.45f),
        backgroundBrush = luxuryCardGradient(color),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = when (result.level) {
                    "dangerous" -> Icons.Rounded.Warning
                    "suspicious" -> Icons.Rounded.ReportProblem
                    else -> Icons.Rounded.VerifiedUser
                },
                tint = color,
                size = 44.dp,
                iconSize = 22.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(riskLabel(result.level), fontFamily = PoppinsFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = color)
                Text("Niveau de risque", fontFamily = PoppinsFontFamily, fontSize = 12.sp, color = VigiaTextSecondary)
            }
            InfoChip("${result.score}/100", color)
        }

        Spacer(Modifier.height(14.dp))
        Text(result.summary, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 21.sp, color = VigiaTextPrimary)

        firstAction?.let { action ->
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(VigiaPrimarySoft).padding(11.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.Lightbulb, null, tint = VigiaPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(action, Modifier.weight(1f), fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, lineHeight = 18.sp, color = VigiaTextPrimary)
            }
        }

        if (offline || !result.syncedWithServer) {
            Spacer(Modifier.height(10.dp))
            InfoChip("Résultat calculé sans réseau", RiskSuspicious)
        }
    }
}
@Composable
private fun MediaAnalyzePanel() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedType by remember { mutableStateOf("") }
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<ai.vigia.app.net.MediaAnalysisResponse?>(null) }
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selectedUri = uri
        if (uri != null) runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        result = null
        error = null
        if (uri != null) {
            val mime = context.contentResolver.getType(uri).orEmpty()
            val extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(uri.toString()).lowercase()
            selectedType = if (mime.startsWith("video/") || extension in setOf("mp4", "m4v", "mov", "webm", "3gp", "mkv", "avi")) "video/mp4" else mime
            previewBitmap = null
            val type = selectedType
            scope.launch(Dispatchers.IO) {
                val bitmap = runCatching {
                    if (type.startsWith("video/")) {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(context, uri)
                            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { frame ->
                                scaleBitmap(frame, 960).also { if (it !== frame) frame.recycle() }
                            }
                        } finally {
                            retriever.release()
                        }
                    } else {
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        context.contentResolver.openInputStream(uri).use { input -> BitmapFactory.decodeStream(input, null, bounds) }
                        var sample = 1
                        while (bounds.outWidth / sample > 1280 || bounds.outHeight / sample > 1280) sample *= 2
                        val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
                        context.contentResolver.openInputStream(uri).use { input -> BitmapFactory.decodeStream(input, null, options) }
                    }
                }.getOrNull()
                withContext(Dispatchers.Main.immediate) {
                    if (selectedUri == uri) previewBitmap = bitmap else bitmap?.recycle()
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard(backgroundColor = Color.White, borderColor = VigiaBorder, cornerRadius = 18.dp) {
            Text("Analyse visuelle", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = VigiaTextPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Ajoutez une photo ou une vidéo. Elle sera analysée par le moteur vision configuré sur le serveur VIGIA.", fontFamily = PoppinsFontFamily, fontSize = 12.sp, color = VigiaTextSecondary, lineHeight = 17.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { launcher.launch(arrayOf("image/*", "video/*")) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Rounded.UploadFile, null); Spacer(Modifier.width(8.dp)); Text("Choisir une photo ou une vidéo", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold)
            }
            if (selectedUri != null) {
                Spacer(Modifier.height(10.dp))
                Text(if (selectedType.startsWith("video/")) "Vidéo sélectionnée — VIGIA analysera plusieurs images extraites." else "Photo sélectionnée", fontFamily = PoppinsFontFamily, fontSize = 12.sp, color = VigiaTextPrimary, fontWeight = FontWeight.SemiBold)
                previewBitmap?.let { bmp -> Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Fit) }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        loading = true; error = null; result = null
                        scope.launch {
                            runCatching { withContext(Dispatchers.IO) { uploadMediaForAnalysis(context, selectedUri!!, selectedType) } }
                                .onSuccess { result = it }
                                .onFailure { failure ->
                                    val detail = (failure as? HttpException)?.response()?.errorBody()?.string()?.let { body ->
                                        runCatching { JSONObject(body).optString("detail").takeIf(String::isNotBlank) }.getOrNull()
                                    }
                                    error = detail ?: failure.message ?: "Analyse visuelle impossible."
                                }
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(), enabled = !loading, shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VigiaPrimary)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White) else Icon(Icons.Rounded.Visibility, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (loading) "Analyse en cours..." else "Analyser maintenant", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        error?.let { ErrorBanner(it) }
        result?.let { res ->
            val mediaColor = riskColor(res.level)
            GlassCard(backgroundColor = Color.White, borderColor = mediaColor.copy(alpha = 0.45f), cornerRadius = 18.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(if (res.level == "dangerous") Icons.Rounded.Warning else if (res.level == "suspicious") Icons.Rounded.ReportProblem else Icons.Rounded.VerifiedUser, mediaColor, size = 42.dp, iconSize = 21.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(riskLabel(res.level), fontFamily = PoppinsFontFamily, fontWeight = FontWeight.ExtraBold, color = mediaColor, fontSize = 15.sp)
                        Text("${res.score}/100", fontFamily = PoppinsFontFamily, fontSize = 12.sp, color = VigiaTextSecondary)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(res.summary, fontFamily = PoppinsFontFamily, fontSize = 13.sp, color = VigiaTextPrimary, lineHeight = 19.sp)
                res.indicators.take(3).forEach { Text("• $it", fontFamily = PoppinsFontFamily, fontSize = 12.sp, color = VigiaTextSecondary, modifier = Modifier.padding(top = 5.dp)) }
                res.recommendedActions.firstOrNull()?.let { action ->
                    Spacer(Modifier.height(10.dp))
                    Text(action, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = VigiaPrimary, lineHeight = 18.sp)
                }
            }
        }
    }
}


private suspend fun uploadMediaForAnalysis(context: android.content.Context, uri: Uri, mime: String): ai.vigia.app.net.MediaAnalysisResponse {
    val cacheDir = File(context.cacheDir, "media_analysis").apply { mkdirs() }
    val type = if (mime.startsWith("video/")) "video" else "image"
    val frameFiles = if (type == "video") {
        extractVideoFrames(context, uri, cacheDir)
    } else {
        listOf(copyAndCompressImage(context, uri, cacheDir, "photo_${System.currentTimeMillis()}.jpg"))
    }
    require(frameFiles.isNotEmpty()) { "Aucune image exploitable n'a pu être préparée." }
    val parts = frameFiles.mapIndexed { index, file ->
        MultipartBody.Part.createFormData("frames", "frame_$index.jpg", file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
    }
    return try {
        ServiceLocator.api.analyzeMedia(
            parts,
            type.toRequestBody("text/plain".toMediaTypeOrNull()),
            "vigia-media.$type".toRequestBody("text/plain".toMediaTypeOrNull())
        )
    } finally {
        frameFiles.forEach { it.delete() }
    }
}

private fun copyAndCompressImage(context: android.content.Context, uri: Uri, cacheDir: File, name: String): File {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri).use { input -> BitmapFactory.decodeStream(input, null, bounds) }
    val maxSide = 1280
    var sample = 1
    while ((bounds.outWidth / sample) > maxSide || (bounds.outHeight / sample) > maxSide) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
    val bitmap = context.contentResolver.openInputStream(uri).use { input ->
        BitmapFactory.decodeStream(input, null, options)
    } ?: throw IllegalArgumentException("Image illisible.")
    val file = File(cacheDir, name)
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 82, it) }
    bitmap.recycle()
    return file
}

private fun extractVideoFrames(context: android.content.Context, uri: Uri, cacheDir: File): List<File> {
    val retriever = MediaMetadataRetriever(); retriever.setDataSource(context, uri)
    val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    val points = if (durationMs <= 0) listOf(0L) else listOf(0L, durationMs / 2, (durationMs - 1).coerceAtLeast(0))
    val files = mutableListOf<File>()
    points.distinct().forEachIndexed { i, us ->
        retriever.getFrameAtTime(us * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { bitmap ->
            val scaled = scaleBitmap(bitmap, 960)
            bitmap.recycle()
            val file = File(cacheDir, "video_${System.currentTimeMillis()}_$i.jpg")
            FileOutputStream(file).use { scaled.compress(Bitmap.CompressFormat.JPEG, 74, it) }
            scaled.recycle(); files += file
        }
    }
    retriever.release(); return files
}

private fun scaleBitmap(source: Bitmap, maxSide: Int): Bitmap {
    val max = maxOf(source.width, source.height)
    if (max <= maxSide) return source
    val scale = maxSide.toFloat() / max.toFloat()
    return Bitmap.createScaledBitmap(source, (source.width * scale).toInt().coerceAtLeast(1), (source.height * scale).toInt().coerceAtLeast(1), true)
}
