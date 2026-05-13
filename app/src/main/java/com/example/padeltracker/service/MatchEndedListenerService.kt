package com.example.padeltracker.service

import android.util.Log
import com.example.padeltracker.shared.WearCommunicationConstants
import com.example.padeltracker.wear.PhoneMatchEndedEventBus
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Receives match-ended messages from the Wear app.
 *
 * This service is not active until it is registered in AndroidManifest.xml.
 */
class MatchEndedListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        Log.d(TAG, "Message received. path=${messageEvent.path}")

        if (messageEvent.path == WearCommunicationConstants.MATCH_ENDED_PATH) {
            val endedAt = messageEvent.data
                ?.toString(Charsets.UTF_8)
                ?.toLongOrNull()
                ?: System.currentTimeMillis()

            Log.d(TAG, "Match ended message received from Wear. endedAt=$endedAt")

            PhoneMatchEndedEventBus.notifyMatchEnded(endedAt)
        } else {
            Log.d(TAG, "Ignored message path: ${messageEvent.path}")
        }
    }

    companion object {
        private const val TAG = "PHONE_MATCH_ENDED"
    }
}
