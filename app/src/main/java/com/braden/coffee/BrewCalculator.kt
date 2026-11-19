package com.braden.coffee

import java.util.Locale
import kotlin.math.max

/** The quantities a user can type into or scrub. */
enum class Field { Ratio, Coffee, Water, Brewed }

/**
 * Immutable brew state.
 *
 * Only the ratio plus the most recently edited quantity ([anchor] / [anchorValue]) are stored;
 * everything else is derived. That way, changing the ratio keeps whatever you last set fixed
 * (e.g. "I want 500 g of brewed coffee") and recomputes the rest.
 */
data class BrewState(
    val ratio: Double = DEFAULT_RATIO,
    val anchor: Field = Field.Coffee,
    val anchorValue: Double = 18.0,
) {
    /** Grams of coffee per gram of water. */
    private val k: Double get() = ratio / 1000.0

    /** Water in g (== mL). */
    val water: Double
        get() = when (anchor) {
            Field.Water, Field.Ratio -> anchorValue
            Field.Coffee -> anchorValue / k
            Field.Brewed -> anchorValue / (1.0 - ABSORPTION * k)
        }

    /** Coffee grounds in g. */
    val coffee: Double
        get() = if (anchor == Field.Coffee) anchorValue else water * k

    /** Brewed yield in g: water minus what the grounds retain (~2x their mass). */
    val brewed: Double
        get() = if (anchor == Field.Brewed) anchorValue else max(0.0, water - ABSORPTION * coffee)

    fun valueOf(field: Field): Double = when (field) {
        Field.Ratio -> ratio
        Field.Coffee -> coffee
        Field.Water -> water
        Field.Brewed -> brewed
    }

    /** Returns a new state with [field] set to [value]; ratio edits keep the current anchor. */
    fun with(field: Field, value: Double): BrewState {
        val v = max(0.0, value)
        return when (field) {
            Field.Ratio -> copy(ratio = v.coerceIn(MIN_RATIO, MAX_RATIO))
            else -> copy(anchor = field, anchorValue = v)
        }
    }

    companion object {
        const val DEFAULT_RATIO = 60.0
        const val ABSORPTION = 2.0
        const val MIN_RATIO = 1.0
        // Must stay below 1000 / ABSORPTION (= 500) or brewed yield becomes <= 0.
        const val MAX_RATIO = 250.0
    }
}

/** Display formatting: ratio as a whole number (or trimmed decimal), others to 1 decimal. */
fun format(field: Field, value: Double): String = when (field) {
    Field.Ratio ->
        if (value % 1.0 == 0.0) String.format(Locale.US, "%.0f", value)
        else String.format(Locale.US, "%.1f", value)
    else -> String.format(Locale.US, "%.1f", value)
}

/** Fine-tuning step applied per scrub tick. */
fun scrubStep(field: Field): Double = when (field) {
    Field.Ratio -> 1.0
    Field.Coffee -> 0.1
    Field.Water, Field.Brewed -> 1.0
}
