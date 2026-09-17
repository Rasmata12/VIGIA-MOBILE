package ai.vigia.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.vigia.app.ui.VigiaApp
import ai.vigia.app.ui.screens.AuthScreen
import ai.vigia.app.ui.theme.BackgroundGradient
import ai.vigia.app.ui.theme.VigiaTheme
import ai.vigia.app.ui.vm.AuthViewModel

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VigiaTheme {
                val authViewModel: AuthViewModel = viewModel()
                val authState by authViewModel.state.collectAsState()
                var loggedIn by remember { mutableStateOf(ServiceLocator.tokens.isLoggedIn) }

                LaunchedEffect(authState.loggedIn) { loggedIn = authState.loggedIn || ServiceLocator.tokens.isLoggedIn }
                LaunchedEffect(loggedIn) {
                    if (loggedIn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                Box(Modifier.fillMaxSize().background(BackgroundGradient)) {
                    if (loggedIn) {
                        VigiaApp(onLoggedOut = {
                            authViewModel.logout()
                            loggedIn = false
                        })
                    } else {
                        AuthScreen(authViewModel) { loggedIn = true }
                    }
                }
            }
        }
    }
}
