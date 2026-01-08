package com.anand.prohands.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.anand.prohands.ui.theme.ProColors
import com.anand.prohands.ui.theme.ProGradients

/**
 * Shimmer loading effect for placeholder content.
 *
 * UI/UX Improvements:
 * - Uses ProGradients shimmer colors for consistent branding
 * - Accepts modifier for flexible sizing
 * - Customizable shape
 * - Smooth infinite animation with optimized performance
 * - Option for branded (primary) or neutral shimmer
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    useBrandedColors: Boolean = false
) {
    // Use ProGradients shimmer colors for consistent app styling
    val shimmerColors = if (useBrandedColors) {
        ProGradients.ShimmerPrimary
    } else {
        ProGradients.ShimmerColors
    }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim.value - 500f, translateAnim.value - 500f),
        end = Offset(translateAnim.value, translateAnim.value)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

/**
 * Shimmer placeholder for a job card.
 * Uses ProColors for consistent styling.
 */
@Composable
fun ShimmerJobCard(
    modifier: Modifier = Modifier
) {
    // Use ProGradients shimmer colors
    val shimmerColors = ProGradients.ShimmerColors

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim.value - 500f, translateAnim.value - 500f),
        end = Offset(translateAnim.value, translateAnim.value)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ProColors.Surface)
            .padding(16.dp)
    ) {
        // Title placeholder
        Spacer(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Description lines
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Spacer(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Wage chip placeholder
        Spacer(
            modifier = Modifier
                .width(80.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Button placeholder
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush)
        )
    }
}

/**
 * Legacy shimmer item for backward compatibility.
 */
@Composable
fun ShimmerItem(brush: Brush) {
    Column(modifier = Modifier.padding(16.dp)) {
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(brush)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Spacer(modifier = Modifier
            .fillMaxWidth(0.7f)
            .height(20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(brush)
        )
    }
}
