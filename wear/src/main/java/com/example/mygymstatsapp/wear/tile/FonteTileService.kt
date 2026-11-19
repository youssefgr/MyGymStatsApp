package com.example.mygymstatsapp.wear.tile

import androidx.concurrent.futures.SuspendToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.TypeBuilders
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.example.mygymstatsapp.shared.LiveRest
import com.example.mygymstatsapp.shared.PhoneState
import com.example.mygymstatsapp.shared.StartableSession
import com.example.mygymstatsapp.wear.MainActivity
import com.example.mygymstatsapp.wear.R
import com.example.mygymstatsapp.wear.container
import com.example.mygymstatsapp.wear.ui.formatClock
import com.google.common.util.concurrent.ListenableFuture
import java.time.Instant

/**
 * Swipe from the watch face: the program's next session with a "Démarrer" button, or, during a
 * workout, its progress and the rest countdown (ticking on the watch, no update needed).
 * Refreshed by WatchListenerService whenever the phone publishes a new state.
 */
class FonteTileService : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        SuspendToFutureAdapter.launchFuture {
            val link = container.link
            link.ensureLoaded()
            TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setTileTimeline(timeline(link.state.value))
                .build()
        }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> =
        SuspendToFutureAdapter.launchFuture {
            ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .addIdToImageMapping(
                    IMAGE_MARK,
                    ResourceBuilders.ImageResource.Builder()
                        .setAndroidResourceByResId(
                            ResourceBuilders.AndroidImageResourceByResId.Builder().setResourceId(R.drawable.ic_fonte_mark).build()
                        )
                        .build(),
                )
                .build()
        }

    private fun timeline(state: PhoneState?): TimelineBuilders.Timeline {
        val workout = state?.workout
        val rest = state?.rest
        if (workout == null || rest == null || rest.isPaused || rest.endsAt <= System.currentTimeMillis()) {
            return TimelineBuilders.Timeline.fromLayoutElement(layout(state, restRunning = false))
        }
        // Countdown until the end of the rest, then "Repos terminé" until the phone sends the next state.
        return TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(entry(layout(state, restRunning = true), until = rest.endsAt))
            .addTimelineEntry(entry(layout(state, restRunning = false, restOver = true), from = rest.endsAt))
            .build()
    }

    private fun entry(element: LayoutElementBuilders.LayoutElement, from: Long? = null, until: Long? = null) =
        TimelineBuilders.TimelineEntry.Builder()
            .setLayout(LayoutElementBuilders.Layout.fromLayoutElement(element))
            .setValidity(
                TimelineBuilders.TimeInterval.Builder()
                    .apply { from?.let { setStartMillis(it) } }
                    .apply { until?.let { setEndMillis(it) } }
                    .build()
            )
            .build()

    private fun layout(state: PhoneState?, restRunning: Boolean, restOver: Boolean = false): LayoutElementBuilders.LayoutElement {
        val workout = state?.workout
        val next = state?.sessions?.firstOrNull { it.isNext } ?: state?.sessions?.firstOrNull()
        val column = LayoutElementBuilders.Column.Builder()
            .setWidth(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(mark())
            .addContent(spacer(4f))

        when {
            workout != null -> {
                column.addContent(text(getString(R.string.home_in_progress), 11f, FONTE, bold = true))
                column.addContent(text(workout.name, 16f, CHALK, bold = true, maxLines = 2))
                column.addContent(spacer(4f))
                val rest = state.rest
                when {
                    restRunning && rest != null -> {
                        column.addContent(text(getString(R.string.rest_label), 11f, ASH))
                        column.addContent(countdown(rest))
                    }
                    restOver -> column.addContent(text(getString(R.string.rest_over), 14f, STEEL, bold = true))
                    rest != null && rest.isPaused -> {
                        column.addContent(text(getString(R.string.rest_paused), 11f, ASH))
                        column.addContent(text(formatClock(rest.remainingMs() / 1000), 28f, STEEL, bold = true))
                    }
                    else -> column.addContent(
                        text(getString(R.string.workout_sets_done, workout.completedSets, workout.totalSets), 12f, ASH)
                    )
                }
                return root(column, openApp(templateId = null, start = false))
            }
            next != null -> {
                column.addContent(text(getString(R.string.home_next), 11f, FONTE, bold = true))
                column.addContent(text(next.name, 16f, CHALK, bold = true, maxLines = 2))
                column.addContent(text(summary(next), 11f, ASH))
                column.addContent(spacer(8f))
                column.addContent(startButton(next))
                return root(column, openApp(templateId = null, start = false))
            }
            else -> {
                column.addContent(text(getString(R.string.app_name).uppercase(), 16f, CHALK, bold = true))
                column.addContent(text(getString(R.string.home_waiting), 11f, ASH, maxLines = 3))
                return root(column, openApp(templateId = null, start = false))
            }
        }
    }

    /** mm:ss computed on the watch from the end time: it ticks without updating the tile. */
    private fun countdown(rest: LiveRest): LayoutElementBuilders.LayoutElement {
        val remaining = DynamicInstant.platformTimeWithSecondsPrecision()
            .durationUntil(DynamicInstant.withSecondsPrecision(Instant.ofEpochMilli(rest.endsAt)))
        val twoDigits = DynamicInt32.IntFormatter.Builder().setMinIntegerDigits(2).build()
        val dynamicText = remaining.getMinutesPart().format(twoDigits)
            .concat(DynamicString.constant(":"))
            .concat(remaining.getSecondsPart().format(twoDigits))
        return LayoutElementBuilders.Text.Builder()
            .setText(
                TypeBuilders.StringProp.Builder(formatClock((rest.remainingMs() + 999) / 1000))
                    .setDynamicValue(dynamicText)
                    .build()
            )
            .setLayoutConstraintsForDynamicText(TypeBuilders.StringLayoutConstraint.Builder("00:00").build())
            .setFontStyle(fontStyle(28f, STEEL, bold = true))
            .build()
    }

    private fun startButton(session: StartableSession): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Box.Builder()
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(openApp(templateId = session.templateId, start = true))
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(FONTE))
                            .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(14f)).build())
                            .build()
                    )
                    .setPadding(
                        ModifiersBuilders.Padding.Builder()
                            .setStart(dp(18f)).setEnd(dp(18f)).setTop(dp(8f)).setBottom(dp(8f))
                            .build()
                    )
                    .build()
            )
            .addContent(text(getString(R.string.tile_start), 14f, CHARCOAL, bold = true))
            .build()

    private fun root(column: LayoutElementBuilders.Column.Builder, clickable: ModifiersBuilders.Clickable) =
        LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(clickable)
                    .setPadding(ModifiersBuilders.Padding.Builder().setStart(dp(20f)).setEnd(dp(20f)).build())
                    .build()
            )
            .addContent(column.build())
            .build()

    /** Opens the app; with [start], it starts [templateId] (empty extra = empty workout). */
    private fun openApp(templateId: String?, start: Boolean): ModifiersBuilders.Clickable {
        val activity = ActionBuilders.AndroidActivity.Builder()
            .setPackageName(packageName)
            .setClassName(MainActivity::class.java.name)
        if (start) {
            activity.addKeyToExtraMapping(
                MainActivity.EXTRA_START_TEMPLATE,
                ActionBuilders.AndroidStringExtra.Builder().setValue(templateId.orEmpty()).build(),
            )
        }
        return ModifiersBuilders.Clickable.Builder()
            .setId(if (start) "start_${templateId.orEmpty()}" else "open")
            .setOnClick(ActionBuilders.LaunchAction.Builder().setAndroidActivity(activity.build()).build())
            .build()
    }

    private fun mark() = LayoutElementBuilders.Image.Builder()
        .setResourceId(IMAGE_MARK)
        .setWidth(dp(22f))
        .setHeight(dp(22f))
        .setColorFilter(LayoutElementBuilders.ColorFilter.Builder().setTint(argb(FONTE)).build())
        .build()

    private fun spacer(height: Float) = LayoutElementBuilders.Spacer.Builder().setHeight(dp(height)).build()

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false, maxLines: Int = 1) =
        LayoutElementBuilders.Text.Builder()
            .setText(value)
            .setMaxLines(maxLines)
            .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
            .setFontStyle(fontStyle(size, color, bold))
            .build()

    private fun fontStyle(size: Float, color: Int, bold: Boolean) = LayoutElementBuilders.FontStyle.Builder()
        .setSize(sp(size))
        .setColor(argb(color))
        .setWeight(if (bold) LayoutElementBuilders.FONT_WEIGHT_BOLD else LayoutElementBuilders.FONT_WEIGHT_NORMAL)
        .build()

    private fun summary(session: StartableSession): String =
        resources.getQuantityString(R.plurals.count_exercises, session.exerciseCount, session.exerciseCount) + " · " +
            resources.getQuantityString(R.plurals.count_sets, session.setCount, session.setCount)

    private companion object {
        const val RESOURCES_VERSION = "1"
        const val IMAGE_MARK = "fonte_mark"
        // Fonte palette (docs/BRAND.md): orange for effort, steel for rest.
        const val FONTE = 0xFFFF5A1F.toInt()
        const val STEEL = 0xFF8FB3C9.toInt()
        const val CHALK = 0xFFEDE6DA.toInt()
        const val ASH = 0xFFA39A8C.toInt()
        const val CHARCOAL = 0xFF15120F.toInt()
    }
}
