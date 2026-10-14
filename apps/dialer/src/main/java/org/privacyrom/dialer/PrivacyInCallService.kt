package org.privacyrom.dialer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService

/** Receives calls from the system once this app is the default phone app. */
class PrivacyInCallService : InCallService() {

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) = CallState.notifyChanged()
    }

    override fun onCallAdded(call: Call) {
        CallState.service = this
        CallState.call = call
        call.registerCallback(callback)
        showCallScreen(call)
    }

    override fun onCallRemoved(call: Call) {
        call.unregisterCallback(callback)
        if (CallState.call == call) CallState.call = null
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) = CallState.notifyChanged()

    override fun onDestroy() {
        CallState.service = null
        super.onDestroy()
    }

    /**
     * Incoming calls need a full-screen notification to show over the lock screen. The
     * notification itself says only "Call": no name, number or nickname.
     */
    private fun showCallScreen(call: Call) {
        val intent = Intent(this, CallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Calls", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null) // Telecom plays the ringtone (IN_CALL_SERVICE_RINGING=false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
        )
        val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val incoming = call.details.state == Call.STATE_RINGING
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(if (incoming) "Incoming call" else "Call")
            .setCategory(Notification.CATEGORY_CALL)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .setOngoing(true)
            .setContentIntent(pending)
            .setFullScreenIntent(pending, true)
            .build()
        nm.notify(NOTIFICATION_ID, notification)
        startActivity(intent)
    }

    private companion object {
        const val CHANNEL = "calls"
        const val NOTIFICATION_ID = 1
    }
}
