package com.example.mygymstatsapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.utils.FIRST_DAY_OF_WEEK
import java.time.format.TextStyle
import java.util.Locale

/** One-letter weekday names in calendar order: "L M M J V S D". */
fun weekdayLetters(): List<String> =
    List(7) { FIRST_DAY_OF_WEEK.plus(it.toLong()).getDisplayName(TextStyle.NARROW, Locale.getDefault()) }

/** A calendar day: filled when trained, ringed when today, highlighted when selected; future days are inert. */
@Composable
fun DayBubble(
    dayOfMonth: Int,
    trained: Boolean,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    isFuture: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val background = when {
        trained -> colors.primary
        selected -> colors.surfaceContainerHighest
        else -> Color.Transparent
    }
    val border = when {
        selected && trained -> BorderStroke(2.dp, colors.onSurface)
        isToday && !trained -> BorderStroke(1.5.dp, colors.primary)
        else -> null
    }
    val textColor = when {
        trained -> colors.onPrimary
        isFuture -> colors.onSurfaceVariant.copy(alpha = 0.4f)
        isToday -> colors.primary
        else -> colors.onSurface
    }
    val trainedLabel = stringResource(R.string.calendar_trained)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(CircleShape)
            .background(background)
            .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
            .then(if (onClick != null && !isFuture) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { if (trained) stateDescription = trainedLabel },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$dayOfMonth",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (trained || isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
        )
    }
}
