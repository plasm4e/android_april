package org.privacyrom.dialer

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.TelecomManager
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Dial pad. Typed digits are masked one second after each key press. */
class DialerActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private val digits = StringBuilder()
    private var showLast = false
    private lateinit var numberView: TextView
    private lateinit var roleBanner: Button
    private val maskLast = Runnable {
        showLast = false
        render()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.SECURE_WINDOWS) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        numberView = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, dp(24))
        }
        roleBanner = Button(this).apply {
            text = "Make this the default phone app"
            setOnClickListener { requestDialerRole() }
        }
        val pad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (row in listOf("123", "456", "789", "*0#")) {
            val line = LinearLayout(this)
            for (c in row) line.addView(key(c.toString()) { press(c) }, weight())
            pad.addView(line)
        }
        val actions = LinearLayout(this).apply {
            addView(key("Recents") { startActivity(Intent(this@DialerActivity, RecentsActivity::class.java)) }, weight())
            addView(key("Call", "#2F6F5E") { placeCall() }, weight())
            addView(key("⌫") { backspace() }, weight())
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B0D10"))
            fitsSystemWindows = true
            setPadding(dp(16), dp(16), dp(16), dp(16))
            addView(roleBanner)
            addView(numberView, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(View(context), LinearLayout.LayoutParams(1, 0, 1f))
            addView(pad)
            addView(actions)
        })
        handleDialIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDialIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        val isDefault = getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_DIALER)
        roleBanner.visibility = if (isDefault) View.GONE else View.VISIBLE
        render()
    }

    /** tel: links from other apps pre-fill the pad (masked) instead of calling directly. */
    private fun handleDialIntent(intent: Intent?) {
        val number = intent?.data?.takeIf { it.scheme == "tel" }?.schemeSpecificPart ?: return
        digits.setLength(0)
        digits.append(number.filter { it.isDigit() || it == '+' || it == '*' || it == '#' })
        showLast = false
        render()
    }

    private fun press(c: Char) {
        digits.append(c)
        showLast = true
        handler.removeCallbacks(maskLast)
        handler.postDelayed(maskLast, 1000)
        render()
    }

    private fun backspace() {
        if (digits.isNotEmpty()) digits.setLength(digits.length - 1)
        showLast = false
        render()
    }

    private fun render() {
        numberView.text = when {
            digits.isEmpty() -> " "
            showLast -> "•".repeat(digits.length - 1) + digits.last()
            else -> "•".repeat(digits.length)
        }
    }

    private fun placeCall() {
        if (digits.isEmpty()) return
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 1)
            return
        }
        getSystemService(TelecomManager::class.java).placeCall(Uri.fromParts("tel", digits.toString(), null), null)
        digits.setLength(0)
        render()
    }

    private fun requestDialerRole() {
        val roles = getSystemService(RoleManager::class.java)
        @Suppress("DEPRECATION") // startActivityForResult keeps us free of AndroidX.
        startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_DIALER), 2)
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 3)
        }
    }

    private fun key(label: String, color: String = "#23272D", onClick: () -> Unit) = Button(this).apply {
        text = label
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, if (label.length == 1) 24f else 15f)
        setBackgroundColor(Color.parseColor(color))
        isSoundEffectsEnabled = false
        setOnClickListener { onClick() }
    }

    private fun weight() = LinearLayout.LayoutParams(0, dp(68), 1f).apply { setMargins(dp(4), dp(4), dp(4), dp(4)) }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
