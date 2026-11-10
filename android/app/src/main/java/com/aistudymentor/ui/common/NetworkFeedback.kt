package com.aistudymentor.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ObserveSnackbarEvents(viewModel: NetworkViewModel, hostState: SnackbarHostState) {
    LaunchedEffect(viewModel, hostState) {
        viewModel.snackbarEvents.collect { event ->
            val result = hostState.showSnackbar(
                message = event.message,
                actionLabel = event.retryLabel,
                withDismissAction = event.retryLabel != null
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.retryLastAction()
        }
    }
}

@Composable
fun ShimmerBlock(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(850), repeatMode = RepeatMode.Reverse),
        label = "shimmer-alpha"
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val brush = Brush.linearGradient(
        colors = listOf(base.copy(alpha = alpha), Color.White.copy(alpha = alpha * 0.7f), base.copy(alpha = alpha))
    )
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(brush))
}

@Composable
fun DashboardShimmer(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(3) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerBlock(Modifier.fillMaxWidth(0.45f).height(18.dp))
                ShimmerBlock(Modifier.fillMaxWidth(0.7f).height(34.dp))
            }
        }
    }
}