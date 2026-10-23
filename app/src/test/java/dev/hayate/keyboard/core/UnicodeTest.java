package dev.hayate.keyboard.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class UnicodeTest {
    @Test public void emptyInputDeletesNothing() { assertEquals(0, Unicode.previousGraphemeLength("")); }
    @Test public void ordinaryTextDeletesLastLetter() { assertEquals(1, Unicode.previousGraphemeLength("hello")); }
    @Test public void supplementaryEmojiDeletesBothSurrogates() { assertEquals(2, Unicode.previousGraphemeLength("hi😀")); }
    @Test public void combiningMarkDeletesTogetherWithBase() { assertEquals(2, Unicode.previousGraphemeLength("cafe\u0301")); }
    @Test public void skinToneDeletesWithEmoji() { assertEquals(4, Unicode.previousGraphemeLength("a👍🏽")); }
    @Test public void familySequenceDeletesAsOneUnit() { assertEquals(11, Unicode.previousGraphemeLength("hi👨‍👩‍👧‍👦")); }
    @Test public void flagDeletesAsOneUnit() { assertEquals(4, Unicode.previousGraphemeLength("hi🇮🇳")); }
    @Test public void adjacentFlagsDoNotDeleteEachOther() { assertEquals(4, Unicode.previousGraphemeLength("🇺🇸🇮🇳")); }
}
