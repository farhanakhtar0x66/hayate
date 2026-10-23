package dev.hayate.keyboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import dev.hayate.keyboard.core.KeySpec;
import dev.hayate.keyboard.core.KeyboardState;

public final class MainActivity extends Activity {
    private Preferences preferences;
    private Palette palette;
    private EditText practice;
    private String practiceText = "";
    private KeyboardView keyboard;
    private boolean renderedEnabled, renderedSelected;
    private final KeyboardState state = new KeyboardState();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = new Preferences(this);
    }
    @Override protected void onResume() { super.onResume(); render(); }
    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // The system keyboard picker is a dialog; closing it need not call onResume.
        if (hasFocus && practice != null && (isEnabled() != renderedEnabled || isSelected() != renderedSelected)) render();
    }

    static GradientDrawable rounded(int color, float radius) {
        GradientDrawable shape = new GradientDrawable(); shape.setColor(color); shape.setCornerRadius(radius); return shape;
    }
    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private TextView text(String label, int size, int color) {
        TextView view = new TextView(this); view.setText(label); view.setTextSize(size); view.setTextColor(color);
        view.setFontFeatureSettings("kern"); return view;
    }
    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL); return view;
    }
    private void gap(LinearLayout parent, int size) { parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(size))); }
    private AlertDialog.Builder dialog() {
        return new AlertDialog.Builder(new ContextThemeWrapper(this, palette.dark ?
            android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert));
    }
    private Button button(String label, Runnable action, boolean accent) {
        Button button = new Button(this); button.setText(label); button.setTextSize(14); button.setAllCaps(false);
        button.setTextColor(accent ? palette.accentInk : palette.ink);
        button.setBackground(rounded(accent ? palette.accent : palette.control, dp(18)));
        button.setMinHeight(dp(48)); button.setMinimumHeight(dp(48));
        button.setPadding(dp(18), dp(4), dp(18), dp(4)); button.setOnClickListener(v -> action.run());
        return button;
    }

    private void render() {
        if (practice != null) practiceText = practice.getText().toString();
        palette = new Palette(this, preferences);
        LinearLayout root = column(); root.setBackgroundColor(palette.background);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
            });
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) controller.setSystemBarsAppearance(palette.dark ? 0 :
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        } else {
            getWindow().setStatusBarColor(palette.background); getWindow().setNavigationBarColor(palette.background);
            getWindow().getDecorView().setSystemUiVisibility(palette.dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = column(); content.setPadding(dp(22), dp(16), dp(22), dp(18));
        scroll.addView(content); root.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        setContentView(root);

        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("hayate", 35, palette.ink); brand.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        brand.setLetterSpacing(-.06f);
        header.addView(brand, new LinearLayout.LayoutParams(0, dp(52), 1));
        Button settings = button("Settings", this::showSettings, false);
        header.addView(settings, new LinearLayout.LayoutParams(-2, dp(48)));
        content.addView(header);
        gap(content, 18);
        TextView headline = text("Less reach.\nMore flow.", 43, palette.ink);
        headline.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL)); headline.setLineSpacing(dp(0), 1.02f);
        content.addView(headline);
        gap(content, 8);
        content.addView(text("Tap a letter. Flick for the rest.", 16, palette.muted));
        gap(content, 22);

        boolean enabled = isEnabled(), selected = isSelected();
        renderedEnabled = enabled; renderedSelected = selected;
        LinearLayout setup = column(); setup.setPadding(dp(16), dp(4), dp(16), dp(4));
        GradientDrawable panel = rounded(palette.surface, dp(22)); panel.setStroke(dp(1), palette.line); setup.setBackground(panel);
        setupRow(setup, enabled ? "✓" : "1", "Enable Hayate", enabled ? "Added to your keyboards" : "Add to your Android keyboards",
            enabled ? "Enabled" : "Enable", () -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)), !enabled);
        View rule = new View(this); rule.setBackgroundColor(palette.line); setup.addView(rule, new LinearLayout.LayoutParams(-1, dp(1)));
        setupRow(setup, selected ? "✓" : "2", "Make it yours", selected ? "Your current keyboard" : "Choose Hayate to start typing",
            selected ? "Active" : "Choose", () -> {
                if (!isEnabled()) { startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)); return; }
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker();
            }, enabled && !selected);
        content.addView(setup);
        gap(content, 24);
        LinearLayout practiceHeader = new LinearLayout(this); practiceHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView practiceTitle = text("A little practice", 21, palette.ink);
        practiceTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        practiceHeader.addView(practiceTitle, new LinearLayout.LayoutParams(0, dp(44), 1));
        TextView guide = text("How to flick", 14, palette.muted); guide.setGravity(Gravity.CENTER); guide.setPadding(dp(10), 0, 0, 0);
        guide.setOnClickListener(v -> showGuide()); guide.setFocusable(true); guide.setContentDescription("Open flick typing guide");
        practiceHeader.addView(guide, new LinearLayout.LayoutParams(-2, dp(48))); content.addView(practiceHeader);

        practice = new EditText(this); practice.setId(R.id.practice); practice.setTextSize(28); practice.setTextColor(palette.ink);
        practice.setHintTextColor(palette.muted); practice.setHint("Say hello…");
        practice.setPadding(dp(16), dp(12), dp(16), dp(12)); practice.setGravity(Gravity.TOP | Gravity.START);
        practice.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        practice.setShowSoftInputOnFocus(false); practice.setBackground(rounded(palette.surface, dp(16)));
        practice.setMinLines(1); practice.setMaxLines(3); practice.setText(practiceText); practice.setSelection(practice.length());
        practice.setSaveEnabled(false); practice.setContentDescription("Practice typing. Text is not saved.");
        content.addView(practice, new LinearLayout.LayoutParams(-1, -2));
        TextView tip = text(practiceTip(), 12, palette.muted); tip.setGravity(Gravity.CENTER_VERTICAL);
        tip.setPadding(dp(2), dp(4), dp(2), dp(4)); content.addView(tip, new LinearLayout.LayoutParams(-1, dp(38)));
        practice.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { practiceText = s.toString(); tip.setText(practiceTip()); }
            public void afterTextChanged(Editable s) { }
        });
        keyboard = new KeyboardView(this, state, this::practiceAction);
        content.addView(keyboard, new LinearLayout.LayoutParams(-1, -2));
        gap(content, 12);
        TextView privacy = text("Your words stay on your device.", 12, palette.muted); privacy.setGravity(Gravity.CENTER);
        content.addView(privacy);
        practice.requestFocus();
    }

    private void setupRow(LinearLayout parent, String number, String title, String subtitle, String action, Runnable callback, boolean accent) {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0, dp(12), 0, dp(12));
        TextView step = text(number, 15, palette.accentInk); step.setGravity(Gravity.CENTER);
        step.setBackground(rounded(palette.accent, dp(14))); row.addView(step, new LinearLayout.LayoutParams(dp(28), dp(28)));
        LinearLayout labels = column(); labels.setPadding(dp(12), 0, dp(6), 0);
        TextView heading = text(title, 15, palette.ink); heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        labels.addView(heading); TextView detail = text(subtitle, 11, palette.muted); labels.addView(detail);
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(button(action, callback, accent), new LinearLayout.LayoutParams(-2, dp(48))); parent.addView(row);
    }

    private boolean isEnabled() {
        InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        for (InputMethodInfo method : manager.getEnabledInputMethodList()) if (method.getPackageName().equals(getPackageName())) return true;
        return false;
    }
    private boolean isSelected() {
        String selected = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        return selected != null && selected.startsWith(getPackageName() + "/");
    }

    private String practiceTip() {
        switch (practiceText) {
            case "": return "Start here: flick left on t to type h.";
            case "h": return "Now tap the center of e.";
            case "he": case "hel": return "Flick down on s to type l.";
            case "hell": return "Flick left on e to type o.";
            default: return practiceText.startsWith("hello") ? "You’ve got it. Try a few words of your own." : "Tap the center · flick for a neighbor · drag space to move";
        }
    }

    private void practiceAction(KeySpec.Value value) {
        if (value.kind == KeySpec.Kind.LAYER) { state.switchLayer(KeyboardState.Layer.valueOf(value.text)); keyboard.rebuild(); return; }
        if (value.kind == KeySpec.Kind.SHIFT) { state.toggleShift(); keyboard.refresh(); return; }
        if (value.kind == KeySpec.Kind.SETTINGS) { showSettings(); return; }
        if (value.kind == KeySpec.Kind.NEXT_KEYBOARD) {
            Toast.makeText(this, "In your keyboard, this opens the Android keyboard picker.", Toast.LENGTH_SHORT).show(); return;
        }
        practice.requestFocus();
        EditorInfo editor = new EditorInfo();
        InputConnection connection = practice.onCreateInputConnection(editor);
        InputActions.execute(connection, editor, value, state.output(value));
        keyboard.refresh();
    }

    private void showGuide() {
        dialog().setTitle("Small moves. All 26 letters.")
            .setMessage("Tap a big letter to type it.\n\nFlick toward a small letter, then lift. For example, flick up on e for x, or left on t for h.\n\nTap shift once for one capital. Tap again for caps lock.\n\nHold delete to erase. Drag the space key to move the cursor.\n\nUse the tabs for numbers, symbols, editing and emoji. The ◎ key switches keyboards.\n\nTry typing hello in the practice area. Follow the hint above the keys.")
            .setPositiveButton("Let’s try", (d, w) -> { }).show();
    }

    private void showSettings() {
        LinearLayout panel = column(); panel.setPadding(dp(24), dp(8), dp(24), dp(12));
        TextView summary = text("Make the keyboard feel like you.", 14, palette.muted); panel.addView(summary); gap(panel, 16);
        Button theme = button("Appearance: " + preferences.theme(), () -> { }, false);
        theme.setOnClickListener(v -> dialog().setTitle("Appearance")
            .setSingleChoiceItems(new String[]{"Follow system", "Light", "Dark"}, preferences.theme().equals("system") ? 0 : preferences.theme().equals("light") ? 1 : 2,
                (dialog, which) -> { preferences.theme(new String[]{"system", "light", "dark"}[which]); theme.setText("Appearance: " + preferences.theme()); dialog.dismiss(); })
            .setNegativeButton("Cancel", null).show());
        panel.addView(theme, new LinearLayout.LayoutParams(-1, dp(48))); gap(panel, 10);
        Button sensitivity = button("Flick distance: " + preferences.threshold() + " dp", () -> { }, false);
        sensitivity.setOnClickListener(v -> dialog().setTitle("Flick distance")
            .setItems(new String[]{"Short · 12 dp", "Balanced · 18 dp", "Deliberate · 24 dp"}, (dialog, which) -> {
                preferences.threshold(new int[]{12, 18, 24}[which]); sensitivity.setText("Flick distance: " + preferences.threshold() + " dp");
            }).show());
        panel.addView(sensitivity, new LinearLayout.LayoutParams(-1, dp(48))); gap(panel, 10);
        Button height = button("Key height: " + preferences.height() + " dp", () -> { }, false);
        height.setOnClickListener(v -> dialog().setTitle("Key height")
            .setItems(new String[]{"Compact · 70 dp", "Balanced · 78 dp", "Roomy · 88 dp"}, (dialog, which) -> {
                preferences.height(new int[]{70, 78, 88}[which]); height.setText("Key height: " + preferences.height() + " dp");
            }).show());
        panel.addView(height, new LinearLayout.LayoutParams(-1, dp(48))); gap(panel, 10);
        Switch haptics = new Switch(this); haptics.setText("Gentle haptics"); haptics.setTextSize(16); haptics.setTextColor(palette.ink);
        haptics.setChecked(preferences.haptics()); haptics.setOnCheckedChangeListener((view, checked) -> preferences.haptics(checked));
        panel.addView(haptics, new LinearLayout.LayoutParams(-1, dp(56)));
        gap(panel, 8); panel.addView(text("Hayate 0.1.0\nNo network access. No typing history.\nEnglish directional flick keyboard.", 12, palette.muted));
        AlertDialog settings = dialog().setTitle("Your Hayate").setView(panel).setPositiveButton("Done", null).create();
        settings.setOnDismissListener(ignored -> render()); settings.show();
    }
}
