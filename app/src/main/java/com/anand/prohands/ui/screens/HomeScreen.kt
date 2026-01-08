package com.anand.prohands.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WorkOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.anand.prohands.data.JobResponse
import com.anand.prohands.ui.components.AppHeader
import com.anand.prohands.ui.components.EmptyStateView
import com.anand.prohands.ui.components.ErrorStateView
import com.anand.prohands.ui.components.ShimmerEffect
import com.anand.prohands.ui.theme.ProColors
import com.anand.prohands.ui.theme.ProGradients
import com.anand.prohands.viewmodel.HomeViewModel
import com.anand.prohands.viewmodel.HomeViewModelFactory
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.util.Locale
import kotlin.math.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    currentUserId: String,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory()),
    onProfileClick: () -> Unit,
    onMessagesClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    val jobs by viewModel.jobs.collectAsState()
    val location by viewModel.location.collectAsState()
    val currentUserProfile by viewModel.currentUserProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            viewModel.fetchUserProfile(currentUserId)
        }
    }

    RequestLocation(context) { newLocation ->
        viewModel.setLocation(newLocation)
    }


    LaunchedEffect(error) {
        error?.let {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = it,
                    actionLabel = "Retry",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    location?.let { loc ->
                        viewModel.fetchJobs(loc.latitude, loc.longitude)
                    }
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            AppHeader(
                isHome = true,
                onProfileClick = onProfileClick,
                onMessagesClick = onMessagesClick,
                onNotificationsClick = onNotificationsClick,
                profileImageUrl = currentUserProfile?.profilePictureUrl
            )
        },
        containerColor = ProColors.Background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = ProColors.Secondary,
                    contentColor = ProColors.OnSecondary,
                    actionColor = ProColors.Primary
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {

                isLoading -> {
                    LoadingJobsList()
                }


                location == null -> {
                    LocationWaitingState()
                }


                error != null && jobs.isEmpty() -> {
                    ErrorStateView(
                        title = "Couldn't load jobs",
                        message = error ?: "Something went wrong. Please try again.",
                        onRetryClick = {
                            location?.let { loc ->
                                viewModel.fetchJobs(loc.latitude, loc.longitude)
                            }
                        }
                    )
                }

             
                jobs.isEmpty() -> {
                    EmptyStateView(
                        title = "No jobs nearby",
                        message = "There are no available jobs in your area right now. Check back later or try expanding your search.",
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.WorkOff,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = ProColors.TextTertiary
                            )
                        },
                        actionLabel = "Refresh",
                        onActionClick = {
                            location?.let { loc ->
                                viewModel.fetchJobs(loc.latitude, loc.longitude)
                            }
                        }
                    )
                }

                // Jobs list
                else -> {
                    JobsList(jobs = jobs, userLocation = location, navController = navController)
                }
            }
        }
    }
}

/**
 * Loading state with shimmer placeholders.
 */
@Composable
private fun LoadingJobsList() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(5) {
            ShimmerEffect(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )
        }
    }
}

/**
 * State shown while waiting for location.
 * Uses ProColors for branded loading experience.
 */
@Composable
private fun LocationWaitingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ProColors.Background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Branded circular progress with track color
        CircularProgressIndicator(
            color = ProColors.Primary,
            trackColor = ProColors.PrimaryContainer,
            strokeWidth = 4.dp,
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Finding your location",
            style = MaterialTheme.typography.titleMedium,
            color = ProColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We need your location to show nearby jobs",
            style = MaterialTheme.typography.bodyMedium,
            color = ProColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Jobs list with proper LazyColumn implementation.
 */
@Composable
private fun JobsList(
    jobs: List<JobResponse>,
    userLocation: Location?,
    navController: NavController
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = jobs,
            key = { it.jobId } // Use stable key for better performance
        ) { job ->
            JobFeedCard(
                job = job,
                userLocation = userLocation,
                onContactClick = {
                    // Navigate to the job provider's profile
                    navController.navigate("worker_profile/${job.providerId}")
                }
            )
        }
    }
}

/**
 * Enhanced job card with better visual hierarchy.
 * Uses ProColors and ProGradients for modern styling.
 */
@Composable
fun JobFeedCard(
    job: JobResponse,
    userLocation: Location?,
    onContactClick: () -> Unit = {}
) {
    var showMap by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Calculate distance from user to job
    val distanceKm = userLocation?.let {
        calculateDistance(
            it.latitude, it.longitude,
            job.latitude, job.longitude
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ProColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Gradient accent strip at top of card for visual interest
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(ProGradients.PrimaryHeader)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // Title row with distance badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ProColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    // Distance badge
                    if (distanceKm != null) {
                        Surface(
                            color = ProColors.SecondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.DirectionsWalk,
                                    contentDescription = "Distance",
                                    tint = ProColors.SecondaryDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatDistance(distanceKm),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ProColors.SecondaryDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description with ellipsis
                Text(
                    text = job.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ProColors.TextSecondary,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Wage chip with PrimaryContainer background
                Surface(
                    color = ProColors.PrimaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "₹${job.wage}/hr",
                        style = MaterialTheme.typography.labelLarge,
                        color = ProColors.PrimaryDark,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Map button
                    OutlinedButton(
                        onClick = { showMap = true },
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(ProColors.Primary)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = ProColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Map",
                            fontWeight = FontWeight.SemiBold,
                            color = ProColors.Primary
                        )
                    }

                    // Contact button - navigates to provider profile
                    Button(
                        onClick = onContactClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProColors.Primary,
                            contentColor = ProColors.OnPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Contact",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // Map dialog
    if (showMap) {
        JobMapDialog(
            job = job,
            userLocation = userLocation,
            onDismiss = { showMap = false },
            onStartNavigation = {
                userLocation?.let {
                    openGoogleMapsNavigation(
                        context = context,
                        userLat = it.latitude,
                        userLon = it.longitude,
                        destLat = job.latitude,
                        destLon = job.longitude
                    )
                }
            }
        )
    }
}

/**
 * Map dialog for job location.
 * Enhanced with ProColors styling and navigation options.
 */
@Composable
private fun JobMapDialog(
    job: JobResponse,
    userLocation: Location?,
    onDismiss: () -> Unit,
    onStartNavigation: () -> Unit = {}
) {
    val context = LocalContext.current
    val jobLocation = LatLng(job.latitude, job.longitude)
    val userLatLng = userLocation?.let { LatLng(it.latitude, it.longitude) }

    // Calculate distance
    val distanceKm = userLocation?.let {
        calculateDistance(
            it.latitude, it.longitude,
            job.latitude, job.longitude
        )
    }

    // State for navigation options
    var showNavigationOptions by remember { mutableStateOf(false) }
    var showInAppNavigation by remember { mutableStateOf(false) }
    var showTurnByTurnNavigation by remember { mutableStateOf(false) }
    var isMapExpanded by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(jobLocation, 14f)
    }

    // Show in-app navigation fullscreen
    if (showInAppNavigation) {
        InAppNavigationView(
            job = job,
            userLocation = userLocation,
            distanceKm = distanceKm,
            isExpanded = isMapExpanded,
            onToggleExpand = { isMapExpanded = !isMapExpanded },
            onOpenGoogleMaps = {
                userLocation?.let {
                    openGoogleMapsNavigation(
                        context = context,
                        userLat = it.latitude,
                        userLon = it.longitude,
                        destLat = job.latitude,
                        destLon = job.longitude
                    )
                }
            },
            onClose = {
                showInAppNavigation = false
                isMapExpanded = false
            }
        )
        return
    }

    // Show turn-by-turn navigation
    if (showTurnByTurnNavigation) {
        TurnByTurnNavigationView(
            job = job,
            userLocation = userLocation,
            onOpenGoogleMaps = {
                userLocation?.let {
                    openGoogleMapsNavigation(
                        context = context,
                        userLat = it.latitude,
                        userLon = it.longitude,
                        destLat = job.latitude,
                        destLon = job.longitude
                    )
                }
            },
            onClose = {
                showTurnByTurnNavigation = false
            }
        )
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ProColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
        ) {
            Column {
                // Header with gradient and distance
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ProGradients.PrimaryHeader)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = job.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ProColors.OnPrimary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )

                        // Distance badge in header
                        if (distanceKm != null) {
                            Surface(
                                color = ProColors.OnPrimary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.DirectionsWalk,
                                        contentDescription = "Distance",
                                        tint = ProColors.OnPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = formatDistance(distanceKm),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ProColors.OnPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // Map
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    cameraPositionState = cameraPositionState
                ) {
                    Marker(
                        state = MarkerState(position = jobLocation),
                        title = job.title,
                        snippet = "Job Location"
                    )
                    userLatLng?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "Your Location",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        )
                    }
                }

                // Action buttons
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Start Navigation button - shows options
                    if (userLocation != null) {
                        Button(
                            onClick = { showNavigationOptions = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ProColors.Success,
                                contentColor = ProColors.OnSuccess
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Navigation,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Navigation",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Close button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(ProColors.Primary)
                        )
                    ) {
                        Text(
                            text = "Close",
                            fontWeight = FontWeight.SemiBold,
                            color = ProColors.Primary
                        )
                    }
                }
            }
        }
    }

    // Navigation options bottom sheet dialog
    if (showNavigationOptions) {
        NavigationOptionsDialog(
            onDismiss = { showNavigationOptions = false },
            onInAppNavigation = {
                showNavigationOptions = false
                showInAppNavigation = true
            },
            onTurnByTurnNavigation = {
                showNavigationOptions = false
                showTurnByTurnNavigation = true
            },
            onGoogleMaps = {
                showNavigationOptions = false
                onDismiss()
                userLocation?.let {
                    openGoogleMapsNavigation(
                        context = context,
                        userLat = it.latitude,
                        userLon = it.longitude,
                        destLat = job.latitude,
                        destLon = job.longitude
                    )
                }
            }
        )
    }
}

/**
 * Navigation options dialog - lets user choose between in-app overview, turn-by-turn, or Google Maps
 */
@Composable
private fun NavigationOptionsDialog(
    onDismiss: () -> Unit,
    onInAppNavigation: () -> Unit,
    onTurnByTurnNavigation: () -> Unit = {},
    onGoogleMaps: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ProColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Text(
                    text = "Choose Navigation",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ProColors.TextPrimary
                )

                Text(
                    text = "How would you like to navigate?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ProColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Turn-by-Turn in-app navigation (with directions)
                NavigationOptionCard(
                    icon = Icons.Outlined.Directions,
                    title = "Turn-by-Turn (In-App)",
                    subtitle = "Step-by-step directions within the app",
                    containerColor = ProColors.InfoContainer,
                    iconTint = ProColors.Info,
                    onClick = onTurnByTurnNavigation
                )

                // Route overview option
                NavigationOptionCard(
                    icon = Icons.Outlined.Map,
                    title = "Route Overview",
                    subtitle = "See route on map with distance info",
                    containerColor = ProColors.PrimaryContainer,
                    iconTint = ProColors.Primary,
                    onClick = onInAppNavigation
                )

                // Google Maps option
                NavigationOptionCard(
                    icon = Icons.Filled.Navigation,
                    title = "Open Google Maps",
                    subtitle = "External app with voice navigation",
                    containerColor = ProColors.SuccessContainer,
                    iconTint = ProColors.Success,
                    onClick = onGoogleMaps
                )


                // Cancel button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Cancel",
                        color = ProColors.TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Navigation option card component
 */
@Composable
private fun NavigationOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    containerColor: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconTint.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ProColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ProColors.TextSecondary
                )
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = iconTint
            )
        }
    }
}

/**
 * In-app navigation view with map and minimize/maximize options.
 * Shows route overview with option to open Google Maps for turn-by-turn directions.
 */
@Composable
private fun InAppNavigationView(
    job: JobResponse,
    userLocation: Location?,
    distanceKm: Double?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenGoogleMaps: () -> Unit,
    onClose: () -> Unit
) {
    val jobLocation = LatLng(job.latitude, job.longitude)
    val userLatLng = userLocation?.let { LatLng(it.latitude, it.longitude) }

    // Camera bounds to show both markers
    val cameraPositionState = rememberCameraPositionState {
        val centerLat = if (userLocation != null) {
            (userLocation.latitude + job.latitude) / 2
        } else job.latitude
        val centerLng = if (userLocation != null) {
            (userLocation.longitude + job.longitude) / 2
        } else job.longitude

        // Zoom level based on distance
        val zoom = when {
            distanceKm == null -> 14f
            distanceKm < 0.5 -> 17f
            distanceKm < 1 -> 16f
            distanceKm < 3 -> 15f
            distanceKm < 5 -> 14f
            distanceKm < 10 -> 13f
            distanceKm < 20 -> 12f
            else -> 10f
        }
        position = CameraPosition.fromLatLngZoom(LatLng(centerLat, centerLng), zoom)
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ProColors.Background)
        ) {
            // Full screen map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    compassEnabled = true,
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false
                )
            ) {
                // Job marker (destination)
                Marker(
                    state = MarkerState(position = jobLocation),
                    title = job.title,
                    snippet = "Destination"
                )

                // User marker
                userLatLng?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = "You",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )

                    // Draw dashed route line
                    Polyline(
                        points = listOf(it, jobLocation),
                        color = ProColors.Primary,
                        width = 10f,
                        pattern = listOf(
                            com.google.android.gms.maps.model.Dash(20f),
                            com.google.android.gms.maps.model.Gap(10f)
                        )
                    )
                }
            }

            // Top navigation card (always visible)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ProColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Header row with destination and close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Destination info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Route to",
                                style = MaterialTheme.typography.labelSmall,
                                color = ProColors.TextTertiary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = job.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ProColors.TextPrimary,
                                maxLines = 2,
                                lineHeight = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Close button
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(36.dp)
                                .background(ProColors.ErrorContainer, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = ProColors.Error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Distance and info row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Distance chip
                        if (distanceKm != null) {
                            Surface(
                                color = ProColors.PrimaryContainer,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.DirectionsWalk,
                                        contentDescription = null,
                                        tint = ProColors.Primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = formatDistance(distanceKm),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = ProColors.Primary
                                    )
                                }
                            }
                        }

                        // Wage chip
                        Surface(
                            color = ProColors.SuccessContainer,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "₹${job.wage}/hr",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ProColors.Success
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Toggle expand/minimize
                        OutlinedButton(
                            onClick = onToggleExpand,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(ProColors.Primary)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                                contentDescription = null,
                                tint = ProColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isExpanded) "Minimize" else "Expand",
                                color = ProColors.Primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Open Google Maps for directions
                        Button(
                            onClick = onOpenGoogleMaps,
                            modifier = Modifier
                                .weight(1.5f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ProColors.Success
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Navigation,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Get Directions",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Info text
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap 'Get Directions' for turn-by-turn navigation",
                        style = MaterialTheme.typography.bodySmall,
                        color = ProColors.TextTertiary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Turn-by-Turn Navigation View with step-by-step directions.
 * Fetches actual route data from Google Directions API.
 */
@Composable
private fun TurnByTurnNavigationView(
    job: JobResponse,
    userLocation: Location?,
    onOpenGoogleMaps: () -> Unit,
    onClose: () -> Unit
) {
    val jobLocation = LatLng(job.latitude, job.longitude)
    val userLatLng = userLocation?.let { LatLng(it.latitude, it.longitude) }

    // Directions state
    var directionsResult by remember { mutableStateOf<com.anand.prohands.data.DirectionsResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentStepIndex by remember { mutableStateOf(0) }

    // Fetch directions on launch
    LaunchedEffect(userLocation, job) {
        if (userLocation != null) {
            isLoading = true
            errorMessage = null
            try {
                val repository = com.anand.prohands.data.DirectionsRepository()
                val apiKey = com.anand.prohands.BuildConfig.MAPS_API_KEY

                if (apiKey.isBlank()) {
                    errorMessage = "Maps API key not configured"
                    isLoading = false
                    return@LaunchedEffect
                }

                val result = repository.getDirections(
                    originLat = userLocation.latitude,
                    originLng = userLocation.longitude,
                    destLat = job.latitude,
                    destLng = job.longitude,
                    apiKey = apiKey
                )

                if (result != null) {
                    directionsResult = result
                } else {
                    errorMessage = "Could not fetch directions"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            }
            isLoading = false
        } else {
            errorMessage = "Location not available"
            isLoading = false
        }
    }

    // Camera position
    val cameraPositionState = rememberCameraPositionState {
        val centerLat = if (userLocation != null) {
            (userLocation.latitude + job.latitude) / 2
        } else job.latitude
        val centerLng = if (userLocation != null) {
            (userLocation.longitude + job.longitude) / 2
        } else job.longitude
        position = CameraPosition.fromLatLngZoom(LatLng(centerLat, centerLng), 14f)
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ProColors.Background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top section with map (40% height)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f)
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(isMyLocationEnabled = false),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true,
                            compassEnabled = true,
                            myLocationButtonEnabled = false,
                            mapToolbarEnabled = false
                        )
                    ) {
                        // Destination marker
                        Marker(
                            state = MarkerState(position = jobLocation),
                            title = job.title,
                            snippet = "Destination"
                        )

                        // User marker
                        userLatLng?.let {
                            Marker(
                                state = MarkerState(position = it),
                                title = "You",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                            )
                        }
                    }

                    // Loading indicator
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(48.dp),
                            color = ProColors.Primary,
                            strokeWidth = 4.dp
                        )
                    }

                    // Error message
                    errorMessage?.let { message ->
                        Text(
                            text = message,
                            color = ProColors.Error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }
                }

                // Bottom section with steps (60% height)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with summary
                    if (directionsResult != null) {
                        Text(
                            text = "Route: ${directionsResult!!.totalDistance} • ${directionsResult!!.totalDuration}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ProColors.TextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Steps list
                        val steps = directionsResult!!.steps

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(steps) { index, step ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (index == currentStepIndex)
                                            ProColors.PrimaryContainer else ProColors.SurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    onClick = { currentStepIndex = index }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Step number
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    if (index == currentStepIndex) ProColors.Primary
                                                    else ProColors.Outline.copy(alpha = 0.3f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (index == currentStepIndex)
                                                    ProColors.OnPrimary else ProColors.TextSecondary
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Instruction
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = step.instruction,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = ProColors.TextPrimary,
                                                maxLines = 2
                                            )
                                            Text(
                                                text = "${step.distance} • ${step.duration}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ProColors.TextSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            // Arrival item
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = ProColors.SuccessContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.LocationOn,
                                            contentDescription = null,
                                            tint = ProColors.Success,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Arrive at ${job.title}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ProColors.Success
                                        )
                                    }
                                }
                            }
                        }
                    } else if (!isLoading && errorMessage == null) {
                        // No directions available
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No directions available",
                                color = ProColors.TextSecondary
                            )
                        }
                    } else {
                        // Loading or error - show placeholder
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Open Google Maps button
                        Button(
                            onClick = onOpenGoogleMaps,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ProColors.Success
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Navigation,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Google Maps",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Close button
                        OutlinedButton(
                            onClick = onClose,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(ProColors.Primary)
                            )
                        ) {
                            Text(
                                text = "Close",
                                fontWeight = FontWeight.SemiBold,
                                color = ProColors.Primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Location request handler with proper lifecycle management.
 */
@Composable
fun RequestLocation(context: Context, onLocationReceived: (Location) -> Unit) {
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    DisposableEffect(Unit) {
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    location?.let { onLocationReceived(it) }
                }.addOnFailureListener { e ->
                    Log.e("HomeScreen", "Error getting location", e)
                }
            }
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error requesting location", e)
        }
        onDispose { }
    }
}

/**
 * Calculate distance between two points using Haversine formula.
 * @return Distance in kilometers
 */
private fun calculateDistance(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val earthRadius = 6371.0 // Earth's radius in kilometers

    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)

    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return earthRadius * c
}

/**
 * Format distance for display.
 */
private fun formatDistance(distanceKm: Double): String {
    return when {
        distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()} m"
        distanceKm < 10.0 -> String.format(Locale.getDefault(), "%.1f km", distanceKm)
        else -> String.format(Locale.getDefault(), "%.0f km", distanceKm)
    }
}

/**
 * Open Google Maps with navigation from user location to destination.
 */
private fun openGoogleMapsNavigation(
    context: Context,
    userLat: Double, userLon: Double,
    destLat: Double, destLon: Double
) {
    // Google Maps navigation URI
    val uri = Uri.parse("google.navigation:q=$destLat,$destLon&mode=d")
    val mapIntent = Intent(Intent.ACTION_VIEW, uri)
    mapIntent.setPackage("com.google.android.apps.maps")

    // Check if Google Maps is installed
    if (mapIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(mapIntent)
    } else {
        // Fallback to browser if Google Maps is not installed
        val browserUri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1&origin=$userLat,$userLon&destination=$destLat,$destLon&travelmode=driving"
        )
        context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
    }
}

