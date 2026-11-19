package com.braden.coffee

import org.junit.Assert.assertEquals
import org.junit.Test

class BrewStateTest {
    private val eps = 1e-9

    @Test
    fun defaults() {
        val s = BrewState()
        assertEquals(60.0, s.ratio, eps)
        assertEquals(500.0, s.water, eps)
        assertEquals(30.0, s.coffee, eps)
        assertEquals(440.0, s.brewed, eps)
    }

    @Test
    fun fromCoffee() {
        val s = BrewState().with(Field.Coffee, 18.0)
        assertEquals(300.0, s.water, eps)
        assertEquals(264.0, s.brewed, eps)
    }

    @Test
    fun fromBrewed() {
        val s = BrewState().with(Field.Brewed, 880.0)
        assertEquals(1000.0, s.water, eps)
        assertEquals(60.0, s.coffee, eps)
    }

    @Test
    fun ratioChangeKeepsLastEditedValue() {
        val s = BrewState().with(Field.Brewed, 440.0).with(Field.Ratio, 70.0)
        assertEquals(440.0, s.brewed, eps)
        assertEquals(440.0 / (1 - 0.14), s.water, eps)
        assertEquals(s.water * 0.07, s.coffee, eps)
    }

    @Test
    fun formatting() {
        assertEquals("60", format(Field.Ratio, 60.0))
        assertEquals("30.0", format(Field.Coffee, 30.0))
        assertEquals("466.7", format(Field.Water, 466.666))
    }
}
