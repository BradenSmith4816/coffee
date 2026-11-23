package com.braden.coffee

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToLong

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { BrewScreen() }
    }
}

private val BrewStateSaver = listSaver<BrewState, Any>(
    save = { listOf(it.ratio, it.anchor.name, it.anchorValue) },
    restore = { BrewState(it[0] as Double, Field.valueOf(it[1] as String), it[2] as Double) },
)

/** Sizing for the calculator column; landscape derives a compact set from the screen height. */
private data class Dimens(
    val valueSize: Float,
    val ratioSize: Float,
    val fieldPadding: Dp,
    val sectionGap: Dp,
    val hintGap: Dp,
) {
    companion object {
        val Regular = Dimens(52f, 40f, 10.dp, 12.dp, 40.dp)

        /** Scales the numbers with available height so all four fields fit without scrolling. */
        fun compact(availableHeight: Dp): Dimens {
            val v = (availableHeight.value * 0.1f).coerceIn(24f, 44f)
            return Dimens(v, v * 0.8f, 3.dp, 6.dp, 10.dp)
        }
    }
}

@Composable
fun BrewScreen() {
    var state by rememberSaveable(stateSaver = BrewStateSaver) { mutableStateOf(BrewState()) }
    // Hoisted here so the timer keeps running when rotating back to portrait.
    var timer by rememberSaveable(stateSaver = TimerStateSaver) { mutableStateOf(TimerState()) }
    val focusManager = LocalFocusManager.current
    val update: (Field, Double) -> Unit = { field, v -> state = state.with(field, v) }

    KeepScreenOn(timer.isRunning)

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Background)
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (maxWidth > maxHeight) {
            val dimens = Dimens.compact(maxHeight)
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Calculator(
                        state, update, dimens,
                        Modifier.widthIn(max = CalculatorMaxWidth).fillMaxWidth(),
                    )
                }
                Spacer(Modifier.width(28.dp))
                VerticalDivider(color = Hairline, modifier = Modifier.fillMaxHeight(0.6f))
                Spacer(Modifier.width(28.dp))
                TimerPane(
                    timer = timer,
                    onTimerChange = { timer = it },
                    timeSize = dimens.valueSize * 1.6f,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Calculator(
                state, update, Dimens.Regular,
                Modifier.widthIn(max = CalculatorMaxWidth).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Calculator(
    state: BrewState,
    update: (Field, Double) -> Unit,
    dimens: Dimens,
    modifier: Modifier = Modifier,
) {
    val v = dimens.valueSize
    val pad = dimens.fieldPadding
    Column(modifier, verticalArrangement = Arrangement.Center) {
        BrewField("Ratio", "g/L", Field.Ratio, state.ratio, update, dimens.ratioSize, pad)
        Spacer(Modifier.height(dimens.sectionGap))
        HorizontalDivider(color = Hairline)
        Spacer(Modifier.height(dimens.sectionGap))
        BrewField("Coffee", "g", Field.Coffee, state.coffee, update, v, pad)
        BrewField("Water", "g", Field.Water, state.water, update, v, pad)
        Spacer(Modifier.height(dimens.sectionGap))
        HorizontalDivider(color = Hairline)
        Spacer(Modifier.height(dimens.sectionGap))
        BrewField("Brewed", "g", Field.Brewed, state.brewed, update, v, pad, highlight = true)
        Spacer(Modifier.height(dimens.hintGap))
        Text(
            "Tap to type  ·  slide to fine-tune",
            color = TextMuted.copy(alpha = 0.75f),
            fontSize = LabelSize,
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun BrewField(
    label: String,
    unit: String,
    field: Field,
    value: Double,
    onValueChange: (Field, Double) -> Unit,
    size: Float,
    verticalPadding: Dp,
    highlight: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    val haptics = LocalHapticFeedback.current
    val pxPerStep = with(LocalDensity.current) { 6.dp.toPx() }

    val formatted = format(field, value)
    var focused by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(TextFieldValue(formatted)) }
    var scrubbing by remember { mutableStateOf(false) }

    // Scrub bookkeeping (not UI state, so plain holders).
    val scrub = remember { object { var acc = 0f; var value = 0.0 } }
    val step = scrubStep(field)

    val active = focused || scrubbing
    val labelColor by animateColorAsState(if (active) Accent else TextMuted, label = "label")
    val valueColor = if (highlight) Accent else TextPrimary

    Column(
        Modifier
            .fillMaxWidth()
            .horizontalScrub(
                onStart = {
                    focusManager.clearFocus()
                    scrubbing = true
                    scrub.acc = 0f
                    scrub.value = value
                },
                onDelta = { dx ->
                    scrub.acc += dx
                    val steps = (scrub.acc / pxPerStep).toInt()
                    if (steps != 0) {
                        scrub.acc -= steps * pxPerStep
                        val raw = scrub.value + steps * step
                        val snapped = (raw / step).roundToLong() * step
                        val next = if (field == Field.Ratio) {
                            snapped.coerceIn(BrewState.MIN_RATIO, BrewState.MAX_RATIO)
                        } else {
                            snapped.coerceAtLeast(0.0)
                        }
                        if (next != scrub.value) {
                            scrub.value = next
                            onValueChange(field, next)
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        }
                    }
                },
                onEnd = { scrubbing = false },
            )
            .padding(vertical = verticalPadding),
    ) {
        Text(
            label.uppercase(),
            color = labelColor,
            fontSize = LabelSize,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Medium,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            BasicTextField(
                value = if (focused) editing else TextFieldValue(formatted),
                onValueChange = { tfv ->
                    val cleaned = tfv.text.replace(',', '.')
                    if (cleaned.isEmpty() || NUMBER_INPUT.matches(cleaned)) {
                        editing = tfv.copy(text = cleaned)
                        cleaned.toDoubleOrNull()?.let { onValueChange(field, it) }
                    }
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = valueColor,
                    fontSize = size.sp,
                    fontWeight = FontWeight.Light,
                    fontFeatureSettings = "tnum",
                ),
                cursorBrush = SolidColor(Accent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged {
                        if (it.isFocused && !focused) {
                            editing = TextFieldValue(formatted, TextRange(0, formatted.length))
                        }
                        focused = it.isFocused
                    },
            )
            Text(
                unit,
                color = TextMuted,
                fontSize = (size * 0.3f).coerceIn(12f, 16f).sp,
                modifier = Modifier.padding(bottom = (size / 5).dp, start = 8.dp),
            )
        }
    }
}

private val NUMBER_INPUT = Regex("""\d*\.?\d*""")

@Preview(showBackground = true, backgroundColor = 0xFF15110F, widthDp = 380, heightDp = 760)
@Composable
private fun BrewScreenPreview() {
    BrewScreen()
}

// Roughly a Pixel 9 in landscape after system bar insets.
@Preview(showBackground = true, backgroundColor = 0xFF15110F, widthDp = 860, heightDp = 340)
@Composable
private fun BrewScreenLandscapePreview() {
    BrewScreen()
}

// Wide portrait (tablet / unfolded) to check the max-width constraint.
@Preview(showBackground = true, backgroundColor = 0xFF15110F, widthDp = 700, heightDp = 900)
@Composable
private fun BrewScreenWidePreview() {
    BrewScreen()
}
