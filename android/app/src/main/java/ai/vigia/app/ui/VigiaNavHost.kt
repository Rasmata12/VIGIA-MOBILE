package ai.vigia.app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ai.vigia.app.ui.screens.*
import ai.vigia.app.ui.theme.*
import ai.vigia.app.ui.vm.*

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

private val NAV_ITEMS = listOf(
    NavItem("dashboard", "Accueil", Icons.Rounded.Shield),
    NavItem("analyze/url", "Vérifier", Icons.Rounded.Search),
    NavItem("before_pay", "Before Pay", Icons.Rounded.AccountBalanceWallet),
    NavItem("guard", "Guard", Icons.Rounded.Security),
    NavItem("settings", "Réglages", Icons.Rounded.Settings)
)

@Composable
fun VigiaApp(onLoggedOut: () -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        containerColor = VigiaCanvas,
        bottomBar = {
            NavigationBar(
                containerColor = VigiaWhite,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .border(
                        androidx.compose.foundation.BorderStroke(1.dp, VigiaBorder),
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                NAV_ITEMS.forEach { item ->
                    val selected = backStack?.destination?.hierarchy?.any {
                        it.route == item.route || (item.route.startsWith("analyze") && it.route?.startsWith("analyze") == true)
                    } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VigiaPrimary,
                            selectedTextColor = VigiaPrimary,
                            unselectedIconColor = VigiaTextMuted,
                            unselectedTextColor = VigiaTextMuted,
                            indicatorColor = Color(0xFFEFF6FF)
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        ) {
            // Accueil / Dashboard
            composable("dashboard") {
                val vm: DashboardViewModel = viewModel()
                LaunchedEffect(currentRoute) { if (currentRoute == "dashboard") vm.refresh() }
                DashboardScreen(
                    viewModel = vm,
                    onAnalyze = { kind -> navController.navigate("analyze/$kind") },
                    onOpenHistory = { navController.navigate("history") },
                    onOpenItem = { navController.navigate("history") },
                    onNavigateToModule = { route -> navController.navigate(route) }
                )
            }

            // Analyseur / Vérification multi-sources
            composable("analyze/url") {
                AnalyzeScreen(viewModel = viewModel(), initialKind = "url", onBack = { navController.popBackStack() })
            }
            composable("analyze/text") {
                AnalyzeScreen(viewModel = viewModel(), initialKind = "text", onBack = { navController.popBackStack() })
            }
            composable("analyze/qr") {
                AnalyzeScreen(viewModel = viewModel(), initialKind = "qr", onBack = { navController.popBackStack() })
            }

            // Before Pay (Anti-Arnaque Transfert)
            composable("before_pay") {
                BeforePayScreen(viewModel = viewModel(), onBack = { navController.popBackStack() })
            }

            // VIGIA Guard (Bouclier en temps réel)
            composable("guard") {
                GuardScreen(
                    viewModel = viewModel(),
                    onBack = { navController.popBackStack() },
                    onOpenPermissions = { navController.navigate("permissions") }
                )
            }

            // Moment & Shield Intelligence
            composable("moment_shield") {
                MomentShieldScreen(viewModel = viewModel(), onBack = { navController.popBackStack() })
            }

            // Historique des analyses
            composable("history") {
                HistoryScreen(viewModel = viewModel())
            }

            // Paramètres & Sécurité
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel(),
                    onLogout = onLoggedOut,
                    onDeleted = onLoggedOut,
                    onOpenPrivacy = { navController.navigate("privacy") },
                    onOpenPermissions = { navController.navigate("permissions") },
                    onOpenDevices = { navController.navigate("devices") }
                )
            }

            // Centre de Confidentialité & RGPD
            composable("privacy") {
                PrivacyScreen(viewModel = viewModel(), onBack = { navController.popBackStack() })
            }

            // Centre des Permissions Android
            composable("permissions") {
                PermissionsScreen(viewModel = viewModel(), onBack = { navController.popBackStack() })
            }

            // Gestion des Appareils
            composable("devices") {
                DevicesScreen(viewModel = viewModel(), onBack = { navController.popBackStack() })
            }

            // Offres d'emploi & de formation
            composable("job_offer") {
                JobOfferScreen(viewModel = viewModel(), onBack = { navController.popBackStack() })
            }

            // Petites annonces
            composable("listing") {
                ListingScreen(viewModel = viewModel(), onBack = { navController.popBackStack() })
            }

            // Espace communautaire
            composable("community") {
                CommunityScreen(viewModel = viewModel(), onBack = { navController.popBackStack() })
            }
        }
    }
}
