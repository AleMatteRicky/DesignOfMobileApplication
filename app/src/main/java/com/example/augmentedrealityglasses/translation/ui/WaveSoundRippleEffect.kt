package com.example.augmentedrealityglasses.translation.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.augmentedrealityglasses.translation.TranslationViewModel

@Composable
fun WaveSoundRippleEffect(modifier: Modifier, viewModel: TranslationViewModel) {

    val minScale = 1f //minimum value of the scale when the current normalised rms value is zero (-2db)
    val maxScale = 1.3f //maximum value of the scale when the current normalised rms value is 1 (10db)
    val targetValue = minScale + viewModel.uiState.currentNormalizedRms * (maxScale - minScale)
    val animatedScale by animateFloatAsState(
        targetValue = targetValue,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ) // quasi real-time
    )

    Box(
        modifier = modifier
            .scale(animatedScale)
            .clip(CircleShape)
            .size(65.dp)
            .background(Color(0xFFDCDCE8))
    )
}