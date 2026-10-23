package dev.hayate.keyboard.core;

import static dev.hayate.keyboard.core.KeySpec.*;
import static dev.hayate.keyboard.core.KeySpec.Kind.*;
import static dev.hayate.keyboard.core.KeyboardState.Layer.*;

public final class Layouts {
    private Layouts() { }
    private static final KeySpec DELETE = simple(BACKSPACE, "⌫");
    private static final KeySpec ENTER = simple(RETURN, "↵");
    private static final KeySpec SHIFT_KEY = simple(SHIFT, "⇧");
    private static final KeySpec ABC = key(layer(LETTERS, "abc"), null, layer(EMOJI, "☺"), null, layer(EDIT, "edit"));
    private static final KeySpec LEFT_KEY = wide(LEFT, "‹");
    private static final KeySpec RIGHT_KEY = wide(RIGHT, "›");
    private static final KeySpec SPACE_KEY = new KeySpec(1, false, action(SPACE, "␣"), null, null, null, null);

    private static final KeySpec[][] LATIN = {
        {key(layer(NUMBERS, "123"), action(NEXT_KEYBOARD, "◎"), layer(EMOJI, "☺"), null, layer(EDIT, "edit")),
         key(layer(SYMBOLS, "#+="), text("@"), action(NEXT_KEYBOARD, "◎"), text("*"), text("_")),
         letters("e", "o", "x", "i", "y"), letters("r", "f", "z", "d", "k"),
         letters(".", ":", "?", "-", "’"), DELETE},
        {LEFT_KEY, letters("a", "/", "m", "j", "u"), SPACE_KEY, letters("s", "p", "c", "—", "l"), RIGHT_KEY},
        {simple(SETTINGS, "⚙"), SHIFT_KEY, letters("n", "q", "v", "g", null),
         letters("t", "h", "w", "b", null), letters(",", "“", "!", "”", ";"), ENTER}
    };

    private static final KeySpec[][] NUMBER = {
        {ABC, key(layer(SYMBOLS, "#+="), null, layer(EMOJI, "☺"), null, action(NEXT_KEYBOARD, "◎")),
         letters("7", "^", "+", "±", "c"), letters("8", "−", "-", "_", "b"), letters("9", "∞", "%", "‰", "a"), DELETE},
        {LEFT_KEY, letters("4", "#", "*", "×", "f"), letters("5", "√", "/", "÷", "e"), letters("6", "≈", "=", "≠", "d"), RIGHT_KEY},
        {SPACE_KEY, letters("1", "(", ",", ")", "¥"), letters("2", "<", ".", ">", "$"), letters("3", "≤", ":", "≥", "°"), letters("0", "00", "′", "″", "‴"), ENTER}
    };

    private static final KeySpec[][] SYMBOL = {
        {ABC, key(layer(NUMBERS, "123"), null, layer(EMOJI, "☺"), null, layer(EDIT, "edit")),
         letters("@", "{", "€", "}", "¥"), letters("#", "«", "‹", "»", "›"), letters("–", "/", "|", "\\", "_"), DELETE},
        {LEFT_KEY, letters("•", "(", "·", ")", "・"), SPACE_KEY, letters("$", "[", "♯", "]", "♭"), RIGHT_KEY},
        {letters("~", "`", "^", "´", "†"), letters("&", "“", "♡", "”", "¶"), letters("*", "<", "^", ">", "™"), letters("'", "\"", "?", "!", "="), letters(";", ":", "…", "%", "+"), ENTER}
    };

    private static final KeySpec[][] EDITING = {
        {ABC, simple(SELECT_ALL, "All"), simple(HOME, "Home"), simple(UP, "↑"), simple(END, "End"), DELETE},
        {LEFT_KEY, simple(CUT, "Cut"), simple(COPY, "Copy"), simple(PASTE, "Paste"), RIGHT_KEY},
        {simple(SETTINGS, "⚙"), simple(UNDO, "Undo"), simple(DOWN, "↓"), simple(REDO, "Redo"), SPACE_KEY, ENTER}
    };

    private static final KeySpec[][] EMOJIS = {
        {ABC, letters("😊", "🙂", "😄", "😂", "🥲"), letters("❤️", "💜", "💛", "💙", "🩷"), letters("👍", "👏", "🙌", "🤝", "👌"), letters("✨", "⭐", "🌙", "☀️", "🌈"), DELETE},
        {LEFT_KEY, letters("👋", "🙏", "💪", "🤞", "✌️"), SPACE_KEY, letters("🎉", "🎂", "🎁", "🎈", "🥳"), RIGHT_KEY},
        {letters("🔥", "💯", "⚡", "💡", "✅"), letters("👀", "🤔", "😎", "🤗", "🫶"), letters("🌿", "🌸", "🌱", "🍀", "🌻"), letters("☕", "🍵", "🍕", "🍰", "🍓"), letters("🐈", "🐕", "🐝", "🦋", "🐧"), ENTER}
    };

    public static KeySpec[][] rows(KeyboardState.Layer layer) {
        switch (layer) {
            case NUMBERS: return NUMBER;
            case SYMBOLS: return SYMBOL;
            case EDIT: return EDITING;
            case EMOJI: return EMOJIS;
            case CODE: return CODE_KEYS;
            default: return LATIN;
        }
    }
    private static final KeySpec[][] CODE_KEYS = {
        {ABC, letters("{", "(", "[", ")", "]"), letters("}", "<", ">", "=", "!"), letters("_", "/", "\\", "|", "&"), letters("\"", "'", "`", ":", ";"), DELETE},
        {LEFT_KEY, letters("=", "+", "-", "*", "/"), SPACE_KEY, letters(";", ":", ",", ".", "?"), RIGHT_KEY},
        {simple(INDENT, "4 sp"), simple(TAB, "Tab"), letters("#", "$", "@", "%", "^"), simple(HOME, "Home"), simple(END, "End"), ENTER}
    };
    private static KeySpec letters(String center, String left, String up, String right, String down) {
        return new KeySpec(1, true, t(center), t(left), t(up), t(right), t(down));
    }
    private static Value t(String s) { return s == null ? null : text(s); }
    private static KeySpec simple(Kind kind, String label) { return key(action(kind, label), null, null, null, null); }
    private static KeySpec wide(Kind kind, String label) { return new KeySpec(1.5f, false, action(kind, label), null, null, null, null); }
    private static KeySpec key(Value center, Value left, Value up, Value right, Value down) { return new KeySpec(1, false, center, left, up, right, down); }
}
