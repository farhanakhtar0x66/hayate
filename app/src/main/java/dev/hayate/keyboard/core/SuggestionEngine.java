package dev.hayate.keyboard.core;

import io.gitlab.rxp90.jsymspell.SymSpell;
import io.gitlab.rxp90.jsymspell.SymSpellBuilder;
import io.gitlab.rxp90.jsymspell.Verbosity;
import io.gitlab.rxp90.jsymspell.api.SuggestItem;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

/** Offline correction uses JSymSpell; this adapter adds prefix completion and case. */
public final class SuggestionEngine {
    private final NavigableMap<String, Long> words = new TreeMap<>();
    private final SymSpell spell;

    public SuggestionEngine(Reader source) throws IOException {
        try (BufferedReader reader = new BufferedReader(source)) {
            for (String line; (line = reader.readLine()) != null;) {
                String[] entry = line.replace("\uFEFF", "").split(" ");
                if (entry.length == 2) words.put(entry[0], Long.parseLong(entry[1]));
            }
        }
        spell = new SymSpellBuilder().setUnigramLexicon(words).setMaxDictionaryEditDistance(1).createSymSpell();
    }

    public List<String> suggest(String input) {
        if (!input.matches("[a-zA-Z]+(?:['’][a-zA-Z]*)?") || input.length() > 40) return Collections.emptyList();
        String lower = input.toLowerCase(Locale.ROOT).replace('’', '\'');
        LinkedHashSet<String> result = new LinkedHashSet<>(); result.add(input);
        if (input.length() >= 2) {
            ArrayList<Map.Entry<String, Long>> completions = new ArrayList<>();
            for (Map.Entry<String, Long> entry : words.tailMap(lower, true).entrySet()) {
                if (!entry.getKey().startsWith(lower)) break;
                if (!entry.getKey().equals(lower)) completions.add(entry);
            }
            completions.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
            for (int i = 0; i < Math.min(2, completions.size()); i++) result.add(matchCase(input, completions.get(i).getKey()));
            try {
                for (SuggestItem item : spell.lookup(lower, Verbosity.ALL)) {
                    if (result.size() >= 3) break;
                    result.add(matchCase(input, item.getSuggestion()));
                }
            } catch (io.gitlab.rxp90.jsymspell.exceptions.NotInitializedException ignored) { /* Empty dictionary: retain literal. */ }
        }
        return new ArrayList<>(result);
    }

    private static String matchCase(String input, String word) {
        if (input.equals(input.toUpperCase(Locale.ROOT))) return word.toUpperCase(Locale.ROOT);
        return Character.isUpperCase(input.charAt(0)) ? Character.toUpperCase(word.charAt(0)) + word.substring(1) : word;
    }
}
