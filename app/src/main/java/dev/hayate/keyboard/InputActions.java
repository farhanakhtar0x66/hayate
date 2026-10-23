package dev.hayate.keyboard;

import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.icu.text.BreakIterator;
import java.util.Locale;
import dev.hayate.keyboard.core.KeySpec;
import dev.hayate.keyboard.core.Unicode;

final class InputActions {
    private InputActions() { }

    static void execute(InputConnection connection, EditorInfo editor, KeySpec.Value value, String text) {
        if (connection == null) return;
        switch (value.kind) {
            case TEXT: connection.commitText(text, 1); break;
            case SPACE: connection.commitText(" ", 1); break;
            case INDENT: connection.commitText("    ", 1); break;
            case TAB: connection.commitText("\t", 1); break;
            case PAIR: CodeActions.pair(connection, value.text); break;
            case BACKSPACE:
                CharSequence selected = connection.getSelectedText(0);
                if (selected != null && selected.length() > 0) connection.commitText("", 1);
                else {
                    CharSequence before = connection.getTextBeforeCursor(256, 0);
                    if (before != null && before.length() > 0)
                        connection.deleteSurroundingText(Unicode.previousGraphemeLength(before.toString()), 0);
                    else send(connection, KeyEvent.KEYCODE_DEL);
                }
                break;
            case RETURN:
                int action = editor == null ? EditorInfo.IME_ACTION_NONE : editor.imeOptions & EditorInfo.IME_MASK_ACTION;
                if (editor != null && (editor.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0 &&
                    action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    if (!connection.performEditorAction(action)) send(connection, KeyEvent.KEYCODE_ENTER);
                } else connection.commitText("\n", 1);
                break;
            case LEFT: moveHorizontal(connection, -1); break;
            case RIGHT: moveHorizontal(connection, 1); break;
            case UP: send(connection, KeyEvent.KEYCODE_DPAD_UP); break;
            case DOWN: send(connection, KeyEvent.KEYCODE_DPAD_DOWN); break;
            case HOME: send(connection, KeyEvent.KEYCODE_MOVE_HOME); break;
            case END: send(connection, KeyEvent.KEYCODE_MOVE_END); break;
            case SELECT_ALL: connection.performContextMenuAction(android.R.id.selectAll); break;
            case CUT: connection.performContextMenuAction(android.R.id.cut); break;
            case COPY: connection.performContextMenuAction(android.R.id.copy); break;
            case PASTE: connection.performContextMenuAction(android.R.id.paste); break;
            case UNDO: connection.performContextMenuAction(android.R.id.undo); break;
            case REDO: connection.performContextMenuAction(android.R.id.redo); break;
            default: break;
        }
    }

    private static void moveHorizontal(InputConnection connection, int delta) {
        ExtractedText text = connection.getExtractedText(new ExtractedTextRequest(), 0);
        if (text == null || text.text == null || text.selectionStart < 0 || text.selectionEnd < 0) {
            send(connection, delta < 0 ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT);
            return;
        }
        int start = Math.min(text.selectionStart, text.selectionEnd);
        int end = Math.max(text.selectionStart, text.selectionEnd);
        int position = delta < 0 ? start : end;
        if (start == end) {
            BreakIterator characters = BreakIterator.getCharacterInstance(Locale.ROOT);
            characters.setText(text.text.toString());
            int next = delta < 0 ? characters.preceding(position) : characters.following(position);
            if (next != BreakIterator.DONE) position = next;
        }
        connection.setSelection(position + text.startOffset, position + text.startOffset);
    }

    private static void send(InputConnection connection, int code) {
        connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }
}
