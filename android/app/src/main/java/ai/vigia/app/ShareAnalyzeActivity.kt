package ai.vigia.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.vigia.app.engine.LocalAnalyzer
import ai.vigia.app.ui.components.ErrorBanner
import ai.vigia.app.ui.components.GradientButton
import ai.vigia.app.ui.screens.ResultSection
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.AnalyzeViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Point d'entree "Partager avec VIGIA".
 * Depuis WhatsApp, SMS, Gmail, le navigateur... : Partager -> VIGIA AI.
 * Le contenu partage est analyse immediatement, sans quitter le contexte.
 */
class ShareAnalyzeActivity : ComponentActivity() {

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shared = extractSharedText(intent)

        setContent {
            VigiaTheme {
                val viewModel: AnalyzeViewModel = viewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()
                val loggedIn = ServiceLocator.tokens.isLoggedIn
                val kind = remember(shared) { if (LocalAnalyzer.looksLikeUrl(shared)) "url" else "text" }

                LaunchedEffect(shared) {
                    if (shared.isNotBlank()) viewModel.analyse(kind, shared)
                }

                ModalBottomSheet(
                    onDismissRequest = { finish() },
                    containerColor = VigiaSurface,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 30.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("VIGIA AI", fontWeight = FontWeight.Bold, color = VigiaTextPrimary, fontSize = 18.sp, modifier = Modifier.weight(1f))
                            Text(if (kind == "url") "Lien partagé" else "Message partagé", color = VigiaTextSecondary, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(12.dp))

                        when {
                            shared.isBlank() -> ErrorBanner("Aucun texte n'a été reçu depuis l'application d'origine.")
                            !loggedIn -> Column {
                                ErrorBanner("Connecte-toi à VIGIA pour enregistrer et approfondir cette analyse.")
                                Spacer(Modifier.height(12.dp))
                                GradientButton("Ouvrir VIGIA", {
                                    startActivity(Intent(this@ShareAnalyzeActivity, MainActivity::class.java))
                                    finish()
                                }, Modifier.fillMaxWidth())
                            }
                            state.loading -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 24.dp)) {
                                CircularProgressIndicator(color = VigiaCyan, strokeWidth = 3.dp, modifier = Modifier.size(26.dp))
                                Spacer(Modifier.width(14.dp))
                                Text("Analyse du contenu en cours…", color = VigiaTextSecondary)
                            }
                            state.error != null -> ErrorBanner(state.error!!, onRetry = { viewModel.analyse(kind, shared) })
                            state.result != null -> Column {
                                ResultSection(state.result!!, offline = state.offline)
                                Spacer(Modifier.height(16.dp))
                                GradientButton("Ouvrir dans VIGIA", {
                                    startActivity(Intent(this@ShareAnalyzeActivity, MainActivity::class.java))
                                    finish()
                                }, Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun extractSharedText(intent: Intent?): String = when (intent?.action) {
        Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString().orEmpty()
        Intent.ACTION_VIEW -> intent.dataString.orEmpty()
        else -> ""
    }.trim()
}
