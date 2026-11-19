package com.example.mygymstatsapp.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PhonelinkErase
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.mygymstatsapp.shared.StartableSession
import com.example.mygymstatsapp.wear.R

/** No workout in progress: start one (the program's next session first), or look back. */
@Composable
fun HomeScreen(
    state: WatchUiState,
    onStart: (templateId: String?) -> Unit,
    onHistory: () -> Unit,
    onRecords: () -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val phone = state.phone
    val next = phone?.sessions?.firstOrNull { it.isNext }
    val others = phone?.sessions?.filter { it.templateId != next?.templateId }.orEmpty()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { Wordmark() }

            if (state.phoneReachable == false) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.PhonelinkErase, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.home_no_phone_title), style = MaterialTheme.typography.labelMedium)
                        }
                        Text(
                            stringResource(R.string.home_no_phone),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (phone == null) {
                item {
                    Text(
                        stringResource(R.string.home_waiting),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                    )
                }
            } else {
                if (next != null) {
                    item { NextSessionButton(next, onClick = { onStart(next.templateId) }) }
                }
                if (others.isNotEmpty()) {
                    item { ListHeader { Text(stringResource(R.string.home_sessions)) } }
                    items(others, key = { it.templateId }) { session ->
                        FilledTonalButton(
                            onClick = { onStart(session.templateId) },
                            modifier = Modifier.fillMaxWidth(),
                            icon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            secondaryLabel = { Text(session.programName ?: sessionSummary(session), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            label = { Text(session.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
                item {
                    FilledTonalButton(
                        onClick = { onStart(null) },
                        modifier = Modifier.fillMaxWidth(),
                        icon = { Icon(Icons.Rounded.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        label = { Text(stringResource(R.string.home_empty_workout)) },
                    )
                }
            }

            item { ListHeader { Text(stringResource(R.string.home_history)) } }
            item {
                FilledTonalButton(
                    onClick = onHistory,
                    modifier = Modifier.fillMaxWidth(),
                    icon = { Icon(Icons.Rounded.History, contentDescription = null) },
                    label = { Text(stringResource(R.string.home_history)) },
                )
            }
            item {
                FilledTonalButton(
                    onClick = onRecords,
                    modifier = Modifier.fillMaxWidth(),
                    icon = { Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                    label = { Text(stringResource(R.string.home_records)) },
                )
            }
        }
    }
}

/** Plate + FONTE, as on the phone's home screen. */
@Composable
private fun Wordmark() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(bottom = 4.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_fonte_mark),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            stringResource(R.string.app_name).uppercase(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun NextSessionButton(session: StartableSession, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        icon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
        secondaryLabel = {
            Column {
                Text(stringResource(R.string.home_next), style = MaterialTheme.typography.labelSmall)
                Text(sessionSummary(session), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        label = { Text(session.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
    )
}

/** "6 exercices · 20 séries". */
@Composable
private fun sessionSummary(session: StartableSession): String =
    pluralStringResource(R.plurals.count_exercises, session.exerciseCount, session.exerciseCount) + " · " +
        pluralStringResource(R.plurals.count_sets, session.setCount, session.setCount)
