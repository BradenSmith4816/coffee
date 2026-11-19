package com.braden.coffee

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs

/**
 * Horizontal "scrub" gesture. Listens on the Initial pass so it wins over child text fields once a
 * horizontal drag passes touch slop; taps (and vertical motion) are left alone, so tapping a field
 * still focuses it for typing.
 */
fun Modifier.horizontalScrub(
    onStart: () -> Unit,
    onDelta: (dx: Float) -> Unit,
    onEnd: () -> Unit,
): Modifier = composed {
    val start by rememberUpdatedState(onStart)
    val delta by rememberUpdatedState(onDelta)
    val end by rememberUpdatedState(onEnd)

    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val slop = viewConfiguration.touchSlop
            var totalX = 0f
            var totalY = 0f
            var dragging = false

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    if (dragging) change.consume()
                    break
                }
                val d = change.positionChange()
                if (!dragging) {
                    totalX += d.x
                    totalY += d.y
                    if (abs(totalY) > slop && abs(totalY) > abs(totalX)) break // vertical: not ours
                    if (abs(totalX) > slop) {
                        dragging = true
                        start()
                    }
                } else {
                    delta(d.x)
                }
                if (dragging) change.consume()
            }
            if (dragging) end()
        }
    }
}
