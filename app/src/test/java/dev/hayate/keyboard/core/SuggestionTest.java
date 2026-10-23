package dev.hayate.keyboard.core;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.StringReader;

public class SuggestionTest {
    private SuggestionEngine engine() throws Exception {
        return new SuggestionEngine(new StringReader("hello 1000\nhelp 900\nheld 800\nworld 700\nthe 600\n"));
    }
    @Test public void correctsTypoWithReusedEngine() throws Exception {
        assertEquals("helo", engine().suggest("helo").get(0));
        assertTrue(engine().suggest("helo").contains("hello"));
        assertTrue(engine().suggest("teh").contains("the"));
    }
    @Test public void completesPrefixAndPreservesCase() throws Exception {
        assertEquals(java.util.Arrays.asList("Hel", "Hello", "Help"), engine().suggest("Hel"));
        assertTrue(engine().suggest("HELO").contains("HELLO"));
        assertTrue(engine().suggest("").isEmpty());
        assertTrue(engine().suggest("some_identifier").isEmpty());
    }
    @Test public void contextCoversWholeWordWithoutEatingPunctuation() {
        WordContext word = WordContext.from("say hel", "lo!");
        assertEquals("hello", word.word);
        assertEquals(3, word.beforeLength);
        assertEquals(2, word.afterLength);
        assertEquals("hello", WordContext.from("hello", "").word);
        assertNull(WordContext.from("hello ", "world"));
        assertNull(WordContext.from("user@exam", "ple.com"));
    }
    @Test public void historyDeduplicatesExpiresAndStaysBounded() {
        ClipHistory history = new ClipHistory();
        history.add("one", 0); history.add("two", 1); history.add("one", 2);
        assertEquals(java.util.Arrays.asList("one", "two"), history.items(3));
        for (int i = 0; i < 15; i++) history.add("clip" + i, 4 + i);
        assertEquals(10, history.items(20).size());
        assertTrue(history.items(3_600_020).isEmpty());
        history.add("new", 4_000_000); history.clear();
        assertTrue(history.items(4_000_001).isEmpty());
    }
}
