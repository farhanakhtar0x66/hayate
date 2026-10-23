package dev.hayate.keyboard;

import android.content.Intent;
import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import dev.hayate.keyboard.core.KeySpec;
import dev.hayate.keyboard.core.KeyboardState;

public final class HayateIme extends InputMethodService {
    private final KeyboardState state = new KeyboardState();
    private KeyboardView keyboard;

    @Override public View onCreateInputView() {
        keyboard = new KeyboardView(this, state, this::onAction);
        if (Build.VERSION.SDK_INT >= 30) {
            Palette palette = new Palette(this, new Preferences(this));
            View decor = getWindow().getWindow().getDecorView();
            // IME windows draw through the gesture bar on recent Android versions.
            // Observe the unconsumed window insets before the framework decor handles them.
            java.util.function.Consumer<WindowInsets> updateInsets = insets -> {
                android.graphics.Insets navigation = insets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars());
                keyboard.setPadding(keyboard.dp(5) + navigation.left, keyboard.dp(2),
                    keyboard.dp(5) + navigation.right, keyboard.dp(8) + navigation.bottom);
                WindowInsetsController controller = decor.getWindowInsetsController();
                if (controller != null) controller.setSystemBarsAppearance(palette.dark ? 0 :
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            };
            decor.setOnApplyWindowInsetsListener((view, insets) -> {
                updateInsets.accept(insets); return view.onApplyWindowInsets(insets);
            });
            WindowInsets current = decor.getRootWindowInsets();
            if (current != null) updateInsets.accept(current);
        }
        return keyboard;
    }

    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        state.shift = KeyboardState.Shift.OFF;
        int type = attribute.inputType & InputType.TYPE_MASK_CLASS;
        state.layer = type == InputType.TYPE_CLASS_NUMBER || type == InputType.TYPE_CLASS_PHONE || type == InputType.TYPE_CLASS_DATETIME
            ? KeyboardState.Layer.NUMBERS : KeyboardState.Layer.LETTERS;
    }

    @Override public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        // Preferences may change while this service stays alive.
        setInputView(onCreateInputView());
        keyboard.setReturnLabel(returnLabel(info));
        syncCaps();
    }

    @Override public boolean onEvaluateFullscreenMode() { return false; }

    private void onAction(KeySpec.Value value) {
        if (value.kind == KeySpec.Kind.LAYER) {
            state.switchLayer(KeyboardState.Layer.valueOf(value.text)); keyboard.rebuild(); return;
        }
        if (value.kind == KeySpec.Kind.SHIFT) { state.toggleShift(); keyboard.refresh(); return; }
        if (value.kind == KeySpec.Kind.SETTINGS) {
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return;
        }
        if (value.kind == KeySpec.Kind.NEXT_KEYBOARD) {
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker(); return;
        }
        InputActions.execute(getCurrentInputConnection(), getCurrentInputEditorInfo(), value, state.output(value));
        keyboard.refresh();
    }

    private void syncCaps() {
        EditorInfo info = getCurrentInputEditorInfo();
        if (info == null || getCurrentInputConnection() == null || state.shift == KeyboardState.Shift.LOCK) return;
        if ((info.inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT &&
            (info.inputType & (InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS)) != 0) {
            state.shift = getCurrentInputConnection().getCursorCapsMode(info.inputType) == 0 ? KeyboardState.Shift.OFF : KeyboardState.Shift.ON;
            if (keyboard != null) keyboard.refresh();
        }
    }

    @Override public void onUpdateSelection(int oldStart, int oldEnd, int newStart, int newEnd, int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldStart, oldEnd, newStart, newEnd, candidatesStart, candidatesEnd);
        syncCaps();
    }

    private String returnLabel(EditorInfo info) {
        if ((info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) return "↵";
        switch (info.imeOptions & EditorInfo.IME_MASK_ACTION) {
            case EditorInfo.IME_ACTION_SEARCH: return "Search";
            case EditorInfo.IME_ACTION_GO: return "Go";
            case EditorInfo.IME_ACTION_SEND: return "Send";
            case EditorInfo.IME_ACTION_NEXT: return "Next";
            case EditorInfo.IME_ACTION_DONE: return "Done";
            default: return "↵";
        }
    }
}
