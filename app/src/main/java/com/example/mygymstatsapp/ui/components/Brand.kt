package com.example.mygymstatsapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.ui.theme.BigShoulders
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** The Fonte plate, tinted with the brand color by default. */
@Composable
fun FonteMark(modifier: Modifier = Modifier, size: Dp = 32.dp, tint: Color = MaterialTheme.colorScheme.primary) {
    Icon(painterResource(R.drawable.ic_fonte_mark), contentDescription = null, modifier = modifier.size(size), tint = tint)
}

/** Plate + app name set in the display face. */
@Composable
fun FonteWordmark(modifier: Modifier = Modifier, markSize: Dp = 28.dp) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(markSize * 0.3f),
    ) {
        FonteMark(size = markSize)
        Text(
            text = stringResource(R.string.app_name).uppercase(),
            fontFamily = BigShoulders,
            fontWeight = FontWeight.Black,
            fontSize = markSize.value.sp,
            lineHeight = markSize.value.sp,
            letterSpacing = (markSize.value * 0.06f).sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Empty-state illustration: a bumper plate (same cut-outs as the logo) with the icon in the hub. */
@Composable
fun PlateIllustration(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 112.dp) {
    val plate = MaterialTheme.colorScheme.surfaceContainerHigh
    val cutOut = MaterialTheme.colorScheme.background
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val radius = this.size.minDimension / 2
            drawCircle(plate, radius)
            drawCircle(cutOut, radius * 0.56f, style = Stroke(width = radius * 0.12f))
            val slot = Size(radius * 0.53f, radius * 0.17f)
            listOf(-1f, 1f).forEach { side ->
                drawRoundRect(
                    color = cutOut,
                    topLeft = Offset(center.x - slot.width / 2, center.y + side * radius * 0.8f - slot.height / 2),
                    size = slot,
                    cornerRadius = CornerRadius(slot.height / 2),
                )
            }
        }
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(size * 0.3f))
    }
}

/** One-shot puff of chalk dust, the end-of-workout celebration. */
@Composable
fun ChalkBurst(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface, particleCount: Int = 36) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMillis = 1600, easing = LinearOutSlowInEasing)) }
    val particles = remember(particleCount) {
        val random = Random(particleCount)
        List(particleCount) { index ->
            ChalkParticle(
                angle = (index + random.nextFloat()) / particleCount * 2 * PI.toFloat(),
                reach = 0.4f + 0.6f * random.nextFloat(),
                radiusDp = 1.5f + 3f * random.nextFloat(),
            )
        }
    }
    Canvas(modifier) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        val maxDistance = this.size.minDimension / 2
        particles.forEach { particle ->
            val distance = maxDistance * particle.reach * p
            drawCircle(
                color = color.copy(alpha = (1f - p) * 0.85f),
                radius = particle.radiusDp.dp.toPx() * (1f - 0.4f * p),
                center = center + Offset(cos(particle.angle) * distance, sin(particle.angle) * distance),
            )
        }
    }
}

private class ChalkParticle(val angle: Float, val reach: Float, val radiusDp: Float)
