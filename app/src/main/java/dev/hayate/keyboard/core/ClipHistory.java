package dev.hayate.keyboard.core;

import java.util.*;

/** Bounded memory only; callers supply monotonic milliseconds. */
public final class ClipHistory {
    private static final long LIFETIME = 3_600_000;
    private final LinkedHashMap<String, Long> clips = new LinkedHashMap<>();
    public void add(String text, long now) {
        if (text == null || text.isEmpty() || text.length() > 10_000) return;
        items(now); clips.remove(text); clips.put(text, now);
        if (clips.size() > 10) clips.remove(clips.keySet().iterator().next());
    }
    public List<String> items(long now) {
        clips.values().removeIf(time -> now - time >= LIFETIME);
        ArrayList<String> list = new ArrayList<>(clips.keySet()); Collections.reverse(list); return list;
    }
    public void remove(String text) { clips.remove(text); }
    public void clear() { clips.clear(); }
    public long timeUntilExpiry(long now) {
        long delay = Long.MAX_VALUE;
        for (long time : clips.values()) delay = Math.min(delay, Math.max(1, time + LIFETIME - now));
        return delay;
    }
}
