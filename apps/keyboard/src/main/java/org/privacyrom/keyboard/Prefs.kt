package org.privacyrom.keyboard

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Hold each word until it is complete (default) instead of sending every letter. */
    var wordMode: Boolean
        get() = sp.getBoolean(KEY_WORD_MODE, true)
        set(v) = sp.edit().putBoolean(KEY_WORD_MODE, v).apply()

    /** Open every text field directly in hidden mode. */
    var hiddenEveryField: Boolean
        get() = sp.getBoolean(KEY_HIDDEN_EVERY_FIELD, false)
        set(v) = sp.edit().putBoolean(KEY_HIDDEN_EVERY_FIELD, v).apply()

    /** After a hidden send, go to the home screen so the app is not left on display. */
    var leaveAfterSend: Boolean
        get() = sp.getBoolean(KEY_LEAVE_AFTER_SEND, false)
        set(v) = sp.edit().putBoolean(KEY_LEAVE_AFTER_SEND, v).apply()

    private companion object {
        const val KEY_WORD_MODE = "word_mode"
        const val KEY_HIDDEN_EVERY_FIELD = "hidden_every_field"
        const val KEY_LEAVE_AFTER_SEND = "leave_after_send"
    }
}
