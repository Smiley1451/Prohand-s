package com.anand.prohands.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavigation(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavigation(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home
    )
    object Search : BottomNavigation(
        route = "search",
        title = "Search",
        icon = Icons.Default.Search
    )
    object PostJob : BottomNavigation(
        route = "post_job",
        title = "Post Job",
        icon = Icons.Default.AddCircle
    )
    object Jobs : BottomNavigation(
        route = "jobs",
        title = "Jobs",
        icon = Icons.Default.Work
    )
    object Profile : BottomNavigation(
        route = "profile",
        title = "Profile",
        icon = Icons.Default.AccountCircle
    )
}