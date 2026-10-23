package dev.hayate.keyboard;

import android.content.Context;
import android.view.inputmethod.*;
import android.widget.Toast;
import dev.hayate.keyboard.core.*;
import java.util.Collections;
import java.util.function.Supplier;

/** Shared editor behavior for the real IME and the launcher practice keyboard. */
final class KeyboardSession {
    final KeyboardView keyboard;
    private final Context context;
    private final KeyboardState state;
    private final Preferences preferences;
    private final Supplier<InputConnection> connection;
    private final Supplier<EditorInfo> editor;
    private final Runnable settings, picker;
    private final SuggestionController suggestions;
    private final ClipboardShelf clipboard;
    private boolean visible, clipboardOpen;

    KeyboardSession(Context context, KeyboardState state, Supplier<InputConnection> connection, Supplier<EditorInfo> editor, Runnable settings, Runnable picker) {
        this.context = context; this.state = state; this.connection = connection; this.editor = editor;
        this.settings = settings; this.picker = picker; preferences = new Preferences(context);
        suggestions = new SuggestionController(context);
        keyboard = new KeyboardView(context, state, this::action);
        clipboard = new ClipboardShelf(context, this::refreshClipboard);
    }
    void start() { visible = true; if (!EditorPolicy.privateField(editor.get())) clipboard.start(); update(); }
    void stop() {
        visible = false; suggestions.clear(); clipboard.stop(); clipboardOpen = false;
        keyboard.closeClipboard(); keyboard.showSuggestions(Collections.emptyList(), ignored -> { });
    }
    private void refreshClipboard() {
        if (visible && clipboardOpen && !EditorPolicy.privateField(editor.get()))
            keyboard.showClipboard(clipboard, text -> {
                InputConnection input = connection.get();
                if (input != null && !EditorPolicy.privateField(editor.get())) input.commitText(text, 1);
                closeClipboard();
            }, () -> { InputActions.execute(connection.get(), editor.get(), KeySpec.action(KeySpec.Kind.PASTE, ""), ""); closeClipboard(); });
    }
    private void closeClipboard() { clipboardOpen = false; clipboard.open(false); keyboard.closeClipboard(); update(); }
    void update() {
        if (EditorPolicy.privateField(editor.get())) { clipboard.stop(); if (clipboardOpen) closeClipboard(); }
        if (!visible || clipboardOpen || state.programmer || !preferences.suggestions() || !EditorPolicy.suggestions(editor.get())) {
            suggestions.clear(); keyboard.showSuggestions(Collections.emptyList(), ignored -> { }); return;
        }
        keyboard.showSuggestions(Collections.emptyList(), ignored -> { });
        suggestions.request(connection.get(), (snapshot, words) -> keyboard.showSuggestions(words, word -> {
            if (visible && !state.programmer && !clipboardOpen && EditorPolicy.suggestions(editor.get()) && preferences.suggestions())
                SuggestionController.apply(connection.get(), snapshot, word);
            update();
        }));
    }
    private void action(KeySpec.Value value) {
        if (value.kind == KeySpec.Kind.CLIPBOARD) {
            if (EditorPolicy.privateField(editor.get())) { Toast.makeText(context, "Clipboard is hidden in password fields", Toast.LENGTH_SHORT).show(); return; }
            if (clipboardOpen) closeClipboard(); else { suggestions.clear(); clipboardOpen = true; clipboard.open(true); refreshClipboard(); }
            return;
        }
        if (clipboardOpen) closeClipboard();
        switch (value.kind) {
            case TOGGLE_CODE:
                state.programmer = !state.programmer; state.shift = KeyboardState.Shift.OFF;
                EditorInfo info = editor.get(); InputConnection input = connection.get();
                if (!state.programmer && info != null && input != null &&
                    (info.inputType & android.text.InputType.TYPE_MASK_CLASS) == android.text.InputType.TYPE_CLASS_TEXT &&
                    (info.inputType & (android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS)) != 0)
                    state.shift = input.getCursorCapsMode(info.inputType) == 0 ? KeyboardState.Shift.OFF : KeyboardState.Shift.ON;
                state.layer = KeyboardState.Layer.LETTERS; keyboard.rebuild(); break;
            case LAYER:
                KeyboardState.Layer layer = KeyboardState.Layer.valueOf(value.text);
                state.switchLayer(state.programmer && layer == KeyboardState.Layer.SYMBOLS ? KeyboardState.Layer.CODE : layer);
                keyboard.rebuild(); break;
            case SHIFT: state.toggleShift(); keyboard.refresh(); break;
            case SETTINGS: settings.run(); return;
            case NEXT_KEYBOARD: picker.run(); return;
            default:
                String output = state.output(value);
                if (state.programmer) output = CodeActions.straight(output);
                if (state.programmer && value.kind == KeySpec.Kind.RETURN) CodeActions.newline(connection.get());
                else InputActions.execute(connection.get(), editor.get(), value, output);
                keyboard.refresh();
        }
        update();
    }
}
