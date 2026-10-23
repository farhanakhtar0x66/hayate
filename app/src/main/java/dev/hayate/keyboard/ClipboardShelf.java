package dev.hayate.keyboard;

import android.content.*;
import android.os.SystemClock;
import dev.hayate.keyboard.core.ClipHistory;
import java.util.*;

final class ClipboardShelf {
    private final ClipboardManager manager;
    private final Preferences preferences;
    private static final ClipHistory history = new ClipHistory();
    private static final android.os.Handler TIMER = new android.os.Handler(android.os.Looper.getMainLooper());
    private static final Set<ClipboardShelf> ACTIVE = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Runnable EXPIRE = ClipboardShelf::expire;
    private final Runnable changed;
    private final ClipboardManager.OnPrimaryClipChangedListener listener;
    private boolean listening;
    private boolean open;
    private String current;
    private boolean sensitive;
    ClipboardShelf(Context context, Runnable changed) {
        manager = context.getSystemService(ClipboardManager.class); preferences = new Preferences(context); this.changed = changed;
        listener = () -> { if (preferences.recentClips() || open) capture(); };
    }
    void start() {
        ACTIVE.add(this);
        if (!listening) { manager.addPrimaryClipChangedListener(listener); listening = true; }
        if (preferences.recentClips()) capture();
    }
    void stop() {
        ACTIVE.remove(this);
        if (listening) manager.removePrimaryClipChangedListener(listener);
        listening = false; current = null; sensitive = false; open = false;
    }
    void open(boolean opened) { open = opened; if (opened) capture(); else current = null; }
    void capture() {
        current = null; sensitive = false;
        if (!preferences.recentClips()) history.clear();
        try {
            ClipDescription description = manager.getPrimaryClipDescription();
            sensitive = description != null && description.getExtras() != null && description.getExtras().getBoolean("android.content.extra.IS_SENSITIVE", false);
            if (!sensitive) {
                ClipData clip = manager.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0 && clip.getItemAt(0).getText() != null) {
                    String text = clip.getItemAt(0).getText().toString();
                    if (!text.isEmpty() && text.length() <= 10_000) current = text;
                }
            }
            if (preferences.recentClips() && current != null) history.add(current, SystemClock.elapsedRealtime());
        } catch (SecurityException ignored) { /* Some editors/devices deny clipboard access. */ }
        scheduleExpiry();
        changed.run();
    }
    boolean sensitive() { return sensitive; }
    List<String> clips() {
        if (!preferences.recentClips()) history.clear();
        ArrayList<String> result = new ArrayList<>();
        if (current != null) result.add(current);
        for (String clip : history.items(SystemClock.elapsedRealtime())) if (!result.contains(clip)) result.add(clip);
        return result;
    }
    private void clearSystem() {
        if (android.os.Build.VERSION.SDK_INT >= 28) manager.clearPrimaryClip();
        else manager.setPrimaryClip(ClipData.newPlainText("", ""));
    }
    private static void scheduleExpiry() {
        TIMER.removeCallbacks(EXPIRE);
        long delay = history.timeUntilExpiry(SystemClock.elapsedRealtime());
        if (delay != Long.MAX_VALUE) TIMER.postDelayed(EXPIRE, delay);
    }
    private static void expire() {
        history.items(SystemClock.elapsedRealtime()); scheduleExpiry();
        for (ClipboardShelf shelf : new ArrayList<>(ACTIVE)) if (shelf.open) shelf.changed.run();
    }
    void remove(String text) { history.remove(text); if (text.equals(current)) { clearSystem(); current = null; } scheduleExpiry(); changed.run(); }
    void clear() { history.clear(); current = null; sensitive = false; clearSystem(); scheduleExpiry(); changed.run(); }
}
