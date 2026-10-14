package org.privacyrom.dialer

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.telecom.TelecomManager
import android.text.format.DateUtils
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.ListView

/** Call history showing only nickname, direction and time; never the number. */
class RecentsActivity : Activity() {

    private data class Entry(val number: String?, val label: String)

    private val entries = mutableListOf<Entry>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.SECURE_WINDOWS) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        setContentView(ListView(this).apply {
            setBackgroundColor(Color.parseColor("#0B0D10"))
            fitsSystemWindows = true
            this.adapter = this@RecentsActivity.adapter
            // Calling back needs a long press, so a stray tap never dials.
            setOnItemLongClickListener { _, _, pos, _ ->
                entries[pos].number?.let { callBack(it) }
                true
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CALL_LOG), 1)
            return
        }
        load()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        if (results.firstOrNull() == PackageManager.PERMISSION_GRANTED) load()
    }

    private fun load() {
        val nicknames = nicknames()
        entries.clear()
        contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE),
            null, null, "${CallLog.Calls.DATE} DESC LIMIT 200",
        )?.use { c ->
            while (c.moveToNext()) {
                val number = c.getString(0)
                val direction = when (c.getInt(1)) {
                    CallLog.Calls.INCOMING_TYPE -> "↙ in"
                    CallLog.Calls.OUTGOING_TYPE -> "↗ out"
                    CallLog.Calls.MISSED_TYPE -> "✕ missed"
                    CallLog.Calls.REJECTED_TYPE, CallLog.Calls.BLOCKED_TYPE -> "⊘ declined"
                    else -> "•"
                }
                val time = DateUtils.getRelativeTimeSpanString(c.getLong(2))
                entries += Entry(number, "${nicknames.of(number)}\n$direction · $time")
            }
        }
        adapter.clear()
        adapter.addAll(entries.map { it.label }.ifEmpty { listOf("No calls yet") })
    }

    private fun callBack(number: String) {
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 2)
            return
        }
        getSystemService(TelecomManager::class.java).placeCall(Uri.fromParts("tel", number, null), null)
    }
}
