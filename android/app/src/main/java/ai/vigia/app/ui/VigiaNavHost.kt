package ai.vigia.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

// Transitions fluides communes à tout l'app : un fondu + glissement léger, nettement
// plus doux que le changement d'écran instantané par défaut de Navigation Compose.
private const val NAV_ANIM_MS = 260

private val navEnter = fadeIn(tween(NAV_ANIM_MS)) + slideInHorizontally(tween(NAV_ANIM_MS)) { it / 6 }
private val navExit = fadeOut(tween(NAV_ANIM_MS)) + slideOutHorizontally(tween(NAV_ANIM_MS)) { -it / 6 }
private val navPopEnter = fadeIn(tween(NAV_ANIM_MS)) + slideInHorizontally(tween(NAV_ANIM_MS)) { -it / 6 }
private val navPopExit = fadeOut(tween(NAV_ANIM_MS)) + slideOutHorizontally(tween(NAV_ANIM_MS)) { it / 6 }

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

// 5 onglets : Guard au centre, toujours mis en avant comme un vrai bouton d'action —
// les outils spécialisés et l'accès à l'Académie vivent dans "Services"; le Profil reste
// réservé au compte et aux préférences.
private const val CENTER_INDEX = 2

private val NAV_ITEMS = listOf(
    NavItem("dashboard", "Accueil", Icons.Rounded.Home),
    NavItem("analyze/url", "Vérifier", Icons.Rounded.Search),
    NavItem("guard", "Guard", Icons.Rounded.Security),
    NavItem("services", "Services", Icons.Rounded.Apps),
    NavItem("profile", "Profil", Icons.Rounded.Person)
)

/** Barre de navigation ultra-premium en verre dépoli blanc lumineux :
 * - Surface immaculée avec ombre portée douce et micro-liseré spéculaire
 * - 5 onglets : Accueil, Vérifier, GUARD au centre (bouton d'action surélevé avec aura vivante), Services, Profil
 * - Bulle de sélection glissante par ressort physique et typographie Poppins nette */
@Composable
private fun LiquidBottomBar(
    items: List<NavItem>,
    isSelected: (NavItem) -> Boolean,
    isGuardActive: Boolean,
    onSelect: (NavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = items.indexOfFirst(isSelected).coerceAtLeast(0)
    val barHeight = 68.dp
    val fabSize = 58.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp)
            .padding(bottom = 6.dp, top = 16.dp)
    ) {
        val isCompact = maxWidth < 360.dp
        val itemWidth = maxWidth / items.size
        val isCenterSelected = selectedIndex == CENTER_INDEX

        val indicatorWidth = minOf(itemWidth - 4.dp, 48.dp)
        val indicatorX by animateDpAsState(
            targetValue = itemWidth * selectedIndex + (itemWidth - indicatorWidth) / 2,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "dockIndicatorX"
        )

        // Corps principal de la barre : Verre dépoli blanc pur
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .align(Alignment.BottomCenter)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = Color(0x14000000),
                    spotColor = Color(0x220B1930)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White)
                .border(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(VigiaBorder),
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            // Indicateur de sélection glissant (pour les onglets non-centraux)
            if (!isCenterSelected) {
                Box(
                    modifier = Modifier
                        .offset(x = indicatorX, y = 8.dp)
                        .size(width = indicatorWidth, height = 36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(VigiaPrimary.copy(alpha = 0.10f))
                )
            }

            // Ligne des 5 onglets
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { i, item ->
                    val selected = i == selectedIndex
                    val isCenter = i == CENTER_INDEX

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelect(item) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCenter) {
                            // Espace réservé pour le bouton Guard flottant
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(top = 34.dp)
                            ) {
                                Text(
                                    text = "Guard",
                                    fontSize = 11.sp,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (selected) (if (isGuardActive) RiskSafe else VigiaPrimary) else VigiaTextSecondary
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (selected) VigiaPrimary else VigiaTextSecondary,
                                    modifier = Modifier.size(23.dp)
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = item.label,
                                    fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) VigiaPrimary else VigiaTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bouton central flottant GUARD surélevé avec aura protectrice
        val centerX = itemWidth * CENTER_INDEX + itemWidth / 2
        val infiniteTransition = rememberInfiniteTransition(label = "guardPulse")
        val pulseRingScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (isGuardActive) 1.25f else 1.05f,
            animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
            label = "guardPulseRing"
        )
        val pulseRingAlpha by infiniteTransition.animateFloat(
            initialValue = if (isGuardActive) 0.5f else 0.15f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
            label = "guardPulseAlpha"
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = centerX - fabSize / 2, y = -(barHeight / 2 - 12.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Anneau d'onde cyber
            if (isGuardActive || isCenterSelected) {
                Box(
                    modifier = Modifier
                        .size(fabSize * pulseRingScale)
                        .clip(CircleShape)
                        .background((if (isGuardActive) RiskSafe else VigiaPrimary).copy(alpha = pulseRingAlpha))
                )
            }

            // Bouton rond Guard
            Box(
                modifier = Modifier
                    .size(fabSize)
                    .shadow(
                        elevation = 14.dp,
                        shape = CircleShape,
                        ambientColor = (if (isGuardActive) RiskSafe else VigiaPrimary).copy(alpha = 0.40f),
                        spotColor = (if (isGuardActive) RiskSafe else VigiaPrimary).copy(alpha = 0.50f)
                    )
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.SolidColor(if (isGuardActive) RiskSafe else VigiaPrimary)
                    )
                    .border(
                        width = if (isCenterSelected) 3.dp else 1.5.dp,
                        color = Color.White,
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(items[CENTER_INDEX]) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = "Guard",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun VigiaApp(onLoggedOut: () -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val guardVm: GuardViewModel = viewModel()
    val guardState by guardVm.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = VigiaCanvas,
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            LiquidBottomBar(
                items = NAV_ITEMS,
                isSelected = { item ->
                    backStack?.destination?.hierarchy?.any {
                        it.route == item.route || (item.route.startsWith("analyze") && it.route?.startsWith("analyze") == true)
                    } == true
                },
                isGuardActive = guardState.localEnabled && guardState.listenerEnabled,
                onSelect = { item ->
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding()
            ),
            enterTransition = { navEnter },
            exitTransition = { navExit },
            popEnterTransition = { navPopEnter },
            popExitTransition = { navPopExit }
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
            composable("analyze/media") {
                AnalyzeScreen(viewModel = viewModel(), initialKind = "media", onBack = { navController.popBackStack() })
            }

            // Services (Before Pay, Offres d'emploi, Annonces, Communauté, Radar, Historique)
            composable("services") {
                ServicesScreen(onNavigate = { route -> navController.navigate(route) })
            }

            // Académie — leçons de cybersécurité dans un espace séparé des outils.
            composable("lessons") {
                LessonsScreen(onBack = { navController.popBackStack() })
            }

            // Before Pay (Anti-Arnaque Transfert)
            composable("before_pay") {
                BeforePayScreen(viewModel = viewModel(), onBack = { navController.popBackStack() })
            }

            // VIGIA Guard (Bouclier en temps réel)
            composable("guard") {
                GuardScreen(
                    viewModel = guardVm,
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
                HistoryScreen(viewModel = viewModel(), onBack = { navController.popBackStack() })
            }

            // Profil (compte + conseils de sécurité)
            composable("profile") {
                ProfileScreen(viewModel = viewModel(), onOpenSettings = { navController.navigate("settings") })
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
