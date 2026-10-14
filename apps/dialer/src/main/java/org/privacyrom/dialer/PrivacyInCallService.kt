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
        override fun onStateChanged(call: Call, state: Int) {
            // Once answered/dialing, swap the "Incoming call" heads-up for a quiet ongoing one.
            if (state != Call.STATE_RINGING && state != Call.STATE_DISCONNECTED) postNotification(call)
            CallState.notifyChanged()
        }
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
        val nm = getSystemService(NotificationManager::class.java)
        nm.cancel(INCOMING_ID)
        nm.cancel(ONGOING_ID)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) = CallState.notifyChanged()

    override fun onDestroy() {
        CallState.service = null
        super.onDestroy()
    }

    private fun showCallScreen(call: Call) {
        postNotification(call)
        startActivity(callIntent())
    }

    /**
     * Ringing calls get a full-screen, high-priority notification (needed to show over the lock
     * screen); other calls a quiet ongoing one. Neither shows a name, number or nickname.
     */
    private fun postNotification(call: Call) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_INCOMING, "Incoming calls", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null) // Telecom plays the ringtone (IN_CALL_SERVICE_RINGING=false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ONGOING, "Ongoing call", NotificationManager.IMPORTANCE_LOW).apply {
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
        )
        val pending = PendingIntent.getActivity(this, 0, callIntent(), PendingIntent.FLAG_IMMUTABLE)
        val ringing = call.details.state == Call.STATE_RINGING
        val builder = Notification.Builder(this, if (ringing) CHANNEL_INCOMING else CHANNEL_ONGOING)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(if (ringing) "Incoming call" else "Call")
            .setCategory(Notification.CATEGORY_CALL)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .setOngoing(true)
            .setContentIntent(pending)
        if (ringing) {
            nm.notify(INCOMING_ID, builder.setFullScreenIntent(pending, true).build())
        } else {
            nm.cancel(INCOMING_ID) // removes the heads-up immediately
            nm.notify(ONGOING_ID, builder.build())
        }
    }

    private fun callIntent() = Intent(this, CallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private companion object {
        const val CHANNEL_INCOMING = "calls"
        const val CHANNEL_ONGOING = "ongoing_call"
        const val INCOMING_ID = 1
        const val ONGOING_ID = 2
    }
}
