package com.anand.prohands.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anand.prohands.ProHandsApplication
import com.anand.prohands.navigation.BottomNavigation
import com.anand.prohands.navigation.NavGraph
import com.anand.prohands.ui.components.LogoutConfirmationDialog
import com.anand.prohands.ui.components.NetworkStatusBanner
import com.anand.prohands.ui.theme.ProColors
import com.anand.prohands.viewmodel.AuthViewModel

/**
 * Main screen with bottom navigation and network status awareness.
 *
 * UI/UX Improvements:
 * - Network connectivity monitoring with visual feedback
 * - Animated bottom navigation with filled/outlined icon states
 * - Smooth logout confirmation dialog
 * - Proper state preservation across configuration changes
 * - Accessibility improvements with proper content descriptions
 */
@Composable
fun MainScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Use the singleton instance from Application to avoid multiple EncryptedSharedPreferences instances
    val sessionManager = (context.applicationContext as ProHandsApplication).sessionManager
    val currentUserId = sessionManager.getUserId() ?: ""

    // State preservation using rememberSaveable for configuration changes
    var showLogoutConfirm by rememberSaveable { mutableStateOf(false) }

    // Network connectivity state
    val isNetworkAvailable by rememberNetworkState(context)

    Scaffold(
        containerColor = ProColors.Background,
        bottomBar = {
            EnhancedBottomBar(navController = navController)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Network status banner - slides in when offline
            NetworkStatusBanner(
                isConnected = isNetworkAvailable,
                isConnecting = false,
                onRetryClick = null // Can be connected to a refresh action
            )

            // Main content
            Box(modifier = Modifier.weight(1f)) {
                NavGraph(
                    navController = navController,
                    currentUserId = currentUserId,
                    authViewModel = authViewModel,
                    onLogout = { showLogoutConfirm = true }
                )
            }
        }
    }

    // Logout confirmation dialog with enhanced styling
    if (showLogoutConfirm) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutConfirm = false
                onLogout()
            },
            onDismiss = { showLogoutConfirm = false }
        )
    }
}

/**
 * Enhanced bottom navigation bar with Material 3 styling.
 *
 * UI/UX Improvements:
 * - Filled icons for selected state, outlined for unselected
 * - Animated indicator with spring animation
 * - Better touch targets (>= 48dp)
 * - Proper content descriptions for accessibility
 * - Badge support for notifications (prepared for future use)
 */
@Composable
fun EnhancedBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Navigation items with filled and outlined icon variants
    val navigationItems = listOf(
        NavigationItem(
            route = BottomNavigation.Home.route,
            title = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            contentDescription = "Go to Home"
        ),
        NavigationItem(
            route = BottomNavigation.Search.route,
            title = "Search",
            selectedIcon = Icons.Filled.Search,
            unselectedIcon = Icons.Outlined.Search,
            contentDescription = "Search for workers"
        ),
        NavigationItem(
            route = BottomNavigation.PostJob.route,
            title = "Post",
            selectedIcon = Icons.Filled.AddCircle,
            unselectedIcon = Icons.Outlined.AddCircle,
            contentDescription = "Post a new job"
        ),
        NavigationItem(
            route = BottomNavigation.Jobs.route,
            title = "Jobs",
            selectedIcon = Icons.Filled.Work,
            unselectedIcon = Icons.Outlined.Work,
            contentDescription = "View your posted jobs"
        ),
        NavigationItem(
            route = BottomNavigation.Profile.route,
            title = "Profile",
            selectedIcon = Icons.Filled.AccountCircle,
            unselectedIcon = Icons.Outlined.AccountCircle,
            contentDescription = "View your profile"
        )
    )

    NavigationBar(
        containerColor = ProColors.Surface,
        contentColor = ProColors.Secondary,
        tonalElevation = 8.dp
    ) {
        navigationItems.forEach { item ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    // Always navigate to root of the tab, even if already on this tab
                    navController.navigate(item.route) {
                        // Pop up to the start destination to clear the back stack
                        popUpTo(navController.graph.findStartDestination().id) {
                            // Don't save state - always go to fresh root page
                            saveState = false
                        }
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                        // Don't restore state - always show root page
                        restoreState = false
                    }
                },
                icon = {
                    // Animated icon transition between selected/unselected states
                    Crossfade(
                        targetState = isSelected,
                        animationSpec = tween(durationMillis = 200),
                        label = "nav_icon_${item.route}"
                    ) { selected ->
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.contentDescription,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ProColors.OnPrimary,
                    selectedTextColor = ProColors.PrimaryVariant,
                    indicatorColor = ProColors.Primary,
                    unselectedIconColor = ProColors.TextTertiary,
                    unselectedTextColor = ProColors.TextTertiary
                ),
                alwaysShowLabel = true
            )
        }
    }
}

/**
 * Data class for navigation items with icon variants.
 */
private data class NavigationItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val contentDescription: String,
    val badgeCount: Int = 0
)

/**
 * Composable function to observe network connectivity state.
 * Uses DisposableEffect to properly manage ConnectivityManager callbacks.
 *
 * Edge Case Handling:
 * - Initial state check on composition
 * - Automatic updates on network changes
 * - Proper cleanup on disposal
 */
@Composable
private fun rememberNetworkState(context: Context): State<Boolean> {
    val connectivityManager = remember {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    // Check initial network state
    val initialState = remember {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    val networkState = remember { mutableStateOf(initialState) }

    DisposableEffect(connectivityManager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                networkState.value = true
            }

            override fun onLost(network: Network) {
                networkState.value = false
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                networkState.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        onDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    return networkState
}

// Keep the old function name for backward compatibility
@Composable
fun BottomBar(navController: NavHostController) {
    EnhancedBottomBar(navController = navController)
}
