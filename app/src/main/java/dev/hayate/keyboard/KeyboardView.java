package dev.hayate.keyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.PopupWindow;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import java.util.List;
import java.util.function.Consumer;
import dev.hayate.keyboard.core.Flick;
import dev.hayate.keyboard.core.KeySpec;
import dev.hayate.keyboard.core.KeyboardState;
import dev.hayate.keyboard.core.Layouts;
import java.util.ArrayList;
import java.util.Locale;

final class KeyboardView extends LinearLayout {
    interface Listener { void onAction(KeySpec.Value value); }
    private final KeyboardState state;
    private final Preferences preferences;
    private final Palette palette;
    private final Listener listener;
    private final ArrayList<Tile> tiles = new ArrayList<>();
    private final ArrayList<TextView> tabs = new ArrayList<>();
    private boolean suppressPointers;
    private String returnLabel = "↵";
    private LinearLayout accessory, body;
    private ClipboardShelf shelf;
    private Consumer<String> pasteClip;
    private Runnable pasteSensitive;
    private final Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);
    private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);

    KeyboardView(Context context, KeyboardState state, Listener listener) {
        super(context);
        this.state = state; this.listener = listener;
        preferences = new Preferences(context); palette = new Palette(context, preferences);
        setOrientation(VERTICAL);
        setPadding(dp(5), dp(2), dp(5), dp(8));
        setBackgroundColor(palette.background);
        rebuild();
    }

    int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    void setReturnLabel(String label) { returnLabel = label; refresh(); }

    void rebuild() {
        for (Tile tile : tiles) tile.cancel();
        removeAllViews(); tiles.clear(); tabs.clear();
        LinearLayout toolbar = new LinearLayout(getContext());
        String[] names = {"abc", "123", "#+=", "edit", "☺"};
        KeyboardState.Layer[] layers = KeyboardState.Layer.values();
        for (int i = 0; i < names.length; i++) {
            final KeyboardState.Layer layer = layers[i];
            TextView tab = new TextView(getContext());
            tab.setText(names[i]); tab.setTextSize(13); tab.setGravity(android.view.Gravity.CENTER);
            tab.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            tab.setContentDescription("Switch to " + layer.name().toLowerCase(Locale.ROOT));
            tab.setOnClickListener(v -> listener.onAction(KeySpec.layer(layer, names[layer.ordinal()])));
            toolbar.addView(tab, new LinearLayout.LayoutParams(0, dp(44), 1)); tabs.add(tab);
        }
        addView(toolbar, new LinearLayout.LayoutParams(-1, dp(44)));
        TextView code = tool("</>", () -> listener.onAction(KeySpec.action(KeySpec.Kind.TOGGLE_CODE, "Code")));
        code.setContentDescription("Toggle programmer mode"); toolbar.addView(code, new LinearLayout.LayoutParams(0, dp(44), 1)); tabs.add(code);
        TextView clipboard = tool("Clips", () -> listener.onAction(KeySpec.action(KeySpec.Kind.CLIPBOARD, "Clipboard")));
        clipboard.setContentDescription("Open clipboard"); toolbar.addView(clipboard, new LinearLayout.LayoutParams(0, dp(44), 1)); tabs.add(clipboard);
        accessory = new LinearLayout(getContext());
        addView(accessory, new LinearLayout.LayoutParams(-1, dp(42)));
        body = new LinearLayout(getContext()); body.setOrientation(VERTICAL);
        addView(body, new LinearLayout.LayoutParams(-1, -2));
        if (shelf != null) { renderClipboard(); refresh(); return; }
        for (KeySpec[] row : Layouts.rows(state.layer)) {
            LinearLayout line = new LinearLayout(getContext());
            for (KeySpec key : row) {
                Tile tile = new Tile(getContext(), key);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, key.weight);
                params.setMargins(dp(3), dp(3), dp(3), dp(3));
                line.addView(tile, params); tiles.add(tile);
            }
            int height = preferences.height();
            if (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) height = Math.min(height, 60);
            body.addView(line, new LinearLayout.LayoutParams(-1, dp(height)));
        }
        showSuggestions(java.util.Collections.emptyList(), ignored -> { });
        refresh();
    }

    void refresh() {
        for (int i = 0; i < tabs.size(); i++) {
            TextView tab = tabs.get(i);
            boolean selected = i < 5 && (i == state.layer.ordinal() || i == 2 && state.layer == KeyboardState.Layer.CODE)
                || i == 5 && state.programmer || i == 6 && shelf != null;
            tab.setTextColor(selected ? palette.ink : palette.muted);
            tab.setSelected(selected);
            tab.setBackground(selected ? MainActivity.rounded(palette.control, dp(12)) : null);
        }
        for (Tile tile : tiles) { tile.updateDescription(); tile.invalidate(); }
    }

    private TextView tool(String label, Runnable action) {
        TextView button = new TextView(getContext()); button.setText(label); button.setTextSize(13);
        button.setTextColor(palette.ink); button.setGravity(android.view.Gravity.CENTER);
        button.setTypeface(medium); button.setPadding(dp(4), 0, dp(4), 0);
        button.setSingleLine(true); button.setEllipsize(android.text.TextUtils.TruncateAt.END);
        button.setFocusable(true); button.setOnClickListener(v -> action.run());
        button.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(view, info); info.setClassName("android.widget.Button");
            }
        });
        return button;
    }
    void showSuggestions(List<String> words, Consumer<String> choose) {
        if (shelf != null) return;
        accessory.removeAllViews();
        if (state.programmer) {
            HorizontalScrollView scroll = new HorizontalScrollView(getContext()); scroll.setHorizontalScrollBarEnabled(false);
            LinearLayout tools = new LinearLayout(getContext());
            addTool(tools, "4 sp", KeySpec.action(KeySpec.Kind.INDENT, "4 spaces"));
            addTool(tools, "Tab", KeySpec.action(KeySpec.Kind.TAB, "Tab"));
            for (String pair : new String[]{"()", "{}", "[]", "\"\"", "''", "``"}) addTool(tools, pair, new KeySpec.Value(KeySpec.Kind.PAIR, pair, pair));
            addTool(tools, "Home", KeySpec.action(KeySpec.Kind.HOME, "Home")); addTool(tools, "End", KeySpec.action(KeySpec.Kind.END, "End"));
            scroll.addView(tools); accessory.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        } else if (words.isEmpty()) {
            TextView hint = tool(preferences.suggestions() ? "Offline suggestions · English" : "Flick to type", () -> { });
            hint.setTextColor(palette.muted); hint.setTextSize(11); hint.setClickable(false); hint.setFocusable(false);
            accessory.addView(hint, new LinearLayout.LayoutParams(-1, -1));
        } else for (String word : words) {
            TextView candidate = tool(word, () -> choose.accept(word)); candidate.setContentDescription("Use suggestion " + word);
            accessory.addView(candidate, new LinearLayout.LayoutParams(0, -1, 1));
        }
    }
    private void addTool(LinearLayout row, String label, KeySpec.Value action) {
        TextView button = tool(label, () -> listener.onAction(action));
        button.setContentDescription(action.kind == KeySpec.Kind.PAIR ? "Insert pair " + label : action.label);
        row.addView(button, new LinearLayout.LayoutParams(dp(48), dp(42)));
    }
    void showClipboard(ClipboardShelf shelf, Consumer<String> paste, Runnable sensitivePaste) {
        this.shelf = shelf; pasteClip = paste; pasteSensitive = sensitivePaste; rebuild();
    }
    void closeClipboard() { shelf = null; rebuild(); }
    private void renderClipboard() {
        accessory.removeAllViews();
        accessory.addView(tool("‹ Keys", () -> listener.onAction(KeySpec.action(KeySpec.Kind.CLIPBOARD, "Clipboard"))), new LinearLayout.LayoutParams(0, -1, 1));
        accessory.addView(tool("Clipboard", () -> { }), new LinearLayout.LayoutParams(0, -1, 2));
        accessory.addView(tool("Clear", shelf::clear), new LinearLayout.LayoutParams(0, -1, 1));
        ScrollView scroll = new ScrollView(getContext()); LinearLayout clips = new LinearLayout(getContext()); clips.setOrientation(VERTICAL);
        TextView remember = tool(preferences.recentClips() ? "Recent clips on · tap to turn off" : "Keep recent clips · off", () -> {
            preferences.recentClips(!preferences.recentClips()); shelf.capture();
        });
        remember.setTextSize(12); remember.setTextColor(palette.muted); clips.addView(remember, new LinearLayout.LayoutParams(-1, dp(44)));
        if (shelf.sensitive()) {
            TextView secret = tool("Paste sensitive clip · preview hidden", pasteSensitive);
            clips.addView(secret, new LinearLayout.LayoutParams(-1, dp(52)));
        }
        List<String> contents = shelf.clips();
        if (contents.isEmpty() && !shelf.sensitive()) {
            TextView empty = tool("Copy some text to get started", () -> { }); empty.setTextColor(palette.muted);
            clips.addView(empty, new LinearLayout.LayoutParams(-1, dp(56)));
        }
        for (String clip : contents) {
            LinearLayout row = new LinearLayout(getContext()); row.setPadding(dp(4), dp(3), dp(4), dp(3));
            TextView preview = tool(clip, () -> pasteClip.accept(clip)); preview.setSingleLine(false); preview.setMaxLines(2);
            preview.setGravity(android.view.Gravity.CENTER_VERTICAL); preview.setPadding(dp(12), dp(6), dp(12), dp(6));
            preview.setBackground(MainActivity.rounded(palette.surface, dp(10))); preview.setContentDescription("Paste " + clip);
            row.addView(preview, new LinearLayout.LayoutParams(0, dp(56), 1));
            TextView remove = tool("×", () -> shelf.remove(clip)); remove.setContentDescription("Remove clip");
            row.addView(remove, new LinearLayout.LayoutParams(dp(44), dp(56))); clips.addView(row);
        }
        TextView notice = tool("Recent clips expire after 1 hour.\nThe current clip follows Android’s clipboard.", () -> { });
        notice.setSingleLine(false); notice.setTextSize(11); notice.setTextColor(palette.muted); notice.setClickable(false);
        clips.addView(notice, new LinearLayout.LayoutParams(-1, dp(52))); scroll.addView(clips);
        int height = preferences.height();
        if (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) height = Math.min(height, 60);
        body.addView(scroll, new LinearLayout.LayoutParams(-1, dp(height * 3)));
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) suppressPointers = false;
        if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            suppressPointers = true;
            for (Tile tile : tiles) tile.cancel();
            MotionEvent cancel = MotionEvent.obtain(event); cancel.setAction(MotionEvent.ACTION_CANCEL);
            super.dispatchTouchEvent(cancel); cancel.recycle();
        }
        return suppressPointers || super.dispatchTouchEvent(event);
    }

    private final class Tile extends View {
        private final KeySpec key;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private float downX, downY;
        private boolean pressed, repeating, dragged, longPressed;
        private PopupWindow choicesPopup;
        private int cursorStep;
        private Flick direction = Flick.CENTER;
        private final Runnable repeat = new Runnable() {
            @Override public void run() {
                if (!pressed) return;
                repeating = true;
                listener.onAction(key.values[0]);
                postDelayed(this, 55);
            }
        };
        private final Runnable longPress = () -> {
            if (pressed && direction == Flick.CENTER) {
                longPressed = true;
                showAlternatives();
            }
        };

        Tile(Context context, KeySpec key) {
            super(context); this.key = key;
            setFocusable(true); setClickable(true); setLongClickable(true);
            setSoundEffectsEnabled(false);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            setOnLongClickListener(v -> showAlternatives());
            updateDescription();
        }

        private String display(KeySpec.Value v) {
            if (v == null) return "";
            if (v.kind == KeySpec.Kind.RETURN) return state.programmer ? "↵" : returnLabel;
            if (v.kind == KeySpec.Kind.SHIFT) return state.shift == KeyboardState.Shift.LOCK ? "⇪" : "⇧";
            String label = state.programmer ? CodeActions.straight(v.label) : v.label;
            if (v.kind == KeySpec.Kind.TEXT && state.shift != KeyboardState.Shift.OFF) return label.toUpperCase(Locale.ROOT);
            return label;
        }

        void updateDescription() {
            StringBuilder description = new StringBuilder(accessibleLabel(key.values[0]));
            String[] directions = {"", "left", "up", "right", "down"};
            for (int i = 1; i < key.values.length; i++) if (key.values[i] != null)
                description.append(", flick ").append(directions[i]).append(" for ").append(accessibleLabel(key.values[i]));
            if (key.letter) description.append(". Long press for letter choices");
            setContentDescription(description.toString());
        }

        private String accessibleLabel(KeySpec.Value value) {
            if (value.kind == KeySpec.Kind.TEXT) return display(value);
            if (value.kind == KeySpec.Kind.LAYER) return value.text.toLowerCase(Locale.ROOT);
            return value.kind.name().replace('_', ' ').toLowerCase(Locale.ROOT);
        }

        @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName("android.widget.Button");
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            boolean shift = key.values[0].kind == KeySpec.Kind.SHIFT && state.shift != KeyboardState.Shift.OFF;
            boolean enter = key.values[0].kind == KeySpec.Kind.RETURN;
            paint.setColor(pressed || shift || enter ? palette.accent : key.letter ? palette.surface : palette.control);
            rect.set(0, 0, getWidth(), getHeight());
            canvas.drawRoundRect(rect, dp(12), dp(12), paint);
            int ink = pressed || shift || enter ? palette.accentInk : palette.ink;
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            drawText(canvas, display(key.values[0]), cx, cy, key.letter ? 27 : key.values[0].kind == KeySpec.Kind.LAYER ? 14 : key.values[0].label.length() > 3 ? 12 : 19, ink, true);
            float dx = Math.min(dp(23), getWidth() * .33f), dy = getHeight() * .32f;
            float[] xs = {cx, cx - dx, cx, cx + dx, cx};
            float[] ys = {cy, cy, cy - dy, cy, cy + dy};
            for (int i = 1; i < key.values.length; i++) {
                if (key.values[i] != null)
                    drawText(canvas, display(key.values[i]), xs[i], ys[i], 11,
                        pressed && direction.ordinal() == i ? palette.accentInk : palette.muted,
                        pressed && direction.ordinal() == i);
            }
            if (pressed && direction != Flick.CENTER && key.values[direction.ordinal()] != null) {
                paint.setColor(palette.accentInk);
                canvas.drawCircle(xs[direction.ordinal()], ys[direction.ordinal()] + dp(9), dp(1.5f), paint);
            }
            if (key.values[0].kind == KeySpec.Kind.SPACE) {
                paint.setColor(palette.muted);
                rect.set(cx - dp(12), getHeight() - dp(12), cx + dp(12), getHeight() - dp(10));
                canvas.drawRoundRect(rect, dp(2), dp(2), paint);
            }
        }

        private void drawText(Canvas c, String text, float x, float y, float size, int color, boolean medium) {
            paint.setColor(color);
            float fontScale = Math.min(getResources().getConfiguration().fontScale, 1.3f);
            paint.setTextSize(dp(size) * fontScale);
            paint.setTypeface(medium ? KeyboardView.this.medium : regular);
            paint.setTextAlign(Paint.Align.CENTER);
            if (paint.measureText(text) > getWidth() - dp(5)) paint.setTextSize(paint.getTextSize() * (getWidth() - dp(5)) / paint.measureText(text));
            c.drawText(text, x, y - (paint.ascent() + paint.descent()) / 2, paint);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    getParent().requestDisallowInterceptTouchEvent(true);
                    pressed = true; repeating = false; dragged = false; longPressed = false; cursorStep = 0;
                    direction = Flick.CENTER; downX = event.getX(); downY = event.getY();
                    if (preferences.haptics()) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    if (key.values[0].kind == KeySpec.Kind.BACKSPACE) {
                        listener.onAction(key.values[0]); postDelayed(repeat, 400);
                    } else if (key.letter) postDelayed(longPress, ViewConfiguration.getLongPressTimeout());
                    invalidate(); return true;
                case MotionEvent.ACTION_MOVE:
                    if (!pressed) return true;
                    float dx = event.getX() - downX, dy = event.getY() - downY;
                    if (Math.max(Math.abs(dx), Math.abs(dy)) > dp(8)) removeCallbacks(longPress);
                    if (key.values[0].kind == KeySpec.Kind.SPACE) {
                        int next = (int) (dx / dp(preferences.threshold()));
                        while (cursorStep != next) {
                            listener.onAction(KeySpec.action(next > cursorStep ? KeySpec.Kind.RIGHT : KeySpec.Kind.LEFT, ""));
                            cursorStep += next > cursorStep ? 1 : -1; dragged = true;
                        }
                    } else {
                        Flick next = Flick.resolve(dx, dy, dp(preferences.threshold()));
                        if (direction != next) { direction = next; invalidate(); }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (pressed && !dragged && !longPressed && key.values[0].kind != KeySpec.Kind.BACKSPACE) {
                        // Accessibility invokes performClick directly; touch commits the resolved direction.
                        Flick resolved = Flick.resolve(event.getX() - downX, event.getY() - downY, dp(preferences.threshold()));
                        if (resolved == Flick.CENTER) performClick();
                        else listener.onAction(key.at(resolved));
                    }
                    cancel(); return true;
                case MotionEvent.ACTION_CANCEL: cancel(); return true;
                default: return true;
            }
        }

        @Override public boolean performClick() {
            super.performClick(); listener.onAction(key.values[0]); return true;
        }

        private boolean showAlternatives() {
            ArrayList<KeySpec.Value> values = new ArrayList<>();
            for (KeySpec.Value v : key.values) if (v != null) values.add(v);
            LinearLayout choices = new LinearLayout(getContext()); choices.setOrientation(VERTICAL);
            choices.setPadding(dp(8), dp(8), dp(8), dp(8));
            choices.setBackground(MainActivity.rounded(palette.surface, dp(16)));
            PopupWindow popup = new PopupWindow(choices, dp(220), -2, true);
            choicesPopup = popup;
            popup.setOnDismissListener(() -> choicesPopup = null);
            popup.setBackgroundDrawable(MainActivity.rounded(palette.surface, dp(16)));
            popup.setElevation(dp(12)); popup.setOutsideTouchable(true);
            for (KeySpec.Value value : values) {
                Button choice = new Button(getContext()); choice.setAllCaps(false);
                choice.setText(accessibleLabel(value)); choice.setTextColor(palette.ink);
                choice.setBackground(MainActivity.rounded(palette.control, dp(10)));
                choice.setStateListAnimator(null);
                choice.setOnClickListener(v -> { listener.onAction(value); popup.dismiss(); });
                LinearLayout.LayoutParams item = new LinearLayout.LayoutParams(-1, dp(48));
                item.setMargins(0, dp(3), 0, dp(3)); choices.addView(choice, item);
            }
            popup.showAtLocation(KeyboardView.this, android.view.Gravity.CENTER, 0, 0);
            return true;
        }

        void cancel() { pressed = false; repeating = false; removeCallbacks(repeat); removeCallbacks(longPress); invalidate(); }
        @Override protected void onDetachedFromWindow() {
            cancel(); if (choicesPopup != null) choicesPopup.dismiss(); super.onDetachedFromWindow();
        }
    }
}
