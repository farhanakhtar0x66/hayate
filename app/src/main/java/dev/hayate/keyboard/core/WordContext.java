package dev.hayate.keyboard.core;

/** Captures a whole English token around a collapsed cursor, bounded by the caller. */
public final class WordContext {
    public final String before, after, word;
    public final int beforeLength, afterLength;
    private WordContext(String before, String after, int start, int end) {
        this.before = before; this.after = after;
        beforeLength = before.length() - start; afterLength = end;
        word = before.substring(start) + after.substring(0, end);
    }
    public static WordContext from(String before, String after) {
        int start = before.length(), end = 0;
        while (start > 0 && wordChar(before.charAt(start - 1))) start--;
        if (start == before.length()) return null;
        while (end < after.length() && wordChar(after.charAt(end))) end++;
        if (start == 0 && before.length() >= 64 || end == after.length() && after.length() >= 64) return null;
        if (start > 0 && special(before.charAt(start - 1)) || end < after.length() && special(after.charAt(end))) return null;
        WordContext context = new WordContext(before, after, start, end);
        return context.word.length() <= 40 ? context : null;
    }
    private static boolean wordChar(char c) { return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '\'' || c == '’'; }
    private static boolean special(char c) { return Character.isLetterOrDigit(c) || c == '_' || c == '@' || c == '/' || c == ':'; }
}
