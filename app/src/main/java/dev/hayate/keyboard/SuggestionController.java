package dev.hayate.keyboard;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.inputmethod.*;
import dev.hayate.keyboard.core.SuggestionEngine;
import dev.hayate.keyboard.core.WordContext;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;

final class SuggestionController {
    static final class Snapshot {
        final WordContext context;
        final int cursor;
        Snapshot(WordContext context, int cursor) { this.context = context; this.cursor = cursor; }
    }
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static SuggestionEngine engine; // Dictionary only; never stores user input.
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile int generation;
    private Runnable pending;
    SuggestionController(Context context) { this.context = context.getApplicationContext(); }

    void clear() { generation++; if (pending != null) main.removeCallbacks(pending); pending = null; }
    void request(InputConnection connection, BiConsumer<Snapshot, List<String>> result) {
        clear(); Snapshot snapshot = capture(connection);
        if (snapshot == null) { result.accept(null, Collections.emptyList()); return; }
        int request = generation;
        pending = () -> WORKER.execute(() -> {
            if (request != generation) return;
            List<String> words;
            try {
                if (engine == null) engine = new SuggestionEngine(new InputStreamReader(context.getAssets().open("english.txt"), StandardCharsets.UTF_8));
                words = engine.suggest(snapshot.context.word);
            } catch (java.io.IOException | RuntimeException ignored) { words = Collections.emptyList(); }
            List<String> choices = words;
            main.post(() -> { if (request == generation) result.accept(snapshot, choices); });
        });
        main.postDelayed(pending, 45);
    }
    static Snapshot capture(InputConnection connection) {
        if (connection == null) return null;
        CharSequence selection = connection.getSelectedText(0);
        if (selection != null && selection.length() != 0) return null;
        CharSequence before = connection.getTextBeforeCursor(64, 0), after = connection.getTextAfterCursor(64, 0);
        if (before == null || after == null) return null;
        WordContext word = WordContext.from(before.toString(), after.toString());
        if (word == null) return null;
        ExtractedText extracted = connection.getExtractedText(new ExtractedTextRequest(), 0);
        if (extracted != null && extracted.selectionStart != extracted.selectionEnd) return null;
        return new Snapshot(word, extracted == null ? -1 : extracted.startOffset + extracted.selectionStart);
    }
    static boolean apply(InputConnection connection, Snapshot snapshot, String choice) {
        if (snapshot == null) return false;
        Snapshot current = capture(connection);
        if (current == null || current.cursor != snapshot.cursor || !current.context.before.equals(snapshot.context.before) ||
            !current.context.after.equals(snapshot.context.after)) return false;
        WordContext word = current.context;
        connection.beginBatchEdit();
        try {
            if (!connection.deleteSurroundingText(word.beforeLength, word.afterLength)) return false;
            return connection.commitText(choice + (word.after.length() == word.afterLength ? " " : ""), 1);
        } finally { connection.endBatchEdit(); }
    }
}
