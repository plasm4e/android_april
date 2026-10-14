package org.privacyrom.keyboard

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

/** Setup + settings screen, with a test field that shows exactly what an app receives. */
class SetupActivity : Activity() {

    private lateinit var prefs: Prefs
    private val received = ArrayDeque<String>()
    private lateinit var receivedView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.SECURE_WINDOWS) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        prefs = Prefs(this)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        col.addView(text("Privacy Keyboard", 24f))
        col.addView(text(
            "No internet access, no learning, no key pop-ups, hidden from screenshots.\n\n" +
                "WORD mode: apps receive whole words only, so search boxes cannot react to every letter.\n" +
                "HIDE: covers the app while you type; nothing reaches it until you press Send.", 14f
        ))
        col.addView(button("1. Enable the keyboard") { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) })
        col.addView(button("2. Switch to it") { getSystemService(InputMethodManager::class.java).showInputMethodPicker() })

        col.addView(switch("Whole-word mode by default", prefs.wordMode) { prefs.wordMode = it })
        col.addView(switch("Start every field hidden", prefs.hiddenEveryField) { prefs.hiddenEveryField = it })
        col.addView(switch("Go to home screen after hidden Send", prefs.leaveAfterSend) { prefs.leaveAfterSend = it })

        col.addView(text("Try it: type a search below", 16f).apply { setPadding(0, dp(24), 0, dp(4)) })
        val field = EditText(this).apply {
            hint = "Search…"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setOnEditorActionListener { _, _, _ -> log("[search pressed]"); true }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable) = log("\"$s\"")
            })
        }
        // Log goes above the field so the keyboard does not cover it.
        receivedView = text("What the app received:", 13f).apply { setTextColor(Color.GRAY) }
        col.addView(receivedView)
        col.addView(field)

        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            addView(col)
        })
    }

    private fun log(event: String) {
        received.addLast(event)
        while (received.size > 6) received.removeFirst()
        receivedView.text = "What the app received (${received.size} latest):\n" + received.joinToString("\n")
    }

    private fun text(s: String, sizeSp: Float) = TextView(this).apply {
        text = s
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setPadding(0, dp(6), 0, dp(6))
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { onClick() }
    }

    @Suppress("DEPRECATION") // android.widget.Switch keeps us free of AndroidX.
    private fun switch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) = Switch(this).apply {
        text = label
        isChecked = checked
        setPadding(0, dp(10), 0, dp(10))
        setOnCheckedChangeListener { _, v -> onChange(v) }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
