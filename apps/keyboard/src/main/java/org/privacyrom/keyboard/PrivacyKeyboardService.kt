package org.privacyrom.keyboard

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.EditorInfo

class PrivacyKeyboardService : InputMethodService(), TypingEngine.Sink, KeyboardUi.Listener {

    private lateinit var prefs: Prefs
    private val engine = TypingEngine(this)
    private var ui: KeyboardUi? = null
    private var note: String? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        // Keep the keyboard (and the hidden-input curtain) out of screenshots and screen recordings.
        if (BuildConfig.SECURE_WINDOWS) window.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onCreateInputView(): View = KeyboardUi(this, this).also { ui = it }.view

    /** The stock landscape "extract" view would echo the field's text full-screen; never use it. */
    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (!restarting) {
            engine.discard()
            note = null
            engine.mode = if (prefs.hiddenEveryField && !isNumeric(info)) TypingEngine.Mode.HIDDEN else defaultMode()
        }
        ui?.resetForField(isNumeric(info), shouldCapitalize())
        applyCurtain()
        refresh()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        // A hidden buffer never leaks into the app by accident; a half-typed word is kept.
        if (engine.mode == TypingEngine.Mode.HIDDEN) engine.discard() else engine.flush()
        ui?.setCurtain(false, 0)
        super.onFinishInputView(finishingInput)
    }

    // --- TypingEngine.Sink ---------------------------------------------------------------

    override fun commit(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    override fun deleteBefore() {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
    }

    override fun enter() {
        val info = currentInputEditorInfo ?: return
        val multiLine = info.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0
        when {
            editorAction(info) != null -> currentInputConnection?.performEditorAction(editorAction(info)!!)
            multiLine -> commit("\n")
            else -> sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
    }

    override fun submit() {
        val action = currentInputEditorInfo?.let(::editorAction)
        if (action != null) {
            currentInputConnection?.performEditorAction(action)
        } else {
            // Typical of chat apps: multi-line field + separate send button. Without a ROM-level
            // helper we cannot press that button, so tell the user instead of adding a newline.
            note = "Text inserted. This app has its own send button: tap SHOW APP to press it."
        }
    }

    // --- KeyboardUi.Listener -------------------------------------------------------------

    override fun onText(text: String) {
        note = null
        engine.type(text)
        refresh()
    }

    override fun onBackspace() {
        engine.backspace()
        refresh()
    }

    override fun onEnter() {
        engine.enter()
        refresh()
    }

    override fun onToggleWordMode() {
        engine.mode = if (engine.mode == TypingEngine.Mode.WORD) TypingEngine.Mode.DIRECT else TypingEngine.Mode.WORD
        prefs.wordMode = engine.mode == TypingEngine.Mode.WORD
        refresh()
    }

    override fun onToggleHidden() {
        engine.mode = if (engine.mode == TypingEngine.Mode.HIDDEN) defaultMode() else TypingEngine.Mode.HIDDEN
        note = null
        applyCurtain()
        refresh()
    }

    override fun onSend() {
        if (engine.pendingLength == 0) return
        note = null
        engine.send()
        refresh()
        if (prefs.leaveAfterSend && note == null) leaveApp()
    }

    override fun onInsert() {
        engine.insert()
        note = null
        refresh()
    }

    override fun onCancel() {
        engine.discard()
        engine.mode = defaultMode()
        note = null
        applyCurtain()
        refresh()
    }

    override fun onLeaveAfterSendChanged(leave: Boolean) {
        prefs.leaveAfterSend = leave
    }

    // --- helpers -------------------------------------------------------------------------

    private fun defaultMode() = if (prefs.wordMode) TypingEngine.Mode.WORD else TypingEngine.Mode.DIRECT

    private fun refresh() {
        val ui = ui ?: return
        ui.render(engine.mode, engine.pendingText, engine.pendingLength, prefs.leaveAfterSend, note)
        ui.setAutoCapitalize(shouldCapitalize())
    }

    /** Covers the whole screen above the keys with an opaque curtain while in hidden mode. */
    private fun applyCurtain() {
        val ui = ui ?: return
        if (engine.mode != TypingEngine.Mode.HIDDEN) {
            ui.setCurtain(false, 0)
            return
        }
        val metrics = getSystemService(WindowManager::class.java).currentWindowMetrics
        val bars = metrics.windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
        )
        // The keyboard view already pads itself above the navigation bar, so only the top bars count.
        val available = metrics.bounds.height() - bars.top
        ui.view.post { ui.setCurtain(true, (available - ui.keyboardHeight()).coerceAtLeast(0)) }
    }

    private fun shouldCapitalize(): Boolean {
        val info = currentInputEditorInfo ?: return false
        if (engine.mode == TypingEngine.Mode.HIDDEN) {
            if (info.inputType and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES == 0) return false
            val t = engine.pendingText
            if (t.isNotEmpty()) {
                return t.last() == '\n' || (t.last() == ' ' && t.trimEnd().lastOrNull() in SENTENCE_END)
            }
            // Empty buffer: decide from the text already in the field, like the other modes.
        }
        if (engine.pendingLength > 0) return false
        val caps = currentInputConnection?.getCursorCapsMode(info.inputType) ?: 0
        return caps != 0
    }

    private fun leaveApp() {
        requestHideSelf(0)
        startActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun editorAction(info: EditorInfo): Int? {
        if (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) return null
        val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
        return action.takeUnless { it == EditorInfo.IME_ACTION_NONE || it == EditorInfo.IME_ACTION_UNSPECIFIED }
    }

    private fun isNumeric(info: EditorInfo): Boolean = when (info.inputType and InputType.TYPE_MASK_CLASS) {
        InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE, InputType.TYPE_CLASS_DATETIME -> true
        else -> false
    }

    private companion object {
        val SENTENCE_END = setOf('.', '!', '?')
    }
}
