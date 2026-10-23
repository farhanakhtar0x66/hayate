package dev.hayate.keyboard.core;

public final class Unicode {
    private Unicode() { }
    /** UTF-16 units for a final character, including common emoji and combining sequences. */
    public static int previousGraphemeLength(String text) {
        int end = text.length();
        if (end == 0) return 0;
        int start = end;
        int cp = text.codePointBefore(start);
        if (regional(cp)) {
            int count = 0, cursor = start;
            while (cursor > 0 && regional(text.codePointBefore(cursor))) {
                cursor -= Character.charCount(text.codePointBefore(cursor)); count++;
            }
            return count % 2 == 0 ? 4 : 2;
        }
        if (cp == '\n' && end > 1 && text.charAt(end - 2) == '\r') return 2;
        do {
            while (start > 0 && extension(text.codePointBefore(start))) start -= Character.charCount(text.codePointBefore(start));
            if (start > 0) start -= Character.charCount(text.codePointBefore(start));
            if (start > 0 && text.codePointBefore(start) == 0x200D) start--;
            else break;
        } while (start > 0);
        return end - start;
    }
    private static boolean regional(int cp) { return cp >= 0x1F1E6 && cp <= 0x1F1FF; }
    private static boolean extension(int cp) {
        int type = Character.getType(cp);
        return type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK ||
            type == Character.ENCLOSING_MARK || cp == 0xFE0F || cp == 0xFE0E ||
            (cp >= 0x1F3FB && cp <= 0x1F3FF) || (cp >= 0xE0020 && cp <= 0xE007F);
    }
}
