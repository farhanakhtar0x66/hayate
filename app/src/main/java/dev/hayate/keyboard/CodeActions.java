package dev.hayate.keyboard;

import android.view.inputmethod.*;

final class CodeActions {
    static String straight(String text) { return text.replace('’', '\'').replace('“', '"').replace('”', '"').replace("—", "-").replace("–", "-"); }
    static void pair(InputConnection connection, String pair) {
        if (pair.length() != 2) return;
        ExtractedText extracted = connection.getExtractedText(new ExtractedTextRequest(), 0);
        if (extracted == null || extracted.selectionStart < 0 || extracted.selectionEnd < 0) return;
        CharSequence selected = connection.getSelectedText(0);
        int selectionStart = Math.min(extracted.selectionStart, extracted.selectionEnd);
        int selectionEnd = Math.max(extracted.selectionStart, extracted.selectionEnd);
        if (selected == null && selectionEnd > selectionStart) {
            if (extracted.text == null || selectionEnd > extracted.text.length()) return;
            selected = extracted.text.subSequence(selectionStart, selectionEnd);
        }
        String middle = selected == null ? "" : selected.toString();
        int start = extracted.startOffset + selectionStart;
        connection.beginBatchEdit();
        try {
            connection.commitText(pair.charAt(0) + middle + pair.charAt(1), 1);
            int cursor = start + 1 + middle.length(); connection.setSelection(cursor, cursor);
        } finally { connection.endBatchEdit(); }
    }
    static void newline(InputConnection connection) {
        if (connection == null) return;
        CharSequence before = connection.getTextBeforeCursor(2000, 0);
        String line = before == null ? "" : before.toString();
        line = line.substring(line.lastIndexOf('\n') + 1);
        int count = 0;
        while (count < line.length() && (line.charAt(count) == ' ' || line.charAt(count) == '\t')) count++;
        String indent = line.substring(0, count);
        String stripped = line.trim();
        if (stripped.endsWith("{") || stripped.endsWith(":") || stripped.endsWith("[") || stripped.endsWith("(")) indent += "    ";
        connection.commitText("\n" + indent, 1);
    }
}
