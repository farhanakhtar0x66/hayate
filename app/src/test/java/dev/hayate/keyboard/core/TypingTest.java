package dev.hayate.keyboard.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class TypingTest {
    @Test public void everyEnglishLetterCanBeTypedWithoutSwitchingLayers() {
        StringBuilder typed = new StringBuilder();
        for (KeySpec[] row : Layouts.rows(KeyboardState.Layer.LETTERS)) {
            for (KeySpec key : row) for (Flick direction : Flick.values()) {
                KeySpec.Value v = key.at(direction);
                if (v.kind == KeySpec.Kind.TEXT) typed.append(v.text);
            }
        }
        for (char c = 'a'; c <= 'z'; c++) assertTrue("Missing " + c, typed.indexOf(String.valueOf(c)) >= 0);
    }
    @Test public void suppliedHoneyKeyFlicksTypeHello() {
        KeySpec[][] rows = Layouts.rows(KeyboardState.Layer.LETTERS);
        assertEquals("h", rows[2][3].at(Flick.LEFT).text);
        assertEquals("e", rows[0][2].at(Flick.CENTER).text);
        assertEquals("l", rows[1][3].at(Flick.DOWN).text);
        assertEquals("o", rows[0][2].at(Flick.LEFT).text);
    }
    @Test public void oneShotShiftIsConsumedOnlyByALetter() {
        KeyboardState s = new KeyboardState();
        s.toggleShift();
        assertEquals(".", s.output(KeySpec.text(".")));
        assertEquals("H", s.output(KeySpec.text("h")));
        assertEquals("a", s.output(KeySpec.text("a")));
    }
    @Test public void capsLockSurvivesLettersAndLayerSwitch() {
        KeyboardState s = new KeyboardState();
        s.toggleShift(); s.toggleShift();
        s.switchLayer(KeyboardState.Layer.NUMBERS);
        assertEquals("7", s.output(KeySpec.text("7")));
        s.switchLayer(KeyboardState.Layer.LETTERS);
        assertEquals("A", s.output(KeySpec.text("a")));
        assertEquals("B", s.output(KeySpec.text("b")));
        s.toggleShift();
        assertEquals("c", s.output(KeySpec.text("c")));
    }
    @Test public void numberLayerContainsAllDigits() {
        StringBuilder result = new StringBuilder();
        for (KeySpec[] row : Layouts.rows(KeyboardState.Layer.NUMBERS))
            for (KeySpec key : row) for (KeySpec.Value v : key.values)
                if (v != null && v.kind == KeySpec.Kind.TEXT) result.append(v.text);
        for (char digit = '0'; digit <= '9'; digit++) assertTrue("Missing digit " + digit, result.indexOf("" + digit) >= 0);
    }
    @Test public void everyLayerHasWorkingReturnToLettersAndDelete() {
        for (KeyboardState.Layer layer : KeyboardState.Layer.values()) {
            boolean letters = layer == KeyboardState.Layer.LETTERS;
            boolean delete = false;
            for (KeySpec[] row : Layouts.rows(layer)) for (KeySpec key : row) for (KeySpec.Value v : key.values) {
                if (v == null) continue;
                letters |= v.kind == KeySpec.Kind.LAYER && v.text.equals("LETTERS");
                delete |= v.kind == KeySpec.Kind.BACKSPACE;
            }
            assertTrue("No letters return in " + layer, letters);
            assertTrue("No delete in " + layer, delete);
        }
    }
}
