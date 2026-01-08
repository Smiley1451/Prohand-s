package com.anand.prohands.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anand.prohands.ui.theme.ProColors

/**
 * Enhanced confirmation dialog with Material 3 styling.
 * Supports custom icons for different action types (logout, delete, warning).
 *
 * UI/UX Improvements:
 * - Added optional icon for visual context
 * - Better button styling with filled/tonal variants
 * - Improved spacing and typography
 * - Destructive actions use error color
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Yes",
    dismissText: String = "No",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    icon: ImageVector? = null,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (isDestructive) ProColors.Error else ProColors.Primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = ProColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) ProColors.Error else ProColors.Primary,
                    contentColor = if (isDestructive) ProColors.OnSecondary else ProColors.OnPrimary
                ),
                modifier = Modifier.heightIn(min = 48.dp) // Accessibility: min touch target
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp) // Accessibility: min touch target
            ) {
                Text(dismissText)
            }
        },
        containerColor = ProColors.Surface,
        tonalElevation = 6.dp
    )
}

/**
 * Pre-configured logout confirmation dialog.
 */
@Composable
fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Sign Out",
        message = "Are you sure you want to sign out? You'll need to sign in again to access your account.",
        confirmText = "Sign Out",
        dismissText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        icon = Icons.AutoMirrored.Outlined.Logout,
        isDestructive = false
    )
}

/**
 * Pre-configured delete confirmation dialog.
 */
@Composable
fun DeleteConfirmationDialog(
    itemName: String = "this item",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Delete $itemName?",
        message = "This action cannot be undone. Are you sure you want to continue?",
        confirmText = "Delete",
        dismissText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        icon = Icons.Outlined.Delete,
        isDestructive = true
    )
}
