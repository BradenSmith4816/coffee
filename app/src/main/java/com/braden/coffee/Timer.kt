package com.braden.coffee

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Single-button stopwatch: idle --tap--> running --tap--> stopped --tap--> idle (reset).
 * Times are [SystemClock.elapsedRealtime] millis, so it keeps counting across rotation.
 */
data class TimerState(
    val startedAt: Long = NOT_RUNNING,
    val elapsedMs: Long = 0L,
) {
    val isRunning: Boolean get() = startedAt != NOT_RUNNING
    val isStopped: Boolean get() = !isRunning && elapsedMs > 0L

    fun elapsed(now: Long): Long =
        if (isRunning) (now - startedAt).coerceAtLeast(0L) else elapsedMs

    fun tap(now: Long): TimerState = when {
        isRunning -> TimerState(elapsedMs = elapsed(now))
        isStopped -> TimerState()
        else -> TimerState(startedAt = now)
    }

    companion object {
        const val NOT_RUNNING = -1L
    }
}

internal val TimerStateSaver = listSaver<TimerState, Long>(
    save = { listOf(it.startedAt, it.elapsedMs) },
    restore = { TimerState(it[0], it[1]) },
)

/** m:ss, or h:mm:ss once past an hour. */
fun formatElapsed(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

/** Keeps the display awake while the timer runs, so the phone doesn't lock mid-brew. */
@Composable
internal fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
internal fun TimerPane(
    timer: TimerState,
    onTimerChange: (TimerState) -> Unit,
    timeSize: Float,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(timer.isRunning) {
        if (timer.isRunning) {
            while (true) {
                now = SystemClock.elapsedRealtime()
                delay(100)
            }
        }
    }

    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "TIMER",
            color = if (timer.isRunning) Accent else TextMuted,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            formatElapsed(timer.elapsed(now)),
            style = TextStyle(
                color = TextPrimary,
                fontSize = timeSize.sp,
                fontWeight = FontWeight.Light,
                fontFeatureSettings = "tnum",
            ),
        )
        Spacer(Modifier.height(12.dp))
        TimerButton(
            timer = timer,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                val t = SystemClock.elapsedRealtime()
                now = t
                onTimerChange(timer.tap(t))
            },
        )
    }
}

@Composable
private fun TimerButton(timer: TimerState, onClick: () -> Unit) {
    val description = when {
        timer.isRunning -> "Stop timer"
        timer.isStopped -> "Reset timer"
        else -> "Start timer"
    }
    Box(
        Modifier
            .size(64.dp)
            .clip(CircleShape)
            .border(1.dp, TextMuted.copy(alpha = 0.35f), CircleShape)
            .clickable(
                interactionSource = null,
                indication = ripple(color = Accent),
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(22.dp)) {
            when {
                timer.isRunning -> drawStopIcon(Accent)
                timer.isStopped -> drawResetIcon(TextPrimary)
                else -> drawPlayIcon(TextPrimary)
            }
        }
    }
}

private fun DrawScope.drawPlayIcon(color: Color) {
    val s = size.minDimension
    val c = center
    val path = Path().apply {
        moveTo(c.x - s * 0.28f, c.y - s * 0.40f)
        lineTo(c.x + s * 0.42f, c.y)
        lineTo(c.x - s * 0.28f, c.y + s * 0.40f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawStopIcon(color: Color) {
    val side = size.minDimension * 0.62f
    drawRoundRect(
        color = color,
        topLeft = center - Offset(side / 2, side / 2),
        size = Size(side, side),
        cornerRadius = CornerRadius(side * 0.15f),
    )
}

/** Circular arrow: a 270° arc with an arrowhead on its end. */
private fun DrawScope.drawResetIcon(color: Color) {
    val s = size.minDimension
    val r = s * 0.36f
    val c = center
    val startAngle = -45f
    val sweep = 270f
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = c - Offset(r, r),
        size = Size(r * 2, r * 2),
        style = Stroke(width = s * 0.1f, cap = StrokeCap.Round),
    )
    val theta = Math.toRadians((startAngle + sweep).toDouble())
    val cosT = cos(theta).toFloat()
    val sinT = sin(theta).toFloat()
    val end = Offset(c.x + r * cosT, c.y + r * sinT)
    val tangent = Offset(-sinT, cosT) // direction of travel (clockwise)
    val normal = Offset(cosT, sinT)
    val a = s * 0.2f
    val head = Path().apply {
        moveTo(end.x + tangent.x * a, end.y + tangent.y * a)
        lineTo(end.x + normal.x * a * 0.75f, end.y + normal.y * a * 0.75f)
        lineTo(end.x - normal.x * a * 0.75f, end.y - normal.y * a * 0.75f)
        close()
    }
    drawPath(head, color)
}
