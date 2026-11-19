package com.example.mygymstatsapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

data class ChartPoint(val x: Long, val y: Double)

data class BarEntry(val label: String, val value: Double)

/**
 * Smoothed line chart with gradient fill, drawn on a Canvas (no chart dependency).
 * Tap or drag horizontally to inspect a point.
 */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    formatY: (Double) -> String,
    formatX: (Long) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    if (points.isEmpty()) return
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val tooltipStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
    )
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val tooltipColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val holeColor = MaterialTheme.colorScheme.surfaceContainer
    val density = LocalDensity.current
    val insets = remember(density) {
        with(density) { ChartInsets(left = 48.dp.toPx(), top = 36.dp.toPx(), right = 12.dp.toPx(), bottom = 24.dp.toPx()) }
    }

    var selected by remember(points) { mutableStateOf<Int?>(null) }
    val reveal = remember(points) { Animatable(0f) }
    LaunchedEffect(points) { reveal.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }

    fun nearestIndex(x: Float, size: Size): Int {
        val geometry = LineGeometry(size, insets, points)
        return points.indices.minBy { abs(geometry.x(points[it].x) - x) }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val index = nearestIndex(offset.x, Size(size.width.toFloat(), size.height.toFloat()))
                    selected = if (selected == index) null else index
                }
            }
            .pointerInput(points) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        selected = nearestIndex(offset.x, Size(size.width.toFloat(), size.height.toFloat()))
                    },
                ) { change, _ ->
                    selected = nearestIndex(change.position.x, Size(size.width.toFloat(), size.height.toFloat()))
                }
            },
    ) {
        val geometry = LineGeometry(size, insets, points)
        drawGrid(geometry, textMeasurer, labelStyle, gridColor, formatY)

        // X labels: first and last date (and the middle one when there is room).
        val xIndices = when {
            points.size == 1 -> listOf(0)
            points.size < 5 -> listOf(0, points.lastIndex)
            else -> listOf(0, points.lastIndex / 2, points.lastIndex)
        }.distinct()
        xIndices.forEach { index ->
            val layout = textMeasurer.measure(formatX(points[index].x), labelStyle)
            val x = (geometry.x(points[index].x) - layout.size.width / 2f)
                .coerceIn(geometry.left - 8f, size.width - layout.size.width.toFloat())
            drawText(layout, topLeft = Offset(x, geometry.bottom + 6.dp.toPx()))
        }

        val offsets = points.map { Offset(geometry.x(it.x), geometry.y(it.y)) }
        val line = Path().apply {
            offsets.forEachIndexed { i, point ->
                if (i == 0) {
                    moveTo(point.x, point.y)
                } else {
                    val previous = offsets[i - 1]
                    val midX = (previous.x + point.x) / 2
                    cubicTo(midX, previous.y, midX, point.y, point.x, point.y)
                }
            }
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(offsets.last().x, geometry.bottom)
            lineTo(offsets.first().x, geometry.bottom)
            close()
        }

        clipRect(right = geometry.left + (size.width - geometry.left) * reveal.value) {
            if (offsets.size > 1) {
                drawPath(
                    path = fill,
                    brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.32f), Color.Transparent),
                        startY = geometry.top,
                        endY = geometry.bottom,
                    ),
                )
                drawPath(
                    path = line,
                    color = color,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
            if (offsets.size <= 24) {
                offsets.forEach { point ->
                    drawCircle(color, radius = 4.5.dp.toPx(), center = point)
                    drawCircle(holeColor, radius = 2.dp.toPx(), center = point)
                }
            }
        }

        selected?.let { index ->
            val point = offsets[index]
            drawLine(
                color = color.copy(alpha = 0.5f),
                start = Offset(point.x, geometry.top),
                end = Offset(point.x, geometry.bottom),
                strokeWidth = 1.5.dp.toPx(),
            )
            drawCircle(color, radius = 7.dp.toPx(), center = point)
            drawCircle(holeColor, radius = 3.dp.toPx(), center = point)
            drawTooltip(
                text = "${formatY(points[index].y)} · ${formatX(points[index].x)}",
                anchorX = point.x,
                textMeasurer = textMeasurer,
                style = tooltipStyle,
                background = tooltipColor,
            )
        }
    }
}

/** Vertical bars; the last (or tapped) bar is highlighted with its value. */
@Composable
fun BarChart(
    entries: List<BarEntry>,
    formatValue: (Double) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    if (entries.isEmpty()) return
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val valueStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
    )
    val tooltipColor = MaterialTheme.colorScheme.surfaceContainerHighest
    var selected by remember(entries) { mutableStateOf(entries.lastIndex) }
    val reveal = remember(entries) { Animatable(0f) }
    LaunchedEffect(entries) { reveal.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(entries) {
                detectTapGestures { offset ->
                    val slot = size.width.toFloat() / entries.size
                    selected = (offset.x / slot).toInt().coerceIn(0, entries.lastIndex)
                }
            },
    ) {
        val top = 32.dp.toPx()
        val bottom = size.height - 22.dp.toPx()
        val slot = size.width / entries.size
        val barWidth = (slot * 0.62f).coerceAtMost(36.dp.toPx())
        val maxValue = entries.maxOf { it.value }.takeIf { it > 0 } ?: 1.0
        val labelEvery = ceil(entries.size / 7f).toInt().coerceAtLeast(1)

        entries.forEachIndexed { index, entry ->
            val centerX = slot * index + slot / 2
            val barHeight = ((entry.value / maxValue).toFloat() * (bottom - top) * reveal.value)
                .coerceAtLeast(if (entry.value > 0) 3.dp.toPx() else 0f)
            drawRoundRect(
                color = if (index == selected) color else color.copy(alpha = 0.3f),
                topLeft = Offset(centerX - barWidth / 2, bottom - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(6.dp.toPx()),
            )
            // Count from the last bar so the most recent label is always shown without overlap.
            if ((entries.lastIndex - index) % labelEvery == 0) {
                val layout = textMeasurer.measure(entry.label, labelStyle)
                drawText(layout, topLeft = Offset(centerX - layout.size.width / 2f, bottom + 6.dp.toPx()))
            }
        }

        val entry = entries[selected]
        drawTooltip(
            text = "${formatValue(entry.value)} · ${entry.label}",
            anchorX = slot * selected + slot / 2,
            textMeasurer = textMeasurer,
            style = valueStyle,
            background = tooltipColor,
        )
    }
}

private data class ChartInsets(val left: Float, val top: Float, val right: Float, val bottom: Float)

private class LineGeometry(size: Size, insets: ChartInsets, points: List<ChartPoint>) {
    val left = insets.left
    val top = insets.top
    val right = size.width - insets.right
    val bottom = size.height - insets.bottom
    private val minX = points.minOf { it.x }
    private val maxX = points.maxOf { it.x }
    val minY: Double
    val maxY: Double

    init {
        var low = points.minOf { it.y }
        var high = points.maxOf { it.y }
        if (high - low < 1e-6) {
            val spread = max(1.0, abs(high) * 0.1)
            low -= spread
            high += spread
        }
        val padding = (high - low) * 0.15
        minY = if (low >= 0) (low - padding).coerceAtLeast(0.0) else low - padding
        maxY = high + padding
    }

    fun x(value: Long): Float =
        if (maxX == minX) (left + right) / 2 else left + (value - minX).toFloat() / (maxX - minX) * (right - left)

    fun y(value: Double): Float = bottom - ((value - minY) / (maxY - minY)).toFloat() * (bottom - top)
}

private fun DrawScope.drawGrid(
    geometry: LineGeometry,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    color: Color,
    formatY: (Double) -> String,
) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
    for (step in 0..2) {
        val value = geometry.minY + (geometry.maxY - geometry.minY) * step / 2
        val y = geometry.y(value)
        drawLine(color, Offset(geometry.left, y), Offset(geometry.right, y), strokeWidth = 1.dp.toPx(), pathEffect = dash)
        val layout = textMeasurer.measure(formatY(value), style)
        drawText(layout, topLeft = Offset(geometry.left - layout.size.width - 8.dp.toPx(), y - layout.size.height / 2f))
    }
}

private fun DrawScope.drawTooltip(
    text: String,
    anchorX: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    background: Color,
) {
    val layout = textMeasurer.measure(text, style)
    val paddingH = 10.dp.toPx()
    val paddingV = 5.dp.toPx()
    val width = layout.size.width + paddingH * 2
    val height = layout.size.height + paddingV * 2
    val left = (anchorX - width / 2).coerceIn(0f, (size.width - width).coerceAtLeast(0f))
    drawRoundRect(background, topLeft = Offset(left, 0f), size = Size(width, height), cornerRadius = CornerRadius(height / 2))
    drawText(layout, topLeft = Offset(left + paddingH, paddingV))
}
