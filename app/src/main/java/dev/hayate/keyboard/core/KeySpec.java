package dev.hayate.keyboard.core;

public final class KeySpec {
    public enum Kind { TEXT, SPACE, BACKSPACE, RETURN, SHIFT, LAYER, LEFT, RIGHT, UP, DOWN, HOME, END, SELECT_ALL, CUT, COPY, PASTE, UNDO, REDO, SETTINGS, NEXT_KEYBOARD }
    public static final class Value {
        public final Kind kind;
        public final String text;
        public final String label;
        public Value(Kind kind, String text, String label) { this.kind = kind; this.text = text; this.label = label; }
    }
    public final Value[] values;
    public final float weight;
    public final boolean letter;
    public KeySpec(float weight, boolean letter, Value... values) { this.weight = weight; this.letter = letter; this.values = values; }
    public Value at(Flick direction) {
        Value v = values[direction.ordinal()];
        return v == null ? values[0] : v;
    }
    public static Value text(String s) { return new Value(Kind.TEXT, s, s); }
    public static Value action(Kind k, String label) { return new Value(k, "", label); }
    public static Value layer(KeyboardState.Layer l, String label) { return new Value(Kind.LAYER, l.name(), label); }
}
