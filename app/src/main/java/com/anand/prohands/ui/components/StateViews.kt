package com.anand.prohands.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SignalWifiOff
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anand.prohands.ui.theme.ProColors
import com.anand.prohands.ui.theme.ProGradients

/**
 * Network/Connection status banner that slides in from top when offline.
 *
 * UI/UX Improvements:
 * - Animated visibility for smooth transitions
 * - Uses ProColors semantic colors (Warning for offline, Info for connecting)
 * - Clear messaging about connection state
 * - Optional retry action
 * - Accessibility-friendly with proper content descriptions
 */
@Composable
fun NetworkStatusBanner(
    isConnected: Boolean,
    modifier: Modifier = Modifier,
    isConnecting: Boolean = false,
    onRetryClick: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = !isConnected,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        // Use gradient for a more polished look
        val backgroundBrush = if (isConnecting) ProGradients.Info else ProGradients.Warning

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundBrush)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isConnecting) Icons.Outlined.Refresh else Icons.Outlined.WifiOff,
                    contentDescription = if (isConnecting) "Connecting" else "No internet connection",
                    modifier = Modifier.size(18.dp),
                    tint = if (isConnecting) ProColors.OnInfo else ProColors.OnWarning
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (isConnecting) "Connecting..." else "No internet connection",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isConnecting) ProColors.OnInfo else ProColors.OnWarning
                )

                if (onRetryClick != null && !isConnecting) {
                    Spacer(modifier = Modifier.width(12.dp))

                    TextButton(
                        onClick = onRetryClick,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = ProColors.OnWarning
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Retry",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Connection status indicator dot for headers.
 * Uses ProColors.Online and ProColors.Offline for consistency.
 */
@Composable
fun ConnectionStatusDot(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    // Use Online/Offline colors instead of Success/Error for status dots
    val color = if (isConnected) ProColors.Online else ProColors.Offline

    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * Empty state placeholder for screens with no data.
 *
 * UI/UX Improvements:
 * - Uses ProColors for consistent styling
 * - Clear visual hierarchy with icon, title, and message
 * - Gradient-styled action button when applicable
 * - Proper spacing and typography
 * - Container background for better visual separation
 */
@Composable
fun EmptyStateView(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = ProColors.TextTertiary
        )
    },
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ProColors.Background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon container with subtle background
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(ProColors.SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = ProColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = ProColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(24.dp))

            // Gradient button for action
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ProColors.Primary,
                    contentColor = ProColors.OnPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .widthIn(min = 160.dp)
            ) {
                Text(
                    text = actionLabel,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Error state view for backend failures.
 * Uses ProColors.Error and ErrorContainer for visual feedback.
 */
@Composable
fun ErrorStateView(
    modifier: Modifier = Modifier,
    title: String = "Something went wrong",
    message: String = "We couldn't load this content. Please try again.",
    onRetryClick: () -> Unit
) {
    EmptyStateView(
        title = title,
        message = message,
        modifier = modifier,
        icon = {
            // Error icon with container background
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ProColors.ErrorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SignalWifiOff,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = ProColors.Error
                )
            }
        },
        actionLabel = "Try Again",
        onActionClick = onRetryClick
    )
}

/**
 * Loading state view with branded shimmer effect.
 */
@Composable
fun LoadingStateView(
    modifier: Modifier = Modifier,
    message: String = "Loading..."
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ProColors.Background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = ProColors.Primary,
            trackColor = ProColors.PrimaryContainer,
            strokeWidth = 4.dp,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = ProColors.TextSecondary
        )
    }
}

/**
 * Success state view for confirmations.
 */
@Composable
fun SuccessStateView(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    EmptyStateView(
        title = title,
        message = message,
        modifier = modifier,
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ProColors.SuccessContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudOff, // TODO: Replace with check icon
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = ProColors.Success
                )
            }
        },
        actionLabel = actionLabel,
        onActionClick = onActionClick
    )
}

