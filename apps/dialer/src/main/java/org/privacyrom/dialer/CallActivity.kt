package org.privacyrom.dialer

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Incoming / in-call screen. Never shows who is calling by default: incoming calls show only
 * "Incoming call", active calls show a nickname, and the real number needs a "Reveal" tap.
 */
class CallActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var who: TextView
    private lateinit var answer: Button
    private lateinit var decline: Button
    private lateinit var hangUp: Button
    private lateinit var mute: Button
    private lateinit var speaker: Button
    private lateinit var reveal: Button
    private var revealed = false
    // The activity is singleInstance and reused across calls: forget "Reveal" for a new call.
    private var shownCall: Call? = null
    private val listener: () -> Unit = { handler.post { render() } }
    private val ticker = object : Runnable {
        override fun run() {
            render()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.SECURE_WINDOWS) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        status = text(18f, Color.parseColor("#9AA3AD"))
        who = text(30f, Color.WHITE)
        answer = button("Answer", "#2F6F5E") { CallState.call?.answer(VideoProfile.STATE_AUDIO_ONLY) }
        decline = button("Decline", "#8A2E2E") { CallState.call?.reject(false, null) }
        hangUp = button("Hang up", "#8A2E2E") { CallState.call?.disconnect() }
        mute = button("Mute", "#30353C") {
            val s = CallState.service ?: return@button
            s.setMuted(s.callAudioState?.isMuted != true)
        }
        speaker = button("Speaker", "#30353C") {
            val s = CallState.service ?: return@button
            val on = s.callAudioState?.route == CallAudioState.ROUTE_SPEAKER
            s.setAudioRoute(if (on) CallAudioState.ROUTE_WIRED_OR_EARPIECE else CallAudioState.ROUTE_SPEAKER)
        }
        reveal = button("Reveal", "#30353C") {
            revealed = !revealed
            render()
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.parseColor("#0B0D10"))
            fitsSystemWindows = true
            setPadding(dp(24), dp(96), dp(24), dp(48))
            addView(status)
            addView(who, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(16) })
            addView(reveal, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(24) })
            addView(android.view.View(context), LinearLayout.LayoutParams(1, 0, 1f))
            addView(row(mute, speaker))
            addView(row(answer, decline))
            addView(row(hangUp))
        }
        setContentView(root)
    }

    override fun onStart() {
        super.onStart()
        CallState.listeners += listener
        handler.post(ticker)
    }

    override fun onStop() {
        CallState.listeners -= listener
        handler.removeCallbacks(ticker)
        super.onStop()
    }

    private fun render() {
        val call = CallState.call
        if (call == null) {
            finishAndRemoveTask() // never leave a call screen around in recents
            return
        }
        if (call != shownCall) {
            shownCall = call
            revealed = false
        }
        val state = call.details.state
        val ringing = state == Call.STATE_RINGING
        val active = state == Call.STATE_ACTIVE
        status.text = when (state) {
            Call.STATE_RINGING -> "Incoming call"
            Call.STATE_DIALING, Call.STATE_CONNECTING -> "Calling…"
            Call.STATE_ACTIVE -> "In call  " + duration(call)
            Call.STATE_HOLDING -> "On hold"
            Call.STATE_DISCONNECTED -> "Call ended"
            else -> "Call"
        }
        val number = CallState.number(call)
        who.text = when {
            revealed -> number ?: "Hidden number"
            ringing -> "" // nothing at all until the user acts
            else -> nicknames().of(number)
        }
        reveal.text = if (revealed) "Hide" else "Reveal"
        answer.visibility = if (ringing) android.view.View.VISIBLE else android.view.View.GONE
        decline.visibility = answer.visibility
        hangUp.visibility = if (ringing) android.view.View.GONE else android.view.View.VISIBLE
        val audio = CallState.service?.callAudioState
        mute.text = if (audio?.isMuted == true) "Unmute" else "Mute"
        speaker.text = if (audio?.route == CallAudioState.ROUTE_SPEAKER) "Earpiece" else "Speaker"
        mute.isEnabled = active
        // Hide the toggle when there is nothing to switch to (e.g. speaker-only devices/emulator).
        val routes = audio?.supportedRouteMask ?: 0
        val canSwitch = routes and CallAudioState.ROUTE_SPEAKER != 0 &&
            routes and CallAudioState.ROUTE_WIRED_OR_EARPIECE != 0
        speaker.visibility = if (canSwitch) android.view.View.VISIBLE else android.view.View.GONE
        speaker.isEnabled = !ringing
    }

    private fun duration(call: Call): String {
        val start = call.details.connectTimeMillis
        if (start <= 0) return ""
        val s = (System.currentTimeMillis() - start) / 1000
        return "%d:%02d".format(s / 60, s % 60)
    }

    private fun text(sizeSp: Float, color: Int) = TextView(this).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(color)
        gravity = Gravity.CENTER
    }

    private fun button(label: String, color: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor(color))
        setOnClickListener { onClick() }
    }

    private fun row(vararg buttons: Button) = LinearLayout(this).apply {
        for (b in buttons) {
            addView(b, LinearLayout.LayoutParams(0, dp(64), 1f).apply { setMargins(dp(8), dp(8), dp(8), dp(8)) })
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
