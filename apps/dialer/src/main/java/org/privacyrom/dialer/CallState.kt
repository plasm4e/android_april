package org.privacyrom.dialer

import android.content.Context
import android.telecom.Call
import android.telecom.InCallService
import android.util.Base64
import java.security.SecureRandom

/** The single active call shared between [PrivacyInCallService] and [CallActivity]. */
object CallState {
    var service: InCallService? = null
    var call: Call? = null
        set(value) {
            field = value
            listeners.forEach { it() }
        }
    val listeners = mutableSetOf<() -> Unit>()

    fun notifyChanged() = listeners.forEach { it() }

    fun number(call: Call?): String? = call?.details?.handle?.schemeSpecificPart
}

/** Per-device random secret for [Nicknames]; created on first use, never leaves the device. */
fun Context.nicknames(): Nicknames {
    val prefs = getSharedPreferences("secret", Context.MODE_PRIVATE)
    val stored = prefs.getString("nickname_key", null)
    val key = if (stored != null) {
        Base64.decode(stored, Base64.NO_WRAP)
    } else {
        ByteArray(32).also {
            SecureRandom().nextBytes(it)
            prefs.edit().putString("nickname_key", Base64.encodeToString(it, Base64.NO_WRAP)).apply()
        }
    }
    return Nicknames(key)
}
