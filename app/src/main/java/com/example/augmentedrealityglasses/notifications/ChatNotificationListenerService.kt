package com.example.augmentedrealityglasses.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.augmentedrealityglasses.App
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceManager
import com.example.augmentedrealityglasses.cache.DefaultTimeProvider
import com.example.augmentedrealityglasses.settings.NotificationSource
import com.example.augmentedrealityglasses.settings.SettingsUIState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class ChatNotificationListenerService : NotificationListenerService() {

    private val TAG = "ChatNotificationService"

    private val scope = CoroutineScope(Dispatchers.IO)

    private val supportedPackages = setOf(
        "com.whatsapp",            // WhatsApp
        "com.whatsapp.w4b",        // WhatsApp Business
        "org.telegram.messenger",  // Telegram
    )

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (pkg !in supportedPackages) return

        //do not disturb mode check
        if (isDNDActiveCompat()) {
            Log.d(TAG, "DND on: discard notification from $pkg")
            return
        }

        val notification = sbn.notification ?: return
        val parsed = parseChatNotification(notification) ?: return

        val appName = when (pkg) {
            "com.whatsapp", "com.whatsapp.w4b" -> NotificationSource.WHATSAPP
            else -> NotificationSource.TELEGRAM
        }

        scope.launch {
            if (!isNotificationsEnabledFor(appName)) return@launch

            val jsonToSend = JSONObject()

            jsonToSend.put("command", "n")
            jsonToSend.put("app", appName)
            //jsonToSend.put("conv", parsed.conversation)
            jsonToSend.put("sender", parsed.sender)
            jsonToSend.put("body", parsed.text)

            val payload = jsonToSend.toString()

            val proxy: RemoteDeviceManager =
                (applicationContext as App).container.proxy

            if (proxy.isConnected()) {
                proxy.send(payload)
                Log.d(TAG, "Sending to device: $payload")
            } else {
                Log.d(TAG, "Device offline. Notification: $payload")
            }
        }
    }

    private suspend fun isNotificationsEnabledFor(app: NotificationSource): Boolean {
        val container = (applicationContext as App).container
        val cache = container.settingsCache
        val policy = container.settingsCachePolicy

        val state = cache.getIfValid(
            key = "settings",
            policy = policy,
            serializer = SettingsUIState.serializer(),
            timeProvider = DefaultTimeProvider
        )

        return state?.notificationEnabled?.get(app) ?: false
    }

    private fun parseChatNotification(notification: Notification): ChatMessage? {
        val extras = notification.extras

        val style =
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)

        if (style != null) {
            val messages = style.messages
            if (!messages.isNullOrEmpty()) {
                val last = messages.last()
                val text = last.text?.toString()?.trim().orEmpty()
                val sender = last.person?.name?.toString() ?: last.sender?.toString()
                //val conv = style.conversationTitle?.toString()
                if (text.isNotEmpty()) return ChatMessage(sender, text)
            }
        }

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text =
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: extras.getCharSequence(
                Notification.EXTRA_BIG_TEXT
            )?.toString()

        val cleanText = text?.trim().orEmpty()
        if (cleanText.isNotEmpty()) {
            //val conv = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            return ChatMessage(sender = title, text = cleanText)
        }

        return null
    }

    private fun isDNDActiveCompat(): Boolean {
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        val filter = nm.currentInterruptionFilter
        return filter == android.app.NotificationManager.INTERRUPTION_FILTER_NONE
                || filter == android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS
                || filter == android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY
    }

    data class ChatMessage(
        val sender: String?,
        val text: String
    )
}