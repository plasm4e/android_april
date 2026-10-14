package org.privacyrom.keyboard

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TypingEngineTest {

    private val log = mutableListOf<String>()
    private lateinit var engine: TypingEngine

    @Before
    fun setUp() {
        engine = TypingEngine(object : TypingEngine.Sink {
            override fun commit(text: String) { log += "commit:$text" }
            override fun deleteBefore() { log += "del" }
            override fun enter() { log += "enter" }
            override fun submit() { log += "submit" }
        })
    }

    @Test
    fun directModeCommitsEveryKey() {
        engine.mode = TypingEngine.Mode.DIRECT
        engine.type("hi")
        assertEquals(listOf("commit:h", "commit:i"), log)
    }

    @Test
    fun wordModeCommitsOnlyWholeWords() {
        engine.mode = TypingEngine.Mode.WORD
        engine.type("pizza")
        assertEquals(emptyList<String>(), log)
        assertEquals("pizza", engine.pendingText)
        engine.type(" ")
        assertEquals(listOf("commit:pizza", "commit: "), log)
    }

    @Test
    fun wordModePunctuationEndsWord() {
        engine.mode = TypingEngine.Mode.WORD
        engine.type("don't-stop,")
        assertEquals(listOf("commit:don't-stop", "commit:,"), log)
    }

    @Test
    fun wordModeEnterFlushesThenEnters() {
        engine.mode = TypingEngine.Mode.WORD
        engine.type("milan")
        engine.enter()
        assertEquals(listOf("commit:milan", "enter"), log)
    }

    @Test
    fun backspaceEditsBufferBeforeTouchingApp() {
        engine.mode = TypingEngine.Mode.WORD
        engine.type("ab")
        engine.backspace()
        engine.backspace()
        assertEquals(emptyList<String>(), log)
        engine.backspace()
        assertEquals(listOf("del"), log)
    }

    @Test
    fun backspaceRemovesWholeEmoji() {
        engine.mode = TypingEngine.Mode.WORD
        engine.type("a😀")
        engine.backspace()
        assertEquals("a", engine.pendingText)
    }

    @Test
    fun hiddenModeRevealsNothingUntilSend() {
        engine.mode = TypingEngine.Mode.HIDDEN
        engine.type("meet at 9, ok?")
        engine.enter()
        engine.backspace() // removes the newline
        engine.backspace() // removes '?'
        assertEquals(emptyList<String>(), log)
        assertEquals("meet at 9, ok", engine.pendingText)
        engine.send()
        assertEquals(listOf("commit:meet at 9, ok", "submit"), log)
    }

    @Test
    fun hiddenModeBackspaceNeverDeletesAppText() {
        engine.mode = TypingEngine.Mode.HIDDEN
        engine.backspace()
        assertEquals(emptyList<String>(), log)
    }

    @Test
    fun leavingHiddenModeDiscardsBuffer() {
        engine.mode = TypingEngine.Mode.HIDDEN
        engine.type("secret")
        engine.mode = TypingEngine.Mode.WORD
        assertEquals(emptyList<String>(), log)
        assertEquals("", engine.pendingText)
    }

    @Test
    fun leavingWordModeKeepsWord() {
        engine.mode = TypingEngine.Mode.WORD
        engine.type("hel")
        engine.mode = TypingEngine.Mode.DIRECT
        assertEquals(listOf("commit:hel"), log)
    }
}
