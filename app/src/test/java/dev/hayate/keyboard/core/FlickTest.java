package dev.hayate.keyboard.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class FlickTest {
    @Test public void smallThumbJitterStaysAtCenter() {
        assertEquals(Flick.CENTER, Flick.resolve(8, -9, 18));
    }
    @Test public void choosesDominantDirectionAtThreshold() {
        assertEquals(Flick.UP, Flick.resolve(4, -18, 18));
        assertEquals(Flick.DOWN, Flick.resolve(-9, 22, 18));
        assertEquals(Flick.LEFT, Flick.resolve(-30, 12, 18));
        assertEquals(Flick.RIGHT, Flick.resolve(30, -12, 18));
    }
    @Test public void diagonalBelowCardinalThresholdStaysTap() {
        assertEquals(Flick.CENTER, Flick.resolve(16, 16, 18));
    }
}
