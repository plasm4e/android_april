package org.privacyrom.keyboard

/**
 * Decides when typed text reaches the target app. Pure Kotlin so it can be unit-tested.
 *
 * - [Mode.DIRECT]: every key is committed immediately, like a normal keyboard.
 * - [Mode.WORD]: characters are held in a private buffer and only committed when a word
 *   boundary is typed, so the app's search/autocomplete never sees partial words.
 * - [Mode.HIDDEN]: everything is held until [send] or [insert]; the app sees nothing
 *   while the user types, and the UI covers the app with an opaque curtain.
 */
class TypingEngine(private val sink: Sink) {

    interface Sink {
        fun commit(text: String)
        fun deleteBefore()
        /** Enter key: the field's action ("Search", "Go"...) or a newline. */
        fun enter()
        /** Fires the field's action (e.g. "Send") if it has one; never inserts a newline. */
        fun submit()
    }

    enum class Mode { DIRECT, WORD, HIDDEN }

    var mode: Mode = Mode.WORD
        set(value) {
            if (field == value) return
            // Leaving HIDDEN never leaks the buffer into the app; leaving WORD keeps the word.
            if (field == Mode.HIDDEN) discard() else flush()
            field = value
        }

    private val pending = StringBuilder()

    val pendingText: String get() = pending.toString()
    val pendingLength: Int get() = pending.codePointCount(0, pending.length)

    fun type(text: String) {
        for (c in text) typeChar(c)
    }

    private fun typeChar(c: Char) {
        when (mode) {
            Mode.DIRECT -> sink.commit(c.toString())
            Mode.HIDDEN -> pending.append(c)
            Mode.WORD -> if (isWordChar(c)) {
                pending.append(c)
            } else {
                flush()
                sink.commit(c.toString())
            }
        }
    }

    fun backspace() {
        if (pending.isNotEmpty()) {
            val cut = if (pending.length >= 2 && pending.last().isLowSurrogate()) 2 else 1
            pending.setLength(pending.length - cut)
        } else if (mode != Mode.HIDDEN) {
            sink.deleteBefore()
        }
    }

    fun enter() {
        if (mode == Mode.HIDDEN) {
            // In hidden mode Enter is a newline inside the private buffer; sending is explicit.
            pending.append('\n')
            return
        }
        flush()
        sink.enter()
    }

    /** Commits the hidden buffer and fires the field's action (e.g. Send). */
    fun send() {
        flush()
        sink.submit()
    }

    /** Commits the hidden buffer without triggering any action. */
    fun insert() = flush()

    fun flush() {
        if (pending.isEmpty()) return
        val text = pending.toString()
        pending.setLength(0)
        sink.commit(text)
    }

    fun discard() {
        pending.setLength(0)
    }

    companion object {
        /** Characters that continue a word; anything else (space, punctuation) ends it. */
        fun isWordChar(c: Char): Boolean =
            c.isLetterOrDigit() || c == '\'' || c == '-' || c == '_' || c == '@' ||
                c.isSurrogate()
    }
}
