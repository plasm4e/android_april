package org.privacyrom.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Programmatic keyboard UI: a "curtain" (shown only in hidden mode), a status strip and the keys.
 * Deliberately has no key preview popups: they would display each typed letter on screen.
 */
@SuppressLint("ClickableViewAccessibility", "SetTextI18n")
class KeyboardUi(private val context: Context, private val listener: Listener) {

    interface Listener {
        fun onText(text: String)
        fun onBackspace()
        fun onEnter()
        fun onToggleWordMode()
        fun onToggleHidden()
        fun onSend()
        fun onInsert()
        fun onCancel()
        fun onLeaveAfterSendChanged(leave: Boolean)
    }

    private enum class Shift { OFF, ONCE, LOCK }

    private var shift = Shift.OFF
    private var symbols = false
    private val handler = Handler(Looper.getMainLooper())
    // Letter keys and their lowercase label, relabelled in place on shift changes: rebuilding the
    // views instead would drop taps that land mid-rebuild.
    private val letterKeys = mutableListOf<Pair<TextView, String>>()
    private var shiftView: TextView? = null

    val view: LinearLayout = vertical().apply { setBackgroundColor(BG) }

    private val curtainStatus = label(18f, Color.WHITE)
    private val curtainPeek = label(16f, PEEK)
    private val leaveCheck = CheckBox(context).apply {
        text = "Leave the app after sending"
        setTextColor(MUTED)
        setOnCheckedChangeListener { _, checked -> listener.onLeaveAfterSendChanged(checked) }
    }
    private val curtain: LinearLayout = buildCurtain()

    private val modeButton = stripButton("") { listener.onToggleWordMode() }
    private val pendingView = label(16f, MUTED).apply { setSingleLine() }
    private val hideButton = stripButton("") { listener.onToggleHidden() }
    private val keys = vertical()

    init {
        val strip = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(4), dp(6), dp(2))
            addView(modeButton)
            addView(pendingView, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                marginStart = dp(10)
            })
            addView(hideButton)
        }
        view.addView(curtain, LinearLayout.LayoutParams(MATCH_PARENT, 0))
        view.addView(strip, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        view.addView(keys, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        keys.setPadding(dp(3), 0, dp(3), dp(4))
        // Android 15 lays the IME window out edge-to-edge: keep the keys above the navigation bar.
        view.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(0, 0, 0, insets.getInsets(WindowInsets.Type.navigationBars()).bottom)
            insets
        }
        rebuildKeys()
    }

    /** Height of everything except the curtain, used to size the curtain to fill the screen. */
    fun keyboardHeight(): Int = view.height - curtain.height

    fun setCurtain(visible: Boolean, heightPx: Int) {
        curtain.visibility = if (visible) View.VISIBLE else View.GONE
        curtain.layoutParams = (curtain.layoutParams as LinearLayout.LayoutParams).apply {
            height = if (visible) heightPx else 0
        }
    }

    fun render(
        mode: TypingEngine.Mode,
        pending: String,
        pendingLength: Int,
        leaveAfterSend: Boolean,
        note: String?,
    ) {
        val hidden = mode == TypingEngine.Mode.HIDDEN
        modeButton.text = if (mode == TypingEngine.Mode.DIRECT) "LIVE" else "WORD"
        modeButton.isEnabled = !hidden
        modeButton.alpha = if (hidden) 0.35f else 1f
        hideButton.text = if (hidden) "SHOW APP" else "HIDE"
        // Hidden mode never shows the buffer on the strip; word mode shows the word being built.
        pendingView.text = if (hidden) "" else pending
        curtainStatus.text = if (note != null) note
        else if (pendingLength == 0) "Type privately. The app sees nothing until you send."
        else "•".repeat(minOf(pendingLength, 20)) + "   $pendingLength chars"
        curtainPeek.tag = pending
        if (leaveCheck.isChecked != leaveAfterSend) leaveCheck.isChecked = leaveAfterSend
    }

    fun resetForField(numeric: Boolean, capitalize: Boolean) {
        symbols = numeric
        shift = if (capitalize) Shift.ONCE else Shift.OFF
        rebuildKeys()
    }

    fun setAutoCapitalize(capitalize: Boolean) {
        if (shift == Shift.LOCK || symbols) return
        val want = if (capitalize) Shift.ONCE else Shift.OFF
        if (want != shift) {
            shift = want
            relabel()
        }
    }

    private fun buildCurtain(): LinearLayout = vertical().apply {
        setBackgroundColor(CURTAIN)
        gravity = Gravity.CENTER
        setPadding(dp(24), dp(24), dp(24), dp(24))
        visibility = View.GONE
        addView(label(13f, MUTED).apply { text = "HIDDEN INPUT" })
        addView(curtainStatus, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            topMargin = dp(12)
        })
        curtainPeek.visibility = View.INVISIBLE
        addView(curtainPeek, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = dp(12)
        })
        val peek = stripButton("HOLD TO PEEK") {}
        peek.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    curtainPeek.text = curtainPeek.tag as? String ?: ""
                    curtainPeek.visibility = View.VISIBLE
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    curtainPeek.text = ""
                    curtainPeek.visibility = View.INVISIBLE
                }
            }
            true
        }
        addView(peek, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(8) })
        val actions = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            addView(actionButton("Cancel", false) { listener.onCancel() })
            addView(actionButton("Insert", false) { listener.onInsert() })
            addView(actionButton("Send", true) { listener.onSend() })
        }
        addView(actions, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(20) })
        addView(leaveCheck, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(8) })
    }

    private fun relabel() {
        val caps = shift != Shift.OFF
        for ((view, c) in letterKeys) view.text = if (caps) c.uppercase() else c
        shiftView?.text = when (shift) { Shift.OFF -> "⇧"; Shift.ONCE -> "⬆"; Shift.LOCK -> "⇪" }
    }

    private fun rebuildKeys() {
        keys.removeAllViews()
        letterKeys.clear()
        shiftView = null
        val rows = if (symbols) SYMBOL_ROWS else LETTER_ROWS
        rows.forEachIndexed { i, row ->
            val line = LinearLayout(context)
            if (i == 1 && !symbols) line.setPadding(dp(16), 0, dp(16), 0) // stagger middle row
            if (i == 2) line.addView(if (symbols) spacer(1.5f) else shiftKey())
            for (k in row) line.addView(charKey(k, letter = !symbols))
            if (i == 2) line.addView(backspaceKey())
            keys.addView(line, LinearLayout.LayoutParams(MATCH_PARENT, dp(KEY_HEIGHT_DP)))
        }
        val bottom = LinearLayout(context).apply {
            addView(specialKey(if (symbols) "ABC" else "?123", 1.5f) {
                symbols = !symbols
                rebuildKeys()
            })
            addView(charKey(","))
            addView(key(" ", 5f, false).apply {
                setOnClickListener { listener.onText(" ") }
            })
            addView(charKey("."))
            addView(specialKey("⏎", 1.5f) { listener.onEnter() })
        }
        keys.addView(bottom, LinearLayout.LayoutParams(MATCH_PARENT, dp(KEY_HEIGHT_DP)))
        relabel()
    }

    private fun charKey(c: String, letter: Boolean = false): TextView = key(c, 1f, false).apply {
        if (letter) letterKeys += this to c
        setOnClickListener {
            // Read the shift state at tap time, not when the key was created.
            listener.onText(if (letter && shift != Shift.OFF) c.uppercase() else c)
            if (letter && shift == Shift.ONCE) {
                shift = Shift.OFF
                relabel()
            }
        }
    }

    private fun shiftKey(): TextView = specialKey("", 1.5f) {
        shift = when (shift) { Shift.OFF -> Shift.ONCE; Shift.ONCE -> Shift.LOCK; Shift.LOCK -> Shift.OFF }
        relabel()
    }.also { shiftView = it }

    private fun backspaceKey(): TextView = key("⌫", 1.5f, true).apply {
        val repeat = object : Runnable {
            override fun run() {
                listener.onBackspace()
                handler.postDelayed(this, 60)
            }
        }
        setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    listener.onBackspace()
                    handler.postDelayed(repeat, 400)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    handler.removeCallbacks(repeat)
                }
            }
            true
        }
    }

    private fun specialKey(text: String, weight: Float, onClick: () -> Unit) =
        key(text, weight, true).apply { setOnClickListener { onClick() } }

    private fun key(text: String, weight: Float, special: Boolean) = TextView(context).apply {
        this.text = text
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, if (text.length > 1) 15f else 21f)
        setBackgroundResource(if (special) R.drawable.key_special_bg else R.drawable.key_bg)
        isSoundEffectsEnabled = false
        layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, weight).apply {
            setMargins(dp(3), dp(4), dp(3), dp(4))
        }
    }

    private fun spacer(weight: Float) = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, weight)
    }

    private fun stripButton(text: String, onClick: () -> Unit) = TextView(context).apply {
        this.text = text
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setPadding(dp(12), dp(6), dp(12), dp(6))
        setBackgroundResource(R.drawable.key_special_bg)
        isSoundEffectsEnabled = false
        setOnClickListener { onClick() }
    }

    private fun actionButton(text: String, primary: Boolean, onClick: () -> Unit) = TextView(context).apply {
        this.text = text
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setPadding(dp(20), dp(12), dp(20), dp(12))
        setBackgroundResource(if (primary) R.drawable.key_primary_bg else R.drawable.key_special_bg)
        isSoundEffectsEnabled = false
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { setMargins(dp(6), 0, dp(6), 0) }
    }

    private fun label(sizeSp: Float, color: Int) = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(color)
        gravity = Gravity.CENTER
    }

    private fun vertical() = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    private fun dp(v: Int): Int = (v * context.resources.displayMetrics.density).toInt()

    private companion object {
        const val KEY_HEIGHT_DP = 54
        val BG = Color.parseColor("#15181C")
        val CURTAIN = Color.parseColor("#0B0D10")
        val MUTED = Color.parseColor("#9AA3AD")
        val PEEK = Color.parseColor("#E8C468")

        val LETTER_ROWS = listOf(
            "qwertyuiop".map { it.toString() },
            "asdfghjkl".map { it.toString() },
            "zxcvbnm".map { it.toString() },
        )
        val SYMBOL_ROWS = listOf(
            "1234567890".map { it.toString() },
            listOf("@", "#", "€", "%", "&", "-", "+", "(", ")", "/"),
            listOf("*", "\"", "'", ":", ";", "!", "?"),
        )
    }
}
