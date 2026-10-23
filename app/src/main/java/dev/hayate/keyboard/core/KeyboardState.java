package dev.hayate.keyboard.core;

import java.util.Locale;

public final class KeyboardState {
    public enum Layer { LETTERS, NUMBERS, SYMBOLS, EDIT, EMOJI, CODE }
    public enum Shift { OFF, ON, LOCK }
    public Layer layer = Layer.LETTERS;
    public Shift shift = Shift.OFF;
    public boolean programmer;
    public void toggleShift() { shift = shift == Shift.OFF ? Shift.ON : shift == Shift.ON ? Shift.LOCK : Shift.OFF; }
    public String output(KeySpec.Value value) {
        if (value.kind != KeySpec.Kind.TEXT || shift == Shift.OFF || !value.text.codePoints().anyMatch(Character::isLetter)) return value.text;
        String output = value.text.toUpperCase(Locale.ROOT);
        if (shift == Shift.ON) shift = Shift.OFF;
        return output;
    }
    public void switchLayer(Layer next) { layer = next; }
}
