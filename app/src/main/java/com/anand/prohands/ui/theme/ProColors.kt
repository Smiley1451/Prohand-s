package com.anand.prohands.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * ProHands App Color Palette
 *
 * A modern, accessible color scheme with:
 * - Fresh mint/teal primary for a professional yet friendly feel
 * - High contrast text colors for readability
 * - Semantic colors for states (error, success, info)
 * - Harmonious color relationships
 * - Beautiful gradient combinations
 */
@Suppress("unused")
object ProColors {
    // Primary Colors - Soft Mint/Teal theme (lighter, more subtle)
    val Primary = Color(0xFF4DD0B8)          // Soft teal - lighter and modern
    val PrimaryLight = Color(0xFF80E8D0)     // Very light teal for hover states
    val PrimaryDark = Color(0xFF26A69A)      // Medium teal for pressed states
    val PrimaryVariant = Color(0xFF4DB6AC)   // Soft variant for emphasis
    val PrimaryContainer = Color(0xFFE0F2F1) // Very light mint container for cards

    // Secondary Colors - Soft Blue accent (lighter tones)
    val Secondary = Color(0xFF5C9CE5)        // Soft blue
    val SecondaryLight = Color(0xFF90CAF9)   // Very light blue
    val SecondaryDark = Color(0xFF1976D2)    // Medium blue
    val SecondaryContainer = Color(0xFFE3F2FD) // Very light blue container

    // Background & Surface
    val Background = Color(0xFFFAFAFA)       // Soft off-white
    val Surface = Color(0xFFFFFFFF)          // Pure white
    val SurfaceVariant = Color(0xFFF5F5F5)   // Slightly darker for cards
    val SurfaceElevated = Color(0xFFFFFFFF)  // Elevated surfaces

    // Text & Content Colors
    val OnPrimary = Color(0xFFFFFFFF)        // White text on primary (better contrast)
    val OnPrimaryDark = Color(0xFF1A1A1A)    // Dark text alternative
    val OnSecondary = Color(0xFFFFFFFF)      // White text on secondary
    val OnBackground = Color(0xFF1A1A1A)     // Almost black for max readability
    val OnSurface = Color(0xFF1A1A1A)        // Almost black
    val OnSurfaceVariant = Color(0xFF49454F)// Medium gray for secondary text

    // Text Hierarchy
    val TextPrimary = Color(0xFF1A1A1A)      // Main text - high contrast
    val TextSecondary = Color(0xFF5F6368)    // Secondary text
    val TextTertiary = Color(0xFF9AA0A6)     // Tertiary/hint text
    val TextDisabled = Color(0xFFBDBDBD)     // Disabled text

    // Semantic Colors
    val Error = Color(0xFFD32F2F)            // Red for errors
    val ErrorContainer = Color(0xFFFFCDD2)   // Light red background
    val OnError = Color(0xFFFFFFFF)          // White on error

    val Success = Color(0xFF2E7D32)          // Green for success
    val SuccessContainer = Color(0xFFC8E6C9) // Light green background
    val OnSuccess = Color(0xFFFFFFFF)        // White on success

    val Warning = Color(0xFFF57C00)          // Orange for warnings
    val WarningContainer = Color(0xFFFFE0B2) // Light orange background
    val OnWarning = Color(0xFFFFFFFF)        // White on warning

    val Info = Color(0xFF1976D2)             // Blue for info
    val InfoContainer = Color(0xFFBBDEFB)    // Light blue background
    val OnInfo = Color(0xFFFFFFFF)           // White on info

    // UI Elements
    val Divider = Color(0xFFE0E0E0)          // Subtle dividers
    val DividerStrong = Color(0xFFBDBDBD)    // Stronger dividers
    val Outline = Color(0xFFE0E0E0)          // Border/outline color
    val OutlineFocused = Color(0xFF00BFA6)   // Focused state outline

    // Interactive States
    val Ripple = Color(0x1F000000)           // Ripple effect color
    val Scrim = Color(0x52000000)            // Modal backdrop
    val Overlay = Color(0x0A000000)          // Light overlay

    // Badge & Notification Colors
    val BadgeBackground = Color(0xFFD32F2F)  // Red badge
    val BadgeText = Color(0xFFFFFFFF)        // White badge text
    val UnreadIndicator = Color(0xFF00BFA6)  // Teal unread dot

    // Online/Status Colors
    val Online = Color(0xFF4CAF50)           // Green online indicator
    val Offline = Color(0xFF9E9E9E)          // Gray offline indicator
    val Away = Color(0xFFFFC107)             // Yellow away indicator
    val Busy = Color(0xFFF44336)             // Red busy indicator
}

/**
 * Beautiful gradient combinations for the ProHands app.
 * Use these for headers, cards, buttons, and backgrounds.
 */
@Suppress("unused")
object ProGradients {

    // ============ PRIMARY GRADIENTS ============

    /** Main header gradient - soft teal (lighter colors) */
    val PrimaryHeader = Brush.horizontalGradient(
        colors = listOf(
            ProColors.Primary,
            ProColors.PrimaryLight
        )
    )

    /** Vertical primary gradient for cards (softer) */
    val PrimaryVertical = Brush.verticalGradient(
        colors = listOf(
            ProColors.PrimaryLight,
            ProColors.Primary
        )
    )

    /** Light primary gradient for subtle backgrounds */
    val PrimaryLight = Brush.verticalGradient(
        colors = listOf(
            ProColors.PrimaryContainer,
            ProColors.Surface
        )
    )

    /** Diagonal primary gradient for dynamic elements */
    val PrimaryDiagonal = Brush.linearGradient(
        colors = listOf(
            ProColors.PrimaryLight,
            ProColors.Primary,
            ProColors.PrimaryDark
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    // ============ SECONDARY GRADIENTS ============

    /** Secondary header gradient - blue tones */
    val SecondaryHeader = Brush.horizontalGradient(
        colors = listOf(
            ProColors.Secondary,
            ProColors.SecondaryDark
        )
    )

    /** Light secondary for info sections */
    val SecondaryLight = Brush.verticalGradient(
        colors = listOf(
            ProColors.SecondaryContainer,
            ProColors.Surface
        )
    )

    // ============ MIXED GRADIENTS ============

    /** Primary to Secondary - for premium feel */
    val PrimaryToSecondary = Brush.horizontalGradient(
        colors = listOf(
            ProColors.Primary,
            ProColors.Secondary
        )
    )

    /** Teal to Blue diagonal - modern app feel */
    val TealBlue = Brush.linearGradient(
        colors = listOf(
            ProColors.Primary,
            ProColors.SecondaryLight,
            ProColors.Secondary
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    // ============ SEMANTIC GRADIENTS ============

    /** Success gradient */
    val Success = Brush.horizontalGradient(
        colors = listOf(
            ProColors.Success,
            Color(0xFF43A047)
        )
    )

    /** Error gradient */
    val Error = Brush.horizontalGradient(
        colors = listOf(
            ProColors.Error,
            Color(0xFFC62828)
        )
    )

    /** Warning gradient */
    val Warning = Brush.horizontalGradient(
        colors = listOf(
            ProColors.Warning,
            Color(0xFFEF6C00)
        )
    )

    /** Info gradient */
    val Info = Brush.horizontalGradient(
        colors = listOf(
            ProColors.Info,
            Color(0xFF1565C0)
        )
    )

    // ============ SURFACE GRADIENTS ============

    /** Subtle surface gradient for cards */
    val Surface = Brush.verticalGradient(
        colors = listOf(
            ProColors.Surface,
            ProColors.SurfaceVariant
        )
    )

    /** Background gradient */
    val Background = Brush.verticalGradient(
        colors = listOf(
            ProColors.Background,
            ProColors.SurfaceVariant
        )
    )

    // ============ SHIMMER GRADIENTS ============

    /** Shimmer colors for loading states */
    val ShimmerColors = listOf(
        ProColors.SurfaceVariant.copy(alpha = 0.9f),
        ProColors.Surface.copy(alpha = 0.4f),
        ProColors.SurfaceVariant.copy(alpha = 0.9f)
    )

    /** Primary shimmer for branded loading */
    val ShimmerPrimary = listOf(
        ProColors.PrimaryContainer.copy(alpha = 0.6f),
        ProColors.PrimaryLight.copy(alpha = 0.2f),
        ProColors.PrimaryContainer.copy(alpha = 0.6f)
    )

    // ============ UTILITY FUNCTIONS ============

    /** Create a custom horizontal gradient from a list of colors */
    fun horizontal(colors: List<Color>): Brush = Brush.horizontalGradient(colors)

    /** Create a custom vertical gradient from a list of colors */
    fun vertical(colors: List<Color>): Brush = Brush.verticalGradient(colors)

    /** Create a diagonal gradient from top-left to bottom-right */
    fun diagonal(colors: List<Color>): Brush = Brush.linearGradient(
        colors = colors,
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    /** Create a radial gradient */
    fun radial(colors: List<Color>, center: Offset = Offset.Unspecified, radius: Float = Float.POSITIVE_INFINITY): Brush =
        Brush.radialGradient(colors = colors, center = center, radius = radius)

    /** Status indicator gradient based on connection state */
    fun statusIndicator(isConnected: Boolean): Brush = horizontal(
        listOf(
            if (isConnected) ProColors.Online else ProColors.Offline,
            if (isConnected) Color(0xFF66BB6A) else Color(0xFFBDBDBD)
        )
    )
}
