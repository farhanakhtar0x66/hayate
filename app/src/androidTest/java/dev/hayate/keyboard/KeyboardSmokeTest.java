package dev.hayate.keyboard;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import java.util.concurrent.atomic.AtomicReference;
import dev.hayate.keyboard.core.KeySpec;

/** Framework-only instrumented tests exercise real Views and EditText InputConnections. */
public final class KeyboardSmokeTest extends Instrumentation {
    private Activity activity;
    private EditText editor;
    private KeyboardView keyboard;
    private int current;
    private int failures;
    private static final int COUNT = 21;

    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
        android.accessibilityservice.AccessibilityServiceInfo accessibility = getUiAutomation().getServiceInfo();
        accessibility.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        getUiAutomation().setServiceInfo(accessibility);
        activity = startActivitySync(new Intent(getTargetContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitForIdleSync();
        ui(() -> { editor = activity.findViewById(R.id.practice); keyboard = findKeyboard((ViewGroup) activity.getWindow().getDecorView()); });
        test("directionalFlicksTypeHello", () -> ui(() -> {
            reset(); key(2, 3, -32, 0); key(0, 2, 0, 0); key(1, 3, 0, 32); key(1, 3, 0, 32); key(0, 2, -32, 0); expect("hello");
        }));
        test("shiftAndCapsWorkInRealEditor", () -> ui(() -> {
            reset(); key(2, 1, 0, 0); key(0, 2, 0, 0); key(0, 2, 0, 0); expect("Ee");
            key(2, 1, 0, 0); key(2, 1, 0, 0); key(0, 2, 0, 0); key(0, 2, 0, 0); expect("EeEE");
            key(2, 1, 0, 0);
        }));
        test("numericLayerAndReturnToLetters", () -> ui(() -> {
            reset(); key(0, 0, 0, 0); key(0, 2, 0, 0); key(2, 4, 0, 0); expect("70");
            key(0, 0, 0, 0); key(0, 2, 0, 0); expect("70e");
        }));
        test("deleteHandlesEmojiAndSelection", () -> ui(() -> {
            reset(); editor.setText("hi👨‍👩‍👧‍👦"); editor.setSelection(editor.length()); key(0, 5, 0, 0); expect("hi");
            editor.setSelection(0, 2); key(0, 5, 0, 0); expect("");
        }));
        test("cancelDoesNotType", () -> ui(() -> {
            reset(); float[] p = point(0, 2); event(MotionEvent.ACTION_DOWN, p[0], p[1]); event(MotionEvent.ACTION_CANCEL, p[0] + 50, p[1]); expect("");
        }));
        test("multiplePointersCancelAndNextTapWorks", () -> ui(() -> {
            reset(); float[] p = point(0, 2); event(MotionEvent.ACTION_DOWN, p[0], p[1]);
            MotionEvent.PointerProperties first = new MotionEvent.PointerProperties(); first.id = 0; first.toolType = MotionEvent.TOOL_TYPE_FINGER;
            MotionEvent.PointerProperties second = new MotionEvent.PointerProperties(); second.id = 1; second.toolType = MotionEvent.TOOL_TYPE_FINGER;
            MotionEvent.PointerCoords a = new MotionEvent.PointerCoords(); a.x = p[0]; a.y = p[1]; a.pressure = 1;
            MotionEvent.PointerCoords b = new MotionEvent.PointerCoords(); b.x = p[0] + 100; b.y = p[1]; b.pressure = 1;
            long now = SystemClock.uptimeMillis();
            MotionEvent multi = MotionEvent.obtain(now, now, MotionEvent.ACTION_POINTER_DOWN | (1 << 8), 2,
                new MotionEvent.PointerProperties[]{first, second}, new MotionEvent.PointerCoords[]{a, b}, 0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
            keyboard.dispatchTouchEvent(multi); multi.recycle(); event(MotionEvent.ACTION_UP, p[0], p[1]); expect("");
            key(0, 2, 0, 0); expect("e");
        }));
        test("heldDeleteStopsOnRelease", () -> {
            ui(() -> { reset(); editor.setText("abcdefghij"); editor.setSelection(10); float[] p = point(0, 5); event(MotionEvent.ACTION_DOWN, p[0], p[1]); });
            SystemClock.sleep(530);
            ui(() -> { float[] p = point(0, 5); event(MotionEvent.ACTION_UP, p[0], p[1]); if (editor.length() > 8) throw new AssertionError("Delete did not repeat"); });
            AtomicReference<String> after = new AtomicReference<>(); ui(() -> after.set(editor.getText().toString()));
            SystemClock.sleep(140); ui(() -> expect(after.get()));
        });
        test("spaceDragMovesCursorWithoutAddingSpace", () -> ui(() -> {
            reset(); editor.setText("hello"); editor.setSelection(5); key(1, 2, -40, 0); expect("hello");
            if (editor.getSelectionStart() != 3) throw new AssertionError("Expected cursor at 3, got " + editor.getSelectionStart());
        }));
        test("returnInMultilineInsertsNewline", () -> ui(() -> {
            reset(); editor.setText("hello"); editor.setSelection(5); key(2, 5, 0, 0); expect("hello\n");
        }));
        test("searchReturnPerformsEditorAction", () -> ui(() -> {
            reset(); editor.setSingleLine(true); editor.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
            boolean[] searched = {false}; editor.setOnEditorActionListener((view, action, event) -> { searched[0] = action == EditorInfo.IME_ACTION_SEARCH; return true; });
            EditorInfo info = new EditorInfo();
            InputActions.execute(editor.onCreateInputConnection(info), info, KeySpec.action(KeySpec.Kind.RETURN, ""), "");
            if (!searched[0]) throw new AssertionError("Search was not dispatched");
        }));
        test("cursorDoesNotSplitEmojiBeforeDeletion", () -> ui(() -> {
            reset(); editor.setText("A❤️B"); editor.setSelection(editor.length());
            key(1, 0, 0, 0); key(1, 0, 0, 0);
            if (editor.getSelectionStart() != 1) throw new AssertionError("Cursor split emoji at " + editor.getSelectionStart());
            key(0, 5, 0, 0); expect("❤️B");
        }));
        test("holdingLetterOffersTappableAlternatives", () -> {
            ui(() -> { reset(); float[] p = point(0, 2); event(MotionEvent.ACTION_DOWN, p[0], p[1]); });
            SystemClock.sleep(700);
            ui(() -> { float[] p = point(0, 2); event(MotionEvent.ACTION_UP, p[0], p[1]); expect(""); });
            waitForIdleSync();
            boolean clicked = false;
            long deadline = SystemClock.uptimeMillis() + 3000;
            while (!clicked && SystemClock.uptimeMillis() < deadline) {
                for (android.view.accessibility.AccessibilityWindowInfo window : getUiAutomation().getWindows()) {
                    android.view.accessibility.AccessibilityNodeInfo root = window.getRoot();
                    if (root != null) for (android.view.accessibility.AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByText("x")) {
                        if ("x".contentEquals(node.getText())) { clicked = node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK); break; }
                    }
                    if (clicked) break;
                }
                if (!clicked) SystemClock.sleep(100);
            }
            if (!clicked) throw new AssertionError("No tappable x alternative");
            waitForIdleSync(); ui(() -> expect("x"));
        });
        test("suggestionReplacesWholeWordAndRejectsStaleContext", () -> ui(() -> {
            reset(); editor.setText("say helo!"); editor.setSelection(7);
            android.view.inputmethod.InputConnection connection = editor.onCreateInputConnection(new EditorInfo());
            SuggestionController.Snapshot snapshot = SuggestionController.capture(connection);
            if (!SuggestionController.apply(connection, snapshot, "hello")) throw new AssertionError("Candidate not applied");
            expect("say hello!");
            editor.setSelection(0);
            if (SuggestionController.apply(connection, snapshot, "help")) throw new AssertionError("Stale candidate applied");
            expect("say hello!");
        }));
        test("programmerPairsWrapSelectionAndIndent", () -> ui(() -> {
            reset(); editor.setText("😀name"); editor.setSelection(2, 6);
            EditorInfo info = new EditorInfo(); android.view.inputmethod.InputConnection connection = editor.onCreateInputConnection(info);
            InputActions.execute(connection, info, new KeySpec.Value(KeySpec.Kind.PAIR, "()", "()"), "()");
            expect("😀(name)");
            if (editor.getSelectionStart() != 7) throw new AssertionError("Pair cursor offset wrong");
            editor.setText(""); editor.setSelection(0);
            InputActions.execute(connection, info, KeySpec.action(KeySpec.Kind.INDENT, "4 spaces"), ""); expect("    ");
        }));
        test("passwordAndNoSuggestionsPolicy", () -> ui(() -> {
            EditorInfo info = new EditorInfo(); info.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD;
            if (!EditorPolicy.privateField(info) || EditorPolicy.suggestions(info)) throw new AssertionError("Password policy failed");
            info.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
            if (EditorPolicy.suggestions(info)) throw new AssertionError("NO_SUGGESTIONS ignored");
        }));
        test("clipboardUsesPlatformAndExcludesSensitiveHistory", () -> ui(() -> {
            android.content.ClipboardManager manager = getTargetContext().getSystemService(android.content.ClipboardManager.class);
            Preferences preferences = new Preferences(getTargetContext()); preferences.recentClips(true);
            ClipboardShelf shelf = new ClipboardShelf(getTargetContext(), () -> { }); shelf.start();
            manager.setPrimaryClip(android.content.ClipData.newPlainText("test", "copied snippet"));
            shelf.capture();
            if (!shelf.clips().contains("copied snippet")) throw new AssertionError("Platform clip missing");
            reset(); editor.onCreateInputConnection(new EditorInfo()).commitText(shelf.clips().get(0), 1); expect("copied snippet");
            android.content.ClipData secret = android.content.ClipData.newPlainText("test", "hidden secret");
            android.os.PersistableBundle extras = new android.os.PersistableBundle(); extras.putBoolean("android.content.extra.IS_SENSITIVE", true);
            secret.getDescription().setExtras(extras); manager.setPrimaryClip(secret); shelf.capture();
            if (shelf.clips().contains("hidden secret")) throw new AssertionError("Sensitive clip retained");
            shelf.clear(); shelf.stop(); preferences.recentClips(false);
        }));
        test("programmerToggleKeepsLetterFlicksAndStraightQuotes", () -> ui(() -> {
            reset(); ((ViewGroup) keyboard.getChildAt(0)).getChildAt(5).performClick();
            key(0, 4, 0, 32); key(2, 4, -32, 0); expect("'\"");
            key(2, 3, -32, 0); expect("'\"h");
            ((ViewGroup) keyboard.getChildAt(0)).getChildAt(5).performClick();
        }));
        test("visibleSuggestionCompletesTypedWord", () -> {
            ui(() -> { reset(); key(2, 3, 0, -32); key(1, 1, 0, 32); key(0, 3, 0, 0); key(1, 3, 0, 32); key(0, 3, 32, 0); expect("wurld"); });
            boolean[] clicked = {false}; long deadline = SystemClock.uptimeMillis() + 5000;
            while (!clicked[0] && SystemClock.uptimeMillis() < deadline) {
                ui(() -> {
                    android.widget.TextView hello = findText(keyboard, "world");
                    if (hello != null) { hello.performClick(); clicked[0] = true; }
                });
                if (!clicked[0]) SystemClock.sleep(100);
            }
            if (!clicked[0]) {
                ui(() -> {
                    ViewGroup row = (ViewGroup) keyboard.getChildAt(1); StringBuilder labels = new StringBuilder();
                    for (int i = 0; i < row.getChildCount(); i++) if (row.getChildAt(i) instanceof android.widget.TextView) labels.append(((android.widget.TextView) row.getChildAt(i)).getText()).append("|");
                    throw new AssertionError("Offline world suggestion did not appear; row=" + labels);
                });
            }
            ui(() -> expect("world "));
        });
        test("programmerShortcutRowPairsAndContinuesIndent", () -> ui(() -> {
            reset(); ((ViewGroup) keyboard.getChildAt(0)).getChildAt(5).performClick();
            editor.setText("if (ok) {"); editor.setSelection(editor.length()); key(2, 5, 0, 0); expect("if (ok) {\n    ");
            android.widget.TextView pair = findText(keyboard, "()");
            if (pair == null) throw new AssertionError("Pair shortcut missing"); pair.performClick(); expect("if (ok) {\n    ()");
            if (editor.getSelectionStart() != 15) throw new AssertionError("Cursor must be inside pair");
            ((ViewGroup) keyboard.getChildAt(0)).getChildAt(5).performClick();
        }));
        test("pairPreservesSelectionWhenSelectedTextQueryIsUnsupported", () -> ui(() -> {
            reset(); editor.setText("name"); editor.setSelection(0, 4);
            android.view.inputmethod.InputConnection base = editor.onCreateInputConnection(new EditorInfo());
            android.view.inputmethod.InputConnection partial = new android.view.inputmethod.InputConnectionWrapper(base, false) {
                @Override public CharSequence getSelectedText(int flags) { return null; }
            };
            InputActions.execute(partial, new EditorInfo(), new KeySpec.Value(KeySpec.Kind.PAIR, "()", "()"), "()");
            expect("(name)");
        }));
        test("leavingProgrammerModeRestoresEditorCapitalization", () -> ui(() -> {
            reset(); editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            ((ViewGroup) keyboard.getChildAt(0)).getChildAt(5).performClick(); key(0, 2, 0, 0); expect("e");
            editor.setText(""); editor.setSelection(0);
            ((ViewGroup) keyboard.getChildAt(0)).getChildAt(5).performClick(); key(0, 2, 0, 0); expect("E");
        }));
        Bundle result = new Bundle(); result.putString("stream", "\nHayate: " + current + " device tests, " + failures + " failures.\n");
        finish(Activity.RESULT_OK, result);
    }

    private void test(String name, Runnable run) {
        current++; Bundle status = new Bundle(); status.putString("id", "Hayate"); status.putString("class", getClass().getName());
        status.putString("test", name); status.putInt("numtests", COUNT); status.putInt("current", current); sendStatus(1, status);
        try { run.run(); status.putString("stream", "."); sendStatus(0, status); }
        catch (Throwable error) { failures++; status.putString("stack", android.util.Log.getStackTraceString(error)); status.putString("stream", "\n" + name + ": " + error + "\n"); sendStatus(-2, status); }
    }
    private void ui(Runnable run) {
        AtomicReference<Throwable> error = new AtomicReference<>();
        runOnMainSync(() -> { try { run.run(); } catch (Throwable t) { error.set(t); } });
        if (error.get() != null) throw new AssertionError(error.get());
    }
    private KeyboardView findKeyboard(ViewGroup root) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i); if (child instanceof KeyboardView) return (KeyboardView) child;
            if (child instanceof ViewGroup) { KeyboardView found = findKeyboard((ViewGroup) child); if (found != null) return found; }
        }
        return null;
    }
    private android.widget.TextView findText(ViewGroup root, String text) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof android.widget.TextView && text.contentEquals(((android.widget.TextView) child).getText())) return (android.widget.TextView) child;
            if (child instanceof ViewGroup) { android.widget.TextView found = findText((ViewGroup) child, text); if (found != null) return found; }
        }
        return null;
    }
    private void reset() {
        editor.setSingleLine(false); editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editor.setOnEditorActionListener(null); editor.setText(""); editor.requestFocus();
        ((ViewGroup) keyboard.getChildAt(0)).getChildAt(0).performClick();
    }
    private float[] point(int row, int col) {
        if (keyboard.isLayoutRequested()) {
            keyboard.measure(View.MeasureSpec.makeMeasureSpec(keyboard.getWidth(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            keyboard.layout(keyboard.getLeft(), keyboard.getTop(), keyboard.getRight(), keyboard.getTop() + keyboard.getMeasuredHeight());
        }
        ViewGroup body = (ViewGroup) keyboard.getChildAt(2);
        ViewGroup line = (ViewGroup) body.getChildAt(row); View tile = line.getChildAt(col);
        return new float[]{body.getLeft() + line.getLeft() + tile.getLeft() + tile.getWidth() / 2f, body.getTop() + line.getTop() + tile.getTop() + tile.getHeight() / 2f};
    }
    private void key(int row, int col, int dxDp, int dyDp) {
        float[] p = point(row, col); float density = getTargetContext().getResources().getDisplayMetrics().density;
        event(MotionEvent.ACTION_DOWN, p[0], p[1]); event(MotionEvent.ACTION_MOVE, p[0] + dxDp * density, p[1] + dyDp * density);
        event(MotionEvent.ACTION_UP, p[0] + dxDp * density, p[1] + dyDp * density);
    }
    private void event(int action, float x, float y) {
        long now = SystemClock.uptimeMillis(); MotionEvent event = MotionEvent.obtain(now, now, action, x, y, 0);
        keyboard.dispatchTouchEvent(event); event.recycle();
    }
    private void expect(String expected) {
        if (!expected.equals(editor.getText().toString())) throw new AssertionError("Expected [" + expected + "], got [" + editor.getText() + "]");
    }
}
